package org.keycloak.services.resources.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.EnvironmentReader;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.exceptions.CustomDomainException;
import org.keycloak.models.CustomDomainMappingModel;
import org.keycloak.models.GeneralResponse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.DnsRepresentation;
import org.keycloak.representations.idm.CertProcessResponse;
import org.keycloak.representations.idm.CustomDomainCertRepresentation;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.utils.GeneralHelper;
import org.xbill.DNS.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class DnsManagementResource {

    public static final String DNS_NAME = "DNS_NAME";
    public static final String DNS_TXT_RECORD_VALUE = "DNS_TXT_RECORD_VALUE";
    public static final String DNS_TXT_RECORD_VERIFICATION = "DNS_TXT_RECORD_VERIFICATION";
    public static final String DNS_A_RECORD_VERIFICATION = "DNS_A_RECORD_VERIFICATION";
    public static final String DNS_COMPLETED = "DNS_COMPLETED";
    private final RealmModel realm;
    private final KeycloakSession session;
    private AdminPermissionEvaluator auth;
    private AdminEventBuilder adminEvent;
    private Properties properties;


    protected static final Logger logger = Logger.getLogger(DnsManagementResource.class);

    public DnsManagementResource(RealmModel realm, KeycloakSession session, AdminPermissionEvaluator auth,
                                 AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.AUTH_FLOW);
        this.properties = GeneralHelper.getProperties(session, realm, auth.adminAuth().getUser());
    }

    public DnsManagementResource(KeycloakSession session, RealmModel realm){
        this.realm = realm;
        this.session = session;
    }

    @GET
    @Path("getAllCustomDomains")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GeneralResponse<Object> getAllCustomDomains(@PathParam("realmName") String realmName) {
        GeneralResponse<Object> response = new GeneralResponse<>();
        RealmModel realm = session.getContext().getRealm();
        if(!realm.isDNSEnabled())
        {
            response.setStatus(403);
            response.setBody(properties.getProperty("no-subscription-for-dns"));
            return response;
        }

        if(realm.getCustomDomainMappings().count() == 0 ) {
            response.setStatus(200);
            response.setBody(properties.getProperty("no-custom-domains-found"));
            return response;
        }

        List<CustomDomainMappingModel> customDomains = realm.getCustomDomainMappings().filter(x->x.isActive()).collect(Collectors.toList());
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("customDomains", customDomains);

        response.setStatus(200);
        response.setBody(responseBody);
        return response;
    }

    @POST
    @Path("txt/create")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GeneralResponse<Object> createTxtRecords(DnsRepresentation dnsRepresentation) {
        GeneralResponse<Object> response = new GeneralResponse<>();
        if(!session.getContext().getRealm().isDNSEnabled())
        {
            response.setStatus(403);
            response.setBody(properties.getProperty("no-subscription-for-dns"));
            return response;
        }

        try {
            validateHostName(dnsRepresentation.getDomainName());
        } catch (TextParseException e) {
            logger.error(e.getMessage(), e);

            response.setStatus(400);
            response.setBody(properties.getProperty("invalid-domain-name"));
            return response;
        } catch (CustomDomainException cde) {
            logger.error(cde.getMessage(), cde);

            response.setStatus(400);
            response.setBody(properties.getProperty("entered-invalid-name"));
            return response;
        }

        Map<String, String> records = new HashMap<>();
        String txtRecordName = "_acme_" + realm.getName().toLowerCase() + "." + dnsRepresentation.getDomainName();
        String txtRecordValue = GeneralHelper.getRandomValue(45);
        CustomDomainMappingModel customDomainMappingByDomain = realm.getCustomDomainMappingByDomain(dnsRepresentation.getDomainName());
        if (customDomainMappingByDomain != null) {
            realm.removeCustomDomainMapping(dnsRepresentation.getDomainName());
        }

        CustomDomainMappingModel customDomainMappingModel = new CustomDomainMappingModel();

        customDomainMappingModel.setCustomDomain(dnsRepresentation.getDomainName());
        customDomainMappingModel.setTxtRecordName(txtRecordName);
        customDomainMappingModel.setTxtRecordValue(txtRecordValue);

        customDomainMappingModel = realm.updateCustomDomainMapping(customDomainMappingModel);

        /*if (recordValue == null) {
            recordValue = GeneralHelper.getRandomValue(45);
            realm.setAttribute(DNS_TXT_RECORD_VALUE, recordValue);
        }
        realm.setAttribute(DNS_NAME, dnsRepresentation.getDomainName());*/
        records.put("Record Name", customDomainMappingModel.getTxtRecordName());
        records.put("Record Value", customDomainMappingModel.getTxtRecordValue());
        records.put("Record Type", "TXT");

        response.setStatus(200);
        response.setBody(records);
        return response;
    }

    @POST
    @Path("txt/verify")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GeneralResponse<Object> checkDNS(DnsRepresentation dnsRepresentation) {
        GeneralResponse<Object> response = new GeneralResponse<>();
        CustomDomainMappingModel customDomainMappingByDomain = realm.getCustomDomainMappingByDomain(dnsRepresentation.getDomainName());

        if (customDomainMappingByDomain == null) {
            response.setStatus(404);
            response.setBody(properties.getProperty("domain-name-not-set"));
            return response;
        }

        if(!session.getContext().getRealm().isDNSEnabled())
        {
            response.setStatus(403);
            response.setBody(properties.getProperty("no-subscription-for-dns"));
            return response;
        }

        String dns = customDomainMappingByDomain.getCustomDomain();
        if (dns == null || dns.isEmpty()) {
            response.setStatus(400);
            response.setBody(properties.getProperty("domain-name-not-set"));
            return response;
        }

        TXTRecord txtRecord = getTxtRecord(customDomainMappingByDomain.getTxtRecordName());
        if (txtRecord == null) {
            response.setStatus(404);
            return response;
        }


        if (txtRecord.getStrings().isEmpty()) {
            response.setStatus(404);
            return response;
        }

        String value = customDomainMappingByDomain.getTxtRecordValue();
        if (value == null || value.isEmpty()) {
            response.setStatus(404);
            response.setBody(properties.getProperty("no-available-text-record"));
            return response;
        }

        if (!value.equals(txtRecord.getStrings().get(0))) {
            response.setStatus(400);
            response.setBody(properties.getProperty("text-record-doesnot-match"));
            return response;
        }

        customDomainMappingByDomain.setTxtRecordVerified(true);
        realm.updateCustomDomainMapping(customDomainMappingByDomain);

        response.setStatus(200);
        response.setBody(properties.getProperty("text-record-verified-successfully"));

        return response;
    }

    @POST
    @Path("a/create")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GeneralResponse<Object> createARecord(DnsRepresentation dnsRepresentation) {
        GeneralResponse<Object> response = new GeneralResponse<>();
        if(!session.getContext().getRealm().isDNSEnabled())
        {
            response.setStatus(403);
            response.setBody(properties.getProperty("no-subscription-for-dns"));
            return response;
        }
        String dns = realm.getCustomDomainMappingByDomain(dnsRepresentation.getDomainName()).getCustomDomain();
        if (dns == null || dns.isEmpty()) {
            response.setStatus(400);
            response.setBody(properties.getProperty("domain-name-not-set"));
            return response;
        }

        boolean isTxtVerificationCompleted = realm.getCustomDomainMappingByDomain(dns).getTxtRecordVerified();
        if (!isTxtVerificationCompleted) {
            response.setStatus(400);
            response.setBody(properties.getProperty("txt-record-verification-not-completed"));
            return response;
        }

        CustomDomainMappingModel customDomainMappingModel = realm.getCustomDomainMappingByDomain(dns);
        customDomainMappingModel.setARecord(customDomainMappingModel.getCustomDomain());
        customDomainMappingModel.setARecordValue(EnvironmentReader.getValue("LB_ENDPOINT"));

        realm.updateCustomDomainMapping(customDomainMappingModel);

        Map<String, String> body = new HashMap<>();
        body.put("Record Name",  dns);
        body.put("Record Value", EnvironmentReader.getValue("LB_ENDPOINT"));
        body.put("Record Type", getRecordVerifyType());

        response.setStatus(200);
        response.setBody(body);
        return response;
    }

    @POST
    @Path("a/verify")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GeneralResponse<Object> verifyARecords(DnsRepresentation dnsRepresentation) {
        GeneralResponse<Object> response = new GeneralResponse<>();
        if(!session.getContext().getRealm().isDNSEnabled())
        {
            response.setStatus(403);
            response.setBody(properties.getProperty("no-subscription-for-dns"));
            return response;
        }
        String dns = realm.getCustomDomainMappingByDomain(dnsRepresentation.getDomainName()).getCustomDomain();
        if (dns == null || dns.isEmpty()) {
            response.setStatus(403);
            response.setBody(properties.getProperty("domain-name-not-set"));
            return response;
        }

        logger.info("RECORD_VERIFICATION_TYPE " +  getRecordVerifyType());
        if (getRecordVerifyType().equalsIgnoreCase("CNAME")) {
            CNAMERecord cNameRecord = getCnameRecord(dns);

            if (cNameRecord == null) {
                response.setStatus(404);
                return response;
            }

            if (cNameRecord.getTarget() == null || !cNameRecord.getTarget().toString().equals(EnvironmentReader.getValue("LB_ENDPOINT"))) {
                response.setStatus(404);
                response.setBody(properties.getProperty("cname-doesnot-match"));
                return response;
            }
            logger.info("Searched Target " +  cNameRecord.getTarget());
        } else {
            ARecord aRecord = getARecord(dns);

            if (aRecord == null) {
                response.setStatus(404);
                return response;
            }

            if (aRecord.getAddress() == null) {
                response.setStatus(404);
                return response;
            }

            if (!aRecord.getAddress().getHostAddress().equals(EnvironmentReader.getValue("LB_ENDPOINT"))){
                response.setStatus(404);
                response.setBody(properties.getProperty("a-record-doesnot-match"));
                return response;
            }
            logger.info("Searched Target " +  aRecord.getAddress().getHostAddress());
        }

        CustomDomainMappingModel customDomainMappingModel = realm.getCustomDomainMappingByDomain(dns);
        customDomainMappingModel.setARecordVerified(true);

        realm.updateCustomDomainMapping(customDomainMappingModel);

        response.setStatus(200);
        response.setBody(properties.getProperty("a-record-verified-succesfully"));
        return response;
    }


    @POST
    @Path("/create/{domain}")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public GeneralResponse<Object> createDNS(@PathParam("domain") String domain, final CustomDomainCertRepresentation rep) {
        GeneralResponse<Object> response = new GeneralResponse<>();
        if(!session.getContext().getRealm().isDNSEnabled())
        {
            response.setStatus(403);
            response.setBody(properties.getProperty("no-subscription-for-dns"));
            return response;
        }
        String dns = realm.getCustomDomainMappingByDomain(domain).getCustomDomain();
        if (dns == null || dns.isEmpty()) {
            response.setStatus(403);
            response.setBody(properties.getProperty("domain-name-not-set"));
            return response;
        }

        CustomDomainMappingModel customDomainMappingModel = realm.getCustomDomainMappingByDomain(dns);

        boolean isTxtVerificationCompleted = customDomainMappingModel.getTxtRecordVerified();
        if (!isTxtVerificationCompleted) {
            response.setStatus(400);
            response.setBody(properties.getProperty("txt-record-verification-not-completed"));
            return response;
        }

        boolean isAVerificationCompleted = customDomainMappingModel.getARecordVerified();
        if (!isAVerificationCompleted) {
            response.setStatus(400);
            response.setBody(properties.getProperty("a-record-verify-completed"));
            return response;
        }

        String token = session.getContext().getRequestHeaders().getRequestHeader("Authorization").get(0);
        Map<String, String> body = new HashMap<>();
        boolean isCustomCert = false;
        body.put("domain", customDomainMappingModel.getCustomDomain());
        body.put("realm", realm.getName());
        if (rep != null && rep.isCustomCert()) {
            isCustomCert = true;
            body.put("crt", rep.getCrt());
            body.put("key", rep.getKey());
        }
        body.put("customCert", String.valueOf(isCustomCert));
        body.put("userEmail", auth.adminAuth().getUser().getEmail());
        body.put("hookUrl", ModelToRepresentation.getWorkspaceUrl(session, realm) + "/auth/admin/realms/"+ realm.getName() +"/dns/cert-process-hook");
        HttpUtility httpUtility = new HttpUtility();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Response ingressResponse = httpUtility.callRequest(MediaType.APPLICATION_JSON, "POST",
                    EnvironmentReader.getValue("INGRESS_ENDPOINT") + "/create", objectMapper.writeValueAsString(body)
                    , token , null, 1, 120000);

            if (ingressResponse != null && (ingressResponse.code() == 422 || ingressResponse.code() == 406)) {
                response.setStatus(406);
                response.setBody(properties.getProperty("please-upload-valid-cert"));
                return response;
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        customDomainMappingModel.setSetupCompleted(true);
        realm.updateCustomDomainMapping(customDomainMappingModel);

        response.setStatus(200);
        response.setBody(properties.getProperty("dns-creation-inprogress"));
        return response;
    }


    @POST
    @Path("cert-process-hook")
    @NoCache
    public void getCertProcessResponse(CertProcessResponse res) {

        String dns = realm.getCustomDomainMappingByDomain(res.getDomain()).getCustomDomain();
        EventBuilder evtBuilder = new EventBuilder(realm, session, null);
        evtBuilder.realm(realm);
        evtBuilder.detail("triggererEmail", res.getUserEmail());
        evtBuilder.detail("newDomain", dns);

        CustomDomainMappingModel customDomainMappingModel = realm.getCustomDomainMappingByDomain(res.getDomain());

        //get CertCreationResponse response
        if(Boolean.parseBoolean(res.certReady())){
            //if cert creation success
            customDomainMappingModel.setActive(true);
            realm.updateCustomDomainMapping(customDomainMappingModel);

            if(realm.getCustomDomainMappings().filter(x->x.isActive()).count() == 1) {
                customDomainMappingModel.setIsPrimary(true);
                markPrimaryInIngress(realm, customDomainMappingModel);
            }
            session.onBoarding().onDNSCompleted(realm, dns);
            evtBuilder.event(EventType.CUSTOM_DOMAIN_SUCCESS);
            adminEvent.operation(OperationType.DNS).resource(ResourceType.DNS_COMPLETED).representation("Custom domain created - " + res.getDomain()).resourcePath(session.getContext().getUri()).success();
        } else {
            // if cert creation fails
            cleanupAttributes(realm);
            customDomainMappingModel.setActive(false);
            evtBuilder.event(EventType.CUSTOM_DOMAIN_FAILURE);
            adminEvent.operation(OperationType.DNS).resource(ResourceType.DNS_FAILURE).representation("Custom domain failed").resourcePath(session.getContext().getUri()).success();
        }

        realm.updateCustomDomainMapping(customDomainMappingModel);
        //success the event to send mail
        evtBuilder.success();
    }

    private void markPrimaryInIngress(RealmModel realm, CustomDomainMappingModel customDomainMappingModel) {
        String token = session.getContext().getRequestHeaders().getRequestHeader("Authorization").get(0);

        Map<String, String> body = new HashMap<>();
        body.put("realm", realm.getName());
        body.put("domain", customDomainMappingModel.getCustomDomain());

        HttpUtility httpUtility = new HttpUtility();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Response ingressResponse = httpUtility.callRequest(MediaType.APPLICATION_JSON, "POST",
                    EnvironmentReader.getValue("INGRESS_ENDPOINT") + "/update", objectMapper.writeValueAsString(body)
                    , token , null, 1, 120000);

            if (ingressResponse != null && (ingressResponse.code() == 422 || ingressResponse.code() == 406)) {
                adminEvent.operation(OperationType.DNS).resource(ResourceType.DNS).representation("Failed to mark as primary domain in ingress").resourcePath(session.getContext().getUri()).success();
                logger.error("DnsManagementResource :: makePrimaryInIngress :: Failed to mark as primary domain in ingress");
            }
            adminEvent.operation(OperationType.DNS).resource(ResourceType.DNS).representation(customDomainMappingModel.getCustomDomain() + " is marked as primary domain").resourcePath(session.getContext().getUri()).success();
            logger.info("DnsManagementResource :: makePrimaryInIngress :: Successfully marked as primary domain :: " + customDomainMappingModel.getCustomDomain());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }


    @POST
    @Path("/{domain}/remove")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GeneralResponse<Object> removeDNS(@PathParam("domain") String domain) {
        GeneralResponse<Object> response = new GeneralResponse<>();
        AdminEventBuilder adEvt = new AdminEventBuilder(realm, new AdminAuth(session.getContext().getRealm(),
                null, null, realm.getClientByClientId("dashboard")), session,
                session.getContext().getConnection());
        if(!session.getContext().getRealm().isDNSEnabled())
        {
            response.setStatus(403);
            response.setBody("no-subscription-for-dns");
            adEvt.operation(OperationType.DNS).resource(ResourceType.DNS_REMOVAL_FAILURE).representation("dns removal failed - dns subscription not found").resourcePath(session.getContext().getUri()).success();
            return response;
        }

        CustomDomainMappingModel customDomainMappingByDomain = realm.getCustomDomainMappingByDomain(domain);
        String dns = realm.getCustomDomainMappingByDomain(domain).getCustomDomain();
        if (dns == null || dns.isEmpty()) {
            response.setStatus(400);
            response.setBody("domain-name-not-set");
            adEvt.operation(OperationType.DNS).resource(ResourceType.DNS_REMOVAL_FAILURE).representation("dns removal failed - dns name not set").resourcePath(session.getContext().getUri()).success();
            return response;
        }

        boolean isDNSCompleted = customDomainMappingByDomain.isSetupCompleted();
        if (!isDNSCompleted) {
            response.setStatus(400);
            response.setBody("dns-creation-not-completed");
            adEvt.operation(OperationType.DNS).resource(ResourceType.DNS_REMOVAL_FAILURE).representation("dns removal failed - dns creation not completed").resourcePath(session.getContext().getUri()).success();
            return response;
        }

        if(customDomainMappingByDomain.isPrimary() && realm.getCustomDomainMappings().filter(x->x.isActive()).count() > 1) {
            response.setStatus(400);
            response.setBody("primary-domain-not-removed");
            adEvt.operation(OperationType.DNS).resource(ResourceType.DNS_REMOVAL_FAILURE).representation("dns removal failed - primary domain not removed").resourcePath(session.getContext().getUri()).success();
            return response;
        }

        Map<String, String> body = new HashMap<>();
        body.put("domain", dns);
        body.put("realm", realm.getName());

        HttpUtility httpUtility = new HttpUtility();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Response ingressResponse = httpUtility.callRequest(MediaType.APPLICATION_JSON, "POST",
                    EnvironmentReader.getValue("INGRESS_ENDPOINT") + "/delete", objectMapper.writeValueAsString(body)
                    , null);
            if (ingressResponse.isSuccessful()) {
                session.onBoarding().onDNSRemoved(realm, dns);
                adEvt.operation(OperationType.DNS).resource(ResourceType.DNS_REMOVAL_SUCCESS).representation("dns removed successfully " + dns).resourcePath(session.getContext().getUri()).success();
                realm.removeCustomDomainMapping(customDomainMappingByDomain.getCustomDomain());
                response.setStatus(200);
                Map<String, String> responseBody = new HashMap<>();
                responseBody.put("message", "dns-removed-successfully");
                responseBody.put("workspaceUrl", ModelToRepresentation.getWorkspaceUrl(session, realm));
                response.setBody(responseBody);
                return response;
            }
        } catch (JsonProcessingException e) {
            adEvt.operation(OperationType.DNS).resource(ResourceType.DNS_REMOVAL_FAILURE).representation("dns removal failed " + e.getMessage()).resourcePath(session.getContext().getUri()).success();
            e.printStackTrace();
            response.setStatus(400);
            response.setBody("dns-removing-failed");
            return response;
        }

        response.setStatus(400);
        adEvt.operation(OperationType.DNS).resource(ResourceType.DNS_REMOVAL_FAILURE).representation("dns removal failed").resourcePath(session.getContext().getUri()).success();
        response.setBody("dns-removing-failed");
        return response;
    }

    @POST
    @Path("/mark-as-primary")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public GeneralResponse<Object> markAsPrimary(DnsRepresentation dnsRepresentation) {
        GeneralResponse<Object> response = new GeneralResponse<>();
        if(!session.getContext().getRealm().isDNSEnabled())
        {
            response.setStatus(403);
            response.setBody(properties.getProperty("no-subscription-for-dns"));
            return response;
        }

        CustomDomainMappingModel customDomainMappingByDomain = realm.getCustomDomainMappingByDomain(dnsRepresentation.getDomainName());
        String dns = customDomainMappingByDomain.getCustomDomain();
        if (dns == null || dns.isEmpty()) {
            response.setStatus(400);
            response.setBody(properties.getProperty("domain-name-not-set"));
            return response;
        }

        boolean isDNSCompleted = customDomainMappingByDomain.isSetupCompleted();
        if (!isDNSCompleted) {
            response.setStatus(400);
            response.setBody(properties.getProperty("dns-creation-not-completed"));
            return response;
        }

        realm.getCustomDomainMappings().filter(x -> x.isPrimary()).forEach(x -> {
            CustomDomainMappingModel customDomainMappingModel = realm.getCustomDomainMappingByDomain(x.getCustomDomain());
            customDomainMappingModel.setIsPrimary(false);
            realm.updateCustomDomainMapping(customDomainMappingModel);
        });

        customDomainMappingByDomain.setIsPrimary(true);
        markPrimaryInIngress(realm, customDomainMappingByDomain);
        realm.updateCustomDomainMapping(customDomainMappingByDomain);

        response.setStatus(200);
        response.setBody(properties.getProperty("custom-domain-marked-as-primary"));
        return response;
    }


    private TXTRecord getTxtRecord(String hostName) {
        try {
            Lookup lookup = new Lookup(hostName + ".", Type.TXT, DClass.IN);
            Lookup.getDefaultCache(DClass.IN).setMaxCache(0);
            Lookup.getDefaultCache(DClass.IN).setMaxNCache(0);
            Lookup.getDefaultCache(DClass.IN).clearCache();
            lookup.setResolver(new ExtendedResolver(getHostName()));
            lookup.run();
            if (lookup.getResult() == Lookup.SUCCESSFUL)
                return (TXTRecord) lookup.getAnswers()[0];
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private ARecord getARecord(String hostName) {

        try {
            Lookup lookup = new Lookup(hostName + ".", Type.A, DClass.IN);
            Lookup.getDefaultCache(DClass.IN).setMaxCache(0);
            Lookup.getDefaultCache(DClass.IN).setMaxNCache(0);
            Lookup.getDefaultCache(DClass.IN).clearCache();
            lookup.setResolver(new ExtendedResolver(getHostName()));
            lookup.run();
            if (lookup.getResult() == Lookup.SUCCESSFUL)
                return (ARecord) lookup.getAnswers()[0];
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String[] getHostName() {
        String env = EnvironmentReader.getValue("DNS_RESOLVER_ADDR");
        if (env == null || env.isEmpty()) {
            logger.info("Environment variable is Empty.Executing with default HostName");
            return new String[]{"8.8.8.8"};
        }
        return env.split(",");
    }

    private void validateHostName(String hostName) throws TextParseException, CustomDomainException {
        if (hostName.startsWith("https://") || hostName.startsWith("https") || hostName.startsWith("http") || hostName.startsWith("www")) {
            throw new TextParseException();
        }

        HttpUtility httpUtility = new HttpUtility();
        Map<String, String> body = new HashMap<>();
        body.put("domain", hostName);
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            Response response = httpUtility.callRequest(MediaType.APPLICATION_JSON, "POST",
                    EnvironmentReader.getValue("INGRESS_ENDPOINT") + "/ingress_check",
                    objectMapper.writeValueAsString(body), null, null, 3);

            if (response != null && response.code() == 409) {
                throw new CustomDomainException("Domain already exists");
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private void cleanupAttributes(RealmModel realm) {
        realm.removeAttribute("DNS_NAME");
        realm.removeAttribute("DNS_TXT_RECORD_VALUE");
        realm.removeAttribute("DNS_A_RECORD_VERIFICATION");
        realm.removeAttribute("DNS_TXT_RECORD_VERIFICATION");
        realm.removeAttribute("DNS_COMPLETED");
    }

    private String getRecordVerifyType(){
        String recType = EnvironmentReader.getValue("LB_RECORD_TYPE");

        if(recType != null && recType.equalsIgnoreCase("cname")){
            return "CNAME";
        }
        return "A";
    }

    private CNAMERecord getCnameRecord(String hostName) {
        try {
            Lookup lookup = new Lookup(hostName + ".", Type.CNAME, DClass.IN);
            Lookup.getDefaultCache(DClass.IN).setMaxCache(0);
            Lookup.getDefaultCache(DClass.IN).setMaxNCache(0);
            Lookup.getDefaultCache(DClass.IN).clearCache();
            lookup.setResolver(new ExtendedResolver(getHostName()));
            lookup.run();
            if (lookup.getResult() == Lookup.SUCCESSFUL)
                return (CNAMERecord) lookup.getAnswers()[0];
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}

