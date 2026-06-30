package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.common.ClientConnection;
import org.keycloak.downloads.DownloadData;
import org.keycloak.downloads.DownloadProvider;
import org.keycloak.downloads.DownloadProviderFactory;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.download.DownloadRepresentation;
import org.keycloak.services.ErrorPageException;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

public class DownloadManagementResource {
        private static final Logger logger = Logger.getLogger(DownloadManagementResource.class);

        protected RealmModel realm;

        private AdminPermissionEvaluator auth;

        private AdminEventBuilder adminEvent;
        private UserModel user;

        @Context
        protected ClientConnection clientConnection;

        @Context
        protected KeycloakSession session;

        @Context
        protected HttpHeaders headers;

        public DownloadManagementResource(RealmModel realm, KeycloakSession session, AdminPermissionEvaluator auth,
                        AdminEventBuilder adminEvent) {
                this.auth = auth;
                this.realm = realm;
                this.session = session;
                this.adminEvent = adminEvent.resource(ResourceType.USER);
        }

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<DownloadRepresentation>> getDownloads() {
        List<ProviderFactory> factoryList = session.getKeycloakSessionFactory().getProviderFactoriesStream(DownloadProvider.class).collect(Collectors.toList());
        Map<String, List<DownloadRepresentation>> downloads = new HashMap<>();

        if (realm.getMode().equalsIgnoreCase("bo")) {
            factoryList.stream().filter(x -> Arrays.asList(((DownloadProviderFactory) x).supportedModes()).contains("bo"))
                    .forEach(x->{
                        DownloadProviderFactory prov = (DownloadProviderFactory) x;
                        if (!downloads.containsKey(prov.getTag().getTag())) {
                            downloads.put(prov.getTag().getTag(), new ArrayList<>());
                        }

                        DownloadRepresentation representation = DownloadRepresentation.builder()
                                .title(prov.getTitle())
                                .downloadButtonName(prov.getDownloadButtonName())
                                .logoUrl(prov.getLogoUrl())
                                .downloadUrl(prov.getId())
                                .build();

                        downloads.get(prov.getTag().getTag()).add(representation);
                    });

        } else {
            List<String> partners = realm.getMarketplaceConfigsStream()
                    .filter(template -> template.getProviderType() != null && template.getProviderType().equalsIgnoreCase("Partners"))
                    .map(partnerName -> partnerName.getProviderId().substring(0, partnerName.getProviderId().indexOf("-")))
                    .collect(Collectors.toList());

            factoryList.stream()
                    .filter(partnerDownloadProvider -> partnerDownloadProvider != null && partnerDownloadProvider.getId() != null && partnerDownloadProvider.getId().contains("partner"))
                    .filter(partner -> !partners.contains(partner.getId().substring(partner.getId().indexOf("-") + 1, partner.getId().length())))
                    .collect(Collectors.toList())
                    .forEach(partnerList -> factoryList.remove(factoryList.indexOf(partnerList)));


            for (ProviderFactory prFact :
                    factoryList) {
                DownloadProviderFactory prov = (DownloadProviderFactory) prFact;
                if (!downloads.containsKey(prov.getTag().getTag())) {
                    downloads.put(prov.getTag().getTag(), new ArrayList<>());
                }

                DownloadRepresentation representation = DownloadRepresentation.builder()
                        .title(prov.getTitle())
                        .downloadButtonName(prov.getDownloadButtonName())
                        .logoUrl(prov.getLogoUrl())
                        .downloadUrl(prov.getId())
                        .build();
                downloads.get(prov.getTag().getTag()).add(representation);
            }

        }
        return downloads;
    }

        @GET
        @Path("/download/{download-id}")
        @NoCache
        @Produces(MediaType.APPLICATION_JSON)
        public DownloadRepresentation getDownloads(@PathParam("download-id") String downloadId) {

                ProviderFactory<DownloadProvider> providerFactory = session.getKeycloakSessionFactory()
                                .getProviderFactory(DownloadProvider.class, downloadId);
                if (providerFactory == null)
                        throw new ErrorPageException(session, Response.Status.BAD_REQUEST, "Invalid Download Request");

                DownloadData downloadData = getDownload(providerFactory.getId());

                return DownloadRepresentation.builder()
                                .description(downloadData != null &&
                                                downloadData.getDescription() != null ? downloadData.getDescription()
                                                                : null)
                                .downloadUrl(downloadData != null
                                                && downloadData.getDownloadUrl() != null ? downloadData.getDownloadUrl()
                                                                : null)
                                .version(downloadData != null &&
                                                downloadData.getVersion() != null ? downloadData.getVersion() : null)
                                .build();
        }

        private DownloadData getDownload(String providerId) {
                DownloadProvider downloadProvider = session.getProvider(DownloadProvider.class, providerId);
                if (downloadProvider == null)
                        throw new NotFoundException("Could not find download resource");

                return downloadProvider.getDownloadDetails();
        }

}
