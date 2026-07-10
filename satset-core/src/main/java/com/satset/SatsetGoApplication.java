package com.satset;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

// JPA repositories are enabled per-datasource in CoreDataSourceConfig (core) and
// WalletDataSourceConfig (wallet), each bound to its own EMF + transaction manager.
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
