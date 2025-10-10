package com.aurionpro.papms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PaPmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaPmsApplication.class, args);
    }

}
