package com.peopleapp.service;

import com.peopleapp.configuration.LocaleMessageReader;
import com.peopleapp.dto.ActivityDetails;
import com.peopleapp.dto.ActivityType;
import com.peopleapp.dto.SQSPayload;
import com.peopleapp.dto.UserActivityData;
import com.peopleapp.dto.requestresponsedto.*;
import com.peopleapp.enums.Action;
import com.peopleapp.enums.ActivityStatus;
import com.peopleapp.enums.RequestType;
import com.peopleapp.exception.BadRequestException;
import com.peopleapp.model.PeopleUser;
import com.peopleapp.model.UserActivity;
import com.peopleapp.model.UserConnection;
import com.peopleapp.model.UserPrivacyProfile;
import com.peopleapp.repository.*;
import com.peopleapp.security.TokenAuthService;
import com.peopleapp.util.PeopleUtils;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

@RunWith(SpringRunner.class)
@SpringBootTest
public class UserActivityServiceTest extends BaseTest {

    @MockBean
    private TokenAuthService tokenAuthService;

    @Inject
    private NetworkService networkService;

    @Inject
    private NetworkRepository networkRepository;

    @Inject
    private NetworkMemberRepository networkMemberRepository;

    @Inject
    private LocaleMessageReader message;

    @Inject
    private UserActivityService userActivityService;

    @Inject
    private RegisteredNumberRepository registeredNumberRepository;

    @Inject
    private PeopleUserRepository peopleUserRepository;

    @Inject
    private PeopleUserService peopleUserService;

    @Inject
    private UserConnectionRepository userConnectionRepository;

    @Inject
    private UserConnectionService userConnectionService;

    @Inject
    private UserPrivacyProfileRepository userPrivacyProfileRepository;

    @Inject
    private UserActivityRepository userActivityRepository;

    @MockBean
    private QueueService queueServiceMock;

    private PeopleUser user1;
    private PeopleUser user2;
    private PeopleUser user3;
    private PeopleUser user4;

    private UserPrivacyProfile user1Profile;
    private UserPrivacyProfile user2Profile;
    private UserPrivacyProfile user3Profile;
    private UserPrivacyProfile user4Profile;

    private UserConnection connection1_2;
    private UserConnection connection1_3;
    private UserConnection connection2_1;
    private UserConnection connection3_1;
    private UserConnection connection3_2;
    private UserConnection connection2_3;
    private UserConnection staticConnection1_4;


    @Before
    public void setUp() {

        // user's initialization
        user1 = peopleUserRepository.save(
                MethodStubs.getWatuUserAccount("8007009001", "testWatuPeople1@mailinator.com",
                        "mockUser1", "", "YML"));
        user2 = peopleUserRepository.save(
                MethodStubs.getWatuUserAccount("8007009002", "testWatuPeople2@mailinator.com",
                        "mockUser2", "", "YML"));
        user3 = peopleUserRepository.save(
                MethodStubs.getWatuUserAccount("8007009003", "testWatuPeople3@mailinator.com",
                        "mockUser3", "", "YML"));
        user4 = peopleUserRepository.save(
                MethodStubs.getWatuUserAccount("8007009004", "testWatuPeople4@mailinator.com",
                        "mockUser4", "", "YML"));


        /*  register numbers to watu */
        registeredNumberRepository.save(MethodStubs.getRegisteredNumber(Arrays.asList("+18007009001", "+18007009002",
                "+18007009003", "+18007009004")));

        /*  creating profiles for users   */
        user1Profile = userPrivacyProfileRepository.save(MethodStubs.getUserPrivacyProfileObj(user1.getUserId(),
                new ArrayList<>(user1.getMetadataMap().keySet())));
        user2Profile = userPrivacyProfileRepository.save(MethodStubs.getUserPrivacyProfileObj(user2.getUserId(),
                new ArrayList<>(user2.getMetadataMap().keySet())));
        user3Profile = userPrivacyProfileRepository.save(MethodStubs.getUserPrivacyProfileObj(user3.getUserId(),
                new ArrayList<>(user3.getMetadataMap().keySet())));
        user4Profile = userPrivacyProfileRepository.save(MethodStubs.getUserPrivacyProfileObj(user4.getUserId(),
                new ArrayList<>(user4.getMetadataMap().keySet())));


        /*  User1 is connected with user2 and user3 */
        connection1_2 = userConnectionRepository.save(MethodStubs.getConnectionWithProfileObj(user1.getUserId(),
                user2.getUserId(), user1Profile.getPrivacyProfileId()));
        connection1_3 = userConnectionRepository.save(MethodStubs.getConnectionWithProfileObj(user1.getUserId(),
                user3.getUserId(), user1Profile.getPrivacyProfileId()));

        connection2_1 = userConnectionRepository.save(MethodStubs.getConnectionWithProfileObj(user2.getUserId(),
                user1.getUserId(), user2Profile.getPrivacyProfileId()));
        connection3_1 = userConnectionRepository.save(MethodStubs.getConnectionWithProfileObj(user3.getUserId(),
                user1.getUserId(), user3Profile.getPrivacyProfileId()));

        /*  user1 has one static contact with user4's  contact detail    */
        staticConnection1_4 = userConnectionRepository.save(MethodStubs.getUserStaticConnectionWithGivenContactNumber(
                user1.getUserId(), user4.getVerifiedContactNumber()));


        /*  User3 and user 2 are connected  */
        connection3_2 = userConnectionRepository.save(MethodStubs.getConnectionWithProfileObj(user3.getUserId(),
                user2.getUserId(), user3Profile.getPrivacyProfileId()));
        connection2_3 = userConnectionRepository.save(MethodStubs.getConnectionWithProfileObj(user2.getUserId(),
                user3.getUserId(), user2Profile.getPrivacyProfileId()));

        mockSQSServices();

    }


