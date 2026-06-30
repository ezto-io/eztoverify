package io.ezto.core.loaders;


import io.ezto.bg.core.IEmailProvider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public class EmailProviderLoaders {
    ServiceLoader<IEmailProvider> loaders;

    public EmailProviderLoaders(){

    }

    public EmailProviderLoaders(ServiceLoader<IEmailProvider> loaders) {
        this.loaders = loaders;
    }

    public List<IEmailProvider> getLoaders() {
        if(loaders == null)
            return new ArrayList<>();

        List<IEmailProvider> services = new ArrayList<>();
        Iterator<IEmailProvider> iterator = loaders.iterator();
        while (iterator.hasNext()) {
            services.add(iterator.next());
        }
        return services;
    }
}
