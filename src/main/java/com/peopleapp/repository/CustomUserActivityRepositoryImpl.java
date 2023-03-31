package com.peopleapp.repository;

import com.mongodb.client.result.UpdateResult;
import com.peopleapp.constant.PeopleCollectionKeys;
import com.peopleapp.dto.ContactNumberDTO;
import com.peopleapp.enums.Action;
import com.peopleapp.enums.ActivityStatus;
import com.peopleapp.enums.RequestType;
import com.peopleapp.model.UserActivity;
import com.peopleapp.model.UserConnection;
import com.peopleapp.util.PeopleUtils;
import org.joda.time.DateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
public class CustomUserActivityRepositoryImpl implements CustomUserActivityRepository {

    @Inject
    private MongoOperations mongoOperations;

    @Inject
    private MongoTemplate mongoTemplate;

    private String collectionName = PeopleCollectionKeys.Collection.USER_ACTIVITY.getCollectionName();

    private static final String INITIATED_BY_ID = "peopleUserById";
    private static final String REQUEST_TYPE = "activityType.requestType";
    private static final String ACTIVITY_ACTION = "activityType.actionTaken";
    private static final String INITIATED_TO_ID = "peopleUserToId";
    private static final String ACTIVITY_STATUS = "overallStatus";
    private static final String INITIATED_TO_NUMBER = "initiateDetails.contactNumber.phoneNumber";
    private static final String INITIATED_TO_CODE = "initiateDetails.contactNumber.countryCode";
    private static final String INITIATE_CONNECTION_ID = "initiateDetails.connectionId";
    private static final String UNIQUE_ID = "_id";
    private static final String NETWORK_ID = "networkId";
    private static final String LAST_UPDATED_ON = "lastUpdatedOn";
    private static final String ACTIVITY_IDS = "activityIds";
    private static final String SHARED_CONNECTION_IDS = "sharedConnectionIdList";
    private static final String SHARED_CONNECTION_IDS_LIST = "listOfSharedConnectionIds";
    private static final String IS_CLEARED = "isCleared";
    private static final String IS_INITIATOR_BLOCKED = "isInitiatorBlocked";
    private static final String SHARED_PRIVACY_PROFILE_ID = "sharedProfileInformationData.privacyProfileId";
    private static final String IS_READ = "isRead";

    @Override
    public List<UserActivity> getConnectionRequestCountForTimeRange(String fromUserId, DateTime startTime, DateTime latestTime) {
        Query query = new Query(Criteria.where(INITIATED_BY_ID)
                .is(PeopleUtils.convertStringToObjectId(fromUserId))
                .andOperator(Criteria.where(PeopleCollectionKeys.CREATED_ON).gte(startTime),
                        Criteria.where(PeopleCollectionKeys.CREATED_ON).lte(latestTime)));
        return mongoOperations.find(query, UserActivity.class, collectionName);
    }


    @Override
    public UpdateResult updateRequestWithDefaultProfile(String defaultProfileId, List<String> deletedProfileList) {
        Query query = new Query(
                Criteria.where(SHARED_PRIVACY_PROFILE_ID).in(deletedProfileList));
        Update update = new Update();
        update.set(SHARED_PRIVACY_PROFILE_ID, defaultProfileId);

        return mongoOperations.updateMulti(query, update, UserConnection.class, collectionName);
    }

    @Override
    public Page<UserActivity> findByInitiatedByIdPageable(String userId, Pageable pageable) {

        Query query = new Query(
                Criteria.where(INITIATED_BY_ID).is(PeopleUtils.convertStringToObjectId(userId))
                        .and(ACTIVITY_STATUS).in(Arrays.asList(ActivityStatus.PENDING, ActivityStatus.ACTIVE))
                        .and(REQUEST_TYPE).nin(Arrays
                        .asList(RequestType.NETWORK_OWNERSHIP_TRANSFER, RequestType.NETWORK_ADMIN_PROMOTION,
                                RequestType.UPDATE_CONTACT_ACTIVITY, RequestType.NETWORK_SHARE,
                                RequestType.NETWORK_MEMBER_INVITE, RequestType.NETWORK_MESSAGE_BROADCAST))
        ).with(pageable);

        List<UserActivity> userActivities = mongoOperations.find(query, UserActivity.class, collectionName);

        return new PageImpl<>(userActivities, pageable, mongoOperations.count(query, collectionName));
    }