    /**
     * Method   -   getActivitiesCreatedForUser
     * TestCase -   Success
     * Description  -   Initially user4 does not have any activity and when user1 send connection request,
     * activity is created for user4
     */
    @Test
    public void testListingAllReceivedActivities() {

        // log into user 4 and check for any activities
        given(this.tokenAuthService.getSessionUser()).willReturn(user4);
        ActivityListResponse activityListResponse = userActivityService.getActivitiesCreatedForUser(0, 10);
        Assert.assertEquals("user 4 has no activity in activity list", 0,
                activityListResponse.getTotalElements());

        // log into user 1 and send connection request to user4
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        SendConnectionRequestResponse responseDTO = userConnectionService.sendConnectionRequest(
                MethodStubs.getConnectionRequestObjectWithUserIDFlow(user4.getUserId(), user1Profile));

        Assert.assertFalse(" Success - connection request was sent successfully ",
                PeopleUtils.isNullOrEmpty(responseDTO.getActivityDetailsList()));

        // log into user 2 and send connection request to user4
        given(this.tokenAuthService.getSessionUser()).willReturn(user2);
        SendConnectionRequestResponse responseDTO2 = userConnectionService.sendConnectionRequest(
                MethodStubs.getConnectionRequestObjectWithUserIDFlow(user4.getUserId(), user2Profile));

        Assert.assertFalse(" Success - connection request was sent successfully ",
                PeopleUtils.isNullOrEmpty(responseDTO2.getActivityDetailsList()));

        // log into user 4 and check for any activities
        given(this.tokenAuthService.getSessionUser()).willReturn(user4);
        ActivityListResponse receivedRequestList = userActivityService.getActivitiesCreatedForUser(0, 1);

        Assert.assertEquals("Success - user 4 has received two connection request activity", 2,
                receivedRequestList.getTotalElements());

        Assert.assertNotNull("Link to fetch next activity list page is present ", receivedRequestList.getNextURL());
    }

