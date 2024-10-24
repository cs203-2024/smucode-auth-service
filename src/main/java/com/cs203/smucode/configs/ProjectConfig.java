package com.cs203.smucode.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@ComponentScan(
    basePackages = {
        "com.cs203.smucode.controllers",
        "com.cs203.smucode.services",
        "com.cs203.smucode.repositories",
        "com.cs203.smucode.proxies"
    }
)
public class ProjectConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
