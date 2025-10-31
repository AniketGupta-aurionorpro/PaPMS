package com.aurionpro.papms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@Slf4j
public class PaPmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaPmsApplication.class, args);
        log.info("PaPmsApplication started successfully!");
    }

}