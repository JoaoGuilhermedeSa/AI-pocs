package com.poc.assistant;

import com.poc.assistant.google.GoogleApiFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PersonalAiAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(PersonalAiAssistantApplication.class, args);
    }

    @Bean
    CommandLineRunner initGoogle(GoogleApiFactory googleApiFactory) {
        return args -> googleApiFactory.initialize();
    }
}
