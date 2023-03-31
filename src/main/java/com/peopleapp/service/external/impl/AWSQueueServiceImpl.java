package com.peopleapp.service.external.impl;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peopleapp.dto.ApplicationConfigurationDTO;
import com.peopleapp.dto.SQSPayload;
import com.peopleapp.service.QueueService;
import com.peopleapp.util.PeopleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AWSQueueServiceImpl implements QueueService {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
    private static final String SMS = "SMS";
    private static final String NOTIFICATION = "NOTIFICATION";

	@Inject
	private AmazonSQS amazonSQSAsync;

	@Inject
	private ApplicationConfigurationDTO properties;

	@Value("${aws.access-key}")
	private String awsAccessKey;

	@Value("${aws.secret-key}")
	private String awsSecretKey;

	@Async
	@Override
	public void sendPayloadToSQS(List<SQSPayload> payloadList) {

		for (SQSPayload payload : PeopleUtils.emptyIfNull(payloadList)) {
			sendMessageToSQS(payload);
		}

	}

	@Async
	@Override
	public void sendPayloadToSQS(SQSPayload payload) {

		sendMessageToSQS(payload);
	}

	private void sendMessageToSQS(SQSPayload payload) {
		try {
			
			BasicAWSCredentials basicAwsCredentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);

	        AmazonSNS sns = AmazonSNSClient
	                .builder()
	                .withRegion(Regions.US_WEST_2)
	                .withCredentials(new AWSStaticCredentialsProvider(basicAwsCredentials))
	                .build();

	        String messageId = null;
	        
            String notificationMode = payload.getNotificationMode();
            if (SMS.equals(notificationMode)) {
                messageId = callSNSToSendSMS(sns, payload);
            } else if (NOTIFICATION.equals(notificationMode)) {
                messageId = callSNSToSendNotification(sns, payload);
            } 
            logger.error("Message send successfully (message Id : "+messageId+")");
            
		} catch (Exception ex) {
			logger.error("error converting sqs payload to string");
		}
	}
	
	private String callSNSToSendSMS(AmazonSNS sns, SQSPayload payload) {

        PublishResult result = sns.publish(new PublishRequest()
                .withMessage(payload.getMessage())
                .withPhoneNumber(payload.getRecipient())
                .withMessageAttributes(payload.getMessageAttributes()));


        return result != null ? result.getMessageId() : null;
    }

    private String callSNSToSendNotification(AmazonSNS sns, SQSPayload payload) {
        String message;
        if(payload.getDeviceTypeId() == 1){
            message = "{\"GCM\": \"" + payload.getMessage() + "\"}";
        }
        else{
            message = "{\"APNS_SANDBOX\": \"" + payload.getMessage() + "\"}";
        }
        PublishResult result = sns.publish(new PublishRequest()
                .withMessage(message)
                .withTargetArn(payload.getRecipient())
                .withMessageStructure("json")
        );

        return result != null ? result.getMessageId() : null;
    }
    
}