    @Override
    public List<UserActivity> findByInitiatedToId(String userId) {
        Query query = new Query(
                Criteria.where(INITIATED_TO_ID).is(PeopleUtils.convertStringToObjectId(userId))
        );
        return mongoOperations.find(query, UserActivity.class, collectionName);
    }

    @Override
    public Page<UserActivity> findPendingJoinRequestForNetwork(String userId, String networkId, Pageable pageable) {
        Query pendingRequests = getPendingJoinRequestForNetworkQuery(userId, networkId).with(pageable);

        List<UserActivity> userActivityList = mongoOperations.find(pendingRequests, UserActivity.class, collectionName);

        return new PageImpl<>(userActivityList, pageable, mongoOperations.count(pendingRequests, collectionName));
    }

    @Override
    public List<UserActivity> findPendingJoinRequestForNetwork(String userId, String networkId) {

        Query query = getPendingJoinRequestForNetworkQuery(userId, networkId);

        return mongoOperations.find(query, UserActivity.class, collectionName);
    }

    @Override
    public long getPendingJoinRequestCountForNetwork(String userId, String networkId) {
        Query query = getPendingJoinRequestForNetworkQuery(userId, networkId);
        return mongoOperations.count(query, collectionName);
    }

    @Override
    public List<UserActivity> getPendingActivityById(List<String> activityIdList) {
        List<String> activityStatusList = new ArrayList<>();
        activityStatusList.add(ActivityStatus.ACTIVE.getValue());
        activityStatusList.add(ActivityStatus.PENDING.getValue());
        Query query = new Query(
                Criteria.where(UNIQUE_ID).in(PeopleUtils.convertStringToObjectId(activityIdList))
                        .and(ACTIVITY_STATUS).in(activityStatusList)
        );

        return mongoOperations.find(query, UserActivity.class, collectionName);
    }

    @Override
    public UserActivity getPendingActivityById(String activityId) {

        List<String> activityStatusList = new ArrayList<>();
        activityStatusList.add(ActivityStatus.ACTIVE.getValue());
        activityStatusList.add(ActivityStatus.PENDING.getValue());
        activityStatusList.add(ActivityStatus.INFORMATIVE.getValue());
        Query query = new Query(
                Criteria.where(UNIQUE_ID).is(activityId)
                        .and(ACTIVITY_STATUS).in(activityStatusList)
        );

        return mongoOperations.findOne(query, UserActivity.class, collectionName);
    }

    @Override
    public List<UserActivity> getAllActivitiesByInitiatedToIdAndActivityIds(String userId, List<String> activityIdList) {

        Query query = new Query(
                Criteria.where(UNIQUE_ID).in(activityIdList)
                        .and(INITIATED_TO_ID).is(PeopleUtils.convertStringToObjectId(userId)));

        return mongoOperations.find(query, UserActivity.class, collectionName);
    }

    @Override
    public List<UserActivity> getActivityListByContactNumber(ContactNumberDTO contactNumber) {
        Query query = new Query(
                Criteria.where(INITIATED_TO_CODE).is(contactNumber.getCountryCode())
                        .and(INITIATED_TO_NUMBER).is(contactNumber.getPhoneNumber())
                        .and(ACTIVITY_STATUS).nin(Arrays.asList(ActivityStatus.EXPIRED, ActivityStatus.INACTIVE))
        );

        return mongoOperations.find(query, UserActivity.class, collectionName);
    }

