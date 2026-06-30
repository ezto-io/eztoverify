package com.qlikverify.backoffice.controllers;

import com.qlikverify.backoffice.jpa.ApplicationDetailsEntity;
import com.qlikverify.extensions.admin.misc.base.BaseAuthCheckResource;
import javax.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.GeneralResponse;
import org.keycloak.models.KeycloakSession;
import java.io.StringWriter;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.keycloak.models.RealmModel;
import org.keycloak.utils.StringUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static com.qlikverify.backoffice.Constants.*;


public class ApplicationDetailsResource extends BaseAuthCheckResource {
    private static final Logger logger = Logger.getLogger(ApplicationDetailsResource.class);
    KeycloakSession session;
    protected EntityManager em;
    public ApplicationDetailsResource(KeycloakSession session) {
        super(session);
        this.session = session;
    }

    @GET
    @Path("/{realmId}/applications/{appType}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getApplicationDetails(@QueryParam("first") Integer firstResult, @QueryParam("max") Integer maxResults,
                                                     @QueryParam("appName") String appName, @QueryParam("platform") String platform,
                                                     @PathParam("realmId") String realmId, @PathParam("appType") String appType) {
        em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        List<ApplicationDetailsEntity> entity = getApplicationDetailsByType(appType, realmId);

        firstResult = firstResult != null ? firstResult : -1;
        maxResults = maxResults != null ? maxResults : Constants.DEFAULT_MAX_RESULTS;

        List<Map<String, String>> applications = new ArrayList<>();
        List<Map<String, String>> finalApplications = applications;
        entity.stream().forEach(x -> {
            finalApplications.add(new HashMap<String, String>() {{
                put(APP_ID, x.getId().toString());
                put(APP_NAME, x.getAppName().toString());
                put(APP_VERSION, x.getAppVersion().toString());
                put(APP_URL, x.getUpdateUrl().toString());
                put(APP_TYPE, x.getAppType());
                put(APP_PLATFORM, x.getAppPlatform());
            }});
        });
        applications = finalApplications;
        applications = applications.stream().filter(x -> appName != null && StringUtil.isNotBlank(appName) && !appName.equalsIgnoreCase("null") ? ((String)x.get(APP_NAME).toLowerCase()).contains(appName.toLowerCase()) : !x.isEmpty())
                .filter(x -> platform != null && StringUtil.isNotBlank(platform) && !platform.equalsIgnoreCase("null") ? platform.equals(x.get(APP_PLATFORM)) : !x.isEmpty())
                .collect(Collectors.toList());
        List<Map<String, String>> finalResult = applications.stream()
                .skip(firstResult)
                .limit(maxResults)
                .collect(Collectors.toList());
        Map<String, Object> userTransactionDetails = new HashMap<>();
        userTransactionDetails.put("result", finalResult);
        userTransactionDetails.put("count", applications.size());
        return userTransactionDetails;
    }

