package com.omnip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories("com.omnip.repositories")
@ComponentScan(basePackages = "com.omnip")
public class OmnipAuthenticationServiceApplication {

    static void main(String[] args) {
        SpringApplication app = new SpringApplication(OmnipAuthenticationServiceApplication.class);
        
        // Optimize startup time
        app.setAdditionalProfiles("default");
        app.setRegisterShutdownHook(false);
        app.setLogStartupInfo(false);
        
        app.run(args);
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();
        String serverPort = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        String host = "localhost";

        System.out.println("\n" +
            "==========================================\n" +
            "Application is ready!\n" +
            "Local URL: http://" + host + ":" + serverPort + contextPath + "\n" +
            "==========================================");
    }

}
