package com.peopleapp.configuration;

import com.peopleapp.util.ApplicationLogger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
@ComponentScan(basePackages = {"com.peopleapp.util"})
public class LoggerConfig {

    @Bean
    public ApplicationLogger applicationLogger() {
        return new ApplicationLogger();
    }
}
