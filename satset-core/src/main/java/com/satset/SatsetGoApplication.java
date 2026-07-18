package com.satset;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

// JPA repositories are enabled in CoreDataSourceConfig — one datasource/EMF/tx
// manager for all slices; wallet lives in its own satset_wallet schema.
@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class SatsetGoApplication {

    static void main(String[] args) {
        SpringApplication app = new SpringApplication(SatsetGoApplication.class);

        // Optimize startup time
        app.setRegisterShutdownHook(false);
        app.setLogStartupInfo(false);

        app.run(args);
    }

}
