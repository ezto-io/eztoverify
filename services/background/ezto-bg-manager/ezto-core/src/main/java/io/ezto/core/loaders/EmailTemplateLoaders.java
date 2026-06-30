package io.ezto.core.loaders;

import io.ezto.bg.core.IEmailTemplate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public class EmailTemplateLoaders {

    ServiceLoader<IEmailTemplate> loaders;

    public EmailTemplateLoaders()
    {

    }

    public EmailTemplateLoaders(ServiceLoader<IEmailTemplate> loaders) {
        this.loaders = loaders;
    }

    public List<IEmailTemplate> getLoaders() {
        if(loaders == null)
            return new ArrayList<>();

        List<IEmailTemplate> services = new ArrayList<>();
        Iterator<IEmailTemplate> iterator = loaders.iterator();
        while (iterator.hasNext()) {
            services.add(iterator.next());
        }
        return services;
    }
}
