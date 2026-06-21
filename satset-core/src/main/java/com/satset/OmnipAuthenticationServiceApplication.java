package com.satset;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@SpringBootApplication
@EnableJpaRepositories(basePackages = {
        "com.satset.catalog.repository",
        "com.satset.transaction.repository",
        "com.satset.identity.repository",
        "com.satset.onboarding.repository"
})
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class OmnipAuthenticationServiceApplication {

    static void main(String[] args) {
        SpringApplication app = new SpringApplication(OmnipAuthenticationServiceApplication.class);

        // Optimize startup time
        app.setRegisterShutdownHook(false);
        app.setLogStartupInfo(false);

        app.run(args);
    }

}