    /**
     * Method   -   getActivitiesCreatedByUser
     * TestCase -   Success
     * Description  -   Initially user1 has not sent any request and when user1 send connection request,
     * one sent activity is listed for user1
     */
    @Test
    public void testListingAllSentActivities() {
        ActivityListResponse sentRequestList = null;
        // log into user 1 and check for any activities
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        sentRequestList = userActivityService.getActivitiesCreatedByUser(0, 10);

        Assert.assertEquals(" user 1 neither sent any request nor initiated any activity ", 0,
                sentRequestList.getTotalElements());

        //  user1 requested user 2 for more info
        userConnectionService.moreInfoRequest(MethodStubs.requestForMoreInfo(connection1_2.getConnectionId()));

        // User 1 sends connection request to user4
        SendConnectionRequestResponse responseDTO = userConnectionService.sendConnectionRequest(
                MethodStubs.getConnectionRequestObjectWithUserIDFlow(user4.getUserId(), user1Profile));

        Assert.assertFalse(" Success - connection request was sent successfully ",
                PeopleUtils.isNullOrEmpty(responseDTO.getActivityDetailsList()));

        sentRequestList = userActivityService.getActivitiesCreatedByUser(0, 1);

        Assert.assertEquals("Success - user 1 has initiated two activities by sending connection request to " +
                "user 4 and asking for more info to user2", 2, sentRequestList.getTotalElements());

        Assert.assertNotNull("Link to fetch next activity list page is present ", sentRequestList.getNextURL());
    }

    /**
     * Method   -   getActivityDetailsByActivityId
     * TestCase -   Success
     * Description  -   Details of the activity in activity list is fetched by passing the activity id.
     */
    @Test
    public void testFetchingActivityDetails() {

        //  User 1 sends connection request to user4
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        userConnectionService.sendConnectionRequest(MethodStubs.getConnectionRequestObjectWithUserIDFlow(
                user4.getUserId(), user1Profile));

        //  user4 login and fetches activity details for the activity created
        given(this.tokenAuthService.getSessionUser()).willReturn(user4);
        //  get activity list
        ActivityListResponse receivedRequestList = userActivityService.getActivitiesCreatedForUser(0, 10);
        //  get activity details
        List<UserActivityData> userActivityData = userActivityService.getActivityDetailsByActivityId(
                receivedRequestList.getUserActivityList().get(0).getActivityDetails().getActivityId());
        UserActivityData activityData = userActivityData.get(0);

        Assert.assertNotNull("Success - details for the activity is fetched", activityData);

        RequestType requestType = activityData.getActivityDetails().getActivityType().getRequestType();

        //  verify the activity details has initiator details and RequestType
        Assert.assertTrue("Activity created is of connection request type ",
                requestType.equals(RequestType.CONNECTION_REQUEST));

        Assert.assertTrue(activityData.getInitiatorDetails().getToUserId().equals(user1.getUserId()));
    }

    /**
     * Method   -   getActivityDetailsByActivityId
     * TestCase -   failure
     * Description  -   Exception will be thrown if trying get activity details which was not created for user.
     */
    @Test
    public void testFetchingActivityDetailsForActivityCreatedForOtherUser() {
        Exception exception = null;

        given(this.tokenAuthService.getSessionUser()).willReturn(user4);
        //  get activity list
        ActivityListResponse receivedRequestList = userActivityService.getActivitiesCreatedForUser(0, 10);

        //no activity was created for user4
        Assert.assertTrue(PeopleUtils.isNullOrEmpty(receivedRequestList.getUserActivityList()));

        try {
            userActivityService.getActivityDetailsByActivityId(new ObjectId().toString());
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Failure - user cannot access the activity which was not created for them ",
                exception instanceof BadRequestException);
    }

    /**
     * Method   -   clearActivity
     * TestCase -   Success
     * Description  -   user can clear all or selected activities
     */
    @Test
    public void testClearingFewActivities() {
        //  User 4 sends connection request to user2
        given(this.tokenAuthService.getSessionUser()).willReturn(user4);
        userConnectionService.sendConnectionRequest(MethodStubs.getConnectionRequestObjectWithUserIDFlow(
                user2.getUserId(), user4Profile));

        //  user1 requested user 2 for more info
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        ActivityDetails activityDetails = userConnectionService.moreInfoRequest(MethodStubs.requestForMoreInfo(
                connection1_2.getConnectionId()));

        // more info activity will be cleared by user2
        ClearActivityRequest clearActivityRequest = new ClearActivityRequest();
        clearActivityRequest.setActivityIdList(Arrays.asList(activityDetails.getActivityId()));
        clearActivityRequest.setIsAllActivityCleared(false);

        given(this.tokenAuthService.getSessionUser()).willReturn(user2);
        ActivityListResponse activityListResponseBeforeClearingActivity =
                userActivityService.getActivitiesCreatedForUser(0, 10);

        Assert.assertEquals("Success - user 2 has two activity in activity list", 2,
                activityListResponseBeforeClearingActivity.getTotalElements());

        userActivityService.clearActivity(clearActivityRequest);

        ActivityListResponse activityListResponseAfterClearingActivity =
                userActivityService.getActivitiesCreatedForUser(0, 10);
        Assert.assertEquals("Success -  user 2 has cleared one activity in activity list", 1,
                activityListResponseAfterClearingActivity.getTotalElements());
    }

