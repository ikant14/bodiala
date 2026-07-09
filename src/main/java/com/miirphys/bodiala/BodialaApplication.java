package com.miirphys.bodiala;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BodialaApplication {

    public static void main(String[] args) {
        SpringApplication.run(BodialaApplication.class, args);
    }

}
