package com.example.orderservicefeb25.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate(InternalServiceAuthInterceptor interceptor) {
        RestTemplate template = new RestTemplate();
        template.getInterceptors().add(interceptor);
        return template;
    }
}
