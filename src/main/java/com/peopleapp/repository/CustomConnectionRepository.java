package com.peopleapp.repository;

import com.peopleapp.dto.requestresponsedto.ManageFavouritesRequestDTO;
import com.peopleapp.model.UserConnection;
import org.joda.time.DateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomConnectionRepository {

    List<UserConnection> findConnectionByConnectionId(List<String> connectionIdList);

    /* get real time connection by connectionId */
    UserConnection findConnectionByConnectionId(String userId, String connectionId);
    
    UserConnection findConnectionByConnectionIdnnNew(String userId, String connectionId);

    UserConnection findConnectionByConnectionIdAndInitiatorId(String userId, String connectionId);

    /* get all valid contacts of a user based on given connection list and status list */
    List<UserConnection> findConnectionByConnectionId(String userId, List<String> connectionIdList);

    List<UserConnection> findConnectionByConnectionIdWithLimitedFields(String userId, List<String> connectionIdList);

    /* get contact - static or connection by connectionId */
    UserConnection findContactByConnectionId(String userId, String connectionId);

    /* get contact - static or connection by connectionId */
    List<UserConnection> findContactByConnectionId(String userId, List<String> connectionIdList);

    /* find a connection based on given from user id and to user id */
    UserConnection findConnectionByFromIdAndToId(String fromUserId, String toUserId);

    /* find a contact based on given from user id and to user id */
    UserConnection findContactByFromIdAndToId(String fromUserId, String toUserId);

    List<UserConnection> findAllContact(String userId);
    
    List<UserConnection> findAllContactID(String userId);

    List<UserConnection> getConnectionsByPeopleUserToIdAndSharedProfileIds(String userId, List<String> profileIds);

    List<UserConnection> getConnectionDataWithProfileModifiedAfterLastSyncTime(String userId, DateTime lastSyncedTime,Pageable pageable);

    Page<UserConnection> getConnectionDataWithProfilePaginated(String userId, Pageable pageable);

    List<UserConnection> getConnectionDataWithProfileForSelectedContact(String userId, List<String> connectionIdList);

    /* returns all collections with Profile Data for a user having connectionTo present in the list passed */
    List<UserConnection> getConnectionDataWithProfileForSelectedToUserIds(String userId, List<String> toUserIds);

    /* returns all collections with Profile Data having connectionTo as the userId passed */
    List<UserConnection> findAllConnectionConnectedToGivenUserWithProfile(String toUserId);

    List<UserConnection> getSharedProfileDataForSelectedContact(List<String> connectionIdList);

    /**
     * Fetch People User data for the connectionList
     *
     * @param userId
     * @param connectionIdList
     * @return List<UserConnection> - with the People user data appended to connectionsList
     */
    List<UserConnection> getPeopleUserDataForConnectionList(String userId, List<String> connectionIdList);

    /* removes all the existing favourites and corresponding sequence for the user */
    void removeFavouritesForGivenUser(String userId);

    /* returns the max sequence number for favourite contacts for the user */
    UserConnection getMaxSequenceNumberConnectionForGivenUser(String userId, List<String> connectionId);

    /* updates the favourites list and corresponding sequence for the user */
    void updateFavouritesForGivenUser(String userId, ManageFavouritesRequestDTO favouritesList);

    /* returns the list of user connections for the user with corresponding privacyProfiles */
    List<UserConnection> findConnectionByUserIdAndPrivacyProfileId(String userId, List<String> privacyProfileId);

    /* returns the paginated and sorted list of favourite contacts */
    Page<UserConnection> getFavouritesForGivenUser(String userId, Pageable pageable);

    /* deletes all connections with connectionFromId of userId*/
    void deleteAllConnectionForAUser(String userId);

    /* delete all connection, in provided list*/
    void deleteConnectionsByUserIdAndConnectionIds(String userId, List<String> connectionsToBeDeleted);

    /* Search user connection by from id and phoneNumber in static data and with given status*/
    List<UserConnection> findByFromIdAndPhoneNumberAndStatus(String fromId, String phoneNumber, List<String> connectionStatus);

    void updateConnectionDataForDeletedAccount(List<String> connectionIds, DateTime lastUpdatedTime);
    
    /* get all Company  Or Tag list */
    List<UserConnection> findCompanyOrTag(String groupOwnerId, String toColumn);
    
    /* get all contact by Companay Name */
    List<UserConnection> findAllContactByCompanyNameOrTagName(String groupOwnerId, String toColumn, String toValue);
    
    /* get all Contact Connected List */
    List<UserConnection> findAllContactByConnected(String groupOwnerId);
    
    /* get all Social Media List */
    List<UserConnection> findSocialMediaOrCityandState(String groupOwnerId);
    
    /* get All Contact by Social Media List */
    List<UserConnection> findAllContactBySocialMediaName(String groupOwnerId, String toValue);
    
    /* get All contact by CityAndState List */
    List<UserConnection> findAllContactByCityAndState(String groupOwnerId); 
    
    /*get all CONNECTED Contact by ToUserID */
    List<UserConnection> findAllConnectedContactByPeopleUserToId(String PeopleUserToId);
}
