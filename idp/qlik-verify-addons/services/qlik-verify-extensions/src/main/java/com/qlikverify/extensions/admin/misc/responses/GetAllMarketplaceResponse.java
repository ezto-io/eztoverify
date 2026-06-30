package com.qlikverify.extensions.admin.misc.responses;

public class GetAllMarketplaceResponse {
    private String id;
    private String author;
    private String category;
    private String description;
    private String logo;
    private String title;
    private String[] keywords;
    private boolean isNew;
    private boolean isImplemented;

    public GetAllMarketplaceResponse(String id, String author, String category, String description, String logo, String title, String[] keywords, boolean implemented, boolean isNew) {
        this.id = id;
        this.author = author;
        this.category = category;
        this.description = description;
        this.logo = logo;
        this.title = title;
        this.keywords = keywords;
        this.isNew = isNew;
        this.isImplemented = implemented;
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

    public String[] getKeywords() {
        return keywords;
    }

    public void setKeywords(String[] keywords) {
        this.keywords = keywords;
    }

    public boolean isNew() {
        return isNew;
    }

    public boolean isImplemented() {
        return isImplemented;
    }
}
