package com.peopleapp.service.external.impl;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.*;
import com.peopleapp.dto.ApplicationConfigurationDTO;
import com.peopleapp.service.EmailService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmailServiceImpl implements EmailService {

    @Inject
    private AmazonSimpleEmailService emailServiceClient;

    @Inject
    private ApplicationConfigurationDTO properties;

    @Override
    @Async
    public void sendEmail(String subject, String mailBody, String emailId) {

        List<String> recipients = new ArrayList<>();
        recipients.add(emailId);

        Destination destination = new Destination(recipients);
        Content subjectContent = new Content().withData(subject);
        Message msg = new Message().withSubject(subjectContent);

        Content htmlContent = new Content().withData(mailBody);
        Body emailBody = new Body().withHtml(htmlContent);
        msg.setBody(emailBody);

        SendEmailRequest request = new SendEmailRequest(properties.getSystemEmailId(), destination, msg);
        emailServiceClient.sendEmail(request);
    }


    @Override
    @Async
    public void sendTemplatedEmail(String templateName, String templateData, String emailId) {

        List<String> recipients = new ArrayList<>();
        recipients.add(emailId);

        Destination destination = new Destination(recipients);

        SendTemplatedEmailRequest emailRequest = new SendTemplatedEmailRequest();
        emailRequest.setSource(properties.getSystemEmailId());
        emailRequest.setDestination(destination);
        emailRequest.setTemplate(templateName);
        emailRequest.setTemplateData(templateData);

        emailServiceClient.sendTemplatedEmail(emailRequest);

    }

}
