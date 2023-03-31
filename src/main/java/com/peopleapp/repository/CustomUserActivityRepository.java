package com.peopleapp.repository;

import com.mongodb.client.result.UpdateResult;
import com.peopleapp.dto.ContactNumberDTO;
import com.peopleapp.enums.RequestType;
import com.peopleapp.model.UserActivity;
import org.joda.time.DateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomUserActivityRepository {

    List<UserActivity> getConnectionRequestCountForTimeRange(String fromUserId, DateTime startTime, DateTime latestTime);

    UpdateResult updateRequestWithDefaultProfile(String defaultProfileId, List<String> deletedProfileList);

    Page<UserActivity> findByInitiatedByIdPageable(String userId, Pageable pageable);

    List<UserActivity> findByInitiatedToId(String userId);

    /* (Paginated) Fetches all network join activity for a particular network */
    Page<UserActivity> findPendingJoinRequestForNetwork(String userId, String networkId, Pageable pageable);

    /* Fetches all network join activity for a particular network  */
    List<UserActivity> findPendingJoinRequestForNetwork(String userId, String networkId);

    long getPendingJoinRequestCountForNetwork(String userId, String networkId);

    List<UserActivity> getPendingActivityById(List<String> activityIdList);

    UserActivity getPendingActivityById(String activityId);

    List<UserActivity> getAllActivitiesByInitiatedToIdAndActivityIds(String userId, List<String> activityIdList);

    List<UserActivity> getActivityListByContactNumber(ContactNumberDTO contactNumber);

    List<UserActivity> getAllUserActivities(String userId);

    Page<UserActivity> getAllUserActivitiesPageable(String userId, Pageable pageable);

    Page<UserActivity> getActionableActivitiesReceivedByUser(String userId, Pageable pageable);

    List<UserActivity> getPendingIntroductionActivities(String initiateId);

    void expireActivityForInitiate(String activityById, List<String> connectionIdList);

    void expireActivityInitiatedByRemovedContact(String sessionUserId, List<String> removedContactsUserId, List<RequestType> activityType);

    UserActivity getPendingConnectionRequestActivity(String initiatorId, ContactNumberDTO initiateContactNumber);

    UserActivity getPendingConnectionRequestActivity(String initiatorId, String receiverId);

    List<UserActivity> getActiveLocationShareActivityByUser(String initiatorId);

    Page<UserActivity> getActiveLocationShareActivityByUserPageable(String initiatorId, Pageable pageable);

    Page<UserActivity> getActiveLocationShareActivityForUser(String userId, Pageable pageable);

    Page<UserActivity> getContactSharedActivityByUser(String initiatorId, Pageable pageable);

    Page<UserActivity> getContactSharedActivityForUser(String userId, Pageable pageable);

    /* fetches the network join request activity for a specific user based on activityId */
    UserActivity findJoinNetworkPendingRequestByActivityIdAndStatus(String activityId, String activityStatus);

    /* fetches the network join request activity for a specific user*/
    UserActivity getPendingNetworkRequestActivityForUser(String userId, String networkId, String activityStatus);

    UserActivity getPendingConnectionRequestActivityByContactNumber(String currentLoggedinUserId,
                                                                    ContactNumberDTO targetUserContactNumberDTO);

    /* give the count of network join request activity created for user */
    long getCountOfSentNetworkRequestActivity(String networkId, String userId);

    /* updates all join request activity by user with "overAllStatus" -> Expired and respective Action*/
    void updateAllJoinRequestWithActivityByIdsAndNetworkId(List<String> activityById, String networkId,
                                                           boolean requestAccepted);

    /**
     * After user accept invitation or if user's join request was accepted,
     * update all invitation and network share sent to user for a network with "overAllStatus" -> Expired
     */
    void updateAllNetworkInvitationAndShareActivityForUser(String userId, String networkId);


    /* Expires all activity created by or created for user */
    void expireAllActivityRelatedToUser(String userId);

    /* Expires all network related activity for the given network */
    void expireAllActivityOfANetwork(String networkId);

    /* bulk handling of join request for a network */
    void updateAllJoinRequestToNetworkByNetworkId(String networkId, boolean isRequestAccepted);

    /* Expire all network activity for user */
    void expireAllNetworkActivityByActivityForIdAndNetworkId(String userId, String networkId);

    List<UserActivity> findPendingActivityByActivityByIdAndForIdAndRequestType(String activityById, String activityForId,
                                                                               String networkId, RequestType activityRequestType);

    UpdateResult updateBlockedStatusByInitiatorAndReceiverId(String initiatorUserId, String receiverUserId);

    void expireActivityCreatedByDeletedContact(List<String> connectionIdList, String activityById);

    List<UserActivity> getPendingActivitiesByInitiatedByIdAndRequestType(String userId, RequestType activityRequestType);
    
    List<UserActivity> getActivitiesByInitiatedByIdAndRequestType(String userId, RequestType activityRequestType);

    /* This method will return the count of unread  activity*/
    long countOfUnreadActivitiesForGivenUser(String userId);

    List<UserActivity> getSharedContactsActivitiesByUserIdAndSharedConnectionIds(String userId, List<String> sharedConnectionIds);
}
