package com.young;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LoanMsApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanMsApplication.class, args);
    }

}
