package com.example.paymentservicefeb25;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PaymentServiceFeb25Application {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceFeb25Application.class, args);
    }

}
