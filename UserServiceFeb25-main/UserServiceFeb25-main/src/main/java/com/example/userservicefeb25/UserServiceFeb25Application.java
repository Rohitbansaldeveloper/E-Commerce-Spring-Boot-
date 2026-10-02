package com.example.userservicefeb25;

import jakarta.persistence.EntityListeners;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableScheduling
@EnableJpaAuditing
public class UserServiceFeb25Application {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceFeb25Application.class, args);
    }

}
