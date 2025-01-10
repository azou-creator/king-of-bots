package com.kob.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaAuditing
@EnableJpaRepositories("com.kob.backend.repository")
@EntityScan("com.kob.backend.entity")
public class KingOfBotsApplication {

    public static void main(String[] args) {
        SpringApplication.run(KingOfBotsApplication.class, args);
    }

}