    @Override
    public List<UserActivity> getAllUserActivities(String userId) {
        Query query = new Query(
                Criteria.where(INITIATED_TO_ID).is(PeopleUtils.convertStringToObjectId(userId))
                        .and(IS_CLEARED).is(Boolean.FALSE)
                        .and(ACTIVITY_STATUS).ne(ActivityStatus.EXPIRED)
        );
        return mongoOperations.find(query, UserActivity.class, collectionName);
    }

    @Override
    public Page<UserActivity> getAllUserActivitiesPageable(String userId, Pageable pageable) {
        Query query = new Query(
                Criteria.where(INITIATED_TO_ID).is(PeopleUtils.convertStringToObjectId(userId))
                        .and(IS_CLEARED).is(Boolean.FALSE)
                        .and(IS_INITIATOR_BLOCKED).is(Boolean.FALSE)
                        .and(ACTIVITY_STATUS).in(Arrays.asList(ActivityStatus.ACTIVE,
                        ActivityStatus.PENDING))

        ).with(pageable);

        List<UserActivity> userActivities = mongoOperations.find(query, UserActivity.class, collectionName);

        return new PageImpl<>(userActivities, pageable, mongoOperations.count(query, collectionName));
    }

    @Override
    public Page<UserActivity> getActionableActivitiesReceivedByUser(String userId, Pageable pageable) {
        Query query = new Query(
                Criteria.where(INITIATED_TO_ID).is(PeopleUtils.convertStringToObjectId(userId))
                        .and(IS_CLEARED).is(Boolean.FALSE)
                        .and(IS_INITIATOR_BLOCKED).is(Boolean.FALSE)
                        .and(ACTIVITY_STATUS).in(Arrays.asList(ActivityStatus.INFORMATIVE, ActivityStatus.ACTIVE,
                        ActivityStatus.PENDING))
                        .and(REQUEST_TYPE).in(Arrays.asList(RequestType.CONNECTION_REQUEST, RequestType.MORE_INFO_REQUEST,
                        RequestType.INTRODUCTION_REQUEST, RequestType.SHARE_LOCATION_ACTIVITY,
                        RequestType.SHARE_CONTACT_ACTIVITY, RequestType.NETWORK_MEMBER_INVITE,
                        RequestType.NETWORK_SHARE))
        ).with(pageable);

        List<UserActivity> userActivities = mongoOperations.find(query, UserActivity.class, collectionName);

        return new PageImpl<>(userActivities, pageable, mongoOperations.count(query, collectionName));
    }

    @Override
    public List<UserActivity> getPendingIntroductionActivities(String initiateId) {

        Query query = new Query(
                Criteria.where(INITIATED_TO_ID).is(PeopleUtils.convertStringToObjectId(initiateId))
                        .and(ACTIVITY_ACTION).is(Action.INITIATED)
                        .and(REQUEST_TYPE).is(RequestType.INTRODUCTION_REQUEST)
                        .and(ACTIVITY_STATUS).is(ActivityStatus.PENDING.getValue())
        );

        return mongoOperations.find(query, UserActivity.class, collectionName);
    }

    @Override
    public void expireActivityForInitiate(String activityById, List<String> connectionIdList) {

        Query query = new Query(
                Criteria.where(INITIATED_BY_ID).in(PeopleUtils.convertStringToObjectId(activityById))
                        .and(INITIATE_CONNECTION_ID).in(connectionIdList)
        );

        Update update = new Update();
        update.set(ACTIVITY_STATUS, ActivityStatus.EXPIRED);
        update.set(LAST_UPDATED_ON, PeopleUtils.getCurrentTimeInUTC());

        mongoOperations.updateMulti(query, update, UserActivity.class, collectionName);
    }

    @Override
    public void expireActivityInitiatedByRemovedContact(String sessionUserId, List<String> removedContactsUserId,
                                                        List<RequestType> activityType) {
        Query query = new Query(
                Criteria.where(INITIATED_TO_ID).is(PeopleUtils.convertStringToObjectId(sessionUserId))
                        .and(INITIATED_BY_ID).in(PeopleUtils.convertStringToObjectId(removedContactsUserId))
                        .and(REQUEST_TYPE).in(activityType)
        );

        Update update = new Update();
        update.set(ACTIVITY_STATUS, ActivityStatus.EXPIRED);
        update.set(LAST_UPDATED_ON, PeopleUtils.getCurrentTimeInUTC());

        mongoOperations.updateMulti(query, update, UserActivity.class, collectionName);
    }

