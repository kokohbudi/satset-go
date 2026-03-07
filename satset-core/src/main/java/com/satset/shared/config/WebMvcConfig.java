package com.satset.shared.config;

import com.satset.shared.interceptor.StoreOnboardingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final StoreOnboardingInterceptor storeOnboardingInterceptor;

    public WebMvcConfig(StoreOnboardingInterceptor storeOnboardingInterceptor) {
        this.storeOnboardingInterceptor = storeOnboardingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(storeOnboardingInterceptor)
                // We only want to apply this to actual application pages
                .addPathPatterns("/**")

                // Exclude static resources
                .excludePathPatterns(
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/webjars/**",
                        "/favicon.ico",
                        "/error")

                // Exclude authentication and onboarding paths
                .excludePathPatterns(
                        "/login",
                        "/logout",
                        "/api/logout",
                        "/oauth2/**", // Keycloak OAuth2 callback redirects
                        "/onboarding", // Let them see the onboarding page!
                        "/onboarding/**")

                // Exclude API paths (REST APIs generally shouldn't redirect to HTML pages)
                // If an API needs store validation, it should return 403 Forbidden instead of
                // 302 Redirect
                .excludePathPatterns("/api/**");
    }
}
