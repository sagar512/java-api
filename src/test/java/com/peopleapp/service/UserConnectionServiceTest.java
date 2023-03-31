package com.peopleapp.service;

import com.peopleapp.configuration.LocaleMessageReader;
import com.peopleapp.dto.*;
import com.peopleapp.dto.requestresponsedto.*;
import com.peopleapp.exception.BadRequestException;
import com.peopleapp.model.*;
import com.peopleapp.repository.*;
import com.peopleapp.enums.ConnectionStatus;
import com.peopleapp.security.TokenAuthService;
import com.peopleapp.util.PeopleUtils;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.util.*;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

@RunWith(SpringRunner.class)
@SpringBootTest
public class UserConnectionServiceTest extends BaseTest {

    @Inject
    private UserConnectionService userConnectionService;

    @Inject
    private PeopleUserRepository peopleUserRepository;

    @Inject
    private UserConnectionRepository userConnectionRepository;

    @Inject
    private UserActivityRepository userActivityRepository;

    @Inject
    private ActivityContactRepository activityContactRepository;

    @Inject
    private LocaleMessageReader messages;

    @Inject
    private UserPrivacyProfileRepository userPrivacyProfileRepository;

    @Inject
    private RegisteredNumberRepository registeredNumberRepository;

    @Inject
    private UserGroupService userGroupService;

    @Inject
    private UserGroupRepository userGroupRepository;

    @MockBean
    private TokenAuthService tokenAuthService;

    @MockBean
    private QueueService queueServiceMock;

    private PeopleUser user1;
    private PeopleUser user2;
    private PeopleUser user3;
    private UserConnection userConnection1_2;
    private UserConnection userConnection2_1;
    private UserConnection userNotConnectedConnection1_3;
    private UserConnection userNotConnectedConnection1_4;
    private UserConnection userNotConnectedConnection1_5;
    private UserConnection userNotConnectedConnection1_6;
    private UserConnection userNotConnectedConnection1_7;
    private UserConnection userConnection1_12;

    private PeopleUser requester;
    private PeopleUser accepter;
    private UserActivity userActivity;
    private UserActivity invalidActivity;
    private UserActivity userActivity2;

    private PeopleUser user4;
    private PeopleUser user5;

    private PeopleUser user6;
    private PeopleUser user7;
    private PeopleUser user8;
    private PeopleUser user9;
    private PeopleUser user10;
    private PeopleUser user11;
    private PeopleUser user12;
    private PeopleUser tempUser;

    private UserConnection userConnection6_7;
    private UserConnection userConnection6_8;
    private UserConnection userConnection6_9;
    private UserConnection userConnection6_10;
    private UserConnection userConnection10_6;
    private UserConnection staticConnection1;

    private UserConnection userStaticConnection6;

    private UserPrivacyProfile user1Profile;
    private UserPrivacyProfile user1ProfileNoValueId;
    private UserPrivacyProfile user2Profile;
    private UserPrivacyProfile user6Profile;
    private UserPrivacyProfile user7Profile;
    private UserPrivacyProfile user8Profile;
    private UserPrivacyProfile user9Profile;
    private UserPrivacyProfile user12Profile;

