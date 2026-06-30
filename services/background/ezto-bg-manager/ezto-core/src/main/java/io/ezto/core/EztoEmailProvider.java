package io.ezto.core;

import io.ezto.core.loaders.EmailProviderLoaders;
import io.ezto.core.loaders.EmailTemplateLoaders;
import io.ezto.bg.core.IEmailProvider;
import io.ezto.bg.core.IEmailTemplate;
import io.ezto.bg.core.exceptions.MailValidationException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

@Component
public class EztoEmailProvider {

    ServiceLoader<IEmailProvider> loader = ServiceLoader
            .load(IEmailProvider.class);

    ServiceLoader<IEmailTemplate> templateLoader = ServiceLoader
            .load(IEmailTemplate.class);

    private static Map<String, IEmailProvider> providers;
    private static Map<String, IEmailTemplate> templateProviders;

    public EztoEmailProvider(EmailTemplateLoaders templateExtLoaders, EmailProviderLoaders emailExtLoaders) {
        providers = new HashMap<>();
        templateProviders = new HashMap<>();
        loader.stream().forEach(x -> providers.put(x.get().getProviderId(), x.get()));
        if(emailExtLoaders != null) {
            emailExtLoaders.getLoaders().stream().filter(x -> !providers.containsKey(x.getProviderId())).forEach(x -> providers.put(x.getProviderId(), x));
        }
        templateLoader.stream().forEach(x -> templateProviders.put(x.get().getId(), x.get()));
        if(templateExtLoaders != null) {
            templateExtLoaders.getLoaders().stream().filter(x -> !templateProviders.containsKey(x.getId())).forEach(x -> templateProviders.put(x.getId(), x));
        }
    }

    public static IEmailProvider getEmailProvider(String provider) throws MailValidationException {
        return providers.get(provider);
    }

    public static IEmailTemplate getEmailTemplate(String templateId) throws MailValidationException {
        return templateProviders.get(templateId);
    }

}
