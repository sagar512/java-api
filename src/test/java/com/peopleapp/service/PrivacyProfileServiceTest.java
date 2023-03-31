package com.peopleapp.service;

import com.peopleapp.dto.ActivityType;
import com.peopleapp.dto.PrivacyProfileData;
import com.peopleapp.dto.SQSPayload;
import com.peopleapp.dto.UserProfileData;
import com.peopleapp.dto.requestresponsedto.*;
import com.peopleapp.enums.Action;
import com.peopleapp.enums.RequestType;
import com.peopleapp.exception.BadRequestException;
import com.peopleapp.model.*;
import com.peopleapp.repository.*;
import com.peopleapp.security.TokenAuthService;
import com.peopleapp.util.PeopleUtils;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PrivacyProfileServiceTest extends BaseTest {

    @Inject
    private UserPrivacyProfileRepository userPrivacyProfileRepository;

    @Inject
    private PrivacyProfileService privacyProfileService;

    @Inject
    private UserConnectionRepository userConnectionRepository;

    @Inject
    private PeopleUserRepository peopleUserRepository;

    @Inject
    private SystemPrivacyProfileRepository systemPrivacyProfileRepository;

    @MockBean
    private UserSessionRepository userSessionRepositoryMock;

    @MockBean
    private TokenAuthService tokenAuthService;

    private PeopleUser user1;
    private PeopleUser user2;

    private UserPrivacyProfile user1Profile;
    private UserPrivacyProfile user1ProfileNotDefault;
    private UserPrivacyProfile user2Profile;
    private UserConnection userConnection12;
    private UserConnection userConnection21;

    @Before
    public void setUp() {
        user1 = peopleUserRepository.save(MethodStubs.getUserObject("9888888888", "testuser1"));
        user2 = peopleUserRepository.save(MethodStubs.getUserObject("9777777777", "testuser2"));

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

        UserProfileData profileData = MethodStubs.getUserProfileDataForSocialProfile();
        existingMetadata.add(profileData);
        user1.setUserMetadataList(existingMetadata);
        user2.setUserMetadataList(existingMetadata);

        user1Profile = userPrivacyProfileRepository.save(MethodStubs.getUserPrivacyProfileObj(user1.getUserId(), valueIds));
        user2Profile = userPrivacyProfileRepository.save(MethodStubs.getUserPrivacyProfileObj(user2.getUserId(),
                valueIds));

        //used in getListOfUserPrivacyProfilesWithNoAnyPrivacyProfile method
        systemPrivacyProfileRepository.save(MethodStubs.getSystemPrivacyProfile());

        user1ProfileNotDefault = MethodStubs.getUserProfileObj(user1.getUserId());
        user1ProfileNotDefault.setValueIdList(Collections.singletonList(profileData.getValueId()));
        user1ProfileNotDefault = userPrivacyProfileRepository.save(user1ProfileNotDefault);

        userConnection12 = userConnectionRepository.save(MethodStubs.getConnectionWithProfileObj(user1.getUserId(), user2.getUserId(),
                user2Profile.getPrivacyProfileId()));
        userConnection21 = userConnectionRepository.save(MethodStubs.getConnectionWithProfileObj(user2.getUserId(), user1.getUserId(),
                user1ProfileNotDefault.getPrivacyProfileId()));

    }

    /**
     * Method - createCustomProfile
     * TestCase - Success
     */
    @Test
    public void testCreateCustomProfile() {

        given(this.tokenAuthService.getSessionUser()).willReturn(user2);
        List<UserPrivacyProfile> userPrivacyProfile;
        CreateCustomProfileRequestDTO createCustomProfileRequestDTO = MethodStubs.getCustomProfilerequestObj();
        PrivacyProfileData privacyProfileData = new PrivacyProfileData();
        privacyProfileData.setProfileName("newProfile");
        createCustomProfileRequestDTO.setPrivacyProfileData(privacyProfileData);

        userPrivacyProfile = userPrivacyProfileRepository.findAllByUserId(user2.getUserId());

        Assert.assertEquals("Success - User has one privacy profile", 1, userPrivacyProfile.size());

        privacyProfileService.createCustomProfile(createCustomProfileRequestDTO);
        userPrivacyProfile = userPrivacyProfileRepository.findAllByUserId(user2.getUserId());

        Assert.assertEquals("Success - user has created custom profile", 2, userPrivacyProfile.size());
    }

    /**
     * Method - createCustomProfile
     * TestCase - Failure
     * All privacy profiles must have unique names
     */
    @Test
    public void testCreatingCustomProfileWithExistingName() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user2);

        List<UserPrivacyProfile> userPrivacyProfile = userPrivacyProfileRepository.findAllByUserId(user2.getUserId());
        Assert.assertEquals("User has one privacy profile created", 1, userPrivacyProfile.size());

        // profile creation request
        CreateCustomProfileRequestDTO createCustomProfileRequestDTO = MethodStubs.getCustomProfilerequestObj();
        PrivacyProfileData privacyProfileData = new PrivacyProfileData();
        privacyProfileData.setProfileName(userPrivacyProfile.get(0).getProfileName());
        createCustomProfileRequestDTO.setPrivacyProfileData(privacyProfileData);

        Exception exception = null;
        try {
            privacyProfileService.createCustomProfile(createCustomProfileRequestDTO);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Failure - creation of custom profile interrupted ", exception instanceof BadRequestException);

    }

    /**
     * Method - createCustomProfile
     * TestCase - Success
     */
    @Test
    public void testCreateCustomProfileWithMetaDataList() {

        given(this.tokenAuthService.getSessionUser()).willReturn(user2);
        List<UserPrivacyProfile> userPrivacyProfile;
        CreateCustomProfileRequestDTO createCustomProfileRequestDTO = MethodStubs.getCustomProfilerequestObj();
        List<UserProfileData> userProfileDataList = new ArrayList<>();
        userProfileDataList.add(MethodStubs.getUserProfileDataForGivenEmail("user2@mailinator.com"));
        createCustomProfileRequestDTO.setUserMetadataList(userProfileDataList);
        PrivacyProfileData privacyProfileData = new PrivacyProfileData();
        privacyProfileData.setProfileName("newProfile");
        privacyProfileData.setImageURL("image Url");
        createCustomProfileRequestDTO.setPrivacyProfileData(privacyProfileData);

        userPrivacyProfile = userPrivacyProfileRepository.findAllByUserId(user2.getUserId());

        Assert.assertEquals("Success - User has one privacy profile", 1, userPrivacyProfile.size());

        privacyProfileService.createCustomProfile(createCustomProfileRequestDTO);
        userPrivacyProfile = userPrivacyProfileRepository.findAllByUserId(user2.getUserId());

        Assert.assertEquals("Success - user has created custom profile", 2, userPrivacyProfile.size());
    }

    /**
     * Method - deleteUserPrivacyProfile
     * TestCase - Success
     */
    @Test
    public void testDeletePrivacyProfileValid() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        UserPrivacyProfile profile = user1ProfileNotDefault;

        List<String> profileIdList = new ArrayList<>();
        profileIdList.add(profile.getPrivacyProfileId());

        privacyProfileService.deleteUserPrivacyProfile(MethodStubs.getPrivacyProfileDeleteRequest(profileIdList));

        profile = userPrivacyProfileRepository.findByProfileIdAndUserId(profile.getPrivacyProfileId(),
                user1.getUserId());

        Assert.assertNull("Success - privacy profile deleted successfully", profile);
    }

    /**
     * Method - setDefaultProfile
     * TestCase - success
     */
    @Test
    public void testSetDefaultPrivacyProfile() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        UserPrivacyProfile profile = userPrivacyProfileRepository.save(MethodStubs.getUserProfileObj(user1.getUserId()));

        Assert.assertEquals("Success: Privacy progfile is not default", false, profile.getIsDefault());

        SetDefaultProfileRequestDTO setDefaultProfileRequestDTO = new SetDefaultProfileRequestDTO();
        setDefaultProfileRequestDTO.setProfileId(profile.getPrivacyProfileId());
        privacyProfileService.setDefaultProfile(setDefaultProfileRequestDTO);
        profile = userPrivacyProfileRepository.findByProfileIdAndUserId(profile.getPrivacyProfileId(), user1.getUserId());

        Assert.assertEquals("Success: Privacy progfile is marked as default", true, profile.getIsDefault());
    }

    /**
     * Method  - editPrivacyProfile
     * TestCase - failure
     */
    @Test
    public void testEditPrivacyProfileIdNotFound() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        Exception exception = null;

        try {
            EditPrivacyProfileRequestDTO editPrivacyProfileRequestDTO = new EditPrivacyProfileRequestDTO();
            PrivacyProfileData privacyProfileData = new PrivacyProfileData();
            privacyProfileData.setProfileName("public");
            privacyProfileData.setPrivacyProfileId(new ObjectId().toString());
            editPrivacyProfileRequestDTO.setPrivacyProfileData(privacyProfileData);
            privacyProfileService.editPrivacyProfile(editPrivacyProfileRequestDTO);
        } catch (Exception e) {
            exception = e;

        }

        Assert.assertTrue("Failure - expected exception", exception instanceof BadRequestException);
    }

    /**
     * Method - editPrivacyProfile
     * TestCase - Success
     */
    @Test
    public void testEditPrivacyProfile() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);

        List<UserPrivacyProfile> profiles = userPrivacyProfileRepository.findAllByUserId(user1.getUserId());

        privacyProfileService.editPrivacyProfile(MethodStubs.getPrivacyProfileEditRequest(
                "Edited profile name", profiles.get(0).getPrivacyProfileId()));

        UserPrivacyProfile userPrivacyProfile = userPrivacyProfileRepository.findByProfileIdAndUserId(
                profiles.get(0).getPrivacyProfileId(), user1.getUserId());
        Assert.assertEquals("Success - privacyProfileOfUser1 name is changed ",
                "Edited profile name", userPrivacyProfile.getProfileName());
    }

    /**
     * Method - editPrivacyProfile
     * TestCase - failure
     * Privacy profile names must be unique
     */
    @Test
    public void testEditingNameOfPrivacyProfileWithExistingName() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);

        List<UserPrivacyProfile> profiles = userPrivacyProfileRepository.findAllByUserId(user1.getUserId());

        CreateCustomProfileRequestDTO createCustomProfileRequestDTO = MethodStubs.getCustomProfilerequestObj();
        PrivacyProfileData privacyProfileData = new PrivacyProfileData();
        privacyProfileData.setProfileName("newProfile");
        createCustomProfileRequestDTO.setPrivacyProfileData(privacyProfileData);
        privacyProfileService.createCustomProfile(createCustomProfileRequestDTO);

        Exception exception = null;
        try {
            privacyProfileService.editPrivacyProfile(MethodStubs.getPrivacyProfileEditRequest(
                    "newProfile", profiles.get(0).getPrivacyProfileId()));
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Failure - editing of custom profile interrupted", exception instanceof BadRequestException);
    }

    /**
     * Method - getListOfUserPrivacyProfiles
     * TestCase - success
     */
    @Test
    public void testFetchingAllUserPrivacyProfiles() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user2);
        GetListOfPrivacyProfilesResponseDTO userPrivacyProfiles = privacyProfileService.getListOfUserPrivacyProfiles();

        Assert.assertEquals("Success - user has only one privacy profile",
                1, userPrivacyProfiles.getUserPrivacyProfileList().size());
    }

    /**
     * Method - getListOfUserPrivacyProfiles
     * TestCase - success
     * user privacy profile added
     */
    @Test
    public void testFetchingAllUserPrivacyProfilesWithoutExistingPrivacyProfile() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user2);
        userPrivacyProfileRepository.delete(user2Profile);
        GetListOfPrivacyProfilesResponseDTO userPrivacyProfiles = privacyProfileService.getListOfUserPrivacyProfiles();

        Assert.assertEquals("Success - System privacy profiles added for the user",
                1, userPrivacyProfiles.getUserPrivacyProfileList().size());
    }

    /**
     * Method - GetPrivacyProfileById
     * TestCase - success
     * Response back with user privacy profile details
     */
    @Test
    public void testGetPrivacyProfileByIdWhenIdIsAvailable() {
        UserPrivacyProfile userPrivacyProfile = privacyProfileService.getPrivacyProfileById(user1Profile.getPrivacyProfileId());
        Assert.assertEquals("Success - privacy profile details found", userPrivacyProfile.getPrivacyProfileId(),
                user1Profile.getPrivacyProfileId());
    }

    /**
     * Method - GetPrivacyProfileById
     * TestCase - Failure
     * Response back with null as user privacy profile not found for given profile id
     */
    @Test
    public void testGetPrivacyProfileByIdWhenIdIsNotAvailable() {
        //setting up with any random object id
        UserPrivacyProfile userPrivacyProfile = privacyProfileService.getPrivacyProfileById(new ObjectId().toString());
        Assert.assertNull("Failure - Not found any privacy profile details",
                userPrivacyProfile);
    }
    /**
     * Method - PrepareSQSPayloadForUpdateContactActivity
     * TestCase - Success
     * SQSPayload get created
     */
    @Test
    public void testPrepareSQSPayloadForUpdateContactActivity() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        UserSession user1Session = MethodStubs.getUserSession(user1.getUserId());
        user1Session.setDeviceTypeId(1);
        user1Session.setEndPointARN("\"arn:aws:sns:us-west-2:\" + RandomStringUtils.randomNumeric(12) + \":app/APNS_SANDBOX/backend-test\"");
        given(this.userSessionRepositoryMock.findActiveSession(ArgumentMatchers.any(ObjectId.class))).willReturn(user1Session);
        UserActivity userActivity = MethodStubs.getUserActivity(user1.getUserId(), user2.getUserId(), user1Profile.getPrivacyProfileId());
        userActivity.setActivityId(new ObjectId());
        ActivityType activityType = new ActivityType();
        activityType.setRequestType(RequestType.NETWORK_JOIN_REQUEST);
        activityType.setActionTaken(Action.INITIATED);
        userActivity.setActivityType(activityType);
        SQSPayload sqsPayload = privacyProfileService.prepareSQSPayloadForUpdateContactActivity(userActivity,
                user1, userConnection12.getConnectionId());
        Assert.assertEquals("Success - SQSPayload get created",sqsPayload.getRecipient(),user1Session.getEndPointARN());

    }

    /**
     * Method - shareTag
     * TestCase - success
     */
    @Test
    public void testShareTag() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        ShareTagRequest shareTagRequest = new ShareTagRequest();
        shareTagRequest.setPrivacyProfileId(user1Profile.getPrivacyProfileId());
        shareTagRequest.setIsTagShared(user1Profile.getIsTagShared());
        privacyProfileService.shareTag(shareTagRequest);
        //doing again db call to get updated value
        user1Profile = userPrivacyProfileRepository.findByProfileIdAndUserId(user1Profile.getPrivacyProfileId(),
                user1.getUserId());
        Assert.assertTrue("Success - Tag get shared", user1Profile.getIsTagShared());
    }

    /**
     * Method - shareTag
     * TestCase - Failure
     * user1 cannot set the share tag option for user2profile,so it will response back with BadRequestException
     */
    @Test(expected = BadRequestException.class)
    public void testShareTagWithDifferentUserProfile() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        ShareTagRequest shareTagRequest = new ShareTagRequest();
        //setting shareTagRequest with user2profile details
        shareTagRequest.setPrivacyProfileId(user2Profile.getPrivacyProfileId());
        shareTagRequest.setIsTagShared(user2Profile.getIsTagShared());
        privacyProfileService.shareTag(shareTagRequest);
    }

    @After
    public void tearDown() {
        userPrivacyProfileRepository.deleteAll();
        systemPrivacyProfileRepository.deleteAll();
    }


}
