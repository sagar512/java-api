package com.peopleapp.service;

import com.peopleapp.configuration.LocaleMessageReader;
import com.peopleapp.constant.MessageConstant;
import com.peopleapp.constant.PeopleConstants;
import com.peopleapp.deviceregistration.RegisterDeviceWithSNS;
import com.peopleapp.dto.*;
import com.peopleapp.dto.requestresponsedto.*;
import com.peopleapp.enums.*;
import com.peopleapp.exception.BadRequestException;
import com.peopleapp.model.*;
import com.peopleapp.repository.*;
import com.peopleapp.security.TokenAuthService;
import com.peopleapp.util.PeopleUtils;
import com.peopleapp.util.TokenGenerator;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PeopleUserServiceTest extends BaseTest {

    @Autowired
    private PeopleUserService peopleUserService;

    @Autowired
    private PeopleUserRepository peopleUserRepository;

    @Autowired
    private VerifyEmailService verifyEmailService;

    @MockBean
    private EmailService emailService;

    @Autowired
    private TokenAuthService tokenAuthService;

    @Inject
    private UserConnectionRepository userConnectionRepository;

    @Inject
    private UserPrivacyProfileRepository userPrivacyProfileRepository;

    @Inject
    private PrivacyProfileService privacyProfileService;

    @Inject
    private UserConnectionService userConnectionService;

    @MockBean
    private TokenAuthService tokenAuthServiceMock;

    @MockBean
    private QueueService queueServiceMock;

    @MockBean
    private RegisterDeviceWithSNS registerDeviceWithSNS;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private TemporarySessionRepository temporarySessionRepository;

    @Autowired
    private ReportedUserDataRepository reportedUserDataRepository;

    @Inject
    private LocaleMessageReader messages;

    @MockBean
    private OTPService otpService;

    private PeopleUser user1;
    private PeopleUser user2;
    private PeopleUser user3;
    private UserPrivacyProfile userProfile1;
    private UserPrivacyProfile userProfile2;
    private UserPrivacyProfile userProfile3;

    private UserConnection userConnection12;
    private UserConnection userConnection21;
    private UserConnection userConnection23;
    private UserConnection userConnection32;
    private VerifyEmail verifyEmail;
    private TemporarySession temporarySession;


    @Before
    public void setUp() {

        user1 = peopleUserRepository.save(MethodStubs.getUserObject("5111111111", "testuser1"));
        user2 = peopleUserRepository.save(MethodStubs.getUserObject("5777777777", "testuser2"));
        user3 = peopleUserRepository.save(MethodStubs.getUserObject("5555555553", "testuser3"));


        // adding valueId in User Obj and User Privacy Profile
        List<String> valueIds = new ArrayList<>();
        List<UserProfileData> existingMetadata = new ArrayList<>();
        List<UserProfileData> metadataList = Arrays.asList(MethodStubs.getUserProfileDataForContactNumber(),
                MethodStubs.getUserProfileDataForSocialProfile(), MethodStubs.getUserProfileDataForGivenEmail("user@mailinator.com"));
        for (UserProfileData newData : PeopleUtils.emptyIfNull(metadataList)) {
            String valueId = new ObjectId().toString();
            newData.setValueId(valueId);
            newData.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
            existingMetadata.add(newData);
            valueIds.add(valueId);
        }
        user1.setUserMetadataList(existingMetadata);
        user2.setUserMetadataList(existingMetadata);

        userProfile1 = userPrivacyProfileRepository.save(MethodStubs.getUserPrivacyProfileObj(user1.getUserId(), valueIds));
        userProfile2 = userPrivacyProfileRepository.save(MethodStubs.getUserPrivacyProfileObj(user2.getUserId(), valueIds));
        userProfile3 = userPrivacyProfileRepository.save(MethodStubs.getUserPrivacyProfileObj(user3.getUserId(),
                valueIds));

        userConnection12 = userConnectionRepository.save(MethodStubs.getConnectionWithProfileObj(user1.getUserId(),
                user2.getUserId(), userProfile2.getPrivacyProfileId()));
        userConnection21 = userConnectionRepository.save(MethodStubs.getConnectionWithProfileObj(user2.getUserId(),
                user1.getUserId(), userProfile1.getPrivacyProfileId()));

        userConnection23 = userConnectionRepository.save(MethodStubs.getConnectionWithProfileObj(user2.getUserId(),
                user3.getUserId(), userProfile3.getPrivacyProfileId()));
        userConnection32 = userConnectionRepository.save(MethodStubs.getConnectionWithProfileObj(user3.getUserId(),
                user2.getUserId(), userProfile2.getPrivacyProfileId()));

        verifyEmail = verifyEmailService.generateAndPersistLinkForPrimaryEmail(user1.getUserId(),
                "user1watu@mailinator.com");

        temporarySession = temporarySessionRepository.save(MethodStubs.getTemporarySessionObj("1111",
                new ContactNumberDTO("+1", "1231231231")));

        userSessionRepository.save(MethodStubs.getUserSession(user1.getUserId()));
        doNothing().when(queueServiceMock).sendPayloadToSQS(isA(SQSPayload.class));
        doNothing().when(queueServiceMock).sendPayloadToSQS(anyList());
        doNothing().when(this.emailService).sendEmail(anyString(), anyString(), anyString());
        doNothing().when(this.emailService).sendTemplatedEmail(anyString(), anyString(), anyString());
        when(registerDeviceWithSNS.registerDevice(anyString(), anyInt()))
                .thenReturn("arn:aws:sns:us-west-2:" + RandomStringUtils.randomNumeric(12) + ":app/APNS_SANDBOX/backend-test");
        doNothing().when(otpService).sendOTP(anyString(), any());

    }

    /**
     * Method - linkPrimaryEmail
     * TestCase - Success
     * linking primary email
     */
    @Test
    public void testLinkingPrimaryEmail() {
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        String email = "unitTest@mailinator.com";
        peopleUserService.linkPrimaryEmail(email);
        Assert.assertTrue("Success - Verification email sent to email", user1.getEmailAddressMap().
                get(email.hashCode()).equals(UserInformationVerification.PENDING));

    }

    /**
     * Method - linkPrimaryEmail
     * TestCase - Failure
     * linking primary email which is already used by some other user
     */
    @Test
    public void testLinkingPrimaryEmailAlreadyInUse() {
        Exception exception = null;
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user2);
        String email = "unitTest@mailinator.com";
        user1.setPrimaryEmail(email);
        Map<Integer, UserInformationVerification> emailMap = user1.getEmailAddressMap();
        emailMap.put(email.hashCode(), UserInformationVerification.PENDING);
        user1.setEmailAddressMap(emailMap);
        peopleUserRepository.save(user1);
        try {
            peopleUserService.linkPrimaryEmail(email);
        } catch (Exception e) {
            exception = e;
        }
        Assert.assertTrue("Failure - BadRequestException expected, once a email is linked by user " +
                "it cannot be reused by another", exception instanceof BadRequestException);
    }

    /**
     * Method - blockUser
     * TestCase - Success
     * Successfully blocking a contact
     */
    @Test
    public void testBlockingAContact() {
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user3);
        BlockUserRequest blockUserRequest = MethodStubs.getBlockUserRequest(user2.getUserId(), true);
        peopleUserService.blockUser(blockUserRequest);
        boolean isUserBlocked = user3.getBlockedUserIdList().contains(user2.getUserId());
        Assert.assertTrue("Success - User Blocked Successfully", isUserBlocked);
    }

    /**
     * Method - blockUser
     * TestCase - Success
     * Successfully unblocking a blocked contact
     */
    @Test
    public void testUnBlockingAContact() {
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user3);
        BlockUserRequest blockUserRequest = MethodStubs.getBlockUserRequest(user2.getUserId(), true);
        peopleUserService.blockUser(blockUserRequest);
        boolean isUserBlocked = user3.getBlockedUserIdList().contains(user2.getUserId());
        Assert.assertTrue("Success - User Blocked Successfully", isUserBlocked);
        blockUserRequest.setIsBlocked(false);
        peopleUserService.blockUser(blockUserRequest);
        boolean isUserUnBlocked = user3.getBlockedUserIdList().contains(user2.getUserId());
        Assert.assertFalse("Success - User UnBlocked Successfully", isUserUnBlocked);
    }

    /**
     * Method - blockUser
     * TestCase - Failure
     * Invalid Operation error is thrown if loggedin userId and userid to be blocked are same
     */
    @Test
    public void testBlockingLoggedInUser() {
        Exception exception = null;
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        BlockUserRequest blockUserRequest = MethodStubs.getBlockUserRequest(user1.getUserId(), true);
        try {
            peopleUserService.blockUser(blockUserRequest);
        } catch (Exception e) {
            exception = e;
        }
        Assert.assertTrue("Invalid operation", exception instanceof BadRequestException);
    }

    /**
     * Method - reportUser
     * Test case - Success
     * "reportMessage" key in request body is empty for user/api/report - Should not throw any exception
     */
    @Test
    public void testReportUserMessageIsEmpty() {
        Exception exception = null;
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        ReportUserRequest userReport = MethodStubs.getReportUserRequest(user2.getUserId(), "");
        try {
            peopleUserService.reportUser(userReport);
        } catch (Exception e) {
            exception = e;
        }
        Assert.assertNull("Success - No Exception is thrown", exception);
    }

    /**
     * Method - reportUser
     * Test case - Success
     * "reportMessage" key in request body is null for user/api/report - Should not throw any exception
     */
    @Test
    public void testReportUserMessageIsNull() {
        Exception exception = null;
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        ReportUserRequest userReport = MethodStubs.getReportUserRequest(user2.getUserId(), null);
        try {
            peopleUserService.reportUser(userReport);
        } catch (Exception e) {
            exception = e;
        }
        Assert.assertNull("Success - No Exception is thrown", exception);
    }

    /**
     * Method - reportUser
     * Test case - Failure
     * Loggedin UserId and UserId of user to be reported must not be same, if same throws BadRequestException
     */
    @Test
    public void testReportUserLoggedInUserIdSameAsReportUserId() {
        Exception exception = null;
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        ReportUserRequest userReport = MethodStubs.getReportUserRequest(user1.getUserId(), null);
        try {
            peopleUserService.reportUser(userReport);
        } catch (Exception e) {
            exception = e;
        }
        Assert.assertTrue("Failure - Invalid operation", exception instanceof BadRequestException);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindUserByContactNumberWhenNumberIsNull() {

        // WHEN
        peopleUserService.findUserByContactNumber(null);
    }

    @Test
    public void testFindUserByContactNumberWhenValidNumber() {

        // GIVEN
        PeopleUser expectedUser = user1;
        ContactNumberDTO expectedNumber = expectedUser.getVerifiedContactNumber();

        // WHEN
        PeopleUser actualUser = peopleUserService.findUserByContactNumber(expectedNumber);

        // THEN
        Assert.assertNotNull("expected user not null", actualUser);
        Assert.assertEquals("expected number", expectedUser.getVerifiedContactNumber(), actualUser.getVerifiedContactNumber());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindUserByUserIdWhenUserIdIsNull() {

        // WHEN
        peopleUserService.findUserByUserId(null);
    }

    @Test
    public void testFindUserByUserIdWhenUserIsValid() {

        // GIVEN
        PeopleUser expectedUser = user1;
        String expectedUserId = expectedUser.getUserId();

        // WHEN
        PeopleUser actualUser = peopleUserService.findUserByUserId(expectedUserId);

        // THEN
        Assert.assertNotNull("expected user not null", actualUser);
        Assert.assertEquals("expected user id", expectedUser.getUserId(), actualUser.getUserId());
    }

    @Test
    public void connectWithNumber() {

        // GIVEN
        ConnectRequestDTO request = MethodStubs.getConnectRequestObj();

        // WHEN
        ConnectResponseDTO response = peopleUserService.connect(request);

        // THEN
        TemporarySession tempSession = tokenAuthService.getTempSessionByTempToken(response.getTempToken());
        Assert.assertNotNull("expected response not null", response);
        Assert.assertNotNull("expected tempToken not null", response.getTempToken());
        Assert.assertEquals("expected phone number verification false", Boolean.FALSE, response.getIsPhoneNumberVerified());
    }

    /**
     * Success Case
     * For valueId belonging to "Social_Profile" Category, status should be updated
     */
    @Test
    public void testSocialHandleVerificationStatus() {
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        String valueId = new ObjectId().toString();
        addSocialProfileToUser(user1, valueId, UserInfoCategory.SOCIAL_PROFILE.getValue());

        UserProfileData socialProfile = MethodStubs.getUserProfileDataForSocialProfile();
        // creating VerificationStatusUpdateRequest Object
        VerificationStatusUpdateRequest statusUpdateRequest = MethodStubs
                .getVerificationStatusUpdateRequest(valueId, UserInformationVerification.VERIFIED.toString(),
                        socialProfile.getKeyValueDataList());

        peopleUserService.updateSocialHandleVerificationStatus(statusUpdateRequest);

        PeopleUser user = peopleUserRepository.findByuserId(user1.getUserId(), UserStatus.ACTIVE.getValue());
        Assert.assertNotNull("Success - Social Map is present", user.getSocialHandleMap());
    }

    /**
     * Failure Case
     * For valueId not belonging to "Social_Profile" Category, then INVALID_VALUE_ID error
     */
    @Test
    public void testSocialHandleVerificationStatusForInvalidValueId() {
        Exception exception = null;
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        String valueId = new Object().toString();
        addSocialProfileToUser(user1, valueId, UserInfoCategory.EMAIL_ADDRESS.getValue());
        UserProfileData socialProfile = MethodStubs.getUserProfileDataForSocialProfile();

        // creating VerificationStatusUpdateRequest Object
        VerificationStatusUpdateRequest statusUpdateRequest = MethodStubs
                .getVerificationStatusUpdateRequest(valueId, UserInformationVerification.VERIFIED.toString(),
                        socialProfile.getKeyValueDataList());

        try {
            peopleUserService.updateSocialHandleVerificationStatus(statusUpdateRequest);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertNotNull("Failure - Expected Exception", exception);
        Assert.assertTrue("Failure - expected BadRequestException", exception instanceof BadRequestException);

    }

    /**
     * Method - getBlockedUserList
     * Test - Success
     * Response back with list of blocked user
     */
    @Test
    public void testGetBlockedUserList() {
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        //CASE 1 : user2 is connected with user1 but it is blocked by user1
        //CASE 2 : user3 is public user and blocked by user1
        user1.setBlockedUserIdList(new HashSet<>(Arrays.asList(user2.getUserId(), user3.getUserId())));
        FetchConnectionListResponseDTO responseDTO = peopleUserService.getBlockedUserList("",
                1, 1, false, 0, 2);
        Assert.assertEquals("Success - Two user are present in the block list", 2,
                responseDTO.getContactList().size());
    }

    /**
     * Method - getBlockedUserList
     * Test - Success
     * Response back with list of blocked user and URL for the next page
     */
    @Test
    public void testGetBlockedUserListWhenUserIsNotConnected() {
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);

        //CASE 3 : user2 is not connected with user1 but it is blocked by user1
        userConnection12.setConnectionStatus(ConnectionStatus.NOT_CONNECTED);

        //setting up static contact data
        userConnection12.setContactStaticData(MethodStubs.getUserInformationObject());

        userConnectionRepository.save(userConnection12);
        user1.setBlockedUserIdList(new HashSet<>(Arrays.asList(user2.getUserId(), user3.getUserId())));

        //setting up page size to 1 so that,there will be only one blocked user per page
        FetchConnectionListResponseDTO responseDTO = peopleUserService.getBlockedUserList("",
                1, 1, false, 0, 1);

        Assert.assertEquals("Success - Only one user will be present on first page of blocked list", 1,
                responseDTO.getContactList().size());
        Assert.assertNotNull("Success - Other blocked user profile will be present on next page",
                responseDTO.getNextURL());
    }

    /**
     * Method - getUserSettings
     * Test - Success Case
     */
    @Test
    public void testGetUserSettings() {
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);

        UserSettingsResponseDTO responseDTO = peopleUserService.getUserSettings();
        Assert.assertNotNull("Success - Response Object is present", responseDTO);
        Assert.assertEquals("Success - notification setting is correct", user1.getIsPushNotificationEnabled(),
                responseDTO.getIsPushNotificationEnabled());
    }

    /**
     * Method - updateUser
     * Test case - Success
     * Verifying the basic profile details and metaList
     */
    @Test
    public void testUpdateUser() {
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);

        UpdateUserInfoRequestDTO requestDTO = MethodStubs.getUpdateUserRequestObj(user1.getUserMetadataList());
        peopleUserService.updateUser(requestDTO);

        PeopleUser updatedUser = peopleUserRepository.findByuserId(user1.getUserId(), UserStatus.ACTIVE.getValue());

        Assert.assertEquals("Success - FirstName is updated", requestDTO.getUserDetails().getFirstName(),
                updatedUser.getFirstName().getValue());
        // cannot match metaList data as valueId is added in response.
        Assert.assertEquals("Success - Request and Response Objects are same",
                requestDTO.getUserDetails().getUserMetadataList().size(),
                updatedUser.getUserMetadataList().size());
        // Since we added one social profile with Verified Status
        Assert.assertEquals("Success - SocialHandleMap is updated", 1,
                updatedUser.getSocialHandleMap().size());
    }

    /**
     * Method - updateUser
     * Test case - Success
     * deleted metaData should be added to connectionDeletedData object
     */
    @Test
    public void testUpdateUserDeletedData() {
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user2);

        UpdateUserInfoRequestDTO requestDTO =
                MethodStubs.getUpdateUserRequestObjDeletedObject(user2.getUserMetadataList(),
                        userProfile2.getValueIdList().get(0));
        peopleUserService.updateUser(requestDTO);

        UserConnection userConnection = userConnectionRepository.findConnectionByConnectionId(user3.getUserId(),
                userConnection32.getConnectionId());

        Assert.assertNotNull("Success - Connection Deleted Data is added", userConnection.getConnectionDeletedData());
    }

    // adding social profile to the people user object
    private void addSocialProfileToUser(PeopleUser user, String valueId, String category) {
        List<UserProfileData> userMetadataList = new ArrayList<>();
        UserProfileData socialProfileData = new UserProfileData();
        socialProfileData.setCategory(category);
        socialProfileData.setValueId(valueId);
        socialProfileData.setLabel("PL.02.06");
        KeyValueData keyValueData = new KeyValueData();
        keyValueData.setKey("profileAddress");
        keyValueData.setVal("Insta");
        socialProfileData.setKeyValueDataList(new ArrayList<>(Arrays.asList(keyValueData)));
        userMetadataList.add(socialProfileData);
        user.setUserMetadataList(userMetadataList);
    }

    /**
     * Method - inviteByNumber
     * TestCase - Success
     * When user sends a invite to contact a static contact will be created
     */
    @Test
    public void testSendingInvitationToContact() {
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        GetListOfPrivacyProfilesResponseDTO userPrivacyProfiles = privacyProfileService.getListOfUserPrivacyProfiles();
        Assert.assertEquals("User 1 had only one contact initially", 1, userConnectionService.getConnectionList(null,
                0, 10, false,null).getTotalElements());
        InviteByNumberResponseDTO inviteResponse = peopleUserService.inviteByNumber(MethodStubs.getInviteByNumberRequestDTO(
                userPrivacyProfiles.getUserPrivacyProfileList().get(0).getPrivacyProfileId()));
        Assert.assertNotNull("Success - Contact was created and invitation was sent", inviteResponse.getUserContact());
        Assert.assertEquals("After sending invite user 1 has 2 contacts", 2, userConnectionService.getConnectionList(null,
                0, 10, false, null).getTotalElements());
    }

    /**
     * Method - inviteByNumber
     * TestCase - Failure
     * Invite can only be sent once to a contact
     */
    @Test
    public void testReSendingInvitationToSameContact() {
        Exception exception = null;
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        GetListOfPrivacyProfilesResponseDTO userPrivacyProfiles = privacyProfileService.getListOfUserPrivacyProfiles();
        Assert.assertEquals("User 1 had only one contact initially", 1, userConnectionService.getConnectionList(null,
                0, 10, false, null).getTotalElements());
        InviteByNumberRequest inviteRequest = MethodStubs.getInviteByNumberRequestDTO(
                userPrivacyProfiles.getUserPrivacyProfileList().get(0).getPrivacyProfileId());
        InviteByNumberResponseDTO inviteResponse = peopleUserService.inviteByNumber(inviteRequest);
        Assert.assertNotNull("Success - Contact was created and invitation was sent", inviteResponse.getUserContact());

        try {
            peopleUserService.inviteByNumber(inviteRequest);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Failure - Invite can only be sent once to a contact",
                exception instanceof BadRequestException);
    }

    /**
     * Method - inviteByNumber
     * TestCase - Failure
     * Invite can only be sent to USA or Canada numbers
     */
    @Test
    public void testSendingInvitationToNonUSAContact() {
        Exception exception = null;
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        GetListOfPrivacyProfilesResponseDTO userPrivacyProfiles = privacyProfileService.getListOfUserPrivacyProfiles();
        InviteByNumberRequest inviteRequest = MethodStubs.getInviteByNumberRequestDTO(
                userPrivacyProfiles.getUserPrivacyProfileList().get(0).getPrivacyProfileId());
        List<KeyValueData> contactData = inviteRequest.getInviteeContactInformation().getUserMetadataList().get(0).getKeyValueDataList();

        for (KeyValueData keyValueData : contactData) {
            if (keyValueData.getKey().equalsIgnoreCase("countryCode")) {
                // NON U.S country code
                keyValueData.setVal("+91");
            }
        }

        try {
            peopleUserService.inviteByNumber(inviteRequest);
        } catch (Exception e) {
            exception = e;
        }
        Assert.assertTrue("Failure - Invite can only be sent to USA or Canada numbers",
                exception instanceof BadRequestException);


    }

    /**
     * Method - inviteByNumber
     * TestCase - Failure
     * Invite can not be sent to watu registered user
     */
    @Test
    public void testSendingInvitationToWatuRegisteredContact() {
        Exception exception = null;
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        GetListOfPrivacyProfilesResponseDTO userPrivacyProfiles = privacyProfileService.getListOfUserPrivacyProfiles();
        InviteByNumberRequest inviteRequest = MethodStubs.getInviteByNumberRequestDTO(
                userPrivacyProfiles.getUserPrivacyProfileList().get(0).getPrivacyProfileId());
        List<KeyValueData> contactData = inviteRequest.getInviteeContactInformation().getUserMetadataList().get(0).getKeyValueDataList();

        for (KeyValueData keyValueData : contactData) {
            if (keyValueData.getKey().equalsIgnoreCase("phoneNumber")) {
                keyValueData.setVal(user2.getVerifiedContactNumber().getPhoneNumber());
            }
        }

        try {
            peopleUserService.inviteByNumber(inviteRequest);
        } catch (Exception e) {
            exception = e;
        }
        Assert.assertTrue("Failure - Invite cannot be sent to watu registered contacts",
                exception instanceof BadRequestException);


    }

    /**
     * Method - GetUserIdByContactNumber
     * TestCase - Failure
     * user ID cannot be sent when contact number is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetUserIdByContactNumberWhenNumberIsNull() {
        peopleUserService.getUserIdByContactNumber(null);
    }

    /**
     * Method - GetUserIdByContactNumber
     * TestCase - Success
     * user ID can be sent only if having valid contact number
     */
    @Test
    public void testGetUserIdByContactNumberWhenNumberIsValid() {

        // GIVEN
        PeopleUser expectedUser = user1;
        ContactNumberDTO expectedNumber = expectedUser.getVerifiedContactNumber();

        // WHEN
        String userId = peopleUserService.getUserIdByContactNumber(expectedNumber);

        // THEN
        Assert.assertEquals("Success - Got the userId with valid contact number ", userId, expectedUser.getUserId());

    }

    /**
     * Method - GetUserIdByContactNumber
     * TestCase - Failure
     * user ID cannot be sent when contact number is invalid
     */
    @Test
    public void testGetUserIdByContactNumberWhenNumberIsInValid() {

        // GIVEN
        PeopleUser expectedUser = user1;
        ContactNumberDTO expectedNumber = expectedUser.getVerifiedContactNumber();
        expectedNumber.setPhoneNumber("123454321");

        // WHEN
        String userId = peopleUserService.getUserIdByContactNumber(expectedNumber);

        // THEN
        Assert.assertNull("Failure - Number is not valid", userId);
    }

    /**
     * Method - authenticateMail
     * TestCase - Success
     * verification link will be sent to specified email id
     */
    @Test
    public void testAuthenticateEmail() {
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        for (UserProfileData userProfileData : user1.getUserMetadataList()) {
            if (userProfileData.getCategory().equals(UserInfoCategory.EMAIL_ADDRESS.getValue())) {
                peopleUserService.authenticateEmail(userProfileData.getValueId());
                Assert.assertTrue("Success - Verification link sent to email", user1.getEmailAddressMap().
                        get(userProfileData.getSingleValueData().hashCode()).equals(UserInformationVerification.PENDING));
                break;
            }
        }
    }

    /**
     * Method - authenticateMail
     * TestCase - Success
     * verification link will be sent to primary email id
     */
    @Test
    public void testAuthenticateEmailWhenEmailIsPrimary() {
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        for (UserProfileData userProfileData : user1.getUserMetadataList()) {
            if (userProfileData.getCategory().equals(UserInfoCategory.EMAIL_ADDRESS.getValue())) {
                //setting up email as primary
                userProfileData.setIsPrimary(Boolean.TRUE);
                peopleUserService.authenticateEmail(userProfileData.getValueId());
                Assert.assertTrue("Success - Verification email sent to email", user1.getEmailAddressMap().
                        get(userProfileData.getSingleValueData().hashCode()).equals(UserInformationVerification.PENDING));
                break;
            }
        }
    }

    /**
     * Method - authenticateMail
     * TestCase - Failure
     * will throw BadRequestException as passing invalid value id
     */
    @Test(expected = BadRequestException.class)
    public void testAuthenticateEmailWhenValueIdIsInvalid() {
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        peopleUserService.authenticateEmail(new ObjectId().toString());
    }

    /**
     * Method - VerifyEmail
     * TestCase - Success
     * Verification of email had done
     */
    @Test
    public void testVerifyEmailWithNewEmail() {
        peopleUserService.verifyEmail(verifyEmail.getVerificationLink().split("=")[1]);
        user1 = peopleUserService.findUserByUserId(user1.getUserId());
        Assert.assertTrue("Success - email got verified", user1.getEmailAddressMap().
                get(verifyEmail.getEmail().hashCode()).equals(UserInformationVerification.VERIFIED));
    }

    /**
     * Method - VerifyEmail
     * TestCase - Failure
     * Token is not valid
     */
    @Test(expected = BadRequestException.class)
    public void testVerifyEmailWithInvalidToken() {
        //generating random token
        peopleUserService.verifyEmail(TokenGenerator.generateTempToken());
    }

    /**
     * Method - VerifyEmail
     * TestCase - Failure
     * verified email of other user can't be verified by you
     */
    @Test(expected = BadRequestException.class)
    public void testVerifyEmailWithOtherUserVerifiedEmail() {
        String email = "user1watu@mailinator.com";
        user2.setPrimaryEmail(email);
        Map<Integer, UserInformationVerification> emailMap = user1.getEmailAddressMap();
        //setting email as verified for user2
        emailMap.put(email.hashCode(), UserInformationVerification.VERIFIED);
        user2.setEmailAddressMap(emailMap);
        peopleUserRepository.save(user2);
        peopleUserService.verifyEmail(verifyEmail.getVerificationLink().split("=")[1]);
    }

    /**
     * Method - VerifyEmail
     * TestCase - Failure
     * Already Verified email of yours can't be verified again
     */
    @Test(expected = BadRequestException.class)
    public void testVerifyEmailWithOwnVerifiedEmail() {
        String email = "user1watu@mailinator.com";
        user1.setPrimaryEmail(email);
        Map<Integer, UserInformationVerification> emailMap = user1.getEmailAddressMap();
        //setting email as verified
        emailMap.put(email.hashCode(), UserInformationVerification.VERIFIED);
        user1.setEmailAddressMap(emailMap);
        peopleUserRepository.save(user1);
        peopleUserService.verifyEmail(verifyEmail.getVerificationLink().split("=")[1]);
    }

    /**
     * Method - UpdatePushNotificationSetting
     * TestCase - Success
     * push notification will get enable
     */
    @Test
    public void testUpdatePushNotificationSettingWithTrueEnableStatus() {
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        UpdatePushNotificationSettingRequestDTO updatePushNotificationSettingRequestDTO = new UpdatePushNotificationSettingRequestDTO();
        updatePushNotificationSettingRequestDTO.setDeviceTypeId(1);
        updatePushNotificationSettingRequestDTO.setEnableSetting(Boolean.TRUE);
        updatePushNotificationSettingRequestDTO.setDeviceToken(RandomStringUtils.randomAlphanumeric(10));
        UpdatePushNotificationSettingResponseDTO updatePushNotificationSettingResponseDTO = peopleUserService.updatePushNotificationSetting(updatePushNotificationSettingRequestDTO);
        Assert.assertTrue("success - push notification got enabled", updatePushNotificationSettingResponseDTO.getIsPushNotificationEnabled());
    }

    /**
     * Method - UpdatePushNotificationSetting
     * TestCase - Success
     * push notification will get disabled
     */
    @Test
    public void testUpdatePushNotificationSettingWithFalseEnableStatus() {
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        UpdatePushNotificationSettingRequestDTO updatePushNotificationSettingRequestDTO = new UpdatePushNotificationSettingRequestDTO();
        updatePushNotificationSettingRequestDTO.setDeviceTypeId(1);
        updatePushNotificationSettingRequestDTO.setEnableSetting(Boolean.FALSE);
        updatePushNotificationSettingRequestDTO.setDeviceToken(RandomStringUtils.randomAlphanumeric(10));
        UpdatePushNotificationSettingResponseDTO updatePushNotificationSettingResponseDTO = peopleUserService.updatePushNotificationSetting(updatePushNotificationSettingRequestDTO);
        Assert.assertFalse("success - push notification got disabled", updatePushNotificationSettingResponseDTO.getIsPushNotificationEnabled());
    }

    /**
     * Method - UpdatePushNotificationSetting
     * TestCase - Failure
     * Exception will occur as invalid device id is entered
     */
    @Test(expected = BadRequestException.class)
    public void testUpdatePushNotificationSettingWithInvalidProperty() {
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        UpdatePushNotificationSettingRequestDTO updatePushNotificationSettingRequestDTO = new UpdatePushNotificationSettingRequestDTO();
        //setting up invalid device type id
        updatePushNotificationSettingRequestDTO.setDeviceTypeId(0);
        updatePushNotificationSettingRequestDTO.setEnableSetting(Boolean.TRUE);
        updatePushNotificationSettingRequestDTO.setDeviceToken(RandomStringUtils.randomAlphanumeric(10));
        peopleUserService.updatePushNotificationSetting(updatePushNotificationSettingRequestDTO);
    }

    /**
     * Method - UpdatePrimaryNumber
     * TestCase - Failure
     * Cannot update primary number with existing primary number of same user
     */
    @Test(expected = BadRequestException.class)
    public void testUpdatePrimaryNumberWithVerifiedNumber() {
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        CanadaAndUSAContactNumberDTO newNumber = new CanadaAndUSAContactNumberDTO();
        newNumber.setCountryCode("+1");
        //setting phone number same as existing verified number
        newNumber.setPhoneNumber("5111111111");
        ChangeMobileNumberRequest changeMobileNumberRequest = new ChangeMobileNumberRequest();
        changeMobileNumberRequest.setNewContactNumber(newNumber);
        peopleUserService.updatePrimaryNumber(changeMobileNumberRequest);
    }

    /**
     * Method - UpdatePrimaryNumber
     * TestCase - Success
     * primary number will get updated with new number
     */
    @Test
    public void testUpdatePrimaryNumberWithNewNumber() {
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        CanadaAndUSAContactNumberDTO newNumber = new CanadaAndUSAContactNumberDTO();
        newNumber.setCountryCode("+1");
        newNumber.setPhoneNumber("1234567890");
        ChangeMobileNumberRequest changeMobileNumberRequest = new ChangeMobileNumberRequest();
        changeMobileNumberRequest.setNewContactNumber(newNumber);
        ChangeMobileNumberResponse response = peopleUserService.updatePrimaryNumber(changeMobileNumberRequest);
        Assert.assertEquals("Success - phone number got updated",
                temporarySessionRepository.findByTempTokenAndStatus(response.getTempToken(), TokenStatus.ACTIVE)
                        .getContactNumber().getPhoneNumber(), newNumber.getPhoneNumber());
    }

    /**
     * Method - UpdatePrimaryNumber
     * TestCase - Failure
     * Cannot update primary number with existing primary number of different user
     */
    @Test(expected = BadRequestException.class)
    public void testUpdatePrimaryNumberWithExistingNumber() {
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        CanadaAndUSAContactNumberDTO newNumber = new CanadaAndUSAContactNumberDTO();
        newNumber.setCountryCode("+1");
        //setting up phone number of user2
        newNumber.setPhoneNumber("5777777777");
        ChangeMobileNumberRequest changeMobileNumberRequest = new ChangeMobileNumberRequest();
        changeMobileNumberRequest.setNewContactNumber(newNumber);
        peopleUserService.updatePrimaryNumber(changeMobileNumberRequest);
    }

    /**
     * Method - GetUserDetails
     * TestCase - Success
     * All the details of user will be fetched
     */
    @Test
    public void testGetUserDetails() {
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        UpdateUserInfoResponseDTO updateUserInfoResponseDTO = peopleUserService.getUserDetails();
        Assert.assertEquals("Success - All the details of user will be fetched", user1.getUserId(),
                updateUserInfoResponseDTO.getUserDetails().getUserId());

    }

    /**
     * Method - UpdateUserDeviceLocation
     * TestCase - Success
     * device location got updated
     */
    @Test
    public void testUpdateUserDeviceLocation() {
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        Coordinates coordinates = new Coordinates();
        coordinates.setLatitude(78.99);
        coordinates.setLongitude(90.88);
        peopleUserService.updateUserDeviceLocation(coordinates);

        Assert.assertEquals("Success - Latitude updated.", user1.getDeviceLocation().getLatitude(),
                coordinates.getLatitude(), 0);
        Assert.assertEquals("Success - Longitude updated.", user1.getDeviceLocation().getLongitude(),
                coordinates.getLongitude(), 0);
    }

    /**
     * Method - searchGivenContactNumber
     * TestCase - Failure
     * cannot search your own number
     */
    @Test(expected = BadRequestException.class)
    public void testSearchGivenContactNumberWithOwnNumber() {
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        //setting user1 contact number
        ContactNumberDTO contactNumberDTO = new ContactNumberDTO("+1", "5111111111");
        peopleUserService.searchGivenContactNumber(contactNumberDTO);
    }

    /**
     * Method - searchGivenContactNumber
     * TestCase - Success
     * Response back with contact details of searched user, it will do public search as number is also not available in
     * user static contact list
     */
    @Test
    public void testSearchGivenContactNumberWithRegisteredUserNumber() {
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        //setting up user3 contact number which is not connected to user1
        ContactNumberDTO contactNumberDTO = new ContactNumberDTO("+1", "5555555553");
        SearchByNumberResponseDTO searchByNumberResponseDTO = peopleUserService.searchGivenContactNumber(contactNumberDTO);
        Assert.assertTrue("Success - getting searched contact details",
                searchByNumberResponseDTO.isSearchedContactExist());
    }

    /**
     * Method - searchGivenContactNumber
     * TestCase - Success
     * Response back with contact details have been found,as contact number is registered user and
     * it is also present in user contact list,so it will return contact details from user's contact list itself
     */
    @Test
    public void testSearchGivenContactNumberWithRegisteredUserButNotConnected() {
        //setting static contact
        userConnectionRepository.save(MethodStubs.getUserStaticConnectionWithGivenContactNumber
                (user1.getUserId(), user3.getVerifiedContactNumber()));
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        ContactNumberDTO contactNumberDTO = new ContactNumberDTO("+1", "5555555553");
        SearchByNumberResponseDTO searchByNumberResponseDTO = peopleUserService.searchGivenContactNumber(contactNumberDTO);
        Assert.assertTrue("Success - contact details found",
                searchByNumberResponseDTO.isSearchedContactExist());
    }

    /**
     * Method - searchGivenContactNumber
     * TestCase - Success
     * Response back with contact details of searched user,which is already connected
     */
    @Test
    public void testSearchGivenContactNumberWithRegisteredUserNumberButConnected() {
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        //setting up user2 contact number which is connected to user1
        ContactNumberDTO contactNumberDTO = new ContactNumberDTO("+1", "5777777777");
        SearchByNumberResponseDTO searchByNumberResponseDTO = peopleUserService.searchGivenContactNumber(contactNumberDTO);
        Assert.assertTrue("Success - getting searched contact details",
                searchByNumberResponseDTO.isSearchedContactExist());
    }

    /**
     * Method - searchGivenContactNumber
     * TestCase - Failure
     * Response back with no contact details have been found,as contact number is not a registered user as well as
     * it is not present in user contact list
     */
    @Test
    public void testSearchGivenContactNumberWithNotRegisteredUser() {
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        //setting up with non registered contact number
        ContactNumberDTO contactNumberDTO = new ContactNumberDTO("+1", "5555557891");
        SearchByNumberResponseDTO searchByNumberResponseDTO = peopleUserService.searchGivenContactNumber(contactNumberDTO);
        Assert.assertFalse("Failure - No contact details found",
                searchByNumberResponseDTO.isSearchedContactExist());
    }

    /**
     * Method - searchGivenContactNumber
     * TestCase - Success
     * Response back with contact details have been found,as contact number is not a registered user but
     * it is present in user static contact list
     */
    @Test
    public void testSearchGivenContactNumberWithNotRegisteredUserAndNotConnected() {
        userConnectionRepository.save(MethodStubs.getUserStaticConnectionWithGivenContactNumber
                (user1.getUserId(), MethodStubs.getContactNumberDTO("+1", "1234567890")));
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        //setting up with static contact number of user1
        ContactNumberDTO contactNumberDTO = new ContactNumberDTO("+1", "1234567890");
        SearchByNumberResponseDTO searchByNumberResponseDTO = peopleUserService.searchGivenContactNumber(contactNumberDTO);
        Assert.assertTrue("Success - contact details found",
                searchByNumberResponseDTO.isSearchedContactExist());
    }

    /**
     * Method - VerifyOtp
     * TestCase - Success
     * verification otp got successful for new user and account got created
     */
    @Test
    public void testVerifyOtpWithNewUser() {
        //mocking this function to return String type
        given(this.tokenAuthServiceMock.getTempToken()).willReturn(temporarySession.getTemporaryToken());
        given(this.tokenAuthServiceMock.getTempSessionByTempToken(anyString())).willReturn(temporarySession);
        VerifyOTPRequestDTO verifyOTPRequestDTO = new VerifyOTPRequestDTO();
        verifyOTPRequestDTO.setOtp("1111");
        VerifyOTPResponseDTO verifyOTPResponseDTO = peopleUserService.verifyOTP(verifyOTPRequestDTO, "1");
        Assert.assertFalse("Success - New user got registered", verifyOTPResponseDTO.getIsExistingUser());
        Assert.assertTrue("Success - Phone number got verified", verifyOTPResponseDTO.getIsPhoneNumberVerified());
    }

    /**
     * Method - VerifyOtp
     * TestCase - Success
     * login operation successful
     */
    @Test
    public void testVerifyOtpWithExistingUserWithSameVerifiedNumber() {
        //setting temporary session id with user2
        temporarySession.setUserId(user2.getUserId());
        temporarySession.setContactNumber(new ContactNumberDTO("+1", "5777777777"));
        temporarySession = temporarySessionRepository.save(temporarySession);
        //mocking this function to return String type
        given(this.tokenAuthServiceMock.getTempToken()).willReturn(temporarySession.getTemporaryToken());
        given(this.tokenAuthServiceMock.getTempSessionByTempToken(anyString())).willReturn(temporarySession);
        VerifyOTPRequestDTO verifyOTPRequestDTO = new VerifyOTPRequestDTO();
        verifyOTPRequestDTO.setOtp("1111");
        VerifyOTPResponseDTO verifyOTPResponseDTO = peopleUserService.verifyOTP(verifyOTPRequestDTO, "1");
        Assert.assertTrue("Success - Login Successful ", verifyOTPResponseDTO.getIsExistingUser());
        Assert.assertEquals("Success - UserId got verified", verifyOTPResponseDTO.getUserId(), temporarySession.getUserId());
    }

    /**
     * Method - VerifyOtp
     * TestCase - Success
     * Changing of primary number
     */
    @Test
    public void testVerifyOtpWithExistingUserWithNewNumber() {
        //setting temporary session id with user2
        temporarySession.setUserId(user2.getUserId());
        temporarySession.setContactNumber(new ContactNumberDTO("+1", "1234567891"));
        temporarySession.setOperation(PeopleConstants.CHANGE_MOBILE_NUMBER);
        temporarySession = temporarySessionRepository.save(temporarySession);
        //mocking this function to return String type
        given(this.tokenAuthServiceMock.getTempToken()).willReturn(temporarySession.getTemporaryToken());
        given(this.tokenAuthServiceMock.getTempSessionByTempToken(anyString())).willReturn(temporarySession);
        VerifyOTPRequestDTO verifyOTPRequestDTO = new VerifyOTPRequestDTO();
        verifyOTPRequestDTO.setOtp("1111");
        VerifyOTPResponseDTO verifyOTPResponseDTO = peopleUserService.verifyOTP(verifyOTPRequestDTO, "1");
        Assert.assertTrue("Success - primary number got changed and verified",
                verifyOTPResponseDTO.getIsPhoneNumberVerified());
        Assert.assertTrue("Success - Login Successful ", verifyOTPResponseDTO.getIsExistingUser());

    }

    /**
     * Method - ReportUser
     * TestCase - Success
     * CASE 1 : if user1 reports user2,then reporting + blocking both will be done for user2.
     */
    @Test
    public void testReportUser() {
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        String message = peopleUserService.reportUser(MethodStubs.getReportUserRequest(user2.getUserId(), "report " +
                "message"));
        ReportedData reportedData = reportedUserDataRepository
                .findByReportedByUserIdAndReportedUserId(PeopleUtils.convertStringToObjectId(user1.getUserId()),
                        PeopleUtils.convertStringToObjectId(user2.getUserId()));
        Assert.assertEquals("Success - User2 reported", reportedData.getReportedByUserId(), user1.getUserId());
        Assert.assertTrue("Success - User2 get blocked ", user1.getBlockedUserIdList().contains(user2.getUserId()));
        Assert.assertEquals("Success - User Reported Successfully message returned",
                messages.get(MessageConstant.USER_REPORTED_SUCCESSFULLY), message);

    }

    /**
     * Method - ReportUser
     * TestCase - Success
     * CASE 2 : After following case 1,if user1 unblock user2,then user2 will be unblocked but report will
     * not be reverted back(user2 still be as reported user) and if again user1 reports user2,
     * then User2 will be blocked and report will remain as it is.
     */
    @Test
    public void testReportWithAlreadyReportedUserButNotBlocked() {
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        ReportUserRequest reportUserRequest = MethodStubs.getReportUserRequest(user2.getUserId(), "report message");
        peopleUserService.reportUser(reportUserRequest);
        ReportedData reportedData = reportedUserDataRepository
                .findByReportedByUserIdAndReportedUserId(PeopleUtils.convertStringToObjectId(user1.getUserId()),
                        PeopleUtils.convertStringToObjectId(user2.getUserId()));

        // Unblocking user2
        peopleUserService.blockUser(MethodStubs.getBlockUserRequest(user2.getUserId(), false));

        // After unblocking, user1 again reported User2
        String message = peopleUserService.reportUser(reportUserRequest);
        ReportedData reportedDataNew = reportedUserDataRepository
                .findByReportedByUserIdAndReportedUserId(PeopleUtils.convertStringToObjectId(user1.getUserId()),
                        PeopleUtils.convertStringToObjectId(user2.getUserId()));
        Assert.assertEquals("Success - same reported data ", reportedData.getId().toString(),
                reportedDataNew.getId().toString());
        Assert.assertTrue("Success - user2 get blocked", user1.getBlockedUserIdList().contains(user2.getUserId()));
        Assert.assertEquals("Success - User Reported Successfully",
                messages.get(MessageConstant.USER_REPORTED_SUCCESSFULLY), message);

    }


    /**
     * Method - ReportUser
     * TestCase - Success
     * CASE 3 :  if a user report to already reported + blocked user.
     */
    @Test
    public void testReportUserWithAlreadyReportedAndBlockedUser() {
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        ReportUserRequest reportUserRequest = MethodStubs.getReportUserRequest(user2.getUserId(), "report message");
        String message = peopleUserService.reportUser(reportUserRequest);
        Assert.assertEquals("Success - User Reported Successfully",
                messages.get(MessageConstant.USER_REPORTED_SUCCESSFULLY), message);

        //Again reporting to already reported+blocked user
        String messageNew = peopleUserService.reportUser(reportUserRequest);
        Assert.assertEquals("Success - Already Reported  User", messages.get(MessageConstant.ALREADY_REPORTED_USER),
                messageNew);

    }

    /**
     * Method - ReportUser
     * TestCase - Success
     * CASE 4 : if a user report to already blocked contact.
     */
    @Test
    public void testReportUserWithAlreadyBlockedButNotReportedUser() {
        given(this.tokenAuthServiceMock.getSessionUser()).willReturn(user1);
        peopleUserService.blockUser(MethodStubs.getBlockUserRequest(user2.getUserId(), true));
        String message =
                peopleUserService.reportUser(MethodStubs.getReportUserRequest(user2.getUserId(), "report message"));

        ReportedData reportedData = reportedUserDataRepository
                .findByReportedByUserIdAndReportedUserId(PeopleUtils.convertStringToObjectId(user1.getUserId()),
                        PeopleUtils.convertStringToObjectId(user2.getUserId()));
        Assert.assertEquals("Success - User Reported Successfully",
                messages.get(MessageConstant.USER_REPORTED_SUCCESSFULLY), message);
        Assert.assertEquals("Success - User2 reported", reportedData.getReportedByUserId(), user1.getUserId());
    }

    @After
    public void tearDown() {
        userPrivacyProfileRepository.deleteAll();
        peopleUserRepository.deleteAll();
        userConnectionRepository.deleteAll();
        userSessionRepository.deleteAll();
        temporarySessionRepository.deleteAll();
        reportedUserDataRepository.deleteAll();
    }

}
