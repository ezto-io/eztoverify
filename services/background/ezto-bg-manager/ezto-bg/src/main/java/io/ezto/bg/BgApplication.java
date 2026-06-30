package io.ezto.bg;


import io.ezto.bg.config.JobRunrConfiguration;
import io.ezto.core.config.LibraryConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.Import;

@SpringBootApplication()
@Import({JobRunrConfiguration.class, LibraryConfig.class})
@ComponentScans({
        @ComponentScan("io.ezto.helpers"),
        @ComponentScan("io.ezto.core"),
        @ComponentScan("io.ezto.extensions"),
        @ComponentScan("io.ezto.email"),
        @ComponentScan("io.ezto.hook"),
        @ComponentScan("io.ezto.license"),
        @ComponentScan("io.ezto.scheduler"),
        @ComponentScan("io.ezto.bg"),
        @ComponentScan("io.ezto.sync"),
})
@ComponentScan("io.ezto")
@EnableConfigurationProperties
public class BgApplication {

    public static void main(String[] args) {
        SpringApplication.run(BgApplication.class, args);
    }


}
