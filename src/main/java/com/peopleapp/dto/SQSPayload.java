package com.peopleapp.dto;

import com.amazonaws.services.sns.model.MessageAttributeValue;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class SQSPayload {

    private static final String SMS = "SMS";

    private static final String NOTIFICATION = "NOTIFICATION";

    private String notificationMode;

    private String message;

    private String recipient;

    private int deviceTypeId;

    private Map<String, MessageAttributeValue> messageAttributes;

    public SQSPayload(String notificationMode, String recipient, String message) {
        this.notificationMode = notificationMode;
        this.message = message;
        this.recipient = recipient;
        this.messageAttributes = setAttribute(notificationMode);
    }

    public SQSPayload(String notificationMode, String recipient, String message, int deviceTypeId) {
        this.notificationMode = notificationMode;
        this.message = message;
        this.recipient = recipient;
        this.messageAttributes = setAttribute(notificationMode);
        this.deviceTypeId = deviceTypeId;
    }

    private Map<String, MessageAttributeValue> prepareMessageAttributes() {
        Map<String, MessageAttributeValue> smsAttributes = new HashMap<>();

        //The sender ID shown on the device.It can be alphanumeric.
        smsAttributes.put("AWS.SNS.SMS.SenderID", new MessageAttributeValue()
                .withStringValue("APPPEOP")
                .withDataType("String"));

        //Sets the max price to 0.50 USD.
        smsAttributes.put("AWS.SNS.SMS.MaxPrice", new MessageAttributeValue()
                .withStringValue("0.10")
                .withDataType("Number"));

        //Sets the type to transactional.
        smsAttributes.put("AWS.SNS.SMS.SMSType", new MessageAttributeValue()
                .withStringValue("Transactional")
                .withDataType("String"));
        return smsAttributes;
    }

    private Map<String, MessageAttributeValue> setAttribute(String notificationMode) {
        return SMS.equals(notificationMode) ?
                prepareMessageAttributes() : null;
    }
}
