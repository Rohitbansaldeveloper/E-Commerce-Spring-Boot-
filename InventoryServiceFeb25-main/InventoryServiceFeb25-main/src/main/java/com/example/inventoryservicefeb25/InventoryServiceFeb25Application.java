package com.example.inventoryservicefeb25;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InventoryServiceFeb25Application {
    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceFeb25Application.class, args);
    }
}
