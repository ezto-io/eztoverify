package io.ezto.core.config;

import io.ezto.core.loaders.EmailProviderLoaders;
import io.ezto.core.loaders.EmailTemplateLoaders;
import io.ezto.core.loaders.JobProviderLoaders;
import io.ezto.bg.core.IEmailProvider;
import io.ezto.bg.core.IEmailTemplate;
import io.ezto.bg.core.IJobProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

@Configuration
@Order(1)
public class LibraryConfig {

    @Value("${library.directory}")
    private String libraryDirectory;

    @Bean
    public ClassLoader libraryClassLoader() throws Exception {
        List<URL> urls = new ArrayList<>();
        File libDir = new File(libraryDirectory);
        if(!libDir.exists())
            return null;

        for (File file : libDir.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".jar")) {
                urls.add(file.toURI().toURL());
            }
        }
        return new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());
    }


    @Bean
    public EmailProviderLoaders emailProviderLoaders() throws Exception {
        if(libraryClassLoader() == null)
            return new EmailProviderLoaders();
        Thread.currentThread().setContextClassLoader(libraryClassLoader());
        return new EmailProviderLoaders(ServiceLoader.load(IEmailProvider.class, libraryClassLoader()));
    }

    @Bean
    public EmailTemplateLoaders emailTemplateLoaders() throws Exception {
        if(libraryClassLoader() == null)
            return new EmailTemplateLoaders();
        Thread.currentThread().setContextClassLoader(libraryClassLoader());
        return new EmailTemplateLoaders(ServiceLoader.load(IEmailTemplate.class, libraryClassLoader()));
    }

    @Bean
    public JobProviderLoaders jobProviderLoaders() throws Exception {
        if(libraryClassLoader() == null)
            return new JobProviderLoaders();
        Thread.currentThread().setContextClassLoader(libraryClassLoader());
        return new JobProviderLoaders(ServiceLoader.load(IJobProvider.class, libraryClassLoader()));
    }
}
