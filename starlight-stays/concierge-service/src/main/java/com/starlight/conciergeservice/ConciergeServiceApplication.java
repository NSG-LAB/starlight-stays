package com.starlight.conciergeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ConciergeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConciergeServiceApplication.class, args);
    }
}