    @Before
    public void setUp() {

        user1 = peopleUserRepository.save(MethodStubs.getUserObject("9888888888", "testuser1"));
        user2 = peopleUserRepository.save(MethodStubs.getUserObject("9777777777", "testuser2"));
        user3 = peopleUserRepository.save(MethodStubs.getUserObject("9666666666", "testuser3"));

        requester = peopleUserRepository.save(MethodStubs.getUserObject("987654321", "testrequester"));
        accepter = peopleUserRepository.save(MethodStubs.getUserObject("1234567890", "testaccepter"));

        userActivity = userActivityRepository.save(MethodStubs.getUserActivity(requester.getUserId(), accepter.getUserId(),
                new ObjectId().toString()));
        invalidActivity = userActivityRepository.save(MethodStubs.getUserActivity(requester.getUserId(), new ObjectId().toString(),
                new ObjectId().toString()));

        user4 = peopleUserRepository.save(MethodStubs.getUserObject("1234567890", "testuser4"));
        user5 = peopleUserRepository.save(MethodStubs.getUserObject("1212121212", "testuser5"));
        userActivity2 = userActivityRepository.save(MethodStubs.getRequestMoreInfoUserActivity(user5.getUserId(),
                user4.getUserId(), "request for more info"));

        user6 = peopleUserRepository.save(MethodStubs.getUserObject("9555555556", "testuser6"));
        user7 = peopleUserRepository.save(MethodStubs.getUserObject("9555555557", "testuser7"));
        user8 = peopleUserRepository.save(MethodStubs.getUserObject("9555555558", "testuser8"));
        user9 = peopleUserRepository.save(MethodStubs.getUserObject("testuser9"));
        user10 = peopleUserRepository.save(MethodStubs.getUserObject("9555555559", "testuser9"));
        user12 = peopleUserRepository.save(MethodStubs.getUserObject("9555555562", "testuser12"));

        userNotConnectedConnection1_3 =
                userConnectionRepository.save(MethodStubs.getConnectionObjWithNotConnectedStatus(user1.getUserId(), user3.getUserId()));
        userNotConnectedConnection1_4 =
                userConnectionRepository.save(MethodStubs.getConnectionObjWithNotConnectedStatus(user1.getUserId(), user4.getUserId()));
        userNotConnectedConnection1_5 =
                userConnectionRepository.save(MethodStubs.getConnectionObjWithNotConnectedStatus(user1.getUserId(), user5.getUserId()));
        userNotConnectedConnection1_6 =
                userConnectionRepository.save(MethodStubs.getConnectionObjWithNotConnectedStatus(user1.getUserId(), user6.getUserId()));
        userNotConnectedConnection1_7 =
                userConnectionRepository.save(MethodStubs.getConnectionObjWithNotConnectedStatus(user1.getUserId(), user7.getUserId()));
        staticConnection1 =
                userConnectionRepository.save(MethodStubs.getConnectionObjForStaticContact(user1.getUserId()));

        // adding valueId in User Obj and User Privacy Profile
        List<String> valueIds = new ArrayList<>();
        List<UserProfileData> existingMetadata = new ArrayList<>();
        List<UserProfileData> metadataList = Collections.singletonList(MethodStubs.getUserProfileDataForContactNumber());
        for (UserProfileData newData : PeopleUtils.emptyIfNull(metadataList)) {
            String valueId = new ObjectId().toString();
            newData.setValueId(valueId);
            newData.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
            existingMetadata.add(newData);
            valueIds.add(valueId);
        }
        user6.setUserMetadataList(existingMetadata);

        user1.setUserMetadataList(existingMetadata);
        user12.setUserMetadataList(existingMetadata);

        user1Profile = userPrivacyProfileRepository.save(MethodStubs.getUserPrivacyProfileObj(user1.getUserId(),
                valueIds));
        user2Profile = userPrivacyProfileRepository.save(MethodStubs.getUserPrivacyProfileObj(user2.getUserId(),
                valueIds));
        user1ProfileNoValueId = userPrivacyProfileRepository.save(MethodStubs.getUserPrivacyProfileObj(user1.getUserId(),
                new ArrayList<>()));
        user12Profile = userPrivacyProfileRepository.save(MethodStubs.getUserPrivacyProfileObj(user12.getUserId(),
                valueIds));

        userConnection1_2 = userConnectionRepository.save(MethodStubs.getConnectionWithProfileObj(user1.getUserId(),
                user2.getUserId(), user2Profile.getPrivacyProfileId()));
        userConnection2_1 = userConnectionRepository.save(MethodStubs.getConnectionWithProfileObj(user2.getUserId(),
                user1.getUserId(), user1Profile.getPrivacyProfileId()));

        userConnection1_12 = userConnectionRepository.save(MethodStubs.getConnectionWithProfileObj(user1.getUserId(),
                user12.getUserId(), user1Profile.getPrivacyProfileId()));
        userConnectionRepository.save(MethodStubs.getConnectionWithProfileObj(user12.getUserId(),
                user1.getUserId(), user12Profile.getPrivacyProfileId()));

        user6Profile = userPrivacyProfileRepository.save(MethodStubs.getUserPrivacyProfileObj(user6.getUserId(),
                valueIds));
        user7Profile = userPrivacyProfileRepository.save(MethodStubs.getUserPrivacyProfileObj(user7.getUserId(),
                valueIds));
        user8Profile = userPrivacyProfileRepository.save(MethodStubs.getUserPrivacyProfileObj(user8.getUserId(),
                valueIds));
        user9Profile = userPrivacyProfileRepository.save(MethodStubs.getUserPrivacyProfileObj(user9.getUserId(),
                valueIds));

        userConnection6_7 = userConnectionRepository.save(MethodStubs.getConnectionWithProfileObj(user6.getUserId(),
                user7.getUserId(), user6Profile.getPrivacyProfileId()));
        userConnectionRepository.save(MethodStubs.getConnectionWithProfileObj(user7.getUserId(),
                user6.getUserId(), user7Profile.getPrivacyProfileId()));

        userConnection6_8 = userConnectionRepository.save(MethodStubs.getConnectionWithProfileObj(user6.getUserId(),
                user8.getUserId(), user6Profile.getPrivacyProfileId()));
        userConnectionRepository.save(MethodStubs.getConnectionWithProfileObj(user8.getUserId(),
                user6.getUserId(), user8Profile.getPrivacyProfileId()));

        userConnection6_9 = userConnectionRepository.save(MethodStubs.getConnectionWithProfileObj(user6.getUserId(),
                user9.getUserId(), user6Profile.getPrivacyProfileId()));
        userConnectionRepository.save(MethodStubs.getConnectionWithProfileObj(user9.getUserId(),
                user6.getUserId(), user9Profile.getPrivacyProfileId()));

        userConnection6_10 = userConnectionRepository.save(MethodStubs.getConnectionObj(user6.getUserId(),
                user10.getUserId()));
        userConnection10_6 = userConnectionRepository.save(MethodStubs.getConnectionObj(user10.getUserId(),
                user6.getUserId()));
        userStaticConnection6 = userConnectionRepository.save(MethodStubs.getConnectionObjForStaticContact(user6.getUserId()));
        String user11PhoneNumber = userStaticConnection6.getContactStaticData().getUserMetadataList().get(0).
                getKeyValueDataList().get(1).getVal();
        user11 = peopleUserRepository.save(MethodStubs.getUserObject(user11PhoneNumber, "testuser11"));

        tempUser = peopleUserRepository.save(MethodStubs.getWatuUserAccount("8887878001",
                "temp@mailinator.com", "tempUser", "", ""));

        registeredNumberRepository.save(MethodStubs.getRegisteredNumber(Arrays.asList("0919888888888", "0919777777777",
                "+19666666666", "+16777777777", "+19555555556", "+19555555557", "+19555555558",
                "+1".concat(user11PhoneNumber), "9555555562", "+18887878001")));
    }

