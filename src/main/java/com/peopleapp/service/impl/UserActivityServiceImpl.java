package com.peopleapp.service.impl;

import com.peopleapp.configuration.LocaleMessageReader;
import com.peopleapp.controller.UserActivityController;
import com.peopleapp.dto.*;
import com.peopleapp.dto.requestresponsedto.*;
import com.peopleapp.enums.*;
import com.peopleapp.exception.BadRequestException;
import com.peopleapp.model.*;
import com.peopleapp.repository.*;
import com.peopleapp.security.TokenAuthService;
import com.peopleapp.service.*;
import com.peopleapp.util.PeopleUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import java.util.*;

@Service
public class UserActivityServiceImpl implements UserActivityService {

    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";

    @Inject
    private TokenAuthService tokenAuthService;

    @Inject
    private UserActivityRepository userActivityRepository;

    @Inject
    private PeopleUserRepository peopleUserRepository;

    @Inject
    private UserConnectionRepository userConnectionRepository;

    @Inject
    private LocaleMessageReader message;

    @Inject
    private HttpServletRequest request;

    @Inject
    private LocaleMessageReader messages;

    @Inject
    private CustomBaseRepository customBaseRepository;

    @Inject
    private UserSessionRepository userSessionRepository;

    @Inject
    private NetworkRepository networkRepository;

    @Value("${server.base-path}")
    private String serverBasePath;

    @Inject
    private PeopleUserService peopleUserService;

    @Inject
    private UserPrivacyProfileRepository userPrivacyProfileRepository;

    @Inject
    private MasterService masterService;

    @Inject
    @Lazy
    private UserConnectionService userConnectionService;

    @Inject
    private RegisteredNumberRepository registeredNumberRepository;

    @Inject
    private ActivityContactRepository activityContactRepository;

    @Inject
    private NotificationService notificationService;

    @Inject
    private QueueService queueService;

    private static final String LAST_UPDATED_ON = "lastUpdatedOn";

    @Override
    public ActivityListResponse getActivitiesCreatedForUser(int pageNumber, int pageSize) {

        Pageable pageable = PageRequest.of(pageNumber, pageSize,new Sort(Sort.Direction.DESC,LAST_UPDATED_ON));

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        String userId = sessionUser.getUserId();

        //get all activities where activityforId is userId and isCleared is false
        Page<UserActivity> userActivityList = userActivityRepository.getAllUserActivitiesPageable(userId, pageable);

        Link link = ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(UserActivityController.class)
                .userReceivedActivities("", pageNumber + 1, pageSize)).withSelfRel();

