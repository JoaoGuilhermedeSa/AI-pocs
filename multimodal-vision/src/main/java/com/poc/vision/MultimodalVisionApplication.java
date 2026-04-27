package com.poc.vision;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class MultimodalVisionApplication {
    public static void main(String[] args) {
        SpringApplication.run(MultimodalVisionApplication.class, args);
    }
}
