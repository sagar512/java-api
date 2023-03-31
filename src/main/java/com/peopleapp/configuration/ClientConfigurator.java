package com.peopleapp.configuration;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.ConfigurableEnvironment;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.peopleapp.dto.ApplicationConfigurationDTO;

import redis.clients.jedis.Jedis;

@Configuration
public class ClientConfigurator {

	@Inject
	private ConfigurableEnvironment environment;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Value("${aws.access-key}")
	private String awsAccessKey;

	@Value("${redis.host}")
	private String redisHost;

	@Value("${redis.port}")
	private String redisPort;

	@Value("${redis.password}")
	private String redisPassword;

	@Value("${aws.secret-key}")
	private String awsSecretKey;

	private static final String LOCAL = "local";
	private static final String TEST = "test";
	private static final String SPRING_PROFILES_ACTIVE = "spring.profiles.active";

	@Bean
	public AmazonSNS snsClient() {
		if (environment.getProperty(SPRING_PROFILES_ACTIVE).equals(LOCAL)) {
			BasicAWSCredentials basicAwsCredentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
			return AmazonSNSClient.builder().withRegion(Regions.US_WEST_2)
					.withCredentials(new AWSStaticCredentialsProvider(basicAwsCredentials)).build();
		} else {
			return AmazonSNSClientBuilder.standard().withRegion(Regions.US_WEST_2).build();
		}
	}

	@Bean
	public AmazonSimpleEmailService sesClient() {
		if (environment.getProperty(SPRING_PROFILES_ACTIVE).equals(LOCAL)) {
			BasicAWSCredentials basicAwsCredentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
			return AmazonSimpleEmailServiceClient.builder().withRegion(Regions.US_WEST_2)
					.withCredentials(new AWSStaticCredentialsProvider(basicAwsCredentials)).build();
		} else {
			return AmazonSimpleEmailServiceClientBuilder.standard().withRegion(Regions.US_WEST_2).build();
		}
	}

	@Bean
	public AmazonSQS sqsClient() {
		if (environment.getProperty(SPRING_PROFILES_ACTIVE).equals(LOCAL)) {
			BasicAWSCredentials basicAwsCredentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
			return AmazonSQSAsyncClient.builder().withRegion(Regions.US_WEST_2)
					.withCredentials(new AWSStaticCredentialsProvider(basicAwsCredentials)).build();
		} else {
			return AmazonSQSAsyncClientBuilder.standard().withRegion(Regions.US_WEST_2).build();
		}
	}

	@Bean
	@DependsOn("environmentProperties")
	public Jedis redisClient(ApplicationConfigurationDTO applicationProperties) {
		Jedis redisClient = null;
		if (environment.getProperty(SPRING_PROFILES_ACTIVE).equals(LOCAL)
				|| environment.getProperty(SPRING_PROFILES_ACTIVE).equals(TEST)) {
			redisClient = new Jedis(redisHost, Integer.parseInt(redisPort));
		} else {
			redisClient = new Jedis(applicationProperties.getRedisHost(),
					Integer.parseInt(applicationProperties.getRedisPort()));
		}
		if (redisPassword.length() > 0) {
			logger.info("Redis Set Password");
			redisClient.auth(redisPassword);
		} else {
			logger.info("Redis Not Set Password");
		}
		redisClient.connect();
		// using database 0
		redisClient.select(0);
		return redisClient;
	}
}