        return prepareActivityListResponse(userActivityList,link,sessionUser);
    }

    @Override
    public ActivityListResponse getActionableActivitiesCreatedForUser(int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize,new Sort(Sort.Direction.DESC, LAST_UPDATED_ON));

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        String userId = sessionUser.getUserId();

        //get all actionable activities where activityforId is userId and isCleared is false
        Page<UserActivity> userActivityList = userActivityRepository.getActionableActivitiesReceivedByUser(userId, pageable);

        Link link = ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(UserActivityController.class)
                .userReceivedActionableActivities("", pageNumber + 1, pageSize)).withSelfRel();

        return prepareActivityListResponse(userActivityList,link,sessionUser);
    }

    @Override
    public ActivityListResponse getActivitiesCreatedByUser(Integer page, Integer size) {

        Pageable pageable = PageRequest.of(page, size,new Sort(Sort.Direction.DESC, LAST_UPDATED_ON));

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        String userId = sessionUser.getUserId();

        //get all activities where activity by is userId and isActionRequired is true
        Page<UserActivity> userActivityList = userActivityRepository.findByInitiatedByIdPageable(userId, pageable);

        Link link = ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(UserActivityController.class)
                .userSentRequests("", page + 1, size)).withSelfRel();

        // prepare response
        ActivityListResponse response = new ActivityListResponse();
        List<UserActivityData> userActivityDataList = new ArrayList<>();

        for (UserActivity userActivity : PeopleUtils.emptyIfNull(userActivityList)) {
            UserActivityData userActivityData = new UserActivityData();

            // Prepare 'activityDescription' node
            String activityDescription = prepareActivityDescriptionForSentRequest(userActivity);

            userActivityData.setActivityDetails(prepareActivityDetails(userActivity, activityDescription, Boolean.FALSE));

            // Prepare 'receiverDetails' node
            // "activityForId" will be null for connection request to a static contact
            // for such cases, we will not prepare "receiverDetails"
            if (userActivity.getActivityForId() != null) {
                PeopleUser receiver = peopleUserService.findUserByUserId(userActivity.getActivityForId());
                if (receiver != null) {
                    userActivityData.setReceiverDetails(prepareTargetUserData(sessionUser, receiver));
                }
            }

            userActivityDataList.add(userActivityData);
        }

        response.setUserActivityList(userActivityDataList);
        response.setTotalNumberOfPages(userActivityList.getTotalPages());
        response.setTotalElements(userActivityList.getTotalElements());
        if (!userActivityList.isLast()) {
            response.setNextURL(link.getHref());
        }
        return response;
    }

    @Override
    public SharedLocationWithOthersResponse getActiveLocationSharedWithOthers(int pageNumber, int pageSize) {

        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        String userId = sessionUser.getUserId();

        //get all activities where activity by is userId and isActionRequired is true
        Page<UserActivity> userActivityList = userActivityRepository.getActiveLocationShareActivityByUserPageable(userId, pageable);

        Link link = ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(UserActivityController.class)
                .getSharedLocationWithOthers("", pageNumber + 1, pageSize)).withSelfRel();

        // prepare response
        SharedLocationWithOthersResponse response = new SharedLocationWithOthersResponse();
        List<SharedLocationWithOtherDetails> sharedLocationList = new ArrayList<>();

        for (UserActivity userActivity : PeopleUtils.emptyIfNull(userActivityList)) {

            SharedLocationWithOtherDetails sharedLocation = new SharedLocationWithOtherDetails();
            sharedLocation.setActivityId(userActivity.getActivityId());
            sharedLocation.setSharedWithUserId(userActivity.getActivityForId());
            sharedLocation.setLocationSharedOn(userActivity.getCreatedOn());
            sharedLocation.setSharedForTimeInMinutes(userActivity.getLocationSharedForTime());
            sharedLocation.setCurrentTime(PeopleUtils.getCurrentTimeInUTC());

            sharedLocationList.add(sharedLocation);

        }

        response.setSharedLocationDetailsList(sharedLocationList);
        response.setTotalNumberOfPages(userActivityList.getTotalPages());
        response.setTotalElements(userActivityList.getTotalElements());
        if (!userActivityList.isLast()) {
            response.setNextURL(link.getHref());
        }
        return response;

    }

    @Override
    public SharedLocationWithMeResponse getActiveLocationSharedWithMe(int pageNumber, int pageSize) {

        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        String userId = sessionUser.getUserId();

        //get all activities where activity by is userId and isActionRequired is true
        Page<UserActivity> userActivityList = userActivityRepository.getActiveLocationShareActivityForUser(userId, pageable);

        Link link = ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(UserActivityController.class)
                .getSharedLocationWithMe("", pageNumber + 1, pageSize)).withSelfRel();

        // prepare response
        SharedLocationWithMeResponse response = new SharedLocationWithMeResponse();
        List<SharedLocationWithMeDetails> sharedLocationList = new ArrayList<>();

        for (UserActivity userActivity : PeopleUtils.emptyIfNull(userActivityList)) {

            SharedLocationWithMeDetails sharedDetails = prepareSharedLocationDetails(userActivity);

            if (sharedDetails != null) {
                sharedLocationList.add(sharedDetails);
            }
        }

        response.setSharedLocationDetailsList(sharedLocationList);
        response.setTotalNumberOfPages(userActivityList.getTotalPages());
        response.setTotalElements(userActivityList.getTotalElements());
        if (!userActivityList.isLast()) {
            response.setNextURL(link.getHref());
        }
        return response;

    }

    @Override
    public SharedContactWithOthersResponse getSharedContactWithOthers(int pageNumber, int pageSize) {

        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        PeopleUser sessionUser = tokenAuthService.getSessionUser();

        Page<UserActivity> userActivityList = userActivityRepository.getContactSharedActivityByUser(sessionUser.getUserId(), pageable);

        Link link = ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(UserActivityController.class)
                .getSharedContactWithOthers("", pageNumber + 1, pageSize)).withSelfRel();

        List<SharedContactWithOtherDetail> sharedContactList = new ArrayList<>();

        for (UserActivity userActivity : PeopleUtils.emptyIfNull(userActivityList)) {

            List<String> sharedConnections = new ArrayList<>();
            userActivity.getListOfSharedConnectionIds().forEach(sharedConnections::addAll);

            SharedContactWithOtherDetail sharedDetail = new SharedContactWithOtherDetail();
            sharedDetail.setActivityIds(userActivity.getActivityIds());
            // Prepare 'initiatorDetails' node
            PeopleUser receiver = peopleUserService.findUserByUserId(userActivity.getActivityForId());
            sharedDetail.setReceiverDetails(prepareTargetUserData(sessionUser, receiver));
            sharedDetail.setNumberOfContactShared(sharedConnections.size());
            sharedContactList.add(sharedDetail);
        }

        SharedContactWithOthersResponse response = new SharedContactWithOthersResponse();
        response.setSharedContactDetailList(sharedContactList);
        response.setTotalNumberOfPages(userActivityList.getTotalPages());
        response.setTotalElements(userActivityList.getTotalElements());
        if (!userActivityList.isLast()) {
            response.setNextURL(link.getHref());
        }
        return response;
    }

    @Override
    public SharedContactWithMeResponse getSharedContactWithMe(int pageNumber, int pageSize) {

        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        PeopleUser sessionUser = tokenAuthService.getSessionUser();

        Page<UserActivity> userActivityList = userActivityRepository.getContactSharedActivityForUser(sessionUser.getUserId(), pageable);

        Link link = ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(UserActivityController.class)
                .getSharedContactWithMe("", pageNumber + 1, pageSize)).withSelfRel();

        List<SharedContactWithMeDetail> sharedContactList = new ArrayList<>();

        for (UserActivity userActivity : PeopleUtils.emptyIfNull(userActivityList)) {
            List<String> sharedConnections = new ArrayList<>();
            userActivity.getListOfSharedConnectionIds().forEach(sharedConnections::addAll);
            SharedContactWithMeDetail sharedDetail = new SharedContactWithMeDetail();
            sharedDetail.setActivityIds(userActivity.getActivityIds());
            sharedDetail.setSharedByUserId(userActivity.getActivityById());
            // Prepare 'initiatorDetails' node
            PeopleUser initiator = peopleUserService.findUserByUserId(userActivity.getActivityById());
            sharedDetail.setInitiatorDetails(prepareTargetUserData(sessionUser, initiator));
            sharedDetail.setNumberOfContactShared(sharedConnections.size());
            sharedContactList.add(sharedDetail);
        }

        SharedContactWithMeResponse response = new SharedContactWithMeResponse();
        response.setSharedContactDetailList(sharedContactList);
        response.setTotalNumberOfPages(userActivityList.getTotalPages());
        response.setTotalElements(userActivityList.getTotalElements());
        if (!userActivityList.isLast()) {
            response.setNextURL(link.getHref());
        }
        return response;
    }

    @Override
    public void clearActivity(ClearActivityRequest clearActivityRequest) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        String userId = sessionUser.getUserId();

        Boolean isAllActivityClear = clearActivityRequest.getIsAllActivityCleared();
        List<UserActivity> userActivityList;

        if (isAllActivityClear) {
            userActivityList = userActivityRepository.getAllUserActivities(userId);
        } else {
            userActivityList = userActivityRepository.getAllActivitiesByInitiatedToIdAndActivityIds(userId, clearActivityRequest.getActivityIdList());
        }


        for (UserActivity userActivity : PeopleUtils.emptyIfNull(userActivityList)) {
            userActivity.setIsCleared(Boolean.TRUE);
        }

        if (!PeopleUtils.isNullOrEmpty(userActivityList)) {
            userActivityRepository.saveAll(userActivityList);
        }
    }

    @Override
    public CancelRequestResponseDTO cancelActivity(CancelRequestDTO cancelRequest) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();

        UserContactData userContact = null;
        List<UserActivity> pendingActivityList = userActivityRepository.getPendingActivityById(cancelRequest.getActivityIdList());

        if (PeopleUtils.isNullOrEmpty(pendingActivityList)) {
            throw new BadRequestException(MessageCodes.INVALID_PENDING_ACTIVITY.getValue());
        }

        List<SQSPayload> sqsPayloadList = new ArrayList<>();
        for (UserActivity pendingActivity : pendingActivityList) {

            userContact = updateConnectionStatusForCancelSentConnectionRequest(
                    sessionUser, pendingActivity, true);

            if (pendingActivity.getActivityType().getRequestType().equals(RequestType.SHARE_LOCATION_ACTIVITY)) {
                sqsPayloadList.add(notificationService.prepareSQSPayloadForSilentNotification(pendingActivity.getActivityForId(),
                        RequestType.STOP_LOCATION_SHARE.getValue(), pendingActivity.getActivityId(), null,
                        null));
            }

            pendingActivity.setOverallStatus(ActivityStatus.INACTIVE);
            ActivityType activityType = pendingActivity.getActivityType();
            activityType.setActionTaken(Action.CANCELLED);
            pendingActivity.setActivityType(activityType);
            pendingActivity.setActionTakenById(sessionUser.getUserId());
        }
        queueService.sendPayloadToSQS(sqsPayloadList);

        // updating all corresponding activity-contacts to inactive
        activityContactRepository.markActivityContactsInActiveByActivityId(cancelRequest.getActivityIdList());

        userActivityRepository.saveAll(pendingActivityList);

        //prepare response
        CancelRequestResponseDTO cancelActivityResponse = new CancelRequestResponseDTO();
        cancelActivityResponse.setContactData(userContact);

        return cancelActivityResponse;
    }


    @Override
    public void ignoreActivity(IgnoreRequestDTO ignoreRequest) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        String initiatorId = sessionUser.getUserId();

        List<String> activityIdList = ignoreRequest.getActivityIdList();
        List<String> activitySubIdList = ignoreRequest.getActivitySubIdList();

        // Ignore activity will be performed by 'receiver'.
        // And it can contain activities like
        // 1. Connection request - ignore
        // 2. Shared contact - ignore/ignore all
        // 3. Introduction - ignore/ignore all
        // For 'connection request' there will be no activity contacts

        List<UserActivity> userActivityList = null;
        List<ActivityContact> activityContactList = null;

        if (PeopleUtils.isNullOrEmpty(activitySubIdList)) {
            userActivityList = userActivityRepository.getPendingActivityById(new ArrayList<>(activityIdList));
        }

        activityContactList = getActivityContactList(activitySubIdList, activityIdList, initiatorId);

        if (PeopleUtils.isNullOrEmpty(activityContactList)) {
            // This scenario is valid for 'connection request' ignore
            // there will not be any 'activity contacts' involved

            if (PeopleUtils.isNullOrEmpty(userActivityList)) {
                throw new BadRequestException(MessageCodes.INVALID_PENDING_ACTIVITY.getValue());
            }

            for (UserActivity pendingActivity : userActivityList) {

                PeopleUser activityCreator =
                        peopleUserRepository.findByuserId(pendingActivity.getActivityById(), UserStatus.ACTIVE.getValue());

                updateConnectionStatusForCancelSentConnectionRequest(activityCreator, pendingActivity, false);

                pendingActivity.setOverallStatus(ActivityStatus.INACTIVE);
                ActivityType activityType = pendingActivity.getActivityType();
                activityType.setActionTaken(Action.IGNORED);
                pendingActivity.setActivityType(activityType);
                pendingActivity.setActionTakenById(initiatorId);
            }

            userActivityRepository.saveAll(userActivityList);

        } else {
            updateActivityAndActivityContacts(activityContactList, userActivityList);
        }

    }

    @Override
    public void deleteActivity(DeleteActivityRequest deleteActivityRequest) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();

        List<UserActivity> pendingActivityList = userActivityRepository.getPendingActivityById(deleteActivityRequest.getActivityIdList());

        if (PeopleUtils.isNullOrEmpty(pendingActivityList)) {
            throw new BadRequestException(MessageCodes.INVALID_PENDING_ACTIVITY.getValue());
        }

        for (UserActivity pendingActivity : pendingActivityList) {

            pendingActivity.setOverallStatus(ActivityStatus.INACTIVE);
            ActivityType activityType = pendingActivity.getActivityType();
            activityType.setActionTaken(Action.DELETED);
            pendingActivity.setActivityType(activityType);
            pendingActivity.setActionTakenById(sessionUser.getUserId());
        }

        // updating all corresponding activity-contacts to inactive
        activityContactRepository.markActivityContactsInActiveByActivityId(deleteActivityRequest.getActivityIdList());

        userActivityRepository.saveAll(pendingActivityList);

    }

    @Override
    public int getCountOfConnectionRequestsForTimeRange(String fromUserId, int timeRange, DateTime lastRequestCreatedTime) {

        int numberOfRequests = 0;

        List<UserActivity> requestsList = userActivityRepository.getConnectionRequestCountForTimeRange(fromUserId,
                PeopleUtils.getCurrentTimeInUTC(), lastRequestCreatedTime);
        if (requestsList != null && !requestsList.isEmpty()) {
            numberOfRequests = requestsList.size();
        }
        return numberOfRequests;
    }

    @Override
    public List<UserActivity> createMultipleRequest(List<UserActivity> userActivityList) {
        return userActivityRepository.saveAll(userActivityList);
    }

    @Override
    public List<UserActivity> findByInitiateContactNumber(ContactNumberDTO contactNumber) {
        return userActivityRepository.getActivityListByContactNumber(contactNumber);
    }

    @Override
    public List<ActivityDetails> prepareActivityDetails(List<UserActivity> userActivityList) {

        List<ActivityDetails> activityDetailsList = new ArrayList<>();
        for (UserActivity userActivity : PeopleUtils.emptyIfNull(userActivityList)) {
            if (userActivity == null) continue;
            activityDetailsList.add(prepareActivityDetails(userActivity, null, Boolean.TRUE));
        }
        return activityDetailsList;
    }

    @Override
    public void editSharedContactActivity(EditSharedContactRequest editSharedContactRequest) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        String initiatorUserId = sessionUser.getUserId();

        List<String> activityIdList = editSharedContactRequest.getActivityIdList();
        List<String> activitySubIdList = editSharedContactRequest.getActivitySubIdList();

        List<ActivityContact> activityContactList = null;

        // edit shared contacts API can be called from 3 places
        // 1. edit single or multiple contact to remove it by initiator or receiver
        // In above case, API request should have 'activitySubIdList' not empty
        // 2. Remove all shared contacts by receiver
        // 3. Stop sharing all shared contacts by initiator
        // In above 2 case, API request should have 'activityIdList' not empty

        // Check if 'activitySubIdList' is not empty
        activityContactList = getActivityContactList(activitySubIdList, activityIdList, initiatorUserId);

        if (PeopleUtils.isNullOrEmpty(activityContactList)) {
            throw new BadRequestException(MessageCodes.INVALID_OPERATION.getValue());
        }

        updateActivityAndActivityContacts(activityContactList, null);
    }

    @Override
    public List<UserActivityData> getActivityDetailsByActivityId(String activityId) {

        List<UserActivityData> userActivityDataList = new ArrayList<>();
        UserActivity userActivity = userActivityRepository.getPendingActivityById(activityId);

        if (userActivity == null) {
            throw new BadRequestException(MessageCodes.INVALID_ACTIVITY.getValue());
        }

        // Update 'isRead' value for this specific activity
        userActivity.setIsRead(Boolean.TRUE);
        userActivityRepository.save(userActivity);

        userActivityDataList.add(prepareActivityDataBasedOnRequestType(userActivity));

        return userActivityDataList;
    }

    @Override
    public ActivityContactsResponseDTO getActivityContactsByActivityId(ActivityContactsAPIParamData
                                                                               activityContactsAPIParamData) {

        //fetching all request parameter values
        String searchString = activityContactsAPIParamData.getSearchString().trim();
        String activityId = activityContactsAPIParamData.getActivityId();
        String initiatorId = activityContactsAPIParamData.getInitiatorId();
        String receiverId = activityContactsAPIParamData.getReceiverId();
        Integer fNameOrder = activityContactsAPIParamData.getFNameOrder();
        Integer lNameOrder = activityContactsAPIParamData.getLNameOrder();
        Boolean lNamePreferred = activityContactsAPIParamData.getLNamePreferred();
        Integer pageNumber = activityContactsAPIParamData.getPageNumber();
        Integer pageSize = activityContactsAPIParamData.getPageSize();

        ActivityContactsResponseDTO response = new ActivityContactsResponseDTO();
        List<UserContactData> activityContactsList = null;
        Page<ActivityContact> activityContactList = null;

        Pageable pageable = PageRequest.of(pageNumber, pageSize,
                getSortOperationForActivityContacts(fNameOrder, lNameOrder, lNamePreferred));

        //get session user
        PeopleUser sessionUser = tokenAuthService.getSessionUser();

        // If 'activityId' is present. Priority will goes to the same
        // Else will fetch activity contacts with 'initiatorId' and 'receiverId' combination

        // "-1" is the default value associated to request param.
        // We will exclude it as the value is default and not specified by user
        if (activityId != null && !activityId.equals("-1")) {

            // Check if activityId is valid
            UserActivity userActivity = userActivityRepository.getPendingActivityById(activityId);

            if (userActivity == null) {
                throw new BadRequestException(MessageCodes.INVALID_ACTIVITY.getValue());
            }

            // Update 'isRead' value for this specific activity
            userActivity.setIsRead(Boolean.TRUE);
            userActivityRepository.save(userActivity);

            activityContactList = activityContactRepository.getActivityContactsByActivityId(activityId, searchString,
                    pageable);

        } else {

            if (initiatorId.equals("-1") || receiverId.equals("-1")) {
                throw new BadRequestException(MessageCodes.INVALID_ACTIVITY.getValue());
            }

            activityContactList = activityContactRepository.getShareActivityContactsByInitiatorIdAndReceiverId(
                    initiatorId, receiverId, searchString, pageable);

        }

        Map<String, List<String>> connectionIdGroupedActivitySubIdListMap =
                prepareActivitySubIdListGroupedByConnectionId(activityContactList.getContent());

        boolean isSharedWithOthers = sessionUser.getUserId().equals(initiatorId);

        activityContactsList = prepareActivityContactsList(activityContactList.getContent(),
                connectionIdGroupedActivitySubIdListMap, sessionUser, isSharedWithOthers);

        response.setActivityContactsList(activityContactsList);
        response.setTotalNumberOfPages(activityContactList.getTotalPages());
        response.setTotalElements(activityContactsList.size());
        if (!activityContactList.isLast()) {
            response.setNextURL(ControllerLinkBuilder.linkTo(ControllerLinkBuilder
                    .methodOn(UserActivityController.class)
                    .getActivityContactsById("", searchString, activityId, initiatorId, receiverId,
                            fNameOrder, lNameOrder, lNamePreferred, pageNumber + 1, pageSize))
                    .withSelfRel()
                    .getHref());
        }

        return response;
    }

    @Override
    public ActivityDetails prepareActivityDetails(UserActivity userActivity, String activityDescription,
                                                  boolean isInitiateInfoRequired) {

        ActivityDetails activityDetails = new ActivityDetails();
        activityDetails.setActivityId(userActivity.getActivityId());
        activityDetails.setActivityType(userActivity.getActivityType());
        activityDetails.setActivityMessage(userActivity.getMessage());
        activityDetails.setActivityById(userActivity.getActivityById());
        activityDetails.setActivityForId(userActivity.getActivityForId());
        activityDetails.setActivityStatus(userActivity.getOverallStatus().getValue());
        activityDetails.setCreatedOn(userActivity.getCreatedOn());
        activityDetails.setLastUpdatedOn(userActivity.getLastUpdatedOn());
        activityDetails.setRequestId(userActivity.getRequestId());
        activityDetails.setIsCleared(userActivity.getIsCleared());

        if (userActivity.getConnectionId() != null) {
            activityDetails.setConnectionId(userActivity.getConnectionId());
        }
        activityDetails.setIsActivityRead(userActivity.getIsRead());

        if (activityDescription != null) {
            activityDetails.setActivityDescription(activityDescription);
        }

        if (userActivity.getNetworkId() != null) {
            activityDetails.setNetworkId(userActivity.getNetworkId());
        }

        if (isInitiateInfoRequired) {
            InitiateDetails initiateDetails = new InitiateDetails();
            initiateDetails.setInitiateContactDetails(userActivity.getInitiateDetails());
            activityDetails.setInitiateDetails(initiateDetails);
        }

        return activityDetails;
    }

    private Coordinates getUserLocation(String userId) {
        PeopleUser peopleUser = peopleUserRepository.findByuserId(userId, UserStatus.ACTIVE.getValue());
        return peopleUser.getDeviceLocation();
    }

    private UserContactData prepareTargetUserData(PeopleUser sessionUser, PeopleUser targetUser) {
        UserConnection userConnection = userConnectionRepository.findContactByFromIdAndToId(
                sessionUser.getUserId(), targetUser.getUserId());
        UserContactData ownerContact = new UserContactData();

        // updating blocked status
        boolean isUserBlocked =
                sessionUser.getBlockedUserIdList().stream().anyMatch(id -> id.equals(targetUser.getUserId()));
        ownerContact.setIsBlocked(isUserBlocked);

        if (userConnection != null) {
            //if UserConnection exist for session user and owner

            UserConnection userContact = userConnectionRepository.getSharedProfileDataForSelectedContact
                    (Arrays.asList(userConnection.getConnectionId())).get(0);

            ownerContact = userConnectionService.prepareContactStaticData(sessionUser, userContact);
            UserInformationDTO contactStaticData = ownerContact.getStaticProfileData();

            Set<String> registeredNumbersList = masterService.getRegisteredNumberList();
            if (contactStaticData != null && (!registeredNumbersList.isEmpty())) {
                userConnectionService.populateStaticDataWithIsVerifiedInfo(contactStaticData, registeredNumbersList);
            }

            switch (userContact.getConnectionStatus()) {
                case CONNECTED:
                    ownerContact = userConnectionService.prepareContactSharedData(
                            sessionUser, userContact);
                    break;
                case NOT_CONNECTED:
                case PENDING:
                default:
                    break;
            }
        } else {
            //if UserConnection do not exist for session user and owner
            ownerContact.setToUserId(targetUser.getUserId());
            ownerContact.setPublicProfileData(masterService.prepareUserPublicData(peopleUserRepository.findByUserIdAndStatus
                    (targetUser.getUserId(), UserStatus.ACTIVE)));
            ownerContact.setConnectionStatus(peopleUserService.checkAndSetConnectionStatus(sessionUser, targetUser));

        }

        return ownerContact;
    }
    
    private UserContactData prepareTargetUserDataByActivityList(PeopleUser sessionUser, PeopleUser targetUser) {
        UserConnection userConnection = userConnectionRepository.findContactByFromIdAndToId(
                sessionUser.getUserId(), targetUser.getUserId());
        UserContactData ownerContact = new UserContactData();

        // updating blocked status
        boolean isUserBlocked =
                sessionUser.getBlockedUserIdList().stream().anyMatch(id -> id.equals(targetUser.getUserId()));
        ownerContact.setIsBlocked(isUserBlocked);
		
        ownerContact.setToUserId(targetUser.getUserId());
        ownerContact.setPublicProfileData(masterService.prepareUserPublicData(peopleUserRepository.findByUserIdAndStatus
                (targetUser.getUserId(), UserStatus.ACTIVE)));
        ownerContact.setConnectionStatus(peopleUserService.checkAndSetConnectionStatus(sessionUser, targetUser));

        return ownerContact;
    }

    private String prepareActivityDescription(UserActivity userActivity) {
        String activityDescription = null;
        RequestType requestType = userActivity.getActivityType().getRequestType();
        Integer numberOfContacts = 1;
        String networkName = null;
        if (userActivity.getNetworkId() != null) {
            networkName = networkRepository.findNetworkByIdAndStatus(userActivity.getNetworkId(),
                    NetworkStatus.ACTIVE.getValue()).getName();
        }

        switch (requestType) {
            case CONNECTION_REQUEST:
                activityDescription = " would like to connect with you!";
                break;
            case CONNECTION_REQUEST_ACCEPTED:
                activityDescription = " accepted your connection request!";
                break;
            case MORE_INFO_REQUEST:
                activityDescription = " would like to request more information!";
                break;
            case INTRODUCTION_REQUEST:
                activityDescription = " would like to introduce you to " +
                        notificationService.preparePartStringIntroductionReceived(userActivity);
                break;
            case SHARE_LOCATION_ACTIVITY:
                activityDescription = " has shared location with you!";
                break;
            case SHARE_CONTACT_ACTIVITY:
                numberOfContacts = userActivity.getSharedConnectionIdList().size();
                activityDescription = " shared " + getPartStringForSharedContactReceived(numberOfContacts);
                break;
            case NETWORK_JOIN_REQUEST:
                activityDescription = " would like to join your network.";
                break;
            case NETWORK_ADMIN_PROMOTION:
                activityDescription = " has promoted you to Admin of the " + networkName +
                        " Network!";
                break;
            case NETWORK_OWNERSHIP_TRANSFER:
                activityDescription = " has transferred ownership of the " +
                        networkName + " Network to you!";
                break;
            case NETWORK_MEMBER_INVITE:
                activityDescription = " has invited you to the " + networkName + " Network!";
                break;
            case NETWORK_MESSAGE_BROADCAST:
                activityDescription = " has sent a message in the network " + networkName;
                break;
            case NETWORK_SHARE:
                activityDescription = " has shared the network " + networkName + " with you!";
                break;
            case NETWORK_JOIN_REQUEST_ACCEPTED:
                activityDescription = " accepted your network join request to " + networkName +
                        " Network! ";
                break;
            case UPDATE_CONTACT_ACTIVITY:
                activityDescription = " has updated contact information.";
                break;
            default:
                throw new BadRequestException(ErrorCode.BAD_REQUEST.getValue());

        }
        return activityDescription;
    }

    private String getPartStringForSharedContactReceived(int numberOfContacts) {
        return ((numberOfContacts > 1) ? (numberOfContacts + " contacts with you!") : ("a contact with you!"));
    }

    private String prepareActivityDescriptionForSentRequest(UserActivity userActivity) {
        String activityDescription = null;

        RequestType requestType = userActivity.getActivityType().getRequestType();
        Integer numberOfContacts = 0;

        String networkName = null;
        if (userActivity.getNetworkId() != null) {
            networkName = networkRepository.findNetworkByIdAndStatus(userActivity.getNetworkId(),
                    NetworkStatus.ACTIVE.getValue()).getName();
        }

        switch (requestType) {
            case CONNECTION_REQUEST:
                activityDescription = "You sent a connection request to " + prepareReceiverUserName(userActivity) + ".";
                break;
            case MORE_INFO_REQUEST:
                activityDescription = "You requested more information from " + prepareReceiverUserName(userActivity) + ".";
                break;
            case INTRODUCTION_REQUEST:
                numberOfContacts = userActivity.getIntroducedContactNumber().size();
                activityDescription = "You introduced" + getPartString(numberOfContacts) + prepareReceiverUserGroupName(userActivity) + ".";
                break;
            case SHARE_LOCATION_ACTIVITY:
                activityDescription = "You shared your location with " + prepareReceiverUserName(userActivity) + ".";
                break;
            case SHARE_CONTACT_ACTIVITY:
                numberOfContacts = userActivity.getSharedConnectionIdList().size();
                activityDescription = "You shared" + getPartString(numberOfContacts) + prepareReceiverUserGroupName(userActivity);
                break;
            case NETWORK_JOIN_REQUEST:
                activityDescription = "You sent network join request to " + networkName + " network.";
                break;
            case NETWORK_MEMBER_INVITE:
                activityDescription = "You invited " + prepareReceiverUserName(userActivity) + " to join the network "
                        + networkName + ".";
                break;
            case NETWORK_SHARE:
                activityDescription = "You shared the information about the network " + networkName + " with "
                        + prepareReceiverUserName(userActivity) + ".";
                break;
            case NETWORK_MESSAGE_BROADCAST:
                activityDescription = "You have sent a message to the network " + networkName + " " + userActivity.getMessage();
                break;
            default:
                throw new BadRequestException(ErrorCode.BAD_REQUEST.getValue());

        }
        return activityDescription;
    }

    private String prepareReceiverUserGroupName(UserActivity userActivity) {
        String activityReceiverUserGroupName = null;
        int counter = 0;

        UserContact userContact = userActivity.getInitiateDetails();

        // Fetch all user connections
        List<UserConnection> userConnections = userConnectionRepository.findConnectionByConnectionId(Arrays.asList(userContact.getConnectionId()));

        for (UserConnection userConnection : userConnections) {
            if (counter == 0) {
                activityReceiverUserGroupName = getReceiverUserName(userConnection);
            } else if (counter == 1) {
                activityReceiverUserGroupName = activityReceiverUserGroupName.concat(", ").concat(getReceiverUserName(userConnection));
            } else {
                activityReceiverUserGroupName = activityReceiverUserGroupName.concat(" and ")
                        .concat(String.valueOf((userConnections.size() - 2))).concat(" others");
            }
            counter++;
        }

        return activityReceiverUserGroupName;
    }

    private String getReceiverUserName(UserConnection userConnection) {
        if (userConnection.getConnectionToId() != null) {
            PeopleUser receiverUser = peopleUserRepository.findByUserIdAndStatus(userConnection.getConnectionToId(), UserStatus.ACTIVE);
            return receiverUser.getNameValue();
        } else {
            UserInformationDTO userContact = userConnection.getContactStaticData();
            String first = userContact.getFirstName();
            String last = userContact.getLastName();
            String fullName = null;
            if (!PeopleUtils.isNullOrEmpty(first) && !PeopleUtils.isNullOrEmpty(last)) {
                fullName = StringUtils.capitalize(first.toLowerCase()).concat(" ").concat(StringUtils.capitalize(last.toLowerCase()));

            } else if (PeopleUtils.isNullOrEmpty(first) && !PeopleUtils.isNullOrEmpty(last)) {
                fullName = StringUtils.capitalize(last.toLowerCase());
            } else if (PeopleUtils.isNullOrEmpty(last) && !PeopleUtils.isNullOrEmpty(first)) {
                fullName = StringUtils.capitalize(first.toLowerCase());
            }
            return fullName != null ? fullName : "Watu user";
        }
    }

    private String getPartString(int numberOfContacts) {
        return ((numberOfContacts > 1) ? (" " + numberOfContacts + " contacts with ") : (" a contact with "));
    }

    private String prepareReceiverUserName(UserActivity userActivity) {
        if (userActivity.getActivityForId() != null) {
            if ((userActivity.getActivityType().getRequestType().equals(RequestType.CONNECTION_REQUEST))
                    && (userActivity.getInitiateDetails().getConnectionId() != null)) {
                UserConnection userConnection =
                        userConnectionRepository.findConnectionByConnectionIdAndInitiatorId(userActivity.getActivityById(), userActivity.getInitiateDetails().getConnectionId());
                String contactStaticFullName = prepareContactStaticName(userConnection.getContactStaticData());
                return contactStaticFullName != null ? contactStaticFullName : returnWatuUserName(userActivity);

            } else {
                // return "WATU user" name
                return returnWatuUserName(userActivity);
            }
        } else {
            ContactNumberDTO contactNumber = userActivity.getInitiateDetails().getContactNumber();
            return contactNumber.getCountryCode() + "-" + contactNumber.getPhoneNumber();
        }
    }

    private String returnWatuUserName(UserActivity userActivity) {
        PeopleUser receiverUser = peopleUserRepository.findByUserIdAndStatus(userActivity.getActivityForId(), UserStatus.ACTIVE);
        return receiverUser.getNameValue();
    }

    private String prepareContactStaticName(UserInformationDTO contactStaticData) {
        String first = contactStaticData.getFirstName();
        String last = contactStaticData.getLastName();
        String companyName = contactStaticData.getCompany();
        String fullname = null;
        if (!PeopleUtils.isNullOrEmpty(first) && !PeopleUtils.isNullOrEmpty(last)) {
            fullname = StringUtils.capitalize(first.toLowerCase()).concat(" ").concat(StringUtils.capitalize(last.toLowerCase()));

        } else if (PeopleUtils.isNullOrEmpty(first) && !PeopleUtils.isNullOrEmpty(last)) {
            fullname = StringUtils.capitalize(last.toLowerCase());
        } else if (PeopleUtils.isNullOrEmpty(last) && !PeopleUtils.isNullOrEmpty(first)) {
            fullname = StringUtils.capitalize(first.toLowerCase());
        } else if (PeopleUtils.isNullOrEmpty(first) && PeopleUtils.isNullOrEmpty(last) &&
                !PeopleUtils.isNullOrEmpty(companyName)) {
            fullname = StringUtils.capitalize(companyName.toLowerCase());
        }
        return fullname != null ? fullname : null;
    }

    private List<UserContactData> prepareActivityContactsList(List<ActivityContact> activityContactList, Map<String,
            List<String>> connectionIdGroupedActivitySubIdListMap, PeopleUser sessionUser, boolean isSharedWithOthers) {

        List<UserContactData> activityContactsList = new ArrayList<>();
        Map<String, String> processedActivityContacts = new HashMap<>();

        // prepare response
        for (ActivityContact activityContact : activityContactList) {

            String activityContactConnectionId = activityContact.getConnectionId();

            // If current 'activityContact's' entry is already present in 'processedActivityContacts' map
            // It means, the new entry will be duplicate. So we are skipping the same to remove duplicate contact(s)
            if (processedActivityContacts.get(activityContactConnectionId) != null) {
                continue;
            }

            processedActivityContacts.put(activityContactConnectionId, activityContact.getUniqueId().toString());

            UserContactData userContactData = prepareUserContactForShareAndIntroduceActivity(sessionUser, activityContact,
                    activityContactConnectionId, isSharedWithOthers, connectionIdGroupedActivitySubIdListMap);

            if (userContactData != null) {
                activityContactsList.add(userContactData);
            }

        }
        return activityContactsList;
    }

    private UserContactData prepareUserContactForShareAndIntroduceActivity(PeopleUser sessionUser, ActivityContact activityContact,
                                                                           String activityContactConnectionId,
                                                                           boolean isSharedWithOthers,
                                                                           Map<String, List<String>> connectionIdGroupedActivitySubIdListMap) {


        if (activityContact.getRequestType().equals(RequestType.SHARE_CONTACT_ACTIVITY)) {

            List<UserConnection> userConnectionList = userConnectionRepository.getConnectionDataWithProfileForSelectedContact
                    (activityContact.getInitiatorId(), new ArrayList<>(Arrays.asList(activityContactConnectionId)));

            validateUserConnectionList(userConnectionList);

            UserConnection activityConnection = userConnectionList.get(0);

            SharedContactDetails sharedContactDetails = new SharedContactDetails();
            sharedContactDetails.setStaticProfileData(activityConnection.getContactStaticData());
            sharedContactDetails.setUserId(activityConnection.getConnectionToId());
            sharedContactDetails.setSharedProfileData(masterService.prepareSharedData1(activityConnection));

            UserContactData userContactData = prepareUserContactDataFromSharedContactDetails(sharedContactDetails);

            List<UserConnection> currentUserConnectionWithSharedUser = new ArrayList<>();

            // Check 'connectionStatus' for shared contact with current session user
            if (isSharedWithOthers) {
                // Adding 'connectionId' if logged-in user is 'initiator'
                // i.e, '/activity-contacts' call originates for '/shared-with-others'
                userContactData.setConnectionId(activityContact.getConnectionId());

                currentUserConnectionWithSharedUser = userConnectionRepository.getConnectionDataWithProfileForSelectedContact
                        (sessionUser.getUserId(), new ArrayList<>(Arrays.asList(activityContactConnectionId)));
            } else {
                if (!PeopleUtils.isNullOrEmpty(activityConnection.getConnectionToId())) {
                    UserConnection userConnection = userConnectionRepository.findConnectionByFromIdAndToId(
                            sessionUser.getUserId(), activityConnection.getConnectionToId());

                    if (userConnection != null) {
                        currentUserConnectionWithSharedUser.add(userConnection);
                    }
                }
            }

            updateConnectionStatusAndActivitySubIdList(sessionUser, currentUserConnectionWithSharedUser, userContactData,
                    activityContact, activityContactConnectionId, activityConnection, connectionIdGroupedActivitySubIdListMap);

            return userContactData;
        } else if (activityContact.getRequestType().equals(RequestType.INTRODUCTION_REQUEST)) {

            ContactNumberDTO introducedContactNumber = activityContact.getIntroducedContactNumber();
            UserInformationDTO introducedUserDetails = new UserInformationDTO();

            UserContactData userContactData = new UserContactData();

            PeopleUser introducedUser =
                    peopleUserRepository.findByCodeAndNumber(introducedContactNumber.getCountryCode(),
                            introducedContactNumber.getPhoneNumber());

            if (introducedUser != null) {
                userContactData = prepareTargetUserData(sessionUser, introducedUser);
            } else {
                introducedUserDetails.setContactNumber(introducedContactNumber);
                SharedContactDetails sharedContactDetails = new SharedContactDetails();
                sharedContactDetails.setStaticProfileData(introducedUserDetails);

                userContactData.setStaticProfileData(prepareStaticProfileDataforStaticIntroduce(
                        sharedContactDetails.getStaticProfileData()));

                // Set 'connectionStatus'
                updateConnectionStatusRespectToTargetUser(sessionUser.getUserId(), introducedContactNumber,
                        null, userContactData);
            }

            // Set 'activitySubIdList'
            userContactData.setActivitySubIdList(connectionIdGroupedActivitySubIdListMap.get(activityContactConnectionId));
            return userContactData;
        }

        return null;
    }

    private void updateConnectionStatusAndActivitySubIdList(PeopleUser sessionUser, List<UserConnection> currentUserConnectionWithSharedUser,
                                                            UserContactData userContactData, ActivityContact activityContact,
                                                            String activityContactConnectionId, UserConnection activityConnection,
                                                            Map<String, List<String>> connectionIdGroupedActivitySubIdListMap) {
        // Set 'connectionStatus'
        if (!PeopleUtils.isNullOrEmpty(currentUserConnectionWithSharedUser)) {
            userContactData.setConnectionStatus(currentUserConnectionWithSharedUser.get(0).getConnectionStatus().getValue());
        } else {
            // Set 'connectionStatus' if it is "PENDING" or "NOT_CONNECTED"
            checkAndSetUserConnectionStatus(sessionUser.getUserId(), activityConnection, userContactData);
        }

        // Set 'activitySubIdList'
        if (activityContact.getSubIdList() != null && !activityContact.getSubIdList().isEmpty()) {
            userContactData.setActivitySubIdList(activityContact.getSubIdList());
        } else {
            userContactData.setActivitySubIdList(
                    connectionIdGroupedActivitySubIdListMap.get(activityContactConnectionId));
        }
    }

    private Map<String, List<String>> prepareActivitySubIdListGroupedByConnectionId(List<ActivityContact> activityContactList) {
        Map<String, List<String>> connectionIdGroupedActivitySubIdListMap = new HashMap<>();

        // Iterating received 'activityContacts' list to prepare 'activitySubIdList'
        for (ActivityContact activityContact : activityContactList) {
            String activityContactConnectionId = activityContact.getConnectionId();
            String activityContactsUniqueId = activityContact.getUniqueId().toString();

            // If current 'activityContact's' entry is already present in 'connectionIdGroupedActivitySubIdListMap' map
            // It means, the new entry will be duplicate. So we are adding uniqueId to the list
            if (connectionIdGroupedActivitySubIdListMap.get(activityContactConnectionId) != null) {
                connectionIdGroupedActivitySubIdListMap.get(activityContactConnectionId).add(activityContactsUniqueId);
            } else {
                connectionIdGroupedActivitySubIdListMap.put(activityContactConnectionId, new ArrayList<>(Arrays.asList(activityContactsUniqueId)));
            }
        }
        return connectionIdGroupedActivitySubIdListMap;
    }

    private void checkAndSetUserConnectionStatus(String currentLoggedinUserId, UserConnection userConnection, UserContactData userContactData) {
        ContactNumberDTO targetUserContactNumberDTO = null;

        if (userConnection.getContactStaticData() != null) {
            List<UserProfileData> userProfileDataList = userConnection.getContactStaticData().getUserMetadataList();
            for (UserProfileData userProfileData : PeopleUtils.emptyIfNull(userProfileDataList)) {
                if (UserInfoCategory.CONTACT_NUMBER.getValue().equalsIgnoreCase(userProfileData.getCategory())) {
                    targetUserContactNumberDTO = userProfileData.getContactNumber();
                    break;
                }
            }
            updateConnectionStatusRespectToTargetUser(currentLoggedinUserId, targetUserContactNumberDTO, null, userContactData);
        } else if (userConnection.getConnectionToId() != null) {
            updateConnectionStatusRespectToTargetUser(currentLoggedinUserId, null,
                    userConnection.getConnectionToId(), userContactData);
        }
    }

    private void updateConnectionStatusRespectToTargetUser(String currentLoggedinUserId, ContactNumberDTO targetUserContactNumberDTO,
                                                           String sharedUserId, UserContactData userContactData) {
        UserActivity existingSendConnectionRequestActivity = null;

        if (targetUserContactNumberDTO != null) {
            existingSendConnectionRequestActivity =
                    userActivityRepository.getPendingConnectionRequestActivityByContactNumber(currentLoggedinUserId, targetUserContactNumberDTO);
        } else if (sharedUserId != null) {
            existingSendConnectionRequestActivity = userActivityRepository.getPendingConnectionRequestActivity(currentLoggedinUserId, sharedUserId);
        }

        if (existingSendConnectionRequestActivity == null) {
            userContactData.setConnectionStatus(ConnectionStatus.NOT_CONNECTED.getValue());
        } else {
            userContactData.setConnectionStatus(ConnectionStatus.PENDING.getValue());
        }
    }

    private UserInformationDTO prepareStaticProfileDataforStaticIntroduce(UserInformationDTO staticProfileData) {
        UserProfileData userProfileData = new UserProfileData();
        userProfileData.setCategory("PHONENUMBER");
        userProfileData.setLabel("PL.00.00");

        KeyValueData countryCodeKeyValueData = new KeyValueData();
        countryCodeKeyValueData.setKey("countryCode");
        countryCodeKeyValueData.setVal(staticProfileData.getContactNumber().getCountryCode());

        KeyValueData phoneNumberKeyValueData = new KeyValueData();
        phoneNumberKeyValueData.setKey("phoneNumber");
        phoneNumberKeyValueData.setVal(staticProfileData.getContactNumber().getPhoneNumber());

        List<KeyValueData> keyValueDataList = new ArrayList<>();
        keyValueDataList.add(countryCodeKeyValueData);
        keyValueDataList.add(phoneNumberKeyValueData);

        userProfileData.setKeyValueDataList(keyValueDataList);

        UserInformationDTO preparedStaticProfileData = new UserInformationDTO();
        List<UserProfileData> userMetadataList = new ArrayList<>(Arrays.asList(userProfileData));
        preparedStaticProfileData.setUserMetadataList(userMetadataList);

        return preparedStaticProfileData;
    }

    /*
      This method will cast SharedContactDetails
      object value to UserContactObject for share contact
      functionality
     */
    private UserContactData prepareUserContactDataFromSharedContactDetails(SharedContactDetails sharedContactDetails) {
        UserContactData userContactData = new UserContactData();

        if (sharedContactDetails.getSharedProfileData() != null) {
            if (sharedContactDetails.getStaticProfileData() == null) {
                sharedContactDetails.setStaticProfileData(new UserInformationDTO());
            }
            masterService.mergeSharedInfoToStaticInfo(sharedContactDetails.getSharedProfileData(), sharedContactDetails.getStaticProfileData());
        }
        userContactData.setStaticProfileData(sharedContactDetails.getStaticProfileData());

        return userContactData;
    }

    private UserActivityData prepareActivityDataBasedOnRequestType(UserActivity userActivity) {

        RequestType requestType = userActivity.getActivityType().getRequestType();

        switch (requestType) {
            case CONNECTION_REQUEST:
            case SHARE_CONTACT_ACTIVITY:
            case INTRODUCTION_REQUEST:
            case MORE_INFO_REQUEST:
            case UPDATE_CONTACT_ACTIVITY:
            case NETWORK_JOIN_REQUEST:
            case NETWORK_ADMIN_PROMOTION:
            case NETWORK_OWNERSHIP_TRANSFER:
            case NETWORK_SHARE:
            case NETWORK_MEMBER_INVITE:
            case NETWORK_MESSAGE_BROADCAST:
            case NETWORK_JOIN_REQUEST_ACCEPTED:
                return prepareActivityDetailsWithInitiatorInformation(userActivity);
            case SHARE_LOCATION_ACTIVITY:
                return prepareActivityDataForShareLocation(userActivity);
            default:
                throw new BadRequestException(MessageCodes.INVALID_OPERATION.getValue());
        }
    }

    private UserActivityData prepareActivityDetailsWithInitiatorInformation(UserActivity userActivity) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();

        UserActivityData userActivityData = new UserActivityData();

        // Prepare 'initiatorDetails' node
        PeopleUser initiator = peopleUserService.findUserByUserId(userActivity.getActivityById());

        // Prepare 'activityDescription' node
        String activityDescription = prepareActivityDescription(userActivity);

        // Prepare 'activityDetails' node
        userActivityData.setActivityDetails(prepareActivityDetails(userActivity, activityDescription, Boolean.FALSE));

        userActivityData.setInitiatorDetails(prepareTargetUserData(sessionUser, initiator));


        return userActivityData;
    }

    private UserActivityData prepareActivityDataForShareLocation(UserActivity userActivity) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();

        UserActivityData userActivityData = new UserActivityData();

        // Prepare 'initiatorDetails' node
        PeopleUser initiator = peopleUserService.findUserByUserId(userActivity.getActivityById());

        // Prepare 'activityDescription' node
        String activityDescription = prepareActivityDescription(userActivity);

        // Prepare 'activityDetails' node
        userActivityData.setActivityDetails(prepareActivityDetails(userActivity, activityDescription, Boolean.FALSE));

        userActivityData.setInitiatorDetails(prepareTargetUserData(sessionUser, initiator));

        userActivityData.setSharedLocationDetails(prepareSharedLocationDetails(userActivity));

        return userActivityData;
    }

    private List<ActivityContact> getActivityContactList(List<String> activitySubIdList, List<String> activityIdList,
                                                         String initiatorUserId) {
        if (activitySubIdList != null) {
            return activityContactRepository.getActivityContactsByIdsAndUserId(activitySubIdList, initiatorUserId);
        } else if (activityIdList != null) {
            return activityContactRepository.getActivityContactsByActivityIdsAndUserId(activityIdList, initiatorUserId);
        } else {
            return new ArrayList<>();
        }
    }

    private void updateActivityAndActivityContacts(List<ActivityContact> activityContactList,
                                                   List<UserActivity> userActivityList) {


        Set<String> involvedActivitySet = new HashSet<>();
        Map<String, List<ActivityContact>> activityContactMap = new HashMap<>();
        // prepare involved activity set for 'shared contacts' and 'introduction'
        prepareInvolvedActivitySet(activityContactList, involvedActivitySet, activityContactMap);

        userActivityList = validateActivity(userActivityList, involvedActivitySet);

        Map<String, UserActivity> userActivityMap = mapActivityIdWithActivity(userActivityList);

        for (Map.Entry<String, List<ActivityContact>> entrySet : activityContactMap.entrySet()) {
            String userActivityId = entrySet.getKey();
            List<ActivityContact> activityContacts = entrySet.getValue();

            UserActivity targetActivity = userActivityMap.get(userActivityId);

            // Update user activity based on activity contacts type
            updateTargetUserActivityByActivityContact(targetActivity, activityContacts);

            // Update 'targetActivity' record
            targetActivity.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
            userActivityRepository.save(targetActivity);

        }
    }

    @Override
    public void updateTargetUserActivityByActivityContact(UserActivity targetActivity, List<ActivityContact> activityContacts) {
        for (ActivityContact activityContact : activityContacts) {

            if (activityContact.getRequestType().equals(RequestType.SHARE_CONTACT_ACTIVITY)) {
                targetActivity.getSharedConnectionIdList().remove(activityContact.getConnectionId());

                if (PeopleUtils.isNullOrEmpty(targetActivity.getSharedConnectionIdList())) {
                    targetActivity.setOverallStatus(ActivityStatus.INACTIVE);
                }

            } else if (activityContact.getRequestType().equals(RequestType.INTRODUCTION_REQUEST)) {
                ContactNumberDTO contactNumberDTO = activityContact.getIntroducedContactNumber();
                targetActivity.getIntroducedContactNumber().remove(contactNumberDTO.getCountryCode()
                        + "_" + contactNumberDTO.getPhoneNumber());

                if (PeopleUtils.isNullOrEmpty(targetActivity.getIntroducedContactNumber())) {
                    targetActivity.setOverallStatus(ActivityStatus.INACTIVE);
                }
            }

            // Update already processed 'activityContact'
            activityContact.setIsActive(Boolean.FALSE);
            activityContactRepository.save(activityContact);
        }
    }

    private void prepareInvolvedActivitySet(List<ActivityContact> activityContactList, Set<String> involvedActivitySet,
                                            Map<String, List<ActivityContact>> activityContactMap) {
        for (ActivityContact activityContact : activityContactList) {
            involvedActivitySet.add(activityContact.getActivityId());

            if (activityContactMap.containsKey(activityContact.getActivityId())) {
                activityContactMap.get(activityContact.getActivityId()).add(activityContact);

            } else {
                List<ActivityContact> activityContactsList = new ArrayList<>();
                activityContactsList.add(activityContact);
                activityContactMap.put(activityContact.getActivityId(), activityContactsList);
            }
        }
    }

    private List<UserActivity> validateActivity(List<UserActivity> userActivityList, Set<String> involvedActivitySet) {
        if (userActivityList == null) {
            userActivityList = userActivityRepository.getPendingActivityById(new ArrayList<>(involvedActivitySet));
        }

        if (PeopleUtils.isNullOrEmpty(userActivityList)) {
            throw new BadRequestException(MessageCodes.INVALID_ACTIVITY.getValue());
        }

        return userActivityList;
    }

    private void validateUserConnectionList(List<UserConnection> userConnectionList) {
        if (PeopleUtils.isNullOrEmpty(userConnectionList)) {
            throw new BadRequestException(MessageCodes.INVALID_OPERATION.getValue());
        }
    }

    private Map<String, UserActivity> mapActivityIdWithActivity(List<UserActivity> userActivityList) {
        Map<String, UserActivity> userActivityMap = new HashMap<>();

        for (UserActivity userActivity : userActivityList) {
            userActivityMap.put(userActivity.getActivityId(), userActivity);
        }

        return userActivityMap;
    }

    private SharedLocationWithMeDetails prepareSharedLocationDetails(UserActivity userActivity) {

        String sharedByUserId = userActivity.getActivityById();

        // get user location
        Coordinates userDeviceLocation = getUserLocation(sharedByUserId);
        if (userDeviceLocation == null) {
            return null;
        }

        SharedLocationWithMeDetails sharedLocation = new SharedLocationWithMeDetails();
        sharedLocation.setActivityId(userActivity.getActivityId());
        sharedLocation.setSharedByUserId(userActivity.getActivityById());
        sharedLocation.setUserLocation(userDeviceLocation);
        sharedLocation.setLocationSharedOn(userActivity.getCreatedOn());
        sharedLocation.setSharedForTimeInMinutes(userActivity.getLocationSharedForTime());
        sharedLocation.setCurrentTime(PeopleUtils.getCurrentTimeInUTC());

        return sharedLocation;
    }

    private Sort getSortOperationForActivityContacts(Integer fNameOrder, Integer lNameOrder, Boolean lNamePreferred) {
        Set<SortElement> set = new TreeSet<>();

        if (lNamePreferred) {
            set.add(new SortElement(LAST_NAME, lNameOrder, 1));
            set.add(new SortElement(FIRST_NAME, fNameOrder, 2));
        } else {
            set.add(new SortElement(FIRST_NAME, fNameOrder, 1));
            set.add(new SortElement(LAST_NAME, lNameOrder, 2));
        }
        return PeopleUtils.getSort(set);
    }

    private UserContactData updateConnectionStatusForCancelSentConnectionRequest(PeopleUser connectionRequestInitiator,
                                                                                 UserActivity pendingActivity,
                                                                                 boolean isCancelActivity) {
        UserContactData contactData = null;
        if (pendingActivity.getActivityType().getRequestType().equals(RequestType.CONNECTION_REQUEST)
                && pendingActivity.getInitiateDetails() != null
                && pendingActivity.getInitiateDetails().getConnectionId() != null) {
            String connectionId = pendingActivity.getInitiateDetails().getConnectionId();

            // check if the "connection_id" is part of
            // connection request initiator contact list
            UserConnection userConnection = userConnectionRepository.
                    findConnectionByConnectionIdAndInitiatorId(connectionRequestInitiator.getUserId(), connectionId);
            if (userConnection != null && userConnection.getConnectionStatus().equals(ConnectionStatus.PENDING)) {
                contactData = updateUserConnectionData(connectionRequestInitiator, userConnection, isCancelActivity);
            }
        }
        return contactData;
    }

    private UserContactData updateUserConnectionData(PeopleUser connectionRequestInitiator, UserConnection userConnection,
                                                     boolean isCancelActivity) {
        UserContactData contactData = null;
        userConnection.setConnectionStatus(ConnectionStatus.NOT_CONNECTED);
        // Updating last updated time to make
        // 'user connection' record eligible for delta update member
        userConnection.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
        userConnectionRepository.save(userConnection);

        if (isCancelActivity) {

            // contacts group information
            Map<String, List<String>> contactToGroupMap =
                    userConnectionService.prepareConnectionIdToGroupIdMap(connectionRequestInitiator.getUserId());
            // update group information for contact
            if (contactToGroupMap.containsKey(userConnection.getConnectionId())) {
                userConnection.setGroupIdList(contactToGroupMap.get(userConnection.getConnectionId()));
            }

            contactData = userConnectionService.prepareContactStaticData(connectionRequestInitiator, userConnection);
            UserInformationDTO contactStaticData = contactData.getStaticProfileData();
            if (contactStaticData != null) {
                userConnectionService.populateStaticDataWithIsVerifiedInfo(contactStaticData,
                        masterService.getRegisteredNumberList());
            }
        }

        return contactData;
    }
    private ActivityListResponse prepareActivityListResponse(Page<UserActivity> userActivityList,Link link,PeopleUser sessionUser){
        // prepare response
        ActivityListResponse response = new ActivityListResponse();
        List<UserActivityData> userActivityDataList = new ArrayList<>();

        for (UserActivity userActivity : PeopleUtils.emptyIfNull(userActivityList)) {
            UserActivityData userActivityData = new UserActivityData();

            // Prepare 'initiatorDetails' node
            PeopleUser initiator = peopleUserService.findUserByUserId(userActivity.getActivityById());
            if (initiator == null) {
                continue;
            }

            // Prepare 'activityDescription' node
            String activityDescription = prepareActivityDescription(userActivity);

            // Prepare 'activityDetails' node
            userActivityData.setActivityDetails(prepareActivityDetails(userActivity, activityDescription, Boolean.FALSE));

            userActivityData.setInitiatorDetails(prepareTargetUserDataByActivityList(sessionUser, initiator));

            userActivityDataList.add(userActivityData);
        }

        response.setUserActivityList(userActivityDataList);
        response.setTotalNumberOfPages(userActivityList.getTotalPages());
        response.setTotalElements(userActivityList.getTotalElements());
        if (!userActivityList.isLast()) {
            response.setNextURL(link.getHref());
        }
        return response;
    }
}
