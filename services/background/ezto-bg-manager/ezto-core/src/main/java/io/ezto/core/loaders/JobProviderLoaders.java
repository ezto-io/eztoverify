package io.ezto.core.loaders;


import io.ezto.bg.core.IJobProvider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;


public class JobProviderLoaders {

    ServiceLoader<IJobProvider> loaders;

    public JobProviderLoaders(ServiceLoader<IJobProvider> loaders)
    {
        this.loaders = loaders;
    }

    public JobProviderLoaders(){}


    public List<IJobProvider> getLoaders() {
        if(loaders == null)
            return new ArrayList<>();
        List<IJobProvider> services = new ArrayList<>();
        Iterator<IJobProvider> iterator = loaders.iterator();
        while (iterator.hasNext()) {
            services.add(iterator.next());
        }
        return services;
    }
}
