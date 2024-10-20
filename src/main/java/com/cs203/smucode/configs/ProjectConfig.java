package com.cs203.smucode.configs;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(
    basePackages = {
        "com.cs203.smucode.controllers",
        "com.cs203.smucode.services",
        "com.cs203.smucode.repositories"
    }
)
public class ProjectConfig {}
