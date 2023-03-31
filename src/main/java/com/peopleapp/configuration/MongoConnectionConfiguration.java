package com.peopleapp.configuration;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.peopleapp.dto.ApplicationConfigurationDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@Profile("!test")
@EnableMongoRepositories("com.peopleapp.repository")
public class MongoConnectionConfiguration extends AbstractMongoConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String database;

    @Value("${spring.data.mongodb.minConnections}")
    private String minConnections;

    @Value("${spring.data.mongodb.maxConnections}")
    private String maxConnections;

    private ApplicationConfigurationDTO applicationProperties;
    private ConfigurableEnvironment environment;

    private static final String LOCAL = "local";
    private static final String SPRING_PROFILES_ACTIVE = "spring.profiles.active";

    public MongoConnectionConfiguration(ApplicationConfigurationDTO applicationProperties,
                                        ConfigurableEnvironment environment){
        this.applicationProperties = applicationProperties;
        this.environment = environment;
    }

    @Override
    public String getDatabaseName() {
        return database;
    }

    @Override
    @Bean
    @DependsOn("environmentProperties")
    public MongoClient mongoClient() {

        // Configuring MongoClientBuilder
        MongoClientOptions.Builder builder = MongoClientOptions.builder();
        builder.minConnectionsPerHost(Integer.parseInt(minConnections));
        builder.connectionsPerHost(Integer.parseInt(maxConnections));

        MongoClientURI mongoClientURI;

        if(environment.getProperty(SPRING_PROFILES_ACTIVE).equals(LOCAL)) {
            mongoClientURI = new MongoClientURI(mongoUri, builder);
        }else{
            mongoClientURI = new MongoClientURI(applicationProperties.getMongoUri(), builder);
        }

        return new MongoClient(mongoClientURI);
    }

    @Override
    @Bean
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(mongoClient(), getDatabaseName());
    }

}