    @Override
    public UserActivity getPendingConnectionRequestActivity(String initiatorId, ContactNumberDTO initiateContactNumber) {

        Query query = new Query(
                Criteria.where(INITIATED_BY_ID).in(PeopleUtils.convertStringToObjectId(initiatorId))
                        .and(REQUEST_TYPE).is(RequestType.CONNECTION_REQUEST)
                        .and(ACTIVITY_ACTION).is(Action.INITIATED)
                        .and(ACTIVITY_STATUS).is(ActivityStatus.PENDING.getValue())
                        .and(INITIATED_TO_CODE).is(initiateContactNumber.getCountryCode())
                        .and(INITIATED_TO_NUMBER).is(initiateContactNumber.getPhoneNumber())
        );

        return mongoOperations.findOne(query, UserActivity.class, collectionName);

    }

    @Override
    public UserActivity getPendingConnectionRequestActivity(String initiatorId, String receiverId) {

        if (initiatorId == null || receiverId == null) {
            return null;
        }

        Query query = new Query(
                Criteria.where(INITIATED_BY_ID).is(PeopleUtils.convertStringToObjectId(initiatorId))
                        .and(REQUEST_TYPE).is(RequestType.CONNECTION_REQUEST)
                        .and(ACTIVITY_ACTION).is(Action.INITIATED)
                        .and(ACTIVITY_STATUS).is(ActivityStatus.PENDING.getValue())
                        .and(INITIATED_TO_ID).is(PeopleUtils.convertStringToObjectId(receiverId))
        );

        return mongoOperations.findOne(query, UserActivity.class, collectionName);

    }

    @Override
    public List<UserActivity> getActiveLocationShareActivityByUser(String initiatorId) {

        Query query = new Query(
                Criteria.where(INITIATED_BY_ID).in(PeopleUtils.convertStringToObjectId(initiatorId))
                        .and(REQUEST_TYPE).is(RequestType.SHARE_LOCATION_ACTIVITY)
                        .and(ACTIVITY_STATUS).is(ActivityStatus.ACTIVE.getValue())
        );

        return mongoOperations.find(query, UserActivity.class, collectionName);
    }

    @Override
    public Page<UserActivity> getActiveLocationShareActivityByUserPageable(String initiatorId, Pageable pageable) {

        Query query = new Query(
                Criteria.where(INITIATED_BY_ID).in(PeopleUtils.convertStringToObjectId(initiatorId))
                        .and(REQUEST_TYPE).is(RequestType.SHARE_LOCATION_ACTIVITY)
                        .and(ACTIVITY_STATUS).is(ActivityStatus.ACTIVE.getValue())
        ).with(pageable)
                .with(new Sort(Sort.Direction.DESC, LAST_UPDATED_ON))
                .skip((long) (pageable.getPageNumber()) * (pageable.getPageSize()));

        List<UserActivity> userActivities = mongoOperations.find(query, UserActivity.class, collectionName);

        return new PageImpl<>(userActivities, pageable, mongoOperations.count(query, collectionName));
    }

    @Override
    public Page<UserActivity> getActiveLocationShareActivityForUser(String userId, Pageable pageable) {

        Query query = new Query(
                Criteria.where(INITIATED_TO_ID).in(PeopleUtils.convertStringToObjectId(userId))
                        .and(REQUEST_TYPE).is(RequestType.SHARE_LOCATION_ACTIVITY)
                        .and(ACTIVITY_STATUS).is(ActivityStatus.ACTIVE.getValue())
        ).with(pageable)
                .with(new Sort(Sort.Direction.DESC, LAST_UPDATED_ON))
                .skip((long) (pageable.getPageNumber()) * (pageable.getPageSize()));

        List<UserActivity> userActivities = mongoOperations.find(query, UserActivity.class, collectionName);

        return new PageImpl<>(userActivities, pageable, mongoOperations.count(query, collectionName));
    }

