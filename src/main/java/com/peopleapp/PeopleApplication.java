package com.peopleapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * {@link PeopleApplication} is the entry point
 * for PeopleApplication
 */
@SpringBootApplication
@EnableScheduling
public class PeopleApplication {

    public static void main(String[] args) {

        SpringApplication.run(PeopleApplication.class, args);
    }

}