    /**
     * Method   -   clearActivity
     * TestCase -   Success
     * Description  -   user can clear all or selected activities
     */
    @Test
    public void testClearingAllActivities() {
        //  User 4 sends connection request to user2
        given(this.tokenAuthService.getSessionUser()).willReturn(user4);
        userConnectionService.sendConnectionRequest(MethodStubs.getConnectionRequestObjectWithUserIDFlow(
                user2.getUserId(), user4Profile));

        //  user1 requested user 2 for more info
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        userConnectionService.moreInfoRequest(MethodStubs.requestForMoreInfo(connection1_2.getConnectionId()));

        // more info activity will be cleared by user2
        ClearActivityRequest clearActivityRequest = new ClearActivityRequest();
        clearActivityRequest.setIsAllActivityCleared(true);

        given(this.tokenAuthService.getSessionUser()).willReturn(user2);
        ActivityListResponse activityListResponseBeforeClearingActivity =
                userActivityService.getActivitiesCreatedForUser(0, 10);

        Assert.assertEquals("Success - user 2 has two activity in activity list", 2,
                activityListResponseBeforeClearingActivity.getTotalElements());

        userActivityService.clearActivity(clearActivityRequest);

        ActivityListResponse activityListResponseAfterClearingActivity =
                userActivityService.getActivitiesCreatedForUser(0, 10);
        Assert.assertEquals("Success -  user 2 has cleared all activities in activity list", 0,
                activityListResponseAfterClearingActivity.getTotalElements());
    }

    /**
     * Method   -   cancelActivity
     * TestCase -   Success
     * Description  -   User can cancel the any request made.
     */
    @Test
    public void testCancelingARequest() {
        ActivityListResponse sentRequestList = null;
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        // User 1 sends connection request to user4
        userConnectionService.sendConnectionRequest(MethodStubs.getConnectionRequestObjectWithUserIDFlow(
                user4.getUserId(), user1Profile));
        //  user 1 shares location with user3
        userConnectionService.shareLocation(MethodStubs.getShareLocationRequestDTO(
                Arrays.asList(connection1_3.getConnectionId()), 2));

        sentRequestList = userActivityService.getActivitiesCreatedByUser(0, 10);

        Assert.assertEquals("Success - user has sent a connection request and shared location," +
                " hence 2 activity is listed", 2, sentRequestList.getTotalElements());

        //  user1 decides to cancel activity
        userActivityService.cancelActivity(MethodStubs.getActivityCancelRequestDTO(Arrays.asList(
                sentRequestList.getUserActivityList().get(0).getActivityDetails().getActivityId(),
                sentRequestList.getUserActivityList().get(1).getActivityDetails().getActivityId()))
        );

        sentRequestList = userActivityService.getActivitiesCreatedByUser(0, 10);

        Assert.assertEquals("Success - after user1 cancel the connection request there will be no sent activity ",
                0, sentRequestList.getTotalElements());
    }

    /**
     * Method   -   cancelActivity
     * TestCase -   Failure
     * Description  -   User can cancel any request made and cancel can be performed on valid activity only once.
     * Performing this action twice on same activity or on invalid activity will result in exception
     */
    @Test
    public void testCancelingARequestWhichIsAlreadyCancelled() {
        ActivityListResponse sentRequestList = null;
        Exception exception = null;
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        sentRequestList = userActivityService.getActivitiesCreatedByUser(0, 10);

        Assert.assertEquals("Success - user has sent a connection request hence one sent activity is listed",
                0, sentRequestList.getTotalElements());

        try {
            userActivityService.cancelActivity(MethodStubs.getActivityCancelRequestDTO(
                    Arrays.asList(new ObjectId().toString())));
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Failure - user trying to cancel invalid activity will result in exception",
                exception instanceof BadRequestException);
    }

