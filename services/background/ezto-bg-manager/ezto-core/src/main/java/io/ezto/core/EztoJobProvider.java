package io.ezto.core;

import io.ezto.core.loaders.JobProviderLoaders;
import io.ezto.bg.core.IJobProvider;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

@Component
public class EztoJobProvider {


    ServiceLoader<IJobProvider> loader = ServiceLoader
            .load(IJobProvider.class);


    private Map<String, IJobProvider> providers;

    public EztoJobProvider(JobProviderLoaders providerLoaders) {
        providers = new HashMap<>();
        loader.stream().forEach(x -> providers.put(x.get().getProviderId(), x.get()));
        if (providerLoaders != null) {
            providerLoaders.getLoaders().stream().filter(x -> !providers.containsKey(x.getProviderId())).forEach(x -> providers.put(x.getProviderId(), x));
        }
    }


    public IJobProvider getJob(String templateId) {
         return providers.get(templateId);
    }

}
