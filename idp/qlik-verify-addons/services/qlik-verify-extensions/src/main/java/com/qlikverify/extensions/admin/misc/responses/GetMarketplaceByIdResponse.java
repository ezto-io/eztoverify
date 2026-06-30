package com.qlikverify.extensions.admin.misc.responses;


import org.keycloak.marketplace.ThirdPartyProvider;

public class GetMarketplaceByIdResponse {
    private String id;
    private String author;
    private String category;
    private String description;
    private String logo;
    private String title;
    private String getOverviewMdUrl;
    private String getInstallationMdUrl;
    private String[] keywords;
    private boolean isImplemented;
    private ThirdPartyProvider thirdPartyProvider;

    public GetMarketplaceByIdResponse(ThirdPartyProvider thirdPartyProvider) {
        this.thirdPartyProvider = thirdPartyProvider;
    }

    public GetMarketplaceByIdResponse(String id, String author, String category, String description, String logo, String title, String getOverviewMdUrl, String getInstallationMdUrl, String[] keywords, boolean isImplemented, ThirdPartyProvider thirdPartyProvider) {
        this.id = id;
        this.author = author;
        this.category = category;
        this.description = description;
        this.logo = logo;
        this.title = title;
        this.getOverviewMdUrl = getOverviewMdUrl;
        this.getInstallationMdUrl = getInstallationMdUrl;
        this.keywords = keywords;
        this.isImplemented = isImplemented;
        this.thirdPartyProvider = thirdPartyProvider;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGetOverviewMdUrl() {
        return getOverviewMdUrl;
    }

    public void setGetOverviewMdUrl(String getOverviewMdUrl) {
        this.getOverviewMdUrl = getOverviewMdUrl;
    }

    public String getGetInstallationMdUrl() {
        return getInstallationMdUrl;
    }

    public void setGetInstallationMdUrl(String getInstallationMdUrl) {
        this.getInstallationMdUrl = getInstallationMdUrl;
    }

    public String[] getKeywords() {
        return keywords;
    }

    public void setKeywords(String[] keywords) {
        this.keywords = keywords;
    }

    public boolean isImplemented() {
        return isImplemented;
    }

    public void setImplemented(boolean implemented) {
        isImplemented = implemented;
    }

    public ThirdPartyProvider getThirdPartyProvider() {
        return thirdPartyProvider;
    }

    public void setThirdPartyProvider(ThirdPartyProvider thirdPartyProvider) {
        this.thirdPartyProvider = thirdPartyProvider;
    }
}