    @Override
    public Page<UserActivity> getContactSharedActivityByUser(String initiatorId, Pageable pageable) {

        Query query = new Query(
                Criteria.where(INITIATED_BY_ID).in(PeopleUtils.convertStringToObjectId(initiatorId))
                        .and(REQUEST_TYPE).is(RequestType.SHARE_CONTACT_ACTIVITY.toString())
                        .and(ACTIVITY_STATUS).is(ActivityStatus.ACTIVE.getValue())
        );

        MatchOperation matchOperation = match(
                Criteria.where(INITIATED_BY_ID).in(PeopleUtils.convertStringToObjectId(initiatorId))
                        .and(REQUEST_TYPE).is(RequestType.SHARE_CONTACT_ACTIVITY.toString())
                        .and(ACTIVITY_STATUS).is(ActivityStatus.ACTIVE.getValue())
        );

        Aggregation aggregation = Aggregation.newAggregation(
                matchOperation, Aggregation.group(INITIATED_TO_ID).addToSet(UNIQUE_ID).as(ACTIVITY_IDS)
                        .first(INITIATED_TO_ID).as(INITIATED_TO_ID).addToSet(SHARED_CONNECTION_IDS).as(SHARED_CONNECTION_IDS_LIST),
                skip((long) (pageable.getPageNumber()) * (pageable.getPageSize())),
                limit(pageable.getPageSize()));

        AggregationResults<UserActivity> userActivities = mongoTemplate.aggregate(aggregation, collectionName, UserActivity.class);

        return new PageImpl<>(userActivities.getMappedResults(), pageable, mongoOperations.count(query, collectionName));
    }

    @Override
    public Page<UserActivity> getContactSharedActivityForUser(String userId, Pageable pageable) {

        Query query = new Query(
                Criteria.where(INITIATED_TO_ID).in(PeopleUtils.convertStringToObjectId(userId))
                        .and(REQUEST_TYPE).is(RequestType.SHARE_CONTACT_ACTIVITY.toString())
                        .and(ACTIVITY_STATUS).is(ActivityStatus.ACTIVE.getValue())
        );

        MatchOperation matchOperation = match(
                Criteria.where(INITIATED_TO_ID).in(PeopleUtils.convertStringToObjectId(userId))
                        .and(REQUEST_TYPE).is(RequestType.SHARE_CONTACT_ACTIVITY.toString())
                        .and(ACTIVITY_STATUS).is(ActivityStatus.ACTIVE.getValue())
        );


        Aggregation aggregation = Aggregation.newAggregation(
                matchOperation, Aggregation.group(INITIATED_BY_ID).addToSet(UNIQUE_ID).as(ACTIVITY_IDS)
                        .first(INITIATED_BY_ID).as(INITIATED_BY_ID).addToSet(SHARED_CONNECTION_IDS).as(SHARED_CONNECTION_IDS_LIST),
                skip((long) (pageable.getPageNumber()) * (pageable.getPageSize())),
                limit(pageable.getPageSize()));

        AggregationResults<UserActivity> userActivities = mongoTemplate.aggregate(aggregation, collectionName, UserActivity.class);

        return new PageImpl<>(userActivities.getMappedResults(), pageable, mongoOperations.count(query, collectionName));
    }

    @Override
    public UserActivity findJoinNetworkPendingRequestByActivityIdAndStatus(String activityId, String activityStatus) {
        Query query = new Query(
                Criteria.where(UNIQUE_ID).is(activityId)
                        .and(ACTIVITY_STATUS).is(activityStatus)
                        .and(REQUEST_TYPE).is(RequestType.NETWORK_JOIN_REQUEST)
        );

        return mongoOperations.findOne(query, UserActivity.class, collectionName);
    }