    /**
     * Method   -   ignoreActivity
     * TestCase -   Success
     * Description  -   When a user receives any activity they can ignore the activity
     * which would not be listed in activity list
     */
    @Test
    public void testIgnoringReceivedActivity() {
        ActivityListResponse activityList;
        //user 1 sends request for more info to user3
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        ActivityDetails activityDetails = userConnectionService.moreInfoRequest(
                MethodStubs.requestForMoreInfo(connection1_3.getConnectionId()));

        //  user 3 login and ignore the activity received
        given(this.tokenAuthService.getSessionUser()).willReturn(user3);
        activityList = userActivityService.getActivitiesCreatedForUser(0, 10);
        Assert.assertEquals("user3 has 1 activity in activity list", 1, activityList.getTotalElements());

        userActivityService.ignoreActivity(MethodStubs.getActivityIgnoreRequestDTO(Arrays.asList(
                activityDetails.getActivityId()), null));

        activityList = userActivityService.getActivitiesCreatedForUser(0, 10);

        Assert.assertEquals("Success - user3 has ignored the request for more info and" +
                " no activity will be listed", 0, activityList.getTotalElements());

    }

    /**
     * Method   -   ignoreActivity
     * TestCase -   Failure
     * Description  -   When a user receives any activity they can ignore the activity only once
     * which would not be listed in activity list. Performing this action twice on same activity or on invalid activity,
     * result in exception
     */
    @Test
    public void testIgnoringReceivedActivityWhichIsIgnoredAlready() {
        ActivityListResponse activityList;
        Exception exception = null;

        //  user3 login and ignore the activity
        given(this.tokenAuthService.getSessionUser()).willReturn(user3);
        activityList = userActivityService.getActivitiesCreatedForUser(0, 5);
        Assert.assertEquals("user3 has nor received any activity", 0, activityList.getTotalElements());

        try {
            userActivityService.ignoreActivity(MethodStubs.getActivityIgnoreRequestDTO(
                    Arrays.asList(new ObjectId().toString()), null));
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Failure - user trying to ignore invalid activity will result in exception being " +
                "thrown", exception instanceof BadRequestException);

    }

    /**
     * Method   -   deleteActivity
     * TestCase -   Success
     * Description  -
     */
    @Test
    public void testDeletingActivity() {
        ActivityListResponse activityList;
        //  user 1 sends request for more info to user3
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        userConnectionService.moreInfoRequest(MethodStubs.requestForMoreInfo(connection1_3.getConnectionId()));

        //  user3 login and ignore the activity
        given(this.tokenAuthService.getSessionUser()).willReturn(user3);
        activityList = userActivityService.getActivitiesCreatedForUser(0, 5);
        Assert.assertEquals("user3 has 1 activity in activity list", 1, activityList.getTotalElements());

        userActivityService.deleteActivity(MethodStubs.getDeleteActivityDTO(Arrays.asList(activityList.getUserActivityList()
                .get(0).getActivityDetails().getActivityId())));

        activityList = userActivityService.getActivitiesCreatedForUser(0, 5);
        Assert.assertEquals("Success - user3 has deleted the received activity and activity list is now empty",
                0, activityList.getTotalElements());

    }

    /**
     * Method   -   deleteActivity
     * TestCase -   Failure
     * Description  -   If user tries to delete the activity which is already deleted or any invalid activity, results
     * in exception. invalid activity -> activity which is sent to another user.
     */
    @Test
    public void testDeletingInValidActivity() {
        ActivityListResponse activityList;
        Exception exception = null;

        given(this.tokenAuthService.getSessionUser()).willReturn(user3);
        activityList = userActivityService.getActivitiesCreatedForUser(0, 5);
        Assert.assertEquals("user3 has not received any activity", 0, activityList.getTotalElements());

        try {
            userActivityService.deleteActivity(MethodStubs.getDeleteActivityDTO(Arrays.asList(new ObjectId().toString())));
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Failure - user3 trying to delete activity that is invalid",
                exception instanceof BadRequestException);

    }

