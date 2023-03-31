package com.peopleapp.dto;

import com.peopleapp.enums.RequestType;
import lombok.Data;

@Data
public class PushNotificationDTO {

    private String activityId;

    private RequestType activityRequestType;

    private String initiatorName;

    private String receiverUserId;

    private int numberOfContacts;

    private String activityMessage;

    private String connectionId;

    private String networkName;

    private String networkId;

    private Boolean isNetworkNotification = Boolean.FALSE;
}