    @Override
    public UserActivity getPendingNetworkRequestActivityForUser(String userId, String networkId, String activityStatus) {

        Query query = new Query(
                Criteria.where(NETWORK_ID).is(PeopleUtils.convertStringToObjectId(networkId))
                        .and(INITIATED_BY_ID).is(PeopleUtils.convertStringToObjectId(userId))
                        .and(REQUEST_TYPE).is(RequestType.NETWORK_JOIN_REQUEST)
                        .and(ACTIVITY_ACTION).is(Action.INITIATED)
                        .and(ACTIVITY_STATUS).is(activityStatus));

        return mongoOperations.findOne(query, UserActivity.class, collectionName);
    }

    @Override
    public UserActivity getPendingConnectionRequestActivityByContactNumber(String currentLoggedinUserId, ContactNumberDTO targetUserContactNumberDTO) {
        Query query = new Query(
                Criteria.where(INITIATED_TO_CODE).is(targetUserContactNumberDTO.getCountryCode())
                        .and(INITIATED_TO_NUMBER).is(targetUserContactNumberDTO.getPhoneNumber())
                        .and(INITIATED_BY_ID).is(PeopleUtils.convertStringToObjectId(currentLoggedinUserId))
                        .and(REQUEST_TYPE).is(RequestType.CONNECTION_REQUEST)
                        .and(ACTIVITY_STATUS).is(ActivityStatus.PENDING.getValue())
        );

        return mongoOperations.findOne(query, UserActivity.class, collectionName);
    }

    @Override
    public long getCountOfSentNetworkRequestActivity(String networkId, String userId) {
        Query sentRequestCount = new Query(
                Criteria
                        .where(NETWORK_ID).is(PeopleUtils.convertStringToObjectId(networkId))
                        .and(INITIATED_BY_ID).is(PeopleUtils.convertStringToObjectId(userId))
                        .and(REQUEST_TYPE).is(RequestType.NETWORK_JOIN_REQUEST)
                        .and(ACTIVITY_STATUS).is(ActivityStatus.PENDING.getValue()));
        return mongoOperations.count(sentRequestCount, UserActivity.class, collectionName);
    }

    @Override
    public void updateAllJoinRequestWithActivityByIdsAndNetworkId(List<String> activityById, String networkId,
                                                                  boolean requestAccepted) {
        Query joinRequest = new Query(
                Criteria
                        .where(NETWORK_ID).is(PeopleUtils.convertStringToObjectId(networkId))
                        .and(INITIATED_BY_ID).in(PeopleUtils.convertStringToObjectId(activityById))
                        .and(REQUEST_TYPE).is(RequestType.NETWORK_JOIN_REQUEST)
                        .and(ACTIVITY_STATUS).is(ActivityStatus.PENDING.getValue()));

        Update update = new Update();
        update.set(ACTIVITY_STATUS, ActivityStatus.EXPIRED.getValue());

        if (requestAccepted) {
            update.set(ACTIVITY_ACTION, Action.ACCEPTED);
        } else {
            update.set(ACTIVITY_ACTION, Action.REJECTED);
        }

        mongoTemplate.updateMulti(joinRequest, update, UserActivity.class, collectionName);
    }

    @Override
    public void updateAllNetworkInvitationAndShareActivityForUser(String userId, String networkId) {
        Query acceptInvite = new Query(
                Criteria
                        .where(NETWORK_ID).is(PeopleUtils.convertStringToObjectId(networkId))
                        .and(INITIATED_TO_ID).is(PeopleUtils.convertStringToObjectId(userId))
                        .and(REQUEST_TYPE).in(Arrays.asList(RequestType.NETWORK_MEMBER_INVITE, RequestType.NETWORK_SHARE))
                        .and(ACTIVITY_STATUS).in(Arrays.asList(ActivityStatus.PENDING.getValue(),
                        ActivityStatus.INFORMATIVE.getValue())));

        Update update = new Update();
        update.set(ACTIVITY_STATUS, ActivityStatus.EXPIRED.getValue());
        update.set(ACTIVITY_ACTION, Action.ACCEPTED);

        mongoTemplate.updateMulti(acceptInvite, update, UserActivity.class, collectionName);
    }

