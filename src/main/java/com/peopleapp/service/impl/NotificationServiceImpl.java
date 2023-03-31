package com.peopleapp.service.impl;

import com.peopleapp.configuration.LocaleMessageReader;
import com.peopleapp.constant.NotificationTemplateKeys;
import com.peopleapp.dto.ContactNumberDTO;
import com.peopleapp.dto.PushNotificationDTO;
import com.peopleapp.dto.SQSPayload;
import com.peopleapp.enums.DeviceType;
import com.peopleapp.enums.ErrorCode;
import com.peopleapp.enums.RequestType;
import com.peopleapp.enums.UserStatus;
import com.peopleapp.exception.BadRequestException;
import com.peopleapp.model.*;
import com.peopleapp.repository.*;
import com.peopleapp.security.TokenAuthService;
import com.peopleapp.service.*;
import com.peopleapp.util.PeopleUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String SMS = "SMS";
    private static final String PUSH_NOTIFICATION = "NOTIFICATION";
    private static final String TITLE = "title";
    private static final String BODY = "body";
    private static final String ALERT = "alert";
    private static final String CONTENT_AVAILABLE = "content-available";
    private static final String APS = "aps";
    private static final String DATA = "data";
    private static final String ACTIVITY_ID = "activityId";
    private static final String ACTIVITY_TYPE = "activityType";
    private static final String CONNECTION_ID = "connectionId";
    private static final String NETWORK_ID = "networkId";
    private static final String BADGE_COUNT = "badge";

    @Inject
    private UserSessionRepository userSessionRepository;

    @Inject
    private QueueService queueService;

    @Inject
    private MasterService masterService;

    @Inject
    private LocaleMessageReader messages;

    @Inject
    private TokenAuthService tokenAuthService;

    @Inject
    private ActivityContactRepository activityContactRepository;

    @Inject
    private PeopleUserRepository peopleUserRepository;

    @Inject
    private UserActivityRepository userActivityRepository;

    @Inject
    private UserConnectionRepository userConnectionRepository;

    @Inject
    private TempSessionService tempSessionService;

    @Inject
    private PeopleUserService peopleUserService;

    @Override
    public void prepareSMSPayloadAndSendToQueue(ContactNumberDTO contactNumber, String messageKey, Object[] messageParam) {

        String message = messages.get(messageKey, messageParam);
        List<SQSPayload> payloadList = new ArrayList<>();

        SQSPayload payload = new SQSPayload(
                SMS,
                contactNumber.getMobileNumber(),
                message);
        payloadList.add(payload);

        queueService.sendPayloadToSQS(payloadList);

    }

    @Override
    public SQSPayload prepareSQSPayloadForSMS(ContactNumberDTO contactNumber, String messageKey, Object[] messageParam) {

        String message = messages.get(messageKey, messageParam);

        return new SQSPayload(
                SMS,
                contactNumber.getMobileNumber(),
                message);

    }

    /*
     * UserId - belongs to the user how will be receiving the push notification
     * */
    @Override
    public SQSPayload prepareSQSPayloadForNotification(String userId, PushNotificationDTO pushNotificationDTO) {
        PeopleUser sessionUser = null;
        try {
            sessionUser = tokenAuthService.getSessionUser();
        } catch (Exception e) {
            /* in case of verify email API call no session token is available and hence the user details are fetched by connection */
            List<UserConnection> userConnection = userConnectionRepository.findConnectionByConnectionId(
                    Collections.singletonList(pushNotificationDTO.getConnectionId()));
            sessionUser = peopleUserRepository.findByuserId(userConnection.get(0).getConnectionToId(), UserStatus.ACTIVE.getValue());
        }

        /* in case of update primary number session token will not be available session user is found by temp token*/
        if (sessionUser == null) {
            String tempToken = tokenAuthService.getTempToken();
            TemporarySession temporarySession = tokenAuthService.getTempSessionByTempToken(tempToken);
            ContactNumberDTO number = temporarySession.getContactNumber();
            sessionUser = peopleUserService.findUserByContactNumber(number);
        }

        String activityDescription = prepareActivityDescription(pushNotificationDTO);

        // get device arn endpoint  if push notification is enabled
        UserSession activeSession = userSessionRepository.findActiveSession(PeopleUtils.convertStringToObjectId(userId));
        long receivedNotificationCountForTargetUser = userActivityRepository.countOfUnreadActivitiesForGivenUser(userId);
        String message = null;
        // for network notifications, we dont have to consider block user scenario
        if (activeSession != null && activeSession.getDeviceTypeId() != null &&
                (pushNotificationDTO.getIsNetworkNotification() || !masterService.isUserBlockedByContact(userId,
                        sessionUser.getUserId()))) {

            if (activeSession.getDeviceTypeId().intValue() == DeviceType.ANDROID.getType()) {
                message = createGCMNotificationJSONRequest(pushNotificationDTO.getActivityMessage(),
                        activityDescription, pushNotificationDTO.getActivityId(),
                        pushNotificationDTO.getActivityRequestType().getValue(), pushNotificationDTO.getConnectionId(),
                        pushNotificationDTO.getNetworkId(), receivedNotificationCountForTargetUser);
            } else if (activeSession.getDeviceTypeId().intValue() == DeviceType.IOS.getType()) {
                message = createAPNSNotificationJSONRequest(pushNotificationDTO.getActivityMessage(),
                        activityDescription, pushNotificationDTO.getActivityId(),
                        pushNotificationDTO.getActivityRequestType().getValue(), pushNotificationDTO.getConnectionId(),
                        pushNotificationDTO.getNetworkId(), receivedNotificationCountForTargetUser);
            }

            if (!PeopleUtils.isNullOrEmpty(activeSession.getEndPointARN())) {
                return new SQSPayload(
                        PUSH_NOTIFICATION,
                        activeSession.getEndPointARN(),
                        message, activeSession.getDeviceTypeId());
            }
        }
        return null;
    }

    @Override
    public SQSPayload prepareSQSPayloadForSilentNotification(String userId, String activityRequestType,
                                                             String activityId, String connectionId, String networkId) {
        UserSession activeSession = userSessionRepository.findActiveSession(PeopleUtils.convertStringToObjectId(userId));

        String message = null;
        if (activeSession != null && activeSession.getDeviceTypeId() != null) {

            if (activeSession.getDeviceTypeId().intValue() == DeviceType.ANDROID.getType()) {
                message = createGCMSilentNotificationJSONRequest(activityId, activityRequestType, connectionId,
                        networkId);
            } else if (activeSession.getDeviceTypeId().intValue() == DeviceType.IOS.getType()) {
                message = createAPNSSilentNotificationJSONRequest(activityId, activityRequestType, connectionId,
                        networkId);
            }

            if (!PeopleUtils.isNullOrEmpty(activeSession.getEndPointARN())) {
                return new SQSPayload(
                        PUSH_NOTIFICATION,
                        activeSession.getEndPointARN(),
                        message, activeSession.getDeviceTypeId());
            }
        }
        return null;
    }

    private String prepareActivityDescription(PushNotificationDTO pushNotificationDTO) {
        String activityDescription = null;
        int numberOfContacts = 0;
        Object[] messagePrams;
        UserActivity userActivity = userActivityRepository.getPendingActivityById(pushNotificationDTO.getActivityId());
        RequestType requestType = pushNotificationDTO.getActivityRequestType();

        switch (requestType) {
            case CONNECTION_REQUEST:
                activityDescription = pushNotificationDTO.getInitiatorName() + " would like to connect with you!";
                break;
            case CONNECTION_REQUEST_ACCEPTED:
                messagePrams = new Object[]{pushNotificationDTO.getInitiatorName()};
                activityDescription = messages.get(NotificationTemplateKeys.CONNECTION_REQUEST_ACCEPTED, messagePrams);
                break;
            case MORE_INFO_REQUEST:
                activityDescription = pushNotificationDTO.getInitiatorName() + " would like to request more information!";
                break;
            case INTRODUCTION_REQUEST:
                activityDescription = pushNotificationDTO.getInitiatorName() + " would like to introduce you to " +
                        preparePartStringIntroductionReceived(userActivity);
                break;
            case SHARE_LOCATION_ACTIVITY:
                activityDescription = pushNotificationDTO.getInitiatorName() + " has shared location with you!";
                break;
            case SHARE_CONTACT_ACTIVITY:
                numberOfContacts = pushNotificationDTO.getNumberOfContacts();
                activityDescription = pushNotificationDTO.getInitiatorName() + " shared " +
                        getPartStringForSharedContactReceived(numberOfContacts);
                break;
            case UPDATE_CONTACT_ACTIVITY:
                messagePrams = new Object[]{pushNotificationDTO.getInitiatorName()};
                activityDescription = messages.get(NotificationTemplateKeys.CONTACT_UPDATED, messagePrams);
                break;
            case NETWORK_ADMIN_PROMOTION:
                messagePrams = new Object[]{pushNotificationDTO.getInitiatorName(), pushNotificationDTO.getNetworkName()};
                activityDescription = messages.get(NotificationTemplateKeys.NETWORK_MEMBER_PROMOTED, messagePrams);
                break;
            case NETWORK_OWNERSHIP_TRANSFER:
                messagePrams = new Object[]{pushNotificationDTO.getInitiatorName(), pushNotificationDTO.getNetworkName()};
                activityDescription = messages.get(NotificationTemplateKeys.NETWORK_OWNERSHIP_TRANSFERRED, messagePrams);
                break;
            case NETWORK_MESSAGE_BROADCAST:
                messagePrams = new Object[]{pushNotificationDTO.getInitiatorName(), pushNotificationDTO.getNetworkName()};
                activityDescription = messages.get(NotificationTemplateKeys.NETWORK_BROADCAST, messagePrams);
                break;
            case NETWORK_JOIN_REQUEST:
                messagePrams = new Object[]{pushNotificationDTO.getInitiatorName(), pushNotificationDTO.getNetworkName()};
                activityDescription = messages.get(NotificationTemplateKeys.NETWORK_JOIN_REQUEST_INITIATED, messagePrams);
                break;
            case NETWORK_JOIN_REQUEST_ACCEPTED:
                messagePrams = new Object[]{pushNotificationDTO.getInitiatorName(), pushNotificationDTO.getNetworkName()};
                activityDescription = messages.get(NotificationTemplateKeys.NETWORK_JOIN_REQUEST_ACCEPTED, messagePrams);
                break;
            case NETWORK_MEMBER_INVITE:
                messagePrams = new Object[]{pushNotificationDTO.getInitiatorName(), pushNotificationDTO.getNetworkName()};
                activityDescription = messages.get(NotificationTemplateKeys.NETWORK_MEMBER_INVITE_INITIATED, messagePrams);
                break;
            case NETWORK_SHARE:
                messagePrams = new Object[]{pushNotificationDTO.getInitiatorName(), pushNotificationDTO.getNetworkName()};
                activityDescription = messages.get(NotificationTemplateKeys.NETWORK_SHARE_INITIATED, messagePrams);
                break;
            default:
                throw new BadRequestException(ErrorCode.BAD_REQUEST.getValue());

        }
        return activityDescription;
    }

    private String getPartStringForSharedContactReceived(int numberOfContacts) {
        return ((numberOfContacts > 1) ? (numberOfContacts + " contacts with you!") : (" a contact with you!"));
    }

    @Override
    public String preparePartStringIntroductionReceived(UserActivity userActivity) {
        int numberOfIntroducedContacts = userActivity.getIntroducedContactNumber().size();
        return ((numberOfIntroducedContacts > 1) ?
                (numberOfIntroducedContacts + " contacts!") : getSingleIntroducedUserName(userActivity));
    }

    @Override
    public String getSingleIntroducedUserName(UserActivity userActivity) {

        String[] introducedContactNumber = userActivity.getIntroducedContactNumber().get(0).split("_");
        String countryCode = introducedContactNumber[0];
        String phoneNumberWithOutCountryCode = introducedContactNumber[1];
        PeopleUser introducedContactUser = peopleUserRepository.findByCodeAndNumber(
                countryCode, phoneNumberWithOutCountryCode);

        if (introducedContactUser != null) {
            return introducedContactUser.getNameValue();
        } else {
            return countryCode + "-" + phoneNumberWithOutCountryCode;
        }
    }

    private String createAPNSNotificationJSONRequest(String activityMessage, String activityDescription,
                                                     String activityId, String activityRequestType, String connectionId,
                                                     String networkId, long receivedNotificationCount) {
        JSONObject apnsSandbox = new JSONObject();
        JSONObject aps = new JSONObject();
        JSONObject alert = new JSONObject();
        alert.put(TITLE, activityDescription);
        alert.put(BODY, activityMessage);

        aps.put(ALERT, alert);
        aps.put(ACTIVITY_ID, activityId);
        aps.put(ACTIVITY_TYPE, activityRequestType);
        aps.put(CONTENT_AVAILABLE, 1);
        if (networkId != null) {
            aps.put(NETWORK_ID, networkId);
        }
        if (connectionId != null) {
            aps.put(CONNECTION_ID, connectionId);
        }
        aps.put(BADGE_COUNT, receivedNotificationCount);
        apnsSandbox.put(APS, aps);
        String stringifyJSON = StringEscapeUtils.escapeJava(apnsSandbox.toString());
        logger.info("Generated stringifyJSON for APNS push notification payload : {}", stringifyJSON);
        return stringifyJSON;
    }

    private String createGCMNotificationJSONRequest(String activityMessage, String activityDescription,
                                                    String activityId, String activityRequestType, String connectionId,
                                                    String networkId, long receivedNotificationCount) {
        JSONObject gcm = new JSONObject();
        JSONObject data = new JSONObject();
        JSONObject alert = new JSONObject();
        alert.put(TITLE, activityDescription);
        alert.put(BODY, activityMessage);

        data.put(ALERT, alert);
        data.put(ACTIVITY_ID, activityId);
        data.put(ACTIVITY_TYPE, activityRequestType);
        if (networkId != null) {
            data.put(NETWORK_ID, networkId);
        }
        if (connectionId != null) {
            data.put(CONNECTION_ID, connectionId);
        }
        data.put(BADGE_COUNT, receivedNotificationCount);
        gcm.put(DATA, data);
        String stringifyJSON = StringEscapeUtils.escapeJava(gcm.toString());
        logger.info("Generated stringifyJSON for GCM Silent push notification payload : {}", stringifyJSON);
        return stringifyJSON;
    }

    private String createAPNSSilentNotificationJSONRequest(String activityId, String activityRequestType, String connectionId,
                                                           String networkId) {
        JSONObject apnsSandbox = new JSONObject();
        JSONObject aps = new JSONObject();

        aps.put(ALERT, "");
        aps.put(CONTENT_AVAILABLE, 1);
        aps.put(ACTIVITY_TYPE, activityRequestType);

        if (activityId != null) {
            aps.put(ACTIVITY_ID, activityId);
        }
        if (networkId != null) {
            aps.put(NETWORK_ID, networkId);
        }
        if (connectionId != null) {
            aps.put(CONNECTION_ID, connectionId);
        }
        apnsSandbox.put(APS, aps);

        String stringifyJSON = StringEscapeUtils.escapeJava(apnsSandbox.toString());
        logger.info("Generated stringifyJSON for APNS Silent push notification payload : {}", stringifyJSON);
        return stringifyJSON;
    }

    private String createGCMSilentNotificationJSONRequest(String activityId, String activityRequestType, String connectionId,
                                                          String networkId) {
        JSONObject gcm = new JSONObject();
        JSONObject data = new JSONObject();

        data.put(ALERT, "");
        data.put(CONTENT_AVAILABLE, 1);
        data.put(ACTIVITY_TYPE, activityRequestType);
        if (activityId != null) {
            data.put(ACTIVITY_ID, activityId);
        }
        if (networkId != null) {
            data.put(NETWORK_ID, networkId);
        }
        if (connectionId != null) {
            data.put(CONNECTION_ID, connectionId);
        }
        gcm.put(DATA, data);
        String stringifyJSON = StringEscapeUtils.escapeJava(gcm.toString());
        logger.info("Generated stringifyJSON for GCM push notification payload : {}", stringifyJSON);
        return stringifyJSON;
    }
}
