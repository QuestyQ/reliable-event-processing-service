package com.example.reliableevents;

import com.example.reliableevents.config.RetryProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RetryProperties.class)
public class ReliableEventProcessingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReliableEventProcessingServiceApplication.class, args);
    }
}
