package org.keycloak.models.jpa;

import org.keycloak.models.BrandingModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.BrandingAttributesEntity;
import org.keycloak.models.jpa.entities.BrandingEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.provider.ProviderConfigProperty;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.*;

public class BrandingAdapter implements BrandingModel, JpaModel<BrandingEntity> {

    protected KeycloakSession session;
    protected RealmModel realm;
    protected EntityManager em;
    protected BrandingEntity entity;

    public BrandingAdapter(RealmModel realm, EntityManager em, KeycloakSession session, BrandingEntity entity) {
        this.session = session;
        this.realm = realm;
        this.em = em;
        this.entity = entity;
    }

    @Override
    public BrandingEntity getEntity() {
        return entity;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public String getName() {
        return entity.getName();
    }

    @Override
    public void setName(String name) {
        entity.setName(name);
    }

    @Override
    public boolean isRealmBrand() {
        return entity.isRealmBrand();
    }

    @Override
    public void setRealmBrand(boolean isRealmBrand) {
        entity.setRealmBrand(isRealmBrand);
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> attrs = new HashMap<>();
        for (BrandingAttributesEntity attr : entity.getAttributes()) {
            attrs.put(attr.getName(), attr.getValue());
        }
        if (!attrs.containsKey("logoPositionValue")) {
            attrs.put("logoPositionValue", "0|0|0|0");
        }
        if (!attrs.containsKey("logoPositionValueType")) {
            attrs.put("logoPositionValueType", "px");
        }

        if (attrs.containsKey("bodyBgImage")) {
            attrs.put("bodyBgImage", getImageAbsURL(attrs.get("bodyBgImage")));
        }

        if (attrs.containsKey("themeLayoutBgImage")) {
            attrs.put("themeLayoutBgImage", getImageAbsURL(attrs.get("themeLayoutBgImage")));
        }

        return attrs;
    }

    @Override
    public void setAttribute(String name, String value) {
        // Remove all existing
        if (value == null) {
            entity.getAttributes().removeIf(a -> a.getName().equals(name));
        } else {
            String firstExistingAttrId = null;
            List<BrandingAttributesEntity> toRemove = new ArrayList<>();
            for (BrandingAttributesEntity attr : entity.getAttributes()) {
                if (attr.getName().equals(name)) {
                    if (firstExistingAttrId == null) {
                        attr.setValue(value);
                        firstExistingAttrId = attr.getId();
                    } else {
                        toRemove.add(attr);
                    }
                }
            }

            if (firstExistingAttrId != null) {
                // Remove attributes through HQL to avoid StaleUpdateException
                Query query = em.createNamedQuery("deleteBrandingAttributesByNameAndBrandOtherThan");
                query.setParameter("name", name);
                query.setParameter("brandId", entity.getId());
                query.setParameter("attrId", firstExistingAttrId);
                int numUpdated = query.executeUpdate();

                // Remove attribute from local entity
                entity.getAttributes().removeAll(toRemove);
            } else {
                persistAttributeValue(name, value);
            }
        }
    }

    private void persistAttributeValue(String name, String value) {
        BrandingAttributesEntity attr = new BrandingAttributesEntity();
        attr.setId(KeycloakModelUtils.generateId());
        attr.setName(name);
        attr.setValue(value);
        attr.setBranding(entity);
        em.persist(attr);
        entity.getAttributes().add(attr);
    }

    @Override
    public void removeAttribute(String name) {
        Iterator<BrandingAttributesEntity> it = entity.getAttributes().iterator();
        while (it.hasNext()) {
            BrandingAttributesEntity attr = it.next();
            if (attr.getName().equals(name)) {
                it.remove();
                em.remove(attr);
            }
        }
    }

    @Override
    public String getAttribute(String name) {
        return getAttributes().get(name);
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty providerConfigProperty;

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("themeLayout");
        providerConfigProperty.setLabel("layout");
        providerConfigProperty.setType(ProviderConfigProperty.LIST_TYPE);
        providerConfigProperty.setGroupBy("layout");
        providerConfigProperty.setGroupLabel("Layout");
        providerConfigProperty.setOptions(new ArrayList<String>() {{
            add("Left");
            add("Right");
            add("Center");
        }});
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("themeLayoutBgType");
        providerConfigProperty.setLabel("section-background-type");
        providerConfigProperty.setType(ProviderConfigProperty.LIST_TYPE);
        providerConfigProperty.setGroupBy("section");
        providerConfigProperty.setGroupLabel("Section");
        providerConfigProperty.setDefaultValue("Text");
        providerConfigProperty.setOptions(new ArrayList<String>() {{
            add("Color");
            add("Image");
            add("Text");
        }});
        providerConfigProperty.setDependsOn(new HashMap<String, Object>() {{
            put("themeLayout", new String[]{"Left", "Right"});
        }});
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("themeLayoutBgColor");
        providerConfigProperty.setLabel("section-background-color");
        providerConfigProperty.setType(ProviderConfigProperty.COLOR_TYPE);
        providerConfigProperty.setGroupBy("section");
        providerConfigProperty.setGroupLabel("Section");
        providerConfigProperty.setDefaultValue("#000000");
        providerConfigProperty.setDependsOn(new HashMap<String, Object>() {{
            put("themeLayout", new String[]{"Left", "Right"});
            put("themeLayoutBgType", "Color");
        }});
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("themeLayoutBgImage");
        providerConfigProperty.setLabel("section-background-image");
        providerConfigProperty.setType(ProviderConfigProperty.IMAGE_TYPE);
        providerConfigProperty.setGroupBy("section");
        providerConfigProperty.setGroupLabel("Section");
        providerConfigProperty.setHelpText("section-background-img-hlp-txt");
        providerConfigProperty.setDependsOn(new HashMap<String, Object>() {{
            put("themeLayout", new String[]{"Left", "Right"});
            put("themeLayoutBgType", "Image");
        }});
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("themeLayoutTextColor");
        providerConfigProperty.setLabel("section-content-color");
        providerConfigProperty.setType(ProviderConfigProperty.COLOR_TYPE);
        providerConfigProperty.setGroupBy("section");
        providerConfigProperty.setGroupLabel("Section");
        providerConfigProperty.setDefaultValue("#ffffff");
        providerConfigProperty.setDependsOn(new HashMap<String, Object>() {{
            put("themeLayoutBgType", "Text");
            put("themeLayout", new String[]{"Left", "Right"});
        }});
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("themeLayoutOneLine");
        providerConfigProperty.setLabel("section-content-description");
        providerConfigProperty.setType(ProviderConfigProperty.STRING_TYPE);
        providerConfigProperty.setGroupBy("section");
        providerConfigProperty.setGroupLabel("Section");
        providerConfigProperty.setDependsOn(new HashMap<String, Object>() {{
            put("themeLayoutBgType", "Text");
            put("themeLayout", new String[]{"Left", "Right"});
        }});
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("themeLayoutFeatures");
        providerConfigProperty.setLabel("section-features-list");
        providerConfigProperty.setType(ProviderConfigProperty.MULTIVALUED_STRING_TYPE);
        providerConfigProperty.setGroupBy("section");
        providerConfigProperty.setGroupLabel("Section");
        providerConfigProperty.setMaxVal(5);
        providerConfigProperty.setHelpText("section-features-list-hlp-txt");
        providerConfigProperty.setDependsOn(new HashMap<String, Object>() {{
            put("themeLayoutBgType", "Text");
            put("themeLayout", new String[]{"Left", "Right"});
        }});
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("textBorderColor");
        providerConfigProperty.setLabel("textbox-border-color");
        providerConfigProperty.setGroupBy("form");
        providerConfigProperty.setGroupLabel("Form");
        providerConfigProperty.setType(ProviderConfigProperty.COLOR_TYPE);
        providerConfigProperty.setDefaultValue("#edeef1");
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("textActiveColor");
        providerConfigProperty.setLabel("textbox-active-color");
        providerConfigProperty.setGroupBy("form");
        providerConfigProperty.setGroupLabel("Form");
        providerConfigProperty.setType(ProviderConfigProperty.COLOR_TYPE);
        providerConfigProperty.setDefaultValue("#165df5");
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("textBoxBgColor");
        providerConfigProperty.setLabel("text-box-bg-color");
        providerConfigProperty.setGroupBy("form");
        providerConfigProperty.setGroupLabel("Form");
        providerConfigProperty.setType(ProviderConfigProperty.COLOR_TYPE);
        providerConfigProperty.setDefaultValue("#ffffff");
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("textBoxTextColor");
        providerConfigProperty.setLabel("text-box-text-color");
        providerConfigProperty.setGroupBy("form");
        providerConfigProperty.setGroupLabel("Form");
        providerConfigProperty.setType(ProviderConfigProperty.COLOR_TYPE);
        providerConfigProperty.setDefaultValue("#0b2238");
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("formTextColor");
        providerConfigProperty.setLabel("form-text-color");
        providerConfigProperty.setGroupBy("form");
        providerConfigProperty.setGroupLabel("Form");
        providerConfigProperty.setType(ProviderConfigProperty.COLOR_TYPE);
        providerConfigProperty.setDefaultValue("#0b2238");
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("formBgColor");
        providerConfigProperty.setLabel("form-background-color");
        providerConfigProperty.setGroupBy("form");
        providerConfigProperty.setGroupLabel("Form");
        providerConfigProperty.setType(ProviderConfigProperty.COLOR_TYPE);
        providerConfigProperty.setDefaultValue("#ffffff");
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("formBorderColor");
        providerConfigProperty.setLabel("form-border-color");
        providerConfigProperty.setGroupBy("form");
        providerConfigProperty.setGroupLabel("Form");
        providerConfigProperty.setType(ProviderConfigProperty.COLOR_TYPE);
        providerConfigProperty.setDefaultValue("#ffffff");
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("formBorderWidth");
        providerConfigProperty.setLabel("form-border-width");
        providerConfigProperty.setGroupBy("form");
        providerConfigProperty.setGroupLabel("Form");
        providerConfigProperty.setType(ProviderConfigProperty.NUMBER_TYPE);
        providerConfigProperty.setDefaultValue("1");
        providerConfigProperty.setMaxVal(24);
        providerConfigProperty.setMinVal(0);
        providerConfigProperty.setHelpText("set-form-border-width-hlp-txt");
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("formBorderRadius");
        providerConfigProperty.setLabel("form-border-radius");
        providerConfigProperty.setGroupBy("form");
        providerConfigProperty.setGroupLabel("Form");
        providerConfigProperty.setType(ProviderConfigProperty.NUMBER_TYPE);
        providerConfigProperty.setDefaultValue("10");
        providerConfigProperty.setMaxVal(120);
        providerConfigProperty.setMinVal(0);
        providerConfigProperty.setHelpText("set-form-border-radius-hlp-txt");
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("buttonBgColor");
        providerConfigProperty.setLabel("button-background-color");
        providerConfigProperty.setGroupBy("form");
        providerConfigProperty.setGroupLabel("Form");
        providerConfigProperty.setType(ProviderConfigProperty.COLOR_TYPE);
        providerConfigProperty.setDefaultValue("#1c64eb");
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("buttonBorderRadius");
        providerConfigProperty.setLabel("button-border-radius");
        providerConfigProperty.setGroupBy("form");
        providerConfigProperty.setGroupLabel("Form");
        providerConfigProperty.setType(ProviderConfigProperty.STRING_TYPE);
        providerConfigProperty.setDefaultValue("8");
        providerConfigProperty.setHelpText("set-in-pixels-hlp-txt");
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("buttonTextColor");
        providerConfigProperty.setLabel("button-text-color");
        providerConfigProperty.setGroupBy("form");
        providerConfigProperty.setGroupLabel("Form");
        providerConfigProperty.setType(ProviderConfigProperty.COLOR_TYPE);
        providerConfigProperty.setDefaultValue("#ffffff");
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("fontPreference");
        providerConfigProperty.setLabel(("font-preference"));
        providerConfigProperty.setGroupBy("layout");
        providerConfigProperty.setGroupLabel("Layout");
        providerConfigProperty.setType(ProviderConfigProperty.LIST_TYPE);
        providerConfigProperty.setDefaultValue("default");
        providerConfigProperty.setOptions(new ArrayList<String>() {{
            add("default");
            add("custom");
        }});
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("bodyFont");
        providerConfigProperty.setLabel("font");
        providerConfigProperty.setType(ProviderConfigProperty.LIST_TYPE);
        providerConfigProperty.setGroupBy("layout");
        providerConfigProperty.setGroupLabel("Layout");
        providerConfigProperty.setDefaultValue("Inter");
        providerConfigProperty.setHelpText("default-branding-font-helper-text");
        providerConfigProperty.setDependsOn(new HashMap<String, Object>() {{
            put("fontPreference", "default");
        }});
        providerConfigProperty.setOptions(new ArrayList<String>() {{
            add("Avenir Next W01");
            add("Proxima Nova W01");
            add("Rubik");
            add("Segoe UI");
            add("Roboto");
            add("Helvetica Neue");
            add("Arial");
            add("Inter");
        }});
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("customFontPreference");
        providerConfigProperty.setLabel("custom-font-preference");
        providerConfigProperty.setType(ProviderConfigProperty.LIST_TYPE);
        providerConfigProperty.setGroupBy("layout");
        providerConfigProperty.setGroupLabel("Layout");
        providerConfigProperty.setDefaultValue("link");
        providerConfigProperty.setDependsOn(new HashMap<String, Object>() {{
            put("fontPreference", "custom");
        }});
        providerConfigProperty.setOptions(new ArrayList<String>() {{
            add("link");
            add("file");
        }});
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("customFontLink");
        providerConfigProperty.setLabel("custom-font-link");
        providerConfigProperty.setType(ProviderConfigProperty.STRING_TYPE);
        providerConfigProperty.setGroupBy("layout");
        providerConfigProperty.setGroupLabel("Layout");
        providerConfigProperty.setHelpText("custom-font-link-helper-text");
        providerConfigProperty.setDependsOn(new HashMap<String, Object>() {{
            put("fontPreference", "custom");
            put("customFontPreference", "link");
        }});
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("customFontFile");
        providerConfigProperty.setLabel("custom-font-file");
        providerConfigProperty.setType(ProviderConfigProperty.FONT_FILE_TYPE);
        providerConfigProperty.setGroupBy("layout");
        providerConfigProperty.setGroupLabel("Layout");
        providerConfigProperty.setHelpText("custom-font-file-helper-text");
        providerConfigProperty.setDependsOn(new HashMap<String, Object>() {{
            put("fontPreference", "custom");
            put("customFontPreference", "file");
        }});
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("logoPosition");
        providerConfigProperty.setLabel("logo-position");
        providerConfigProperty.setGroupBy("form");
        providerConfigProperty.setGroupLabel("Form");
        providerConfigProperty.setType(ProviderConfigProperty.LIST_TYPE);
        providerConfigProperty.setDefaultValue("logo-left");
        providerConfigProperty.setOptions(new ArrayList<String>() {{
            add("logo-center");
            add("logo-left");
            add("logo-right");
        }});
        providerConfigProperty.setDependsOn(new HashMap<String, Object>() {{
            put("showLogoInForm", true);
        }});
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("logoHeight");
        providerConfigProperty.setLabel("logo-height");
        providerConfigProperty.setGroupBy("form");
        providerConfigProperty.setGroupLabel("Form");
        providerConfigProperty.setType(ProviderConfigProperty.NUMBER_TYPE);
        providerConfigProperty.setDefaultValue("38");
        providerConfigProperty.setMaxVal(100);
        providerConfigProperty.setHelpText("logo-height-hlp-txt");
        providerConfigProperty.setDependsOn(new HashMap<String, Object>() {{
            put("showLogoInForm", true);
        }});
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("logoPositionValue");
        providerConfigProperty.setLabel("custom-logo-position");
        providerConfigProperty.setType(ProviderConfigProperty.INPUT_GROUP);
        providerConfigProperty.setDefaultValue("0|0|0|0");
        providerConfigProperty.setGroupBy("form");
        providerConfigProperty.setGroupLabel("Form");
        providerConfigProperty.setHelpText("Top|Right|Bottom|Left");
        providerConfigProperty.setDependsOn(new HashMap<String, Object>() {{
            put("showLogoInForm", true);
        }});
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("logoPositionValueType");
        providerConfigProperty.setType(ProviderConfigProperty.INPUT_GROUP);
        providerConfigProperty.setDefaultValue("px");
        providerConfigProperty.setGroupBy("form");
        providerConfigProperty.setGroupLabel("Form");
        providerConfigProperty.setHelpText("Type");
        providerConfigProperty.setDependsOn(new HashMap<String, Object>() {{
            put("showLogoInForm", true);
        }});
        providerConfigProperty.setOptions(new ArrayList<String>() {{
            add("px");
            add("%");
            add("rem");
        }});
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("bodyBgType");
        providerConfigProperty.setLabel("body-background-type");
        providerConfigProperty.setGroupBy("layout");
        providerConfigProperty.setGroupLabel("Layout");
        providerConfigProperty.setType(ProviderConfigProperty.LIST_TYPE);
        providerConfigProperty.setDefaultValue("Image");
        providerConfigProperty.setOptions(new ArrayList<String>() {{
            add("Color");
            add("Image");
        }});
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("bodyBgColor");
        providerConfigProperty.setLabel("body-background-color");
        providerConfigProperty.setType(ProviderConfigProperty.COLOR_TYPE);
        providerConfigProperty.setGroupBy("layout");
        providerConfigProperty.setGroupLabel("Layout");
        providerConfigProperty.setDefaultValue("#000000");
        providerConfigProperty.setDependsOn(new HashMap<String, Object>() {{
            put("bodyBgType", "Color");
        }});
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("bodyBgImage");
        providerConfigProperty.setLabel("body-background-image");
        providerConfigProperty.setGroupBy("layout");
        providerConfigProperty.setGroupLabel("Layout");
        providerConfigProperty.setHelpText("section-background-img-hlp-txt");
        providerConfigProperty.setType(ProviderConfigProperty.IMAGE_TYPE);
        providerConfigProperty.setDependsOn(new HashMap<String, Object>() {{
            put("bodyBgType", "Image");
        }});
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("bodyBgImageOpacity");
        providerConfigProperty.setLabel("body-image-opacity");
        providerConfigProperty.setGroupBy("layout");
        providerConfigProperty.setGroupLabel("Layout");
        providerConfigProperty.setType(ProviderConfigProperty.DECIMAL_TYPE);
        providerConfigProperty.setDefaultValue("0.25");
        providerConfigProperty.setHelpText("body-image-opacity-hlp-txt");
        providerConfigProperty.setMaxVal(1);
        providerConfigProperty.setMinVal(0);
        providerConfigProperty.setDependsOn(new HashMap<String, Object>() {{
            put("bodyBgType", "Image");
        }});
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("showFooter");
        providerConfigProperty.setLabel("show-footer");
        providerConfigProperty.setGroupBy("footer");
        providerConfigProperty.setGroupLabel("Footer");
        providerConfigProperty.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        providerConfigProperty.setDefaultValue(true);
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("footerText");
        providerConfigProperty.setLabel("footer-text");
        providerConfigProperty.setGroupBy("footer");
        providerConfigProperty.setGroupLabel("Footer");
        providerConfigProperty.setType(ProviderConfigProperty.STRING_TYPE);
        providerConfigProperty.setDefaultValue("From Grootan");
        providerConfigProperty.setDependsOn(new HashMap<String, Object>() {{
            put("showFooter", true);
        }});
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("footerTextColor");
        providerConfigProperty.setLabel("footer-text-color");
        providerConfigProperty.setGroupBy("footer");
        providerConfigProperty.setGroupLabel("Footer");
        providerConfigProperty.setType(ProviderConfigProperty.COLOR_TYPE);
        providerConfigProperty.setDefaultValue("#6c7a87");
        providerConfigProperty.setDependsOn(new HashMap<String, Object>() {{
            put("showFooter", true);
        }});
        configProperties.add(providerConfigProperty);
    }

    @Override
    public List<ProviderConfigProperty> getConfig() {
        return configProperties;
    }

    @Override
    public void setCreatedTimeStamp(long timeStamp) {
        entity.setCreatedTimestamp(timeStamp);
    }

    @Override
    public long getCreatedTimeStamp() {
        return entity.getCreatedTimestamp();
    }


    private String getImageAbsURL(String relUrl) {
        if (relUrl.startsWith("http")) {  //for older URLs
            return relUrl;
        } else {
            return ModelToRepresentation.getDashboardUrl() + "/" + relUrl;
        }
    }

}