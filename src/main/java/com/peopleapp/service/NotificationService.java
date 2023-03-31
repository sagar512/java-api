package com.peopleapp.service;

import com.peopleapp.dto.ContactNumberDTO;
import com.peopleapp.dto.PushNotificationDTO;
import com.peopleapp.dto.SQSPayload;
import com.peopleapp.model.UserActivity;

public interface NotificationService {

    void prepareSMSPayloadAndSendToQueue(ContactNumberDTO contactNumber, String messageKey, Object[] messageParam);

    SQSPayload prepareSQSPayloadForSMS(ContactNumberDTO contactNumber, String messageKey, Object[] messageParam);

    SQSPayload prepareSQSPayloadForNotification(String userId, PushNotificationDTO pushNotificationDTO);

    SQSPayload prepareSQSPayloadForSilentNotification(String userId, String activityRequestType,
                                                      String activityId, String connectionId, String networkId);

    String getSingleIntroducedUserName(UserActivity userActivity);

    String preparePartStringIntroductionReceived(UserActivity userActivity);
}
