package com.vinhtran.dogbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.vinhtran.dogbot")
@EnableScheduling
public class DogBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(DogBotApplication.class, args);
    }
}
