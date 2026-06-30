package com.qlikverify.extensions.admin.controllers.marketplace;

import com.qlikverify.extensions.admin.misc.models.Constants;
import com.qlikverify.extensions.admin.misc.responses.GetAllMarketplaceResponse;
import com.qlikverify.extensions.admin.misc.responses.GetMarketplaceByIdResponse;
import com.qlikverify.extensions.admin.misc.responses.QlikVerifyResponse;
import org.jboss.logging.Logger;
import org.keycloak.marketplace.ThirdPartyProvider;
import org.keycloak.marketplace.template.MarketplaceTemplateProvider;
import org.keycloak.marketplace.template.MarketplaceTemplateProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderFactory;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MarketplaceFunctions {
    private KeycloakSession session;
    private static final Logger logger = Logger.getLogger(MarketplaceFunctions.class);

    public MarketplaceFunctions(KeycloakSession session){
        this.session = session;
    }

    public Response getAllMarketplaceCategories() {
        List<String> marketplaceCategories =  new ArrayList<>(org.keycloak.models.Constants.marketplaceCategories);
        List<String> nonEmptyCategories = marketplaceCategories.stream()
                .filter(category -> {
                    List<GetAllMarketplaceResponse> responses =
                            (List<GetAllMarketplaceResponse>) getMarketplaceTemplatesByCategory(category).getEntity();
                    return !responses.isEmpty();
                })
                .collect(Collectors.toList());
        return Response.status(Response.Status.OK).entity(nonEmptyCategories).build();
    }

    public Response getMarketplaceTemplatesByCategory(String category){
        try{
            List<ProviderFactory> factories = session.getKeycloakSessionFactory().getProviderFactories(MarketplaceTemplateProvider.class);
            List<ProviderFactory> categorizedTemplateFactories;
            if (category.equalsIgnoreCase("recommended")) {
                categorizedTemplateFactories = factories.stream().filter(x -> ((MarketplaceTemplateProviderFactory) x).isRecommended()).collect(Collectors.toList());
            } else {
                categorizedTemplateFactories = factories.stream().filter(x -> ((MarketplaceTemplateProviderFactory) x).getCategory().equalsIgnoreCase(category)).collect(Collectors.toList());
            }
            List<GetAllMarketplaceResponse> getAllMarketplaceResponseList = new ArrayList<>();
            for (ProviderFactory factory : categorizedTemplateFactories) {
                MarketplaceTemplateProviderFactory template = (MarketplaceTemplateProviderFactory) factory;
                getAllMarketplaceResponseList.add(new GetAllMarketplaceResponse(
                        template.getId(),
                        template.getAuthor(session),
                        template.getCategory(),
                        template.getDescription(session),
                        template.getLogo(),
                        template.getTitle(session),
                        template.getKeywords(),
                        template.isImplemented(),
                        template.isNew()
                ));
            }
            return Response.status(Response.Status.OK).entity(getAllMarketplaceResponseList).build();
        } catch (Exception e){
            logger.error(e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    public Response getMarketplaceTemplateById(String id) {
        try {
            List<ProviderFactory> factories = session.getKeycloakSessionFactory().getProviderFactories(MarketplaceTemplateProvider.class);
            ProviderFactory factoryById =
                    factories.stream().filter(x -> ((MarketplaceTemplateProviderFactory)x).getId().equals(id)).findFirst().orElse(null);
            if(factoryById == null)
            {
                QlikVerifyResponse qvResponse = new QlikVerifyResponse(Constants.TEMPLATE_ID_NOT_FOUND,Constants.NOT_FOUND);
                return Response.status(Response.Status.NOT_FOUND).entity(qvResponse).build();
            }
            else {
                MarketplaceTemplateProviderFactory result = (MarketplaceTemplateProviderFactory) factoryById;
                ThirdPartyProvider thirdPartyProvider = result.get3rdPartyProvider();
                GetMarketplaceByIdResponse getMarketplaceByIdResponse = new GetMarketplaceByIdResponse(thirdPartyProvider);
                getMarketplaceByIdResponse.setAuthor(result.getAuthor(session));
                getMarketplaceByIdResponse.setCategory(result.getCategory());
                getMarketplaceByIdResponse.setDescription(result.getDescription(session));
                getMarketplaceByIdResponse.setId(result.getId());
                getMarketplaceByIdResponse.setTitle(result.getTitle(session));
                getMarketplaceByIdResponse.setGetOverviewMdUrl(result.getOverviewMdUrl());
                getMarketplaceByIdResponse.setLogo(result.getLogo());
                getMarketplaceByIdResponse.setGetInstallationMdUrl(result.getInstallationMdUrl());
                getMarketplaceByIdResponse.setKeywords(result.getKeywords());
                getMarketplaceByIdResponse.setImplemented(result.isImplemented());
                getMarketplaceByIdResponse.setThirdPartyProvider(thirdPartyProvider);
                return Response.status(Response.Status.OK).entity(getMarketplaceByIdResponse).build();
            }
        } catch (Exception e) {
            QlikVerifyResponse qvResponse = new QlikVerifyResponse(Constants.EXCEPTION_FETCHING_TEMPLATES,Constants.BAD_REQUEST);
            return Response.status(Response.Status.BAD_REQUEST).entity(qvResponse).build();
        }
    }

    public Response searchMarketplaceTemplates(String pattern){

        try{
            //getting all factories of marketplace
            List<ProviderFactory> factories = session.getKeycloakSessionFactory().getProviderFactories(MarketplaceTemplateProvider.class);

            //searching templates using pattern
            List<ProviderFactory> resultantTemplates = factories.stream().filter(x -> ((MarketplaceTemplateProviderFactory) x).getTitle(session).toUpperCase().contains(pattern.toUpperCase()) || Arrays.stream(((MarketplaceTemplateProviderFactory) x).getKeywords()).anyMatch(s -> s.toUpperCase().contains(pattern.toUpperCase()))).collect(Collectors.toList());

            //making responses based on searched templates, if no templates are matched,then it returns empty list
            List<GetAllMarketplaceResponse> resultantResponseList = new ArrayList<>();
            for (ProviderFactory factory : resultantTemplates) {
                MarketplaceTemplateProviderFactory template = (MarketplaceTemplateProviderFactory) factory;
                resultantResponseList.add(new GetAllMarketplaceResponse(
                        template.getId(),
                        template.getAuthor(session),
                        template.getCategory(),
                        template.getDescription(session),
                        template.getLogo(),
                        template.getTitle(session),
                        template.getKeywords(),
                        template.isImplemented(),
                        template.isNew()
                ));
            }
            return Response.status(Response.Status.OK).entity(resultantResponseList).build();
        } catch (Exception e){
            logger.error(e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

}