    /**
     * Method   -   getSharedContactWithOthers
     * TestCase -   Success
     * Description  -   user can share static/connected contacts only with connected contact
     */
    @Test
    public void testFetchingDetailsOfContactsSharedWithOthers() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        //  user1 shares contact of user4 with user2 and user3

        userConnectionService.shareContact(MethodStubs.getShareContactRequestDTO(
                Arrays.asList(connection1_2.getConnectionId(), connection1_3.getConnectionId()),
                Arrays.asList(staticConnection1_4.getConnectionId())));

        SharedContactWithOthersResponse sharedWithOthersResponse = userActivityService.getSharedContactWithOthers(0, 1);
        Assert.assertEquals("Success - User1 shared user4 contact with users 2 and 3, " +
                "there will be separate sharedContact details", 2, sharedWithOthersResponse.getTotalElements());
        Assert.assertNotNull("Success - Due to page size being one and 2 records in list, nextURL is generated",
                sharedWithOthersResponse.getNextURL());

    }

    /**
     * Method   -   getSharedContactWithMe
     * TestCase -   Success
     */
    @Test
    public void testFetchingDetailsOfContactsSharedWithME() {

        //  user1 shares contact of user4 and user3 with user2
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        userConnectionService.shareContact(MethodStubs.getShareContactRequestDTO(
                Arrays.asList(connection1_2.getConnectionId()),
                Arrays.asList(staticConnection1_4.getConnectionId(), connection1_3.getConnectionId())));

        //  user3 shares user1 contact with user2
        given(this.tokenAuthService.getSessionUser()).willReturn(user3);
        userConnectionService.shareContact(MethodStubs.getShareContactRequestDTO(
                Arrays.asList(connection3_2.getConnectionId()),
                Arrays.asList(connection3_1.getConnectionId())));

        given(this.tokenAuthService.getSessionUser()).willReturn(user2);
        SharedContactWithMeResponse sharedWithMeResponse = userActivityService.getSharedContactWithMe(0, 1);
        Assert.assertEquals("Success - User1 and user 3 shared contacts with user2," +
                " user2 will have 2 contacts details", 2, sharedWithMeResponse.getTotalElements());
        Assert.assertNotNull("Success - Due to page size being one and 2 records in list, nextURL is generated",
                sharedWithMeResponse.getNextURL());

    }

    /**
     * Method   - getActiveLocationSharedWithOthers
     * TestCase -   Success
     */
    @Test
    public void testLocationSharedWithOthers() {
        //  user1 shares location with user 2 and 3
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        userConnectionService.shareLocation(MethodStubs.getShareLocationRequestDTO(
                Arrays.asList(connection1_2.getConnectionId(), connection1_3.getConnectionId()), 2));

        SharedLocationWithOthersResponse sharedLocationWithOthers =
                userActivityService.getActiveLocationSharedWithOthers(0, 1);

        Assert.assertEquals("Success - User1 has shared location with user2 and user3 ",
                2, sharedLocationWithOthers.getTotalElements());
        Assert.assertNotNull("with page size of 1, there will be 2 pages  ",
                sharedLocationWithOthers.getNextURL());
    }

    /**
     * Method   - getActiveLocationSharedWithMe
     * TestCase -   Success
     */
    @Test
    public void testLocationSharedWithMe() {
        //  user 1 shares location with user3
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        userConnectionService.shareLocation(MethodStubs.getShareLocationRequestDTO(
                Arrays.asList(connection1_3.getConnectionId()), 2));

        //  user 2 shares location with user3
        given(this.tokenAuthService.getSessionUser()).willReturn(user2);
        userConnectionService.shareLocation(MethodStubs.getShareLocationRequestDTO(
                Arrays.asList(connection2_3.getConnectionId()), 2));

        //  user 3 login and checks all locations shared with them
        given(this.tokenAuthService.getSessionUser()).willReturn(user3);
        SharedLocationWithMeResponse sharedLocationWithOthers =
                userActivityService.getActiveLocationSharedWithMe(0, 1);

        Assert.assertEquals("Success - User1 has shared location with user2 and user3 ",
                2, sharedLocationWithOthers.getTotalElements());
        Assert.assertNotNull("with page size of 1, there will be 2 pages  ",
                sharedLocationWithOthers.getNextURL());
    }

    /**
     * Method   -   editSharedContactActivity
     * TestCase -   Success
     * Description -    User sharing the the contact can stop sharing it by making this call or
     * the receiver can make this call to remove all the shared contacts
     */
    @Test
    public void testEditingSharedContacts() {
        Exception exception = null;
        ActivityListResponse sentRequestList;
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        //  user1 shares contact of user4 with user and user3
        userConnectionService.shareContact(MethodStubs.getShareContactRequestDTO(
                Arrays.asList(connection1_2.getConnectionId()),
                Arrays.asList(staticConnection1_4.getConnectionId())));
        sentRequestList = userActivityService.getActivitiesCreatedByUser(0, 10);
        Assert.assertEquals(1, sentRequestList.getTotalElements());

        try {
            userActivityService.editSharedContactActivity(MethodStubs.getEditSharedContactDTO(
                    Arrays.asList(sentRequestList.getUserActivityList().get(0).getActivityDetails().getActivityId()), null));
            sentRequestList = userActivityService.getActivitiesCreatedByUser(0, 10);
        } catch (Exception e) {
            exception = e;
        }


        Assert.assertNull("Success - Edited successfully.", exception);

        Assert.assertEquals("Success - initiator is making this call, it stops sharing all shared contacts",
                0, sentRequestList.getTotalElements());

    }

    /**
     * Method   -   editSharedContactActivity
     * TestCase -   Failure
     */
    @Test
    public void testEditingSharedContactsFailureCase() {
        Exception exception = null;
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        try {
            userActivityService.editSharedContactActivity(MethodStubs.getEditSharedContactDTO(
                    Arrays.asList(new ObjectId().toString()), null));
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Failure - Trying to perform action on invalid activity ",
                exception instanceof BadRequestException);

    }

    /**
     * Method   -   GetActionableActivitiesCreatedForUser
     * TestCase -   Success
     * Description - From set of all activities this method will return only
     * actionable type of activities.
     */
    @Test
    public void testGetActionableActivitiesCreatedForUserWithActionableActivity(){
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        //setting request type which is actionable
        userActivityRepository.save(MethodStubs.getUserActivityBasedOnRequestType(user1.getUserId(),user2.getUserId(),
                RequestType.MORE_INFO_REQUEST,ActivityStatus.PENDING,Action.INITIATED));
        ActivityListResponse activityListResponse=userActivityService.getActionableActivitiesCreatedForUser(0,10);
        Assert.assertEquals("Success - User will get only actionable activity list",1,activityListResponse.getUserActivityList().size());
    }

    /**
     * Method   -   GetActionableActivitiesCreatedForUser
     * TestCase -   Success
     * Description - As CONNECTION_REQUEST_ACCEPTED is non actionable activity.So we are getting 0 activities.
     */
    @Test
    public void testGetActionableActivitiesCreatedForUserWithNonActionableActivity(){
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        //setting request type which is non actionable
        userActivityRepository.save(MethodStubs.getUserActivityBasedOnRequestType(user1.getUserId(),user2.getUserId(),
                RequestType.CONNECTION_REQUEST_ACCEPTED,ActivityStatus.ACTIVE,Action.ACCEPTED));
        ActivityListResponse activityListResponse=userActivityService.getActionableActivitiesCreatedForUser(0,10);
        Assert.assertEquals("Success - user will not get any non actionable activity list",0,activityListResponse.getUserActivityList().size());
    }
    private void mockSQSServices() {
        doNothing().when(queueServiceMock).sendPayloadToSQS(isA(SQSPayload.class));
        doNothing().when(queueServiceMock).sendPayloadToSQS(anyList());
    }

    @After
    public void tearDown() {
        registeredNumberRepository.deleteAll();
        peopleUserRepository.deleteAll();
        userActivityRepository.deleteAll();
    }

}
