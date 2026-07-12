package com.codecore.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.codecore.api",
        "com.codecore.iam",
        "com.codecore.organization",
        "com.codecore.patient",
        "com.codecore.appointment",
        "com.codecore.encounter",
        "com.codecore.inventory"
})
public class CodeCoreApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeCoreApiApplication.class, args);
    }

}