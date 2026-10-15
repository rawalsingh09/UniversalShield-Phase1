package com.universalshield;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point of the UniversalShield application.
 * Starts the embedded web server, the REST API and the static dashboard.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class UniversalShieldApplication {

    public static void main(String[] args) {
        SpringApplication.run(UniversalShieldApplication.class, args);
    }
}
