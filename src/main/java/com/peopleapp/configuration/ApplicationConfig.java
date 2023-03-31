package com.peopleapp.configuration;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersResult;
import com.peopleapp.dto.ApplicationConfigurationDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class ApplicationConfig {

    @Inject
    private ConfigurableEnvironment environment;

    @Value("${aws.access-key}")
    private String awsAccessKey;

    @Value("${aws.secret-key}")
    private String awsSecretKey;

    @Value("${aws.ssm.env.hierarchy}")
    private String ssmHierarchy;

    private static final String LOCAL = "local";
    private static final String TEST = "test";
    private static final String SPRING_PROFILES_ACTIVE = "spring.profiles.active";

    private static final String MONGO_URI = "mongoUri";
    private static final String REDIS_HOST = "redisHost";
    private static final String REDIS_PORT = "redisPort";
    private static final String S3_BASE_URL_NETWORK_CATEGORY = "s3BaseUrlNetworkCategory";
    private static final String SQS_URL = "sqsUrl";
    private static final String SNS_ARN_APNS = "snsArnAPNS";
    private static final String SNS_ARN_GCM = "snsArnGCM";
    private static final String SYSTEM_EMAIL_ID = "systemEmailId";

    @Bean(name="environmentProperties")
    public ApplicationConfigurationDTO setEnvironmentVariables(){
        ApplicationConfigurationDTO configurationProp = new ApplicationConfigurationDTO();
        if (environment.getProperty(SPRING_PROFILES_ACTIVE).equals(TEST)) {
            return configurationProp;
        }
        mapEnvironmentProperties();
        configurationProp.setMongoUri(environment.getProperty(getParamKey(MONGO_URI)));
        configurationProp.setRedisHost(environment.getProperty(getParamKey(REDIS_HOST)));
        configurationProp.setRedisPort(environment.getProperty(getParamKey(REDIS_PORT)));
        configurationProp.setS3BaseUrlNetworkCategory(environment.getProperty(getParamKey(S3_BASE_URL_NETWORK_CATEGORY)));
        configurationProp.setSnsArnAPNS(environment.getProperty(getParamKey(SNS_ARN_APNS)));
        configurationProp.setSnsArnGCM(environment.getProperty(getParamKey(SNS_ARN_GCM)));
        configurationProp.setSqsURL(environment.getProperty(getParamKey(SQS_URL)));
        configurationProp.setSystemEmailId(environment.getProperty(getParamKey(SYSTEM_EMAIL_ID)));
        return configurationProp;
    }

    private AWSSimpleSystemsManagement awsClient() {

        AWSSimpleSystemsManagement awsClient = null;

        if (environment.getProperty(SPRING_PROFILES_ACTIVE).equals(LOCAL)) {
            BasicAWSCredentials awsCreds = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
            awsClient = AWSSimpleSystemsManagementClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(awsCreds)).withRegion(Regions.US_WEST_2)
                    .build();
        } else {
            awsClient = AWSSimpleSystemsManagementClientBuilder.standard().withRegion(Regions.US_WEST_2).build();
        }

        return awsClient;
    }

    private void mapEnvironmentProperties() {

        AWSSimpleSystemsManagement awsClient = awsClient();

        Map<String, Object> props = new HashMap<>();

        GetParametersRequest paramRequest = new GetParametersRequest()
                .withNames(getParamKey(MONGO_URI), getParamKey(S3_BASE_URL_NETWORK_CATEGORY)
                        , getParamKey(SQS_URL), getParamKey(SNS_ARN_APNS), getParamKey(SNS_ARN_GCM)
                        , getParamKey(REDIS_HOST), getParamKey(REDIS_PORT), getParamKey(SYSTEM_EMAIL_ID))
                .withWithDecryption(true);

        GetParametersResult parameters = awsClient.getParameters(paramRequest);
        parameters.getParameters().forEach(parameter ->
                props.put(parameter.getName(), parameter.getValue())
        );

        MapPropertySource mapSource = new MapPropertySource("aws-ssm", props);
        environment.getPropertySources().addFirst(mapSource);
    }

    private String getParamKey(String param){
        return String.format(ssmHierarchy, param);
    }
}