    @POST
    @Path("/{realmId}/application/register")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addApplicationDetail(@PathParam("realmId") String realmId, Map<String, String> reqBody) {
        GeneralResponse response = isValid(reqBody, realmId);
        if (response.getStatus() == (400)) {
            return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
        }
        try{
            em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            addApplicationDetails(reqBody, realmId);
        } catch (Exception e){
            e.printStackTrace();
            logger.error(e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
        }
        return Response.status(Response.Status.OK).entity(response).build();
    }

    @PUT
    @Path("/{realmId}/{appType}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateApplicationDetails(@PathParam("realmId") String realmId, Map<String, String> reqBody) {
        GeneralResponse response = isValid(reqBody, realmId);
        if (response.getStatus() == (400)) {
            return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
        }
        try{
        em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        updateApplicationDetails(reqBody, realmId);
        } catch (Exception e){
            e.printStackTrace();
            logger.error(e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
        }
        return Response.status(Response.Status.OK).entity(response).build();
    }

    @DELETE
    @Path("/{realmId}/{appId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteApplicationDetails(@PathParam("appId") String appId, @PathParam("realmId") String realmId) {
        GeneralResponse response = new GeneralResponse();
        try{
        em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        removeApplicationDetails(appId, realmId);
        } catch (Exception e){
            e.printStackTrace();
            logger.error(e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
        }
        return Response.status(Response.Status.OK).entity(response).build();
    }

    @GET
    @Path("/{realmId}/application/{appId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String getApplicationDetailById(@PathParam("realmId") String realmId, @PathParam("appId") String appId) {
        em = session.getProvider(JpaConnectionProvider.class).getEntityManager();

        ApplicationDetailsEntity entity = getApplicationDetailsById(appId, realmId);
        if(entity == null){
            return "Invalid app id";
        }
        return getApplicationXmlDetailsById(entity);
    }

    public String getApplicationXmlDetailsById(ApplicationDetailsEntity entity){

        try{
            // Create a new Document
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            Document doc = dbFactory.newDocumentBuilder().newDocument();

            // Create the root element <rss>
            Element rss = doc.createElement("rss");
            rss.setAttribute("version", "2.0");
            rss.setAttribute("xmlns:sparkle", "http://www.andymatuschak.org/xml-namespaces/sparkle");
            doc.appendChild(rss);

            // Create the <channel> element
            Element channel = doc.createElement("channel");
            rss.appendChild(channel);

            // Create the <title> element under <channel>
            Element title = doc.createElement("title");
            title.appendChild(doc.createTextNode("ezto verify"));
            channel.appendChild(title);

            // Create the <item> element under <channel>
            Element item = doc.createElement("item");
            channel.appendChild(item);

            // Create the <title> element under <item>
            Element itemTitle = doc.createElement("title");
            itemTitle.appendChild(doc.createTextNode(entity.getAppVersion()));
            item.appendChild(itemTitle);

            // Create the <description> element under <item>
            Element description = doc.createElement("description");
            description.appendChild(doc.createTextNode(""));
            item.appendChild(description);

            // Create the <enclosure> element under <item>
            Element enclosure = doc.createElement("enclosure");
            enclosure.setAttribute("url", entity.getUpdateUrl());
            enclosure.setAttribute("sparkle:version", entity.getAppVersion());
            enclosure.setAttribute("sparkle:os", entity.getAppPlatform());
            item.appendChild(enclosure);

            // Transform the XML content to a string
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            StringWriter writer = new StringWriter();

            // Add custom headers to the XML declaration
            transformer.setOutputProperty("method", "xml");
            transformer.setOutputProperty("omit-xml-declaration", "no");
            transformer.setOutputProperty("encoding", "UTF-8");
            transformer.setOutputProperty("standalone", "no");

            transformer.transform(new DOMSource(doc), new StreamResult(writer));

            // Get the XML content as a string
            return writer.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



    private GeneralResponse isValid( Map<String, String> reqBody, String realmId) {
        GeneralResponse generalResponse = new GeneralResponse();
        generalResponse.setStatus(400);

        if (realmId == null) {
            generalResponse.setBody("No workspaceid is set");
            return generalResponse;
        }

        RealmModel realmModel = session.realms().getRealm(realmId);

        if (realmModel == null) {
            generalResponse.setBody("Invalid workspace");
            return generalResponse;
        }

        if(!reqBody.get(APP_URL).startsWith("https://")){
            generalResponse.setBody("Invalid App URL");
            return generalResponse;
        }

        generalResponse.setStatus(200);
        return generalResponse;

    }

    public List<ApplicationDetailsEntity> getApplicationDetailsByType(String appType, String realmId){
        TypedQuery<ApplicationDetailsEntity> query = em.createNamedQuery("getAppsDetails", ApplicationDetailsEntity.class);
        query.setParameter(REALM_ID, realmId);
        query.setParameter(APP_TYPE, appType);
        return query.getResultList().stream().collect(Collectors.toList());
    }

    public ApplicationDetailsEntity getApplicationDetailsById(String appId, String realmId){
        TypedQuery<ApplicationDetailsEntity> query = em.createNamedQuery("getAppDetailsById", ApplicationDetailsEntity.class);
        query.setParameter(REALM_ID, realmId);
        query.setParameter("id", appId);

        Optional<ApplicationDetailsEntity> entity = query.getResultList().stream().findFirst();
        if (!entity.isPresent()) return null;
        return entity.get();
    }

    public void addApplicationDetails(Map<String, String> reqBody, String realmId){
        ApplicationDetailsEntity applicationDetailsEntity = new ApplicationDetailsEntity();
        applicationDetailsEntity.setId(UUID.randomUUID().toString());
        applicationDetailsEntity.setAppName(reqBody.get(APP_NAME));
        applicationDetailsEntity.setAppVersion(reqBody.get(APP_VERSION));
        applicationDetailsEntity.setUpdateUrl(reqBody.get(APP_URL));
        applicationDetailsEntity.setAppType(reqBody.get(APP_TYPE));
        applicationDetailsEntity.setAppPlatform(reqBody.get(APP_PLATFORM));
        applicationDetailsEntity.setRealmId(realmId);
        em.persist(applicationDetailsEntity);
    }

    public void updateApplicationDetails(Map<String, String> reqBody, String realmId){
        Query query = em.createNamedQuery("updateAppDetails");
        query.setParameter(APP_VERSION, reqBody.get(APP_VERSION));
        query.setParameter(UPDATE_URL, reqBody.get(APP_URL));
        query.setParameter(APP_TYPE, reqBody.get(APP_TYPE));
        query.setParameter(APP_PLATFORM, reqBody.get(APP_PLATFORM));
        query.setParameter(REALM_ID, realmId);
        query.setParameter(APP_NAME, reqBody.get(APP_NAME));
        query.setParameter("id", reqBody.get(APP_ID));

        query.executeUpdate();
    }

    public void removeApplicationDetails(String appId, String realmId){
        Query query = em.createNamedQuery("removeAppsById");
        query.setParameter("id", appId);
        query.setParameter(REALM_ID, realmId);
        query.executeUpdate();
    }
}
