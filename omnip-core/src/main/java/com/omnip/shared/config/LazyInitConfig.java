package com.omnip.shared.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.main.lazy-initialization", havingValue = "true")
public class LazyInitConfig {
    // Lazy initialization is enabled globally via application.yml
    // This configuration class can be used for additional lazy-specific beans if needed
}