    @Override
    public void expireAllActivityOfANetwork(String networkId) {

        Query networkActivities = new Query(
                Criteria
                        .where(NETWORK_ID).is(PeopleUtils.convertStringToObjectId(networkId))
                        .and(ACTIVITY_STATUS).ne(ActivityStatus.EXPIRED.getValue())
        );

        Update update = Update.update(ACTIVITY_STATUS, ActivityStatus.EXPIRED.getValue());

        mongoTemplate.updateMulti(networkActivities, update, UserActivity.class, collectionName);
    }

    @Override
    public void updateAllJoinRequestToNetworkByNetworkId(String networkId, boolean isRequestAccepted) {
        Query joinRequest = new Query(
                Criteria
                        .where(NETWORK_ID).is(PeopleUtils.convertStringToObjectId(networkId))
                        .and(REQUEST_TYPE).is(RequestType.NETWORK_JOIN_REQUEST)
                        .and(ACTIVITY_STATUS).is(ActivityStatus.PENDING.getValue()));

        Update update = new Update();
        update.set(ACTIVITY_STATUS, ActivityStatus.EXPIRED.getValue());

        if (isRequestAccepted) {
            update.set(ACTIVITY_ACTION, Action.ACCEPTED);
        } else {
            update.set(ACTIVITY_ACTION, Action.REJECTED);
        }

        mongoTemplate.updateMulti(joinRequest, update, UserActivity.class, collectionName);
    }

    @Override
    public void expireAllNetworkActivityByActivityForIdAndNetworkId(String userId, String networkId) {
        Query allActivity = new Query(
                Criteria
                        .where(NETWORK_ID).is(PeopleUtils.convertStringToObjectId(networkId))
                        .and(INITIATED_TO_ID).is(PeopleUtils.convertStringToObjectId(userId))
                        .and(ACTIVITY_STATUS).ne(ActivityStatus.EXPIRED.getValue())
        );

        Update update = new Update();
        update.set(ACTIVITY_STATUS, ActivityStatus.EXPIRED.getValue())
                .set(ACTIVITY_ACTION, Action.DELETED);

        mongoTemplate.updateMulti(allActivity, update, UserActivity.class, collectionName);

    }

    @Override
    public List<UserActivity> findPendingActivityByActivityByIdAndForIdAndRequestType(String activityById,
                                                                                      String activityForId,
                                                                                      String networkId,
                                                                                      RequestType activityRequestType) {
        Query pendingActivity = new Query(Criteria
                .where(INITIATED_BY_ID).is(PeopleUtils.convertStringToObjectId(activityById))
                .and(INITIATED_TO_ID).is(PeopleUtils.convertStringToObjectId(activityForId))
                .and(REQUEST_TYPE).is(activityRequestType)
                .and(NETWORK_ID).is(PeopleUtils.convertStringToObjectId(networkId))
                .and(ACTIVITY_STATUS).in(Arrays.asList(ActivityStatus.PENDING, ActivityStatus.INFORMATIVE))
        );

        return mongoOperations.find(pendingActivity, UserActivity.class, collectionName);
    }

    @Override
    public void expireAllActivityRelatedToUser(String userId) {

        Query findAllActivity = new Query(new Criteria().orOperator(
                Criteria
                        .where(INITIATED_TO_ID).is(PeopleUtils.convertStringToObjectId(userId)),
                Criteria
                        .where(INITIATED_BY_ID).is(PeopleUtils.convertStringToObjectId(userId)))
        );

        Update expireAllActivity = Update.update(ACTIVITY_STATUS, ActivityStatus.EXPIRED.getValue());

        mongoOperations.updateMulti(findAllActivity, expireAllActivity, UserActivity.class, collectionName);

    }