    /**
     * Method: manageFavouritesForContact
     * Test case - Success
     * Sending an empty list should not throw any error and empty list should be returned
     */
    @Test
    public void testManageFavouritesForContact() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        ManageFavouritesRequestDTO manageFavouritesRequestDTO = MethodStubs.getFavouritesRequestDTO(new ArrayList<>());
        boolean isResultingListEmpty =
                userConnectionService.manageFavouritesForContact(manageFavouritesRequestDTO).isEmpty();
        Assert.assertTrue("Success - Resulting list of favourites connection is empty ", isResultingListEmpty);
    }

    /**
     * Method - setFavouriteForContact
     * TestCase - Failure
     * If all the connection in the request is invalid then exception will be thrown
     */
    @Test
    public void testUpdateFavouriteInvalid() {

        Exception exception = null;
        UpdateFavouriteRequestDTO requestDTO = new UpdateFavouriteRequestDTO();
        requestDTO.setFavouriteContactList(Arrays.asList(MethodStubs.getFavouriteContact(new ObjectId().toString(),
                true)));

        given(this.tokenAuthService.getSessionUser()).willReturn(user3);

        try {
            userConnectionService.setFavouriteForContact(requestDTO);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("failure - expected BadRequestException", exception instanceof BadRequestException);
    }

    /**
     * Method - setFavouriteForContact
     * TestCase - Success
     * User can mark any valid contact as favourite
     */
    @Test
    public void testUpdateFavouriteValid() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        UpdateFavouriteRequestDTO requestDTO = new UpdateFavouriteRequestDTO();

        requestDTO.setFavouriteContactList(Arrays.asList(MethodStubs.getFavouriteContact(userConnection1_2.getConnectionId(),
                true)));

        userConnectionService.setFavouriteForContact(requestDTO);

        UserConnection updatedConnection = userConnectionRepository.findConnectionByConnectionId(user1.getUserId(),
                userConnection1_2.getConnectionId());

        Assert.assertTrue("Success - connection marked as favourite", updatedConnection.getIsFavourite());
    }

    /*
     * verify sequence number updation for favourite connection when no favourite connection present
     * */
    @Test
    public void testUpdateFavouriteForSequence() {
        UpdateFavouriteRequestDTO requestDTO = new UpdateFavouriteRequestDTO();
        requestDTO.setFavouriteContactList(Arrays.asList(MethodStubs.getFavouriteContact(userConnection6_7.getConnectionId(),
                true)));
        given(this.tokenAuthService.getSessionUser()).willReturn(user6);

        userConnectionService.setFavouriteForContact(requestDTO);

        UserConnection responseConnection = userConnectionRepository.findConnectionByConnectionId(user6.getUserId(),
                userConnection6_7.getConnectionId());

        Assert.assertEquals("Success - Sequence Number Verified", 0,
                responseConnection.getSequenceNumber().longValue());
    }

    /*
     * verify sequence number updation for favourite connection when favourite connection exists
     * */
    @Test
    public void testUpdateFavouriteForExistingSequence() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user6);

        UserConnection updateConnection = userConnectionRepository.findConnectionByConnectionId(user6.getUserId(),
                userConnection6_7.getConnectionId());
        updateConnection.setSequenceNumber(1);
        updateConnection.setIsFavourite(true);
        userConnectionRepository.save(updateConnection);

        UpdateFavouriteRequestDTO requestDTO = new UpdateFavouriteRequestDTO();
        requestDTO.setFavouriteContactList(Arrays.asList(MethodStubs.getFavouriteContact(userConnection6_8.getConnectionId(),
                true)));

        userConnectionService.setFavouriteForContact(requestDTO);

        UserConnection updatedConnection = userConnectionRepository.findConnectionByConnectionId(user6.getUserId(),
                userConnection6_8.getConnectionId());
        //Expected Sequence Number is 2 as Connection with Sequence Number 1 already exists
        Assert.assertEquals("Success - Sequence Number Verified", 2, updatedConnection.getSequenceNumber().longValue());
    }

    /*
     * verify sequence number unset and isFavourite flag when unfavouriting a connection
     * */
    @Test
    public void testUpdateFavouriteWhenUnFavouriting() {

        given(this.tokenAuthService.getSessionUser()).willReturn(user6);

        UserConnection updateConnection = userConnectionRepository.findConnectionByConnectionId(user6.getUserId(),
                userConnection6_7.getConnectionId());
        updateConnection.setSequenceNumber(1);
        updateConnection.setIsFavourite(true);
        userConnectionRepository.save(updateConnection);

        UpdateFavouriteRequestDTO requestDTO = new UpdateFavouriteRequestDTO();
        requestDTO.setFavouriteContactList(Arrays.asList(MethodStubs.getFavouriteContact(userConnection6_7.getConnectionId(),
                false)));

        userConnectionService.setFavouriteForContact(requestDTO);

        UserConnection responseConnection = userConnectionRepository.findConnectionByConnectionId(user6.getUserId(),
                userConnection6_7.getConnectionId());

        Assert.assertEquals("Success - Connection Removed from Favourite", false,
                responseConnection.getIsFavourite());
        Assert.assertNull("Success - Sequence Number is removed", responseConnection.getSequenceNumber());
    }

    @Test
    public void testIntroducingMultipleContactsWithEachOther() {
        Exception exception = null;
        given(this.tokenAuthService.getSessionUser()).willReturn(user6);
        mockSQSServices();
        SendMultiIntroRequestDTO sendMultiIntroRequestDTO = MethodStubs.getMultiIntroRequestDTOWithoutContactNumber
                (userConnection6_7.getConnectionId(), userConnection6_8.getConnectionId(), userStaticConnection6.getConnectionId());
        try {
            userConnectionService.introduceContactToEachOtherRequest(sendMultiIntroRequestDTO);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertNull("Success - 3 contacts were introduced to each other", exception);
    }

    /**
     * Method - introduceContactRequest
     * TestCase - Success
     */
    @Test
    public void testIntroduceContactRequest() {
        Exception exception = null;
        given(this.tokenAuthService.getSessionUser()).willReturn(user6);
        mockSQSServices();

        SendSingleIntroRequestDTO sendSingleIntroRequestDTO = MethodStubs.getSendSingleIntroRequestDTO
                (userConnection6_7.getConnectionId(), userConnection6_8.getConnectionId());
        try {
            userConnectionService.introduceContactRequest(sendSingleIntroRequestDTO);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertNull("Success - contacts were introduced to each other", exception);
    }

    @Test
    public void testManageFavouritesForOneContact() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user6);

        List<String> connectionList = new ArrayList<String>();
        connectionList.add(userConnection6_7.getConnectionId());
        ManageFavouritesRequestDTO requestDTO = MethodStubs.getFavouritesRequestDTO(connectionList);
        List<String> modifiedConnectionIds = userConnectionService.manageFavouritesForContact(requestDTO);
        Assert.assertEquals("Success - Update for 1 connection", 1, modifiedConnectionIds.size());
    }

    @Test
    public void testManageFavouritesForMultipleContact() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user6);

        List<String> connectionList = new ArrayList<String>();
        connectionList.add(userConnection6_7.getConnectionId());
        connectionList.add(userConnection6_8.getConnectionId());
        ManageFavouritesRequestDTO requestDTO = MethodStubs.getFavouritesRequestDTO(connectionList);
        List<String> modifiedConnectionIds = userConnectionService.manageFavouritesForContact(requestDTO);

        Assert.assertEquals("Success - Update for 2 connection", 2, modifiedConnectionIds.size());
    }

    @Test
    public void testManageFavouritesForContactInvalidConnectionId() {
        Exception exception = null;
        given(this.tokenAuthService.getSessionUser()).willReturn(user6);

        List<String> connectionList = new ArrayList<String>();
        connectionList.add(userConnection6_7.getConnectionId());
        connectionList.add(new ObjectId().toString());

        ManageFavouritesRequestDTO requestDTO = MethodStubs.getFavouritesRequestDTO(connectionList);
        List<String> modifiedConnectionIds = userConnectionService.manageFavouritesForContact(requestDTO);

        //Expected output is 1, since only one connectionId is valid
        Assert.assertEquals("Success - Update for 1 connection", 1, modifiedConnectionIds.size());
    }

    /**
     * Method: changePrivacyProfileForConnection
     * Success Case
     */
    @Test
    public void testChangePrivacyProfileForConnection() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        List<String> valueIds = new ArrayList<>();
        valueIds.add(new ObjectId().toString());
        String privacyProfileId = user1ProfileNoValueId.getPrivacyProfileId();

        ChangePrivacyProfileRequestDTO changePrivacyProfileRequestDTO = MethodStubs.getChangePrivacyProfileRequestDTO(
                userConnection1_2.getConnectionId(), privacyProfileId, valueIds);

        userConnectionService.changePrivacyProfileForConnection(changePrivacyProfileRequestDTO);

        UserConnection updatedConnection = userConnectionRepository.findConnectionByConnectionId(user2.getUserId(),
                userConnection2_1.getConnectionId());
        Assert.assertEquals("Success - privacyProfileId updated", privacyProfileId,
                updatedConnection.getRealTimeSharedData().getPrivacyProfileId());
        Assert.assertEquals("Success - valueIdList updated", valueIds,
                updatedConnection.getRealTimeSharedData().getValueIdList());
    }

    /**
     * Method: changePrivacyProfileForConnection
     * Failure Case
     */
    @Test
    public void testChangePrivacyProfileForConnectionFailure() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        Exception exception = null;
        List<String> valueIds = new ArrayList<>();
        valueIds.add(new ObjectId().toString());
        String privacyProfileId = new ObjectId().toString();

        ChangePrivacyProfileRequestDTO changePrivacyProfileRequestDTO = MethodStubs.getChangePrivacyProfileRequestDTO(
                userConnection2_1.getConnectionId(), privacyProfileId, valueIds);

        try {
            userConnectionService.changePrivacyProfileForConnection(changePrivacyProfileRequestDTO);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertNotNull("Failure - Expected Exception", exception);
        Assert.assertTrue("Failure - expected BadRequestException", exception instanceof BadRequestException);
    }

    @Test
    public void testGetConnectionList() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user6);
        FetchConnectionListResponseDTO responseDTO = userConnectionService.getConnectionList(null,
                0, 2, false, null);

        Assert.assertEquals("Success - User 6 has 5 connections", 5, responseDTO.getTotalElements());

        Assert.assertEquals("Success - Two elements are returned", 2, responseDTO.getContactList().size());

        Assert.assertEquals("Success - Total two pages", 3, responseDTO.getTotalNumberOfPages());

        Assert.assertNotNull("Success - Next URL is present", responseDTO.getNextURL());
    }

    @Test
    public void testGetConnectionListWithLastSyncedTime() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user6);
        DateTime lastSyncedTime = new DateTime(0).toDateTime(DateTimeZone.UTC);
        FetchConnectionListResponseDTO responseDTO = userConnectionService.getConnectionList(lastSyncedTime,
                0, 2, false, null);

        Assert.assertEquals("Success - User 6 has 5 connections", 5, responseDTO.getTotalElements());

        Assert.assertEquals("Success - Two elements are returned", 2, responseDTO.getContactList().size());

        Assert.assertEquals("Success - Total two pages", 3, responseDTO.getTotalNumberOfPages());

        Assert.assertNotNull("Success - Next URL is present", responseDTO.getNextURL());
    }

    /**
     * Method - deleteContact
     * TestCase - Success
     */
    @Test
    public void testdeleteContact() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user6);

        List<String> connectionIdsList = new ArrayList<>();
        connectionIdsList.add(userConnection6_10.getConnectionId());
        DeleteContactRequest deleteContactRequest = MethodStubs.getDeleteContactRequest(connectionIdsList);

        userConnectionService.deleteContact(deleteContactRequest);

        UserConnection userConnection = userConnectionRepository.findContactByConnectionId(user6.getUserId(),
                userConnection6_10.getConnectionId());

        Assert.assertNull("Success - Contact deleted", userConnection);
    }

    /**
     * Method - deleteContact
     * TestCase - Failure
     */
    @Test
    public void testdeleteContactFailure() {
        Exception exception = null;
        given(this.tokenAuthService.getSessionUser()).willReturn(user6);

        List<String> connectionIdsList = new ArrayList<>();
        connectionIdsList.add(userConnection1_2.getConnectionId());
        DeleteContactRequest deleteContactRequest = MethodStubs.getDeleteContactRequest(connectionIdsList);

        try {
            userConnectionService.deleteContact(deleteContactRequest);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertNotNull("Failure - Expected Exception", exception);
        Assert.assertTrue("Failure - expected BadRequestException", exception instanceof BadRequestException);
    }

    /**
     * Method - fetchConnectionDetails
     * TestCase - Success
     * Fetches connection details w.r.t sessionUser and given connectionId
     */
    @Test
    public void testFetchingConnectionDetails() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user6);

        ConnectionDetailsResponseDTO connectionDetail = userConnectionService.fetchConnectionDetails(
                userConnection6_8.getConnectionId());

        Assert.assertEquals("Success - details for the given connection id is fetched",
                userConnection6_8.getConnectionId(), connectionDetail.getUserConnectionDetail().getConnectionId());

    }

    /**
     * Method - fetchConnectionDetails
     * TestCase - Failure
     * Fetches connection details w.r.t sessionUser and given connectionId,
     * if no connection exist then exception is thrown
     */
    @Test
    public void testFetchingConnectionDetailsWithInvalidConnectionId() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user6);
        Exception exception = null;
        try {
            userConnectionService.fetchConnectionDetails(userConnection1_2.getConnectionId());
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertNotNull("Failure - Expected Exception", exception);
        Assert.assertTrue("Failure - expected BadRequestException", exception instanceof BadRequestException);
    }

    /**
     * Method - syncContacts
     * TestCase - Success
     */
    @Test
    public void testSyncContacts() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user7);

        ContactSyncRequestDTO requestDTO = MethodStubs.getContactSyncRequestDTO();

        ContactSyncResponseDTO responseDTO = userConnectionService.syncContacts(requestDTO);

        Assert.assertNotNull("Success - Contact synced", responseDTO.getUserContactList());
    }

    /**
     * Method - sendConnectionRequest
     * Flow - Single ConnectionId
     * TestCase - Success
     */
    @Test
    public void testSendConnectionRequestSingleConnectionId() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        mockSQSServices();

        SendConnectionRequest requestDTO = new SendConnectionRequest();
        UserContact userContact1_3 = MethodStubs.getUserContact(userNotConnectedConnection1_3.getConnectionId(),
                user3.getVerifiedContactNumber());
        requestDTO.setInitiateContactDetailsList(Arrays.asList(userContact1_3));
        requestDTO.setSharedPrivacyProfileKey(MethodStubs.getSharedProfileInformationData(user1Profile.getPrivacyProfileId()
                , new ArrayList<>()));

        SendConnectionRequestResponse responseDTO = userConnectionService.sendConnectionRequest(requestDTO);

        Assert.assertNotNull("Success - Connection Request Sent", responseDTO.getActivityDetailsList());
    }

    /**
     * Method - sendConnectionRequest
     * Flow - Single ConnectionId - Already Connected
     * TestCase - Failure
     */
    @Test
    public void testSendConnectionRequestSingleConnectionIdFailureCase1() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        Exception exception = null;

        SendConnectionRequest requestDTO = new SendConnectionRequest();
        UserContact userContact1_2 = MethodStubs.getUserContact(userConnection1_2.getConnectionId()
                , user2.getVerifiedContactNumber());
        requestDTO.setInitiateContactDetailsList(Arrays.asList(userContact1_2));
        requestDTO.setSharedPrivacyProfileKey(MethodStubs.getSharedProfileInformationData(user1Profile.getPrivacyProfileId()
                , new ArrayList<>()));

        try {
            userConnectionService.sendConnectionRequest(requestDTO);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Failure - expected BadRequestException", exception instanceof BadRequestException);
    }

    /**
     * Method - sendConnectionRequest
     * Flow - Single ConnectionId - Invalid ConnectionId
     * TestCase - Failure
     */
    @Test
    public void testSendConnectionRequestSingleConnectionIdFailureCase2() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        Exception exception = null;

        SendConnectionRequest requestDTO = new SendConnectionRequest();
        UserContact userContact = MethodStubs.getUserContact(new ObjectId().toString(), user2.getVerifiedContactNumber());
        requestDTO.setInitiateContactDetailsList(Arrays.asList(userContact));
        requestDTO.setSharedPrivacyProfileKey(MethodStubs.getSharedProfileInformationData(user1Profile.getPrivacyProfileId(),
                new ArrayList<>()));

        try {
            userConnectionService.sendConnectionRequest(requestDTO);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Failure - expected BadRequestException", exception instanceof BadRequestException);
    }

    /**
     * Method - sendConnectionRequest
     * Flow - Single ConnectionId - Blocked User
     * TestCase - Failure
     */
    @Test
    public void testSendConnectionRequestSingleConnectionIdFailureCase3() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        user1.setBlockedUserIdList(new HashSet<>(Arrays.asList(user6.getUserId())));
        peopleUserRepository.save(user1);
        Exception exception = null;

        SendConnectionRequest requestDTO = new SendConnectionRequest();
        UserContact userContact = MethodStubs.getUserContact(userNotConnectedConnection1_6.getConnectionId(),
                user6.getVerifiedContactNumber());
        requestDTO.setInitiateContactDetailsList(Arrays.asList(userContact));
        requestDTO.setSharedPrivacyProfileKey(MethodStubs.getSharedProfileInformationData(user1Profile.getPrivacyProfileId(),
                new ArrayList<>()));

        try {
            userConnectionService.sendConnectionRequest(requestDTO);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Failure - expected BadRequestException", exception instanceof BadRequestException);
    }

    /**
     * Method - sendConnectionRequest
     * Flow - Bulk ConnectionIds
     * TestCase - Success
     */
    @Test
    public void testSendConnectionRequestBulkConnectionId() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        mockSQSServices();

        SendConnectionRequest requestDTO = new SendConnectionRequest();
        UserContact userContact1_3 = MethodStubs.getUserContact(userNotConnectedConnection1_3.getConnectionId()
                , user3.getVerifiedContactNumber());
        UserContact userContact1_4 = MethodStubs.getUserContact(userNotConnectedConnection1_4.getConnectionId()
                , user4.getVerifiedContactNumber());
        requestDTO.setInitiateContactDetailsList(Arrays.asList(userContact1_3, userContact1_4));

        requestDTO.setSharedPrivacyProfileKey(MethodStubs.getSharedProfileInformationData(user1Profile.getPrivacyProfileId()
                , new ArrayList<>()));

        SendConnectionRequestResponse responseDTO = userConnectionService.sendConnectionRequest(requestDTO);

        Assert.assertNotNull("Success - Connection Request Sent", responseDTO.getActivityDetailsList());
    }

    /**
     * Method - sendConnectionRequest
     * Flow - Bulk ConnectionIds with one blocked user
     * TestCase - Success
     */
    @Test
    public void testSendConnectionRequestBulkConnectionIdFailureCase1() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        mockSQSServices();
        Exception exception = null;

        user1.setBlockedUserIdList(new HashSet<>(Arrays.asList(user6.getUserId())));
        peopleUserRepository.save(user1);

        SendConnectionRequest requestDTO = new SendConnectionRequest();
        UserContact userContact1_5 = MethodStubs.getUserContact(userNotConnectedConnection1_5.getConnectionId()
                , user5.getVerifiedContactNumber());
        UserContact userContact1_6 = MethodStubs.getUserContact(userNotConnectedConnection1_6.getConnectionId()
                , user6.getVerifiedContactNumber());
        requestDTO.setInitiateContactDetailsList(Arrays.asList(userContact1_5, userContact1_6));

        requestDTO.setSharedPrivacyProfileKey(MethodStubs.getSharedProfileInformationData(user1Profile.getPrivacyProfileId()
                , new ArrayList<>()));

        try {
            userConnectionService.sendConnectionRequest(requestDTO);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Failure - expected BadRequestException", exception instanceof BadRequestException);
    }

    /**
     * Method - sendConnectionRequest
     * Flow - Bulk ConnectionIds with Both Blocked
     * TestCase - Success
     */
    @Test
    public void testSendConnectionRequestBulkConnectionIdFailureCase2() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        mockSQSServices();
        Exception exception = null;

        user1.setBlockedUserIdList(new HashSet<>(Arrays.asList(user6.getUserId(), user7.getUserId())));
        peopleUserRepository.save(user1);

        SendConnectionRequest requestDTO = new SendConnectionRequest();
        UserContact userContact1_6 = MethodStubs.getUserContact(userNotConnectedConnection1_6.getConnectionId()
                , user6.getVerifiedContactNumber());
        UserContact userContact1_7 = MethodStubs.getUserContact(userNotConnectedConnection1_7.getConnectionId()
                , user7.getVerifiedContactNumber());
        requestDTO.setInitiateContactDetailsList(Arrays.asList(userContact1_6, userContact1_7));

        requestDTO.setSharedPrivacyProfileKey(MethodStubs.getSharedProfileInformationData(user1Profile.getPrivacyProfileId()
                , new ArrayList<>()));

        try {
            userConnectionService.sendConnectionRequest(requestDTO);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Failure - expected BadRequestException", exception instanceof BadRequestException);
    }


    /**
     * Method - sendConnectionRequest
     * Flow - Invalid ProfileId
     * TestCase - Failure
     */
    @Test
    public void testSendConnectionRequestFailureCase1() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        Exception exception = null;

        SendConnectionRequest requestDTO = new SendConnectionRequest();

        UserContact userContact1_3 = MethodStubs.getUserContact(userNotConnectedConnection1_3.getConnectionId(),
                user3.getVerifiedContactNumber());
        requestDTO.setInitiateContactDetailsList(Arrays.asList(userContact1_3));
        requestDTO.setSharedPrivacyProfileKey(MethodStubs.getSharedProfileInformationData(new ObjectId().toString(),
                new ArrayList<>()));

        try {
            userConnectionService.sendConnectionRequest(requestDTO);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Failure - expected BadRequestException", exception instanceof BadRequestException);
    }

    /**
     * Method - sendConnectionRequest
     * Flow - UserId
     * TestCase - Success
     */
    @Test
    public void testSendConnectionRequestUserId() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        mockSQSServices();

        SendConnectionRequest requestDTO = new SendConnectionRequest();
        requestDTO.setInitiateUserIdList(Arrays.asList(user5.getUserId()));
        requestDTO.setSharedPrivacyProfileKey(MethodStubs.getSharedProfileInformationData(user1Profile.getPrivacyProfileId()
                , new ArrayList<>()));

        SendConnectionRequestResponse responseDTO = userConnectionService.sendConnectionRequest(requestDTO);

        Assert.assertNotNull("Success - Connection Request Sent", responseDTO.getActivityDetailsList());
    }

    /**
     * Method - sendConnectionRequest
     * Flow - UserId - Invalid UserId
     * TestCase - Failure
     */
    @Test
    public void testSendConnectionRequestUserIdFailureCase1() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        mockSQSServices();
        Exception exception = null;

        SendConnectionRequest requestDTO = new SendConnectionRequest();
        requestDTO.setInitiateUserIdList(Arrays.asList(new ObjectId().toString()));
        requestDTO.setSharedPrivacyProfileKey(MethodStubs.getSharedProfileInformationData(user1Profile.getPrivacyProfileId()
                , new ArrayList<>()));

        try {
            userConnectionService.sendConnectionRequest(requestDTO);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Failure - expected BadRequestException", exception instanceof BadRequestException);
    }

    /**
     * Method - sendConnectionRequest
     * Flow - UserId - Already Connected
     * TestCase - Failure
     */
    @Test
    public void testSendConnectionRequestUserIdFailureCase2() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        mockSQSServices();
        Exception exception = null;

        SendConnectionRequest requestDTO = new SendConnectionRequest();
        requestDTO.setInitiateUserIdList(Arrays.asList(user2.getUserId()));
        requestDTO.setSharedPrivacyProfileKey(MethodStubs.getSharedProfileInformationData(user1Profile.getPrivacyProfileId()
                , new ArrayList<>()));

        try {
            userConnectionService.sendConnectionRequest(requestDTO);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Failure - expected BadRequestException", exception instanceof BadRequestException);
    }

    /**
     * Method - sendConnectionRequest
     * Flow - ActivityId
     * TestCase - Success & Failure(Multiple Connection Request to same user)
     */
    @Test
    public void testSendConnectionRequestActivityId() {
        Exception exception = null;
        given(this.tokenAuthService.getSessionUser()).willReturn(user6);
        mockSQSServices();

        SendSingleIntroRequestDTO sendSingleIntroRequestDTO = MethodStubs.getSendSingleIntroRequestDTO
                (userConnection6_7.getConnectionId(), userConnection6_8.getConnectionId());
        userConnectionService.introduceContactRequest(sendSingleIntroRequestDTO);

        List<UserActivity> userActivities = userActivityRepository.getPendingIntroductionActivities(user7.getUserId());

        Assert.assertEquals("Success - User7 has one introduction activity", 1, userActivities.size());

        // user7 sends a connection request to user8
        given(this.tokenAuthService.getSessionUser()).willReturn(user7);
        SendConnectionRequest requestDTO = new SendConnectionRequest();
        requestDTO.setActivityIdList(Arrays.asList(userActivities.get(0).getActivityId()));
        requestDTO.setSharedPrivacyProfileKey(MethodStubs.getSharedProfileInformationData(user7Profile.getPrivacyProfileId()
                , new ArrayList<>()));
        userConnectionService.sendConnectionRequest(requestDTO);

        // sending connection request again will throw exception
        try {
            userConnectionService.sendConnectionRequest(requestDTO);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Failure - expected BadRequestException", exception instanceof BadRequestException);
    }

    /**
     * Method - sendConnectionRequest
     * Flow - ActivitySubId
     * TestCase - Success
     */
    @Test
    public void testSendConnectionRequestActivitySubId() {
        Exception exception = null;
        given(this.tokenAuthService.getSessionUser()).willReturn(user6);
        mockSQSServices();

        SendSingleIntroRequestDTO sendSingleIntroRequestDTO = MethodStubs.getSendSingleIntroRequestDTO
                (userConnection6_7.getConnectionId(), userConnection6_8.getConnectionId());
        userConnectionService.introduceContactRequest(sendSingleIntroRequestDTO);

        List<UserActivity> userActivities = userActivityRepository.getPendingIntroductionActivities(user8.getUserId());

        Assert.assertEquals("Success - User8 has one introduction activity", 1, userActivities.size());

        // user8 sends a connection request to user7
        given(this.tokenAuthService.getSessionUser()).willReturn(user8);

        List<ActivityContact> activityContacts =
                activityContactRepository.getActivityContactsByActivityIdsAndUserId(Arrays.asList(userActivities.get(0).getActivityId()), user8.getUserId());

        SendConnectionRequest requestDTO = new SendConnectionRequest();
        requestDTO.setActivitySubIdList(Arrays.asList(activityContacts.get(0).getUniqueId().toString()));
        requestDTO.setSharedPrivacyProfileKey(MethodStubs.getSharedProfileInformationData(user8Profile.getPrivacyProfileId()
                , new ArrayList<>()));
        userConnectionService.sendConnectionRequest(requestDTO);
    }

    /**
     * Method - sendConnectionRequest
     * Flow - UserId with name(to create static contact)
     * TestCase - Success
     */
    @Test
    public void testSendConnectionRequestUserIdWithName() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        mockSQSServices();

        FetchConnectionListResponseDTO userConnections = userConnectionService.getConnectionList(null,
                0, 10, false, null);
        Assert.assertEquals("User1 has 8 contacts ", 8, userConnections.getContactList().size());

        UserInformationDTO userInformationDTO = MethodStubs.getUserInformationObject();
        userInformationDTO.setUserMetadataList(Collections.singletonList(
                MethodStubs.getUserProfileDataForGivenContactNumber("+1",
                        tempUser.getVerifiedContactNumber().getPhoneNumber())));

        SendConnectionRequestResponse responseDTO = userConnectionService.sendConnectionRequest(
                MethodStubs.getSendConnectionRequestDTOWithUserIdAndUserInformation(
                        tempUser.getUserId(), user1Profile.getPrivacyProfileId(), userInformationDTO));

        userConnections = userConnectionService.getConnectionList(null, 0, 10, false, null);

        Assert.assertNotNull("Success - Connection Request is Sent and static contact was created", responseDTO.getContactDetails());
        Assert.assertEquals("Static contact was created for user 1, user1 now has 9 contacts ", 9, userConnections.getContactList().size());
    }

    /**
     * Method - sendConnectionRequest
     * Flow - UserId with name(to create static contact)
     * TestCase - Failure
     * Name information of initiate is a mandatory field when isRequestToBeSentToSearchedNumber is set to true
     */
    @Test
    public void testSendConnectionRequestUserIdWithNameWithoutInitiateInformation() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        mockSQSServices();
        SendConnectionRequest connectionRequest = MethodStubs.getSendConnectionRequestDTOWithUserIdAndUserInformation(
                tempUser.getUserId(), user1Profile.getPrivacyProfileId(), null);

        Exception exception = null;

        try {
            userConnectionService.sendConnectionRequest(connectionRequest);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Failure - Initiate information can not be empty",
                exception instanceof BadRequestException);
    }


    /**
     * Method - shareLocation
     * TestCase - Success
     */
    @Test
    public void testShareLocation() {
        Exception exception = null;
        given(this.tokenAuthService.getSessionUser()).willReturn(user6);
        mockSQSServices();

        ShareLocationRequest request =
                MethodStubs.getShareLocationRequestDTO(Arrays.asList(userConnection6_7.getConnectionId()), 10);
        userConnectionService.shareLocation(request);

        List<UserActivity> userActivities =
                userActivityRepository.getActiveLocationShareActivityByUser(user6.getUserId());

        Assert.assertEquals("Success - One location share activity created for user6", 1, userActivities.size());
        Assert.assertEquals("Success - Location share activity created for user7", user7.getUserId(),
                userActivities.get(0).getActivityForId());
        Assert.assertEquals("Success - Location shared for 10 mins", new Integer(10),
                userActivities.get(0).getLocationSharedForTime());

        // update existing location share flow
        request.setTimeInMinutes(15);
        // send another location share request with updated time
        userConnectionService.shareLocation(request);

        userActivities = userActivityRepository.getActiveLocationShareActivityByUser(user6.getUserId());

        Assert.assertEquals("Success - Only One location share activity created for user6", 1, userActivities.size());
        Assert.assertEquals("Success - Location share activity created for user7", user7.getUserId(),
                userActivities.get(0).getActivityForId());
        Assert.assertEquals("Success - Existing activity time Updated", new Integer(15),
                userActivities.get(0).getLocationSharedForTime());
    }

    /**
     * Method - addContactToGroup and removeContactFromGroup
     * TestCase - Success
     */
    @Test
    public void testAddAndRemoveContactToGroup() {
        Exception exception = null;
        given(this.tokenAuthService.getSessionUser()).willReturn(user6);
        mockSQSServices();

        List<UserGroupData> userGroupDataList = MethodStubs.getUserGroupData();
        UserGroupRequestDTO userGroupRequestDTO = new UserGroupRequestDTO();
        userGroupRequestDTO.setUserGroupList(userGroupDataList);
        AddUserGroupResponseDTO addUserGroupResponseDTO = userGroupService.addGroups(userGroupRequestDTO);
        String groupId = addUserGroupResponseDTO.getUserGroupList().get(0).getGroupId();

        AddContactsToGroupRequestDTO requestDTO = new AddContactsToGroupRequestDTO();
        requestDTO.setContactIdList(Arrays.asList(userConnection6_7.getConnectionId(), userConnection6_8.getConnectionId()));
        requestDTO.setGroupId(groupId);

        AddContactsToGroupResponseDTO responseDTO = userConnectionService.addContactToGroup(requestDTO);

        Assert.assertEquals("Success: Two contacts added to Group", 2, responseDTO.getContactIdList().size());

        // remove contact from group
        RemoveContactsFromGroupRequestDTO removeContactsFromGroupRequestDTO = new RemoveContactsFromGroupRequestDTO();
        removeContactsFromGroupRequestDTO.setContactIdList(Arrays.asList(userConnection6_7.getConnectionId()));
        removeContactsFromGroupRequestDTO.setGroupId(groupId);

        ContactListDTO contactListResponseDTO =
                userConnectionService.removeContactFromGroup(removeContactsFromGroupRequestDTO);

        Assert.assertEquals("Success: One contact removed from group", 1, contactListResponseDTO.getContactList().size());
        Assert.assertEquals("Success: User7 is removed from group", userConnection6_7.getConnectionId(),
                contactListResponseDTO.getContactList().get(0));

    }

    /**
     * Method - updateContactImage
     * TestCase - Success
     */
    @Test
    public void testUpdateContactImage() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user6);
        UpdateContactImageRequest requestDTO = new UpdateContactImageRequest();
        requestDTO.setContactImageList(Arrays.asList(MethodStubs.getContactImageObj(userConnection6_7.getConnectionId(), "www.updateImage.com")));

        List<String> response = userConnectionService.updateContactImage(requestDTO);

        Assert.assertNotNull("Success - Contact Image Updated", response);
        Assert.assertEquals("Success - image updated for connection6_7", userConnection6_7.getConnectionId(),
                response.get(0));
    }

    /**
     * Method - removeConnection
     * TestCase - Success
     */
    @Test
    public void testRemoveConnection() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        RemoveConnectionRequest requestDTO = new RemoveConnectionRequest();
        requestDTO.setConnectionIdList(Arrays.asList(userConnection1_12.getConnectionId()));

        RemoveConnectionResponse responseDTO = userConnectionService.removeConnection(requestDTO);

        Assert.assertNotNull("Success - Connection Removed Successfully", responseDTO);
        Assert.assertEquals("Success - User12 Connection Removed", userConnection1_12.getConnectionId(),
                responseDTO.getRemovedConnectionIdList().get(0).getConnectionId());
    }

    /**
     * Method - removeConnection
     * TestCase - Failure - Invalid connectionId
     */
    @Test
    public void testRemoveConnectionFailure() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        Exception exception = null;

        RemoveConnectionRequest requestDTO = new RemoveConnectionRequest();
        requestDTO.setConnectionIdList(Arrays.asList(new ObjectId().toString()));

        try {
            userConnectionService.removeConnection(requestDTO);
        } catch (Exception e) {
            exception = e;
        }
        Assert.assertTrue("Failure - expected BadRequestException", exception instanceof BadRequestException);
    }

    /**
     * Method - deleteInfo
     * TestCase - Success
     * user1 saves the deleted info of user2 as static data
     */
    @Test
    public void testDeleteInfo() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        //user2 deleted his one of the UserProfileData
        UserProfileData deletedUserProfileData = user2.getUserMetadataList().remove(0);
        peopleUserRepository.save(user2);
        DeleteInfoRequestDTO deleteInfoRequestDTO = new DeleteInfoRequestDTO();
        UserInformationDTO userInformationDTO = MethodStubs.getUserInformationObject();
        userInformationDTO.setUserMetadataList(Arrays.asList(deletedUserProfileData));
        //setting up the deleted data in user1_2 connection object
        userConnection1_2.setConnectionDeletedData(userInformationDTO);
        userConnectionRepository.save(userConnection1_2);
        deleteInfoRequestDTO.setConnectionId(userConnection1_2.getConnectionId());
        deleteInfoRequestDTO.setRetrieveDeletedInfo(Boolean.TRUE);
        DeletedInfoResponseDTO deletedInfoResponseDTO = userConnectionService.deleteInfo(deleteInfoRequestDTO);
        boolean flag=false;
        for (UserProfileData userProfileData : deletedInfoResponseDTO.getContactData().getStaticProfileData().getUserMetadataList()) {
            if (deletedUserProfileData.getValueId().equals(userProfileData.getValueId())) {
                flag = true;
                break;
            }
        }
        Assert.assertTrue("Success - deleted info saved in static profile",flag);
    }
    /**
     * Method - deleteInfo
     * TestCase - Failure
     * Invalid connection id
     */
    @Test(expected = BadRequestException.class)
    public void testDeleteInfoWithInvalidConnectionId() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        DeleteInfoRequestDTO deleteInfoRequestDTO = new DeleteInfoRequestDTO();
        UserInformationDTO userInformationDTO = MethodStubs.getUserInformationObject();
        userConnection1_2.setConnectionDeletedData(userInformationDTO);
        userConnectionRepository.save(userConnection1_2);
        //setting random connection Id
        deleteInfoRequestDTO.setConnectionId(new ObjectId().toString());
        deleteInfoRequestDTO.setRetrieveDeletedInfo(Boolean.TRUE);
        userConnectionService.deleteInfo(deleteInfoRequestDTO);

    }

    private void mockSQSServices() {
        doNothing().when(queueServiceMock).sendPayloadToSQS(isA(SQSPayload.class));
        doNothing().when(queueServiceMock).sendPayloadToSQS(anyList());
    }

    /**
     * Method - UpdateContactStaticData
     * TestCase - Success
     * Updated the Last Name and GroupList of given static contact,Which is not a Registered user.
     */
    @Test
    public void testUpdateContactStaticDataForNotRegisteredUser() {
        //created group and added staticConnection1 into it
        UserGroup userGroup1=userGroupRepository.save(MethodStubs.getUserGroupWithContactsAdded(user1.getUserId(),
                Arrays.asList(staticConnection1.getConnectionId(),userNotConnectedConnection1_4.getConnectionId())));
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        EditStaticDataRequestDTO editStaticDataRequestDTO = new EditStaticDataRequestDTO();
        List<ContactStaticData> contactStaticDataList=new ArrayList<>();
        ContactStaticData contactStaticData=new ContactStaticData();
        contactStaticData.setConnectionId(staticConnection1.getConnectionId());
        UserInformationDTO userInformationDTO=new UserInformationDTO();
        userInformationDTO.setLastName("Last Name");
        contactStaticData.setStaticProfileData(userInformationDTO);
        contactStaticDataList.add(contactStaticData);
        editStaticDataRequestDTO.setContactStaticDataList(contactStaticDataList);
        UserContactData userContactData=null;
        EditStaticDataResponseDTO editStaticDataResponseDTO=userConnectionService.updateContactStaticData(editStaticDataRequestDTO);
        for(UserContactData userContact :editStaticDataResponseDTO.getEditedContactDataList()){
            if(userContact.getConnectionId().equals(staticConnection1.getConnectionId())){
                userContactData=userContact;
                break;
            }
        }
        Assert.assertEquals("Success - Last name updated for given static contact",userInformationDTO.getLastName(),userContactData.getStaticProfileData().getLastName());
        Assert.assertEquals("Success - Contact present in userGroup1 ",1,userContactData.getGroupIdList().size());
    }
    /**
     * Method - UpdateContactStaticData
     * TestCase - Success
     * Updated the Last Name and TagList of given static contact,Which is Registered as well as connected user.
     */
    @Test
    public void testUpdateContactStaticDataForRegisteredAndConnectedUser() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        //changing connection status to connected
        userConnection1_2.setConnectionStatus(ConnectionStatus.CONNECTED);
        EditStaticDataRequestDTO editStaticDataRequestDTO = new EditStaticDataRequestDTO();
        List<ContactStaticData> contactStaticDataList=new ArrayList<>();
        ContactStaticData contactStaticData=new ContactStaticData();
        contactStaticData.setConnectionId(userConnection1_2.getConnectionId());
        UserInformationDTO userInformationDTO=new UserInformationDTO();
        userInformationDTO.setLastName("Last Name");
        userInformationDTO.setTagList(Arrays.asList("Musician","Singer"));
        contactStaticData.setStaticProfileData(userInformationDTO);
        contactStaticDataList.add(contactStaticData);
        editStaticDataRequestDTO.setContactStaticDataList(contactStaticDataList);
        UserContactData userContactData=null;
        EditStaticDataResponseDTO editStaticDataResponseDTO=userConnectionService.updateContactStaticData(editStaticDataRequestDTO);
        for(UserContactData userContact :editStaticDataResponseDTO.getEditedContactDataList()){
            if(userContact.getConnectionId().equals(userConnection1_2.getConnectionId())){
                userContactData=userContact;
                break;
            }
        }
        Assert.assertEquals("Success - Last name updated for given static contact",userInformationDTO.getLastName(),userContactData.getStaticProfileData().getLastName());
        Assert.assertEquals("Success - 2 New tag added to contact",2,userContactData.getStaticProfileData().getTagList().size());
    }
    /**
     * Method - UpdateContactStaticData
     * TestCase - Failure
     * Response back with bad request exception as passing invalid connection ID
     */
    @Test(expected = BadRequestException.class)
    public void testUpdateContactStaticDataWhenConnectionIdIsInvalid() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        EditStaticDataRequestDTO editStaticDataRequestDTO = new EditStaticDataRequestDTO();
        List<ContactStaticData> contactStaticDataList=new ArrayList<>();
        ContactStaticData contactStaticData=new ContactStaticData();
        //creating random id
        contactStaticData.setConnectionId(new ObjectId().toString());
        UserInformationDTO userInformationDTO=new UserInformationDTO();
        userInformationDTO.setLastName("Last Name");
        contactStaticData.setStaticProfileData(userInformationDTO);
        contactStaticDataList.add(contactStaticData);
        editStaticDataRequestDTO.setContactStaticDataList(contactStaticDataList);
        userConnectionService.updateContactStaticData(editStaticDataRequestDTO);
    }

    /**
     * Method - mergeContacts
     * TestCase - Success
     * Merging contacts and syncing the contact details to group*/
    @Test
    public void testMergeContactsSyncingGroupContacts() {

        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        List<String> contactsInGroup = Arrays.asList(userNotConnectedConnection1_3.getConnectionId(), userNotConnectedConnection1_4.getConnectionId(),
                userNotConnectedConnection1_5.getConnectionId());
//       A group is created for user1 and user 1 has only one group
        UserGroup userGroup1 = userGroupRepository.save(MethodStubs.getUserGroupWithContactsAdded(user1.getUserId(),
                contactsInGroup));

        Assert.assertEquals("There are 3 contacts in group", 3, userGroup1.getContactIdList().size());
        Assert.assertTrue("List of contacts in group ", userGroup1.getContactIdList().containsAll(contactsInGroup));

        userConnectionService.mergeContacts(MethodStubs.getMergeContactsRequestDTO(userConnection1_2.getConnectionId(), contactsInGroup));

        List<UserGroup> userGroups = userGroupRepository.fetchAllUserGroups(user1.getUserId());

        Assert.assertEquals("After merging the contacts 3 contacts with the master contact, group contains only" +
                " 1 contact of master", 1, userGroups.get(0).getContactIdList().size());

        Assert.assertTrue("All the 3 contacts were merged to userConnection1_2", userGroups.get(0).getContactIdList()
                .contains(userConnection1_2.getConnectionId()));

    }

    /**
     * Method - mergeContacts
     * TestCase - Success
     * Merging contacts and ignoring then master connection id if present in merged contacts list
     * If the master id was not ignored then it would also be deleted  as part of deleting merged contacts*/
    @Test
    public void testMergeContactsRemoving() {

        given(this.tokenAuthService.getSessionUser()).willReturn(user1);

        UserContactData masterContact = userConnectionService.fetchConnectionDetails(userConnection1_2.getConnectionId())
                .getUserConnectionDetail();
        Assert.assertNotNull("Selected valid master contact ", masterContact);
        List<String> contactsToBeMerged = new ArrayList<>();
        contactsToBeMerged.add(userNotConnectedConnection1_3.getConnectionId());
        contactsToBeMerged.add(userConnection1_2.getConnectionId());
        userConnectionService.mergeContacts(MethodStubs.getMergeContactsRequestDTO(masterContact.getConnectionId(), contactsToBeMerged));

        Exception exception = null;
        try {
            userConnectionService.fetchConnectionDetails(userNotConnectedConnection1_3.getConnectionId());
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Merged Contact data is deleted, since no data was found exception is thrown",
                exception instanceof BadRequestException);

        Assert.assertNotNull("Master contact data is not affected even though it was part of merged contact list",
                userConnectionService.fetchConnectionDetails(userConnection1_2.getConnectionId()));

    }


    @After
    public void tearDown() {
        userConnectionRepository.deleteAll();
        userActivityRepository.deleteAll();
        activityContactRepository.deleteAll();
        peopleUserRepository.deleteAll();
        userPrivacyProfileRepository.deleteAll();
        userGroupRepository.deleteAll();
    }

}
