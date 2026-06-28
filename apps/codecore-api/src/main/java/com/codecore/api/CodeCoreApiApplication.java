package com.codecore.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.codecore.api", "com.codecore.iam", "com.codecore.organization"})
public class CodeCoreApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeCoreApiApplication.class, args);
    }

}