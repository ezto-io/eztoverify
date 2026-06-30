package com.qlikverify.services.backgroundService.backgroundEmailService;

import com.qlikverify.services.backgroundService.BackgroundJobRequestBody;
import org.keycloak.models.Constants;
import org.keycloak.models.UserModel;

import java.util.Locale;
import java.util.Map;

public class BackgroundEmailRequestBody implements BackgroundJobRequestBody {
    public final String provider;
    public final String template;
    public final String to;
    public final String subject;
    public final Map<String, Object> template_attributes;
    public final Map<String, Object> provider_attributes;

    public final Locale locale;


    public BackgroundEmailRequestBody(String provider, String template, String to, String subject, Map<String, Object> provider_attributes, Map<String, Object> template_attributes, Locale locale) {
        this.provider = provider;
        this.template = template;
        this.to = to;
        this.subject = subject;
        this.template_attributes = template_attributes;
        this.provider_attributes = provider_attributes;
        this.locale = locale;
    }
}
