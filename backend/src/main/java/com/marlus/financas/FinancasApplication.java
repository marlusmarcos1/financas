package com.marlus.financas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.marlus.financas")
@EntityScan(basePackages = "com.marlus.financas")
public class FinancasApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinancasApplication.class, args);
    }
}