    @Override
    public UpdateResult updateBlockedStatusByInitiatorAndReceiverId(String initiatorUserId, String receiverUserId) {
        Query query = new Query(
                Criteria.where(INITIATED_BY_ID).is(PeopleUtils.convertStringToObjectId(initiatorUserId))
                        .and(INITIATED_TO_ID).is(PeopleUtils.convertStringToObjectId(receiverUserId))
                        .and(IS_INITIATOR_BLOCKED).is(Boolean.TRUE)
        );
        Update update = new Update();
        update.set(IS_INITIATOR_BLOCKED, Boolean.FALSE);

        return mongoOperations.updateMulti(query, update, UserActivity.class, collectionName);
    }

    @Override
    public void expireActivityCreatedByDeletedContact(List<String> connectionIdList, String activityById) {

        Query query = new Query(
                Criteria.where(INITIATED_BY_ID).in(PeopleUtils.convertStringToObjectId(connectionIdList))
                        .and(INITIATED_TO_ID).is(PeopleUtils.convertStringToObjectId(activityById))
        );

        Update update = new Update();
        update.set(ACTIVITY_STATUS, ActivityStatus.EXPIRED);
        update.set(LAST_UPDATED_ON, PeopleUtils.getCurrentTimeInUTC());

        mongoOperations.updateMulti(query, update, UserActivity.class, collectionName);
    }

    @Override
    public List<UserActivity> getPendingActivitiesByInitiatedByIdAndRequestType(String userId, RequestType activityRequestType) {
        Query query = new Query(Criteria
                .where(INITIATED_TO_ID).is(PeopleUtils.convertStringToObjectId(userId))
                .and(REQUEST_TYPE).is(activityRequestType)
                .and(ACTIVITY_STATUS).in(Arrays.asList(ActivityStatus.ACTIVE,
                ActivityStatus.PENDING))
        );

        return mongoOperations.find(query, UserActivity.class, collectionName);
    }
    
    @Override
    public List<UserActivity> getActivitiesByInitiatedByIdAndRequestType(String userId, RequestType activityRequestType) {
        Query query = new Query(Criteria
                .where(INITIATED_BY_ID).is(PeopleUtils.convertStringToObjectId(userId))
                .and(REQUEST_TYPE).is(activityRequestType)
                .and(ACTIVITY_STATUS).is(ActivityStatus.PENDING)
        );

        return mongoOperations.find(query, UserActivity.class, collectionName);
    }

    @Override
    public long countOfUnreadActivitiesForGivenUser(String userId) {
        Query query = new Query(
                Criteria.where(INITIATED_TO_ID).is(PeopleUtils.convertStringToObjectId(userId))
                        .and(IS_CLEARED).is(false)
                        .and(IS_READ).is(false)
                        .and(ACTIVITY_STATUS).nin(
                        Arrays.asList(ActivityStatus.EXPIRED, ActivityStatus.INACTIVE, ActivityStatus.NA))
        );
        return mongoOperations.count(query, UserActivity.class, collectionName);
    }

    @Override
    public List<UserActivity> getSharedContactsActivitiesByUserIdAndSharedConnectionIds(String userId, List<String> sharedConnectionIds) {
        Query query = new Query(Criteria
                .where(INITIATED_BY_ID).is(PeopleUtils.convertStringToObjectId(userId))
                .and(REQUEST_TYPE).is(RequestType.SHARE_CONTACT_ACTIVITY)
                .and(ACTIVITY_STATUS).in(Arrays.asList(ActivityStatus.PENDING, ActivityStatus.ACTIVE))
                .and(SHARED_CONNECTION_IDS).in(sharedConnectionIds)
        );

        return mongoOperations.find(query, UserActivity.class, collectionName);
    }

    private Query getPendingJoinRequestForNetworkQuery(String userId, String networkId) {
        return new Query(Criteria
                .where(NETWORK_ID).is(PeopleUtils.convertStringToObjectId(networkId))
                .and(INITIATED_TO_ID).is(PeopleUtils.convertStringToObjectId(userId))
                .and(ACTIVITY_STATUS).is(ActivityStatus.PENDING)
                .and(REQUEST_TYPE).is(RequestType.NETWORK_JOIN_REQUEST)
        );
    }


}