package com.peopleapp.service;

import com.peopleapp.dto.*;
import com.peopleapp.dto.requestresponsedto.*;
import com.peopleapp.enums.*;
import com.peopleapp.model.*;
import com.peopleapp.util.PeopleUtils;
import com.peopleapp.util.TokenGenerator;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;

import java.util.*;

public class MethodStubs {


    static PeopleUser getUserObject(String name) {
        PeopleUser newUser;
        newUser = new PeopleUser();
        ContactNumberDTO contactNumberDTO = new ContactNumberDTO();
        ObjectId userId = new ObjectId();
        newUser.setUserId(userId);
        newUser.setFirstName(name);
        newUser.setStatus(UserStatus.ACTIVE);
        return newUser;
    }

    static PeopleUser getUserObject(String phoneNumber, String name) {
        PeopleUser newUser = new PeopleUser();
        ContactNumberDTO contactNumberDTO = new ContactNumberDTO();
        ObjectId userId = new ObjectId();
        newUser.setUserId(userId);
        newUser.setFirstName(name);
        newUser.setStatus(UserStatus.ACTIVE);
        newUser.setReferralCode(RandomStringUtils.randomAlphanumeric(12));
        newUser.setDefaultImageUrl("ImageURL");
        contactNumberDTO.setPhoneNumber(phoneNumber);
        contactNumberDTO.setCountryCode("+1");
        newUser.setVerifiedContactNumber(contactNumberDTO);
        return newUser;
    }

    static PeopleUser getUserObjectWithTags(String phoneNumber, String name) {
        PeopleUser peopleUser = getUserObject(phoneNumber, name);
        SortedMap<String, TagData> tagDataMap = new TreeMap<>();
        TagData tagData = new TagData();

        // Add developer tag
        tagData.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
        tagData.setCreatedOn(PeopleUtils.getCurrentTimeInUTC());
        tagData.setTagName("developer");
        tagData.setIsProfileTag(Boolean.FALSE);
        tagDataMap.put("developer", tagData);

        // Add actor tag
        tagData.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
        tagData.setCreatedOn(PeopleUtils.getCurrentTimeInUTC());
        tagData.setTagName("actor");
        tagData.setIsProfileTag(Boolean.FALSE);
        tagDataMap.put("actor", tagData);

        peopleUser.setTagMap(tagDataMap);
        return peopleUser;
    }

    static UserConnection getConnectionObj(String from, String to) {
        UserConnection userConnection;
        userConnection = new UserConnection();
        ObjectId connectionId = new ObjectId();
        userConnection.setConnectionId(connectionId);
        userConnection.setConnectionFromId(from);
        userConnection.setConnectionToId(to);
        userConnection.setIsFavourite(false);
        userConnection.setConnectionStatus(ConnectionStatus.CONNECTED);
        userConnection.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
        return userConnection;
    }

    static UserConnection getConnectionObjWithNotConnectedStatus(String from, String to) {
        UserConnection userConnection;
        userConnection = new UserConnection();
        ObjectId connectionId = new ObjectId();
        userConnection.setConnectionId(connectionId);
        userConnection.setConnectionFromId(from);
        userConnection.setConnectionToId(to);
        userConnection.setIsFavourite(false);
        userConnection.setConnectionStatus(ConnectionStatus.NOT_CONNECTED);
        userConnection.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
        return userConnection;
    }

    static UserPrivacyProfile getPublicUserProfileObj(String userId) {
        UserPrivacyProfile profile = new UserPrivacyProfile();
        profile.setIsPublic(Boolean.TRUE);
        profile.setUserId(userId);
        profile.setImageURL(null);
        profile.setIsDefault(false);
        profile.setPrivacyProfileId(new ObjectId());
        profile.setProfileName("public");
        return profile;
    }

    static UserPrivacyProfile getUserProfileObj(String userId) {
        UserPrivacyProfile profile = new UserPrivacyProfile();
        profile.setIsPublic(Boolean.FALSE);
        profile.setIsDefault(Boolean.FALSE);
        profile.setUserId(userId);
        profile.setPrivacyProfileId(new ObjectId());
        profile.setProfileName("testprofile");
        return profile;
    }

    static CreateCustomProfileRequestDTO getCustomProfilerequestObj() {
        ProfileKey profileKey = new ProfileKey();
        List<ProfileKey> profileKeyList = new ArrayList<>();
        profileKey.setCategory("name");
        profileKey.setLabel("NA");
        profileKeyList.add(profileKey);
        PrivacyProfileData privacyProfileData = new PrivacyProfileData();
        CreateCustomProfileRequestDTO createCustomProfileRequestDTO = new CreateCustomProfileRequestDTO();
        createCustomProfileRequestDTO.setPrivacyProfileData(privacyProfileData);
        return createCustomProfileRequestDTO;
    }

    static List<UserGroupData> getUserGroupData() {
        List<UserGroupData> userGroups = new ArrayList<>();
        UserGroupData groupData1 = new UserGroupData();
        groupData1.setTitle("title1");
        groupData1.setContactIdList(new ArrayList<>());
        groupData1.setDesc("description1");
        groupData1.setImageURL("S3-Url1");
        groupData1.setOrderNumber(1);

        UserGroupData groupData2 = new UserGroupData();
        groupData2.setTitle("title2");
        groupData2.setContactIdList(new ArrayList<>());
        groupData2.setDesc("description2");
        groupData2.setImageURL("S3-2");
        groupData2.setOrderNumber(2);


        userGroups.add(groupData1);
        userGroups.add(groupData2);

        return userGroups;
    }

    static UserGroup getUserGroupWithContactsAdded(String ownerUserId, List<String> contactIds) {
        UserGroup userGroup = new UserGroup();
        userGroup.setGroupId(new ObjectId());
        userGroup.setOwnerId(ownerUserId);
        userGroup.setTitle("Group title");
        userGroup.setContactIdList(contactIds);
        userGroup.setDesc("Group description");
        userGroup.setImageURL("S3-URL");
        userGroup.setOrderNumber(1);
        return userGroup;
    }

    static UserActivity getUserActivity(String fromId, String toId, String privacyProfileId) {
        UserActivity userActivity = new UserActivity();
        userActivity.setActivityById(fromId);
        userActivity.setActivityForId(toId);
        ContactNumberDTO ContactNumberDTO = new ContactNumberDTO();
        ContactNumberDTO.setCountryCode("XX");
        ContactNumberDTO.setPhoneNumber("XXXXX");

        SharedProfileInformationData sharedProfileInformationData = new SharedProfileInformationData();
        sharedProfileInformationData.setPrivacyProfileId(privacyProfileId);
        userActivity.setSharedProfileInformationData(sharedProfileInformationData);
        return userActivity;
    }

    static UserConnection getConnectionWithProfileObj(String from, String to, String profileId) {
        UserConnection userConnection;
        userConnection = new UserConnection();
        ObjectId connectionId = new ObjectId();
        userConnection.setConnectionId(connectionId);
        userConnection.setConnectionFromId(from);
        userConnection.setConnectionToId(to);
        userConnection.setIsFavourite(false);
        userConnection.setConnectionStatus(ConnectionStatus.CONNECTED);
        SharedProfileInformationData profile = new SharedProfileInformationData();
        profile.setPrivacyProfileId(profileId);
        userConnection.setRealTimeSharedData(profile);
        userConnection.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
        return userConnection;
    }

    static UserConnection getConnectionWithProfileAndContactStaticObj(String from, String to, String profileId) {
        UserConnection userConnection;
        userConnection = getConnectionWithProfileObj(from, to, profileId);
        UserInformationDTO contactStaticData = new UserInformationDTO();

        contactStaticData.setName("FName LName");
        contactStaticData.setFirstName("FName");
        contactStaticData.setLastName("LName");
        contactStaticData.setTagList(Arrays.asList("Developer", "Singer"));

        List<UserProfileData> metadataList = Collections.singletonList(getUserProfileDataForContactNumber());
        contactStaticData.setUserMetadataList(metadataList);

        userConnection.setContactStaticData(contactStaticData);
        userConnection.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
        return userConnection;
    }

    static UserPrivacyProfile getUserPrivacyProfileObj(String userId, List<String> valueIds) {
        UserPrivacyProfile userProfile = new UserPrivacyProfile();
        userProfile.setPrivacyProfileId(new ObjectId());
        userProfile.setUserId(userId);
        userProfile.setIsDefault(true);
        userProfile.setIsSystem(true);
        userProfile.setIsPublic(false);
        userProfile.setProfileName("acquaintance");
        userProfile.setProfileDesc("acquaintance");
        userProfile.setValueIdList(valueIds);
        userProfile.setImageURL("ImageURL");

        Map<String, List<String>> profileKeyMap = new HashMap<>();
        profileKeyMap.put("NA", Arrays.asList("name"));

        return userProfile;
    }

    static UserActivity getRequestMoreInfoUserActivity(String fromId, String toId, String message) {
        UserActivity userActivity = new UserActivity();
        userActivity.setActivityById(fromId);
        userActivity.setActivityForId(toId);
        userActivity.setMessage(message);
        return userActivity;
    }


    static UserInformationDTO getUserInformationObject() {
        UserInformationDTO userInformation = new UserInformationDTO();
        userInformation.setImageURL("url");
        userInformation.setCompany("ymedia");
        userInformation.setFirstName("testuser");
        userInformation.setLastName("testuser");
        userInformation.setMiddleName("test");
        userInformation.setGender("male");
        userInformation.setPosition("developer");
        userInformation.setNickName("test");
        userInformation.setMaidenName("test");
        userInformation.setTagList(Arrays.asList("Developer", "Singer"));
        return userInformation;
    }

    /*
    Networks
     */

    static Network getPublicNetworkObj() {
        Network network = new Network();
        network.setNetworkId(new ObjectId().toString());
        network.setName("Cricket club");
        network.setPrivacyType("PUBLIC");
        network.setPrimaryContactMethod(getNetworkContactMethod());
        network.setNetworkCategory("Sports");
        network.setLastModifiedTime(new DateTime());
        return network;
    }

    static Network getPrivateNetworkObj() {
        Network network = new Network();
        network.setNetworkId(new ObjectId().toString());
        network.setName("golf club");
        network.setPrivacyType("PRIVATE");
        network.setPrimaryContactMethod(getNetworkContactMethod());
        network.setNetworkCategory("Sports");
        network.setLastModifiedTime(new DateTime());
        return network;
    }

    static Network getOpenNetworkObj() {
        Network network = new Network();
        network.setNetworkId(new ObjectId().toString());
        network.setName("Football club");
        network.setPrivacyType("OPEN");
        network.setPrimaryContactMethod(getNetworkContactMethod());
        network.setNetworkCategory("Sports");
        network.setLastModifiedTime(new DateTime());
        return network;
    }

    static NetworkMember getNetworkMemberObj(String memberId, String networkId, String memberRole) {

        NetworkMember networkMember = new NetworkMember();
        networkMember.setMemberId(memberId);
        networkMember.setNetworkId(networkId);
        networkMember.setMemberRole(memberRole);
        networkMember.setIsFavourite(Boolean.TRUE);
        return networkMember;
    }

    static CreateNetworkRequestDTO getCreateNetworkReqObj() {

        CreateNetworkRequestDTO createNetworkRequest = new CreateNetworkRequestDTO();
        createNetworkRequest.setName("reading club");
        createNetworkRequest.setPrimaryContactMethod(getNetworkContactMethod());
        createNetworkRequest.setPrivacyType("public");
        createNetworkRequest.setNetworkCategory("Education");
        return createNetworkRequest;
    }

    static EditNetworkRequestDTO getEditNetworkReqObj(String networkId) {

        EditNetworkRequestDTO editNetworkRequest = new EditNetworkRequestDTO();
        NetworkDetails networkDetails = new NetworkDetails();
        networkDetails.setName("SportsClub");
        networkDetails.setPrimaryContactMethod(getNetworkContactMethod());
        networkDetails.setPrivacyType("public");
        networkDetails.setNetworkCategory("Sports");
        editNetworkRequest.setNetworkDetails(networkDetails);
        editNetworkRequest.setNetworkId(networkId);

        return editNetworkRequest;
    }

    static NetworkCategory getNetworkCategory(String category) {
        NetworkCategory networkCategory = new NetworkCategory();
        networkCategory.setName(category);
        networkCategory.setImageURL("image Url");
        networkCategory.setDescription("description");
        return networkCategory;
    }

    static RecentActiveNetwork getRecentActiveNetworks(String networkId, String category) {
        RecentActiveNetwork recentActiveNetwork = new RecentActiveNetwork();
        recentActiveNetwork.setNetworkId(networkId);
        recentActiveNetwork.setId(new ObjectId());
        recentActiveNetwork.setNetworkCategory(category);
        return recentActiveNetwork;
    }

    static Address getNetworkAddress(double latitude, double longitude) {
        Address address = new Address();
        address.setCity("city");
        List<Double> coordinates = Arrays.asList(latitude, longitude);
        address.setCoordinates(coordinates);
        return address;
    }

    static ConnectRequestDTO getConnectRequestObj() {

        ConnectRequestDTO request = new ConnectRequestDTO();
        CanadaAndUSAContactNumberDTO contactNumber = new CanadaAndUSAContactNumberDTO();
        contactNumber.setCountryCode("+1");
        contactNumber.setPhoneNumber("5222222222");
        request.setContactNumber(contactNumber);
        return request;
    }

    static UpdateUserInfoRequestDTO getUpdateUserRequestObj(List<UserProfileData> userProfileDataList) {

        UpdateUserInfoRequestDTO request = new UpdateUserInfoRequestDTO();
        UserInformationDTO userDetails = getUserInformationObject();
        List<UserProfileData> metadataList = new ArrayList<>();

        // updating existing Profile Data by mapping valueId
        UserProfileData profileData = getUserProfileDataForContactNumber();
        profileData.setValueId(userProfileDataList.get(0).getValueId());
        metadataList.add(profileData);

        // adding a new Contact Number Profile Data Object
        metadataList.add(getUserProfileDataForContactNumber());

        // adding a new Social Profile Data Object
        metadataList.add(getUserProfileDataForSocialProfile());

        userDetails.setUserMetadataList(metadataList);
        request.setUserDetails(userDetails);
        return request;
    }

    static UpdateUserInfoRequestDTO getUpdateUserRequestObjDeletedObject(List<UserProfileData> userProfileDataList,
                                                                         String valueId) {

        UpdateUserInfoRequestDTO request = new UpdateUserInfoRequestDTO();
        UserInformationDTO userDetails = getUserInformationObject();
        List<UserProfileData> metadataList = new ArrayList<>();

        // adding all userProfileData except for the given valueId
        for (UserProfileData profileData : userProfileDataList) {
            if (!profileData.getValueId().equals(valueId)) {
                metadataList.add(profileData);
            }
        }

        userDetails.setUserMetadataList(metadataList);
        request.setUserDetails(userDetails);
        return request;
    }

    static UserProfileData getUserProfileDataForContactNumber() {
        UserProfileData userProfileData = new UserProfileData();
        userProfileData.setCategory(UserInfoCategory.CONTACT_NUMBER.getValue());
        userProfileData.setLabel("PL.00.03");
        List<KeyValueData> keyValueDataList = new ArrayList<>();
        KeyValueData keyValueData = new KeyValueData();
        keyValueData.setKey("countryCode");
        keyValueData.setVal("+1");
        keyValueDataList.add(keyValueData);

        KeyValueData keyValueData1 = new KeyValueData();
        keyValueData1.setKey("phoneNumber");
        keyValueData1.setVal(RandomStringUtils.randomNumeric(10));
        keyValueDataList.add(keyValueData1);

        userProfileData.setKeyValueDataList(keyValueDataList);
        return userProfileData;
    }

    static UserProfileData getUserProfileDataForSocialProfile() {
        UserProfileData userProfileData = new UserProfileData();
        userProfileData.setCategory(UserInfoCategory.SOCIAL_PROFILE.getValue());
        userProfileData.setLabel("PL.02.00");
        userProfileData.setSocialProfileId(RandomStringUtils.randomAlphabetic(15));
        userProfileData.setSocialProfileImageURL("http://randomeImageUrl.com");
        userProfileData.setVerification(UserInformationVerification.VERIFIED);

        List<KeyValueData> keyValueDataList = new ArrayList<>();
        KeyValueData keyValueData = new KeyValueData();
        keyValueData.setKey("handle");
        keyValueData.setVal("test@yemdia.com");
        keyValueDataList.add(keyValueData);

        userProfileData.setKeyValueDataList(keyValueDataList);
        return userProfileData;
    }

    static SendSingleIntroRequestDTO getSendSingleIntroRequestDTO(String introducedConnectionId, String connectionId) {
        SendSingleIntroRequestDTO requestDTO = new SendSingleIntroRequestDTO();
        List<UserContact> userContactList = new ArrayList<>();

        UserContact userContact = new UserContact();
        userContact.setConnectionId(introducedConnectionId);
        requestDTO.setIntroducedContact(userContact);

        userContact = new UserContact();
        userContact.setConnectionId(connectionId);
        userContactList.add(userContact);

        requestDTO.setInitiateContactDetailsList(userContactList);
        return requestDTO;
    }

    static SendMultiIntroRequestDTO getMultiIntroRequestDTOWithoutContactNumber(String connectionId1, String connectionId2,
                                                                                String connectionId3) {
        SendMultiIntroRequestDTO requestDTO = new SendMultiIntroRequestDTO();
        List<UserContact> userContactList = new ArrayList<>();

        UserContact userContact = new UserContact();
        userContact.setConnectionId(connectionId1);

        userContactList.add(userContact);

        userContact = new UserContact();
        userContact.setConnectionId(connectionId2);

        userContactList.add(userContact);

        userContact = new UserContact();
        userContact.setConnectionId(connectionId3);

        userContactList.add(userContact);

        requestDTO.setContactDetailsList(userContactList);
        return requestDTO;
    }

    static UserConnection getConnectionObjForStaticContact(String from) {
        UserConnection userConnection;
        userConnection = new UserConnection();
        ObjectId connectionId = new ObjectId();
        userConnection.setConnectionId(connectionId);
        userConnection.setConnectionFromId(from);
        userConnection.setIsFavourite(false);
        userConnection.setConnectionStatus(ConnectionStatus.NOT_CONNECTED);

        UserInformationDTO contactStaticData = new UserInformationDTO();

        contactStaticData.setName("FName LName");
        contactStaticData.setFirstName("FName");
        contactStaticData.setLastName("LName");
        contactStaticData.setTagList(Arrays.asList("Developer", "Singer"));

        List<UserProfileData> metadataList = Collections.singletonList(getUserProfileDataForContactNumber());
        contactStaticData.setUserMetadataList(metadataList);
        userConnection.setContactStaticData(contactStaticData);
        userConnection.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
        return userConnection;
    }

    static ManageFavouritesRequestDTO getFavouritesRequestDTO(List<String> connectionIdList) {
        ManageFavouritesRequestDTO manageFavouritesRequestDTO = new ManageFavouritesRequestDTO();
        List<FavouriteConnectionSequenceDTO> favouritesRequestDTOList = new ArrayList<>();
        int counter = 1;
        for (String connectionId : connectionIdList) {
            FavouriteConnectionSequenceDTO sequenceDTO = new FavouriteConnectionSequenceDTO();
            sequenceDTO.setConnectionId(connectionId);
            sequenceDTO.setSequenceNumber(counter);
            favouritesRequestDTOList.add(sequenceDTO);
            counter++;
        }
        manageFavouritesRequestDTO.setFavouriteConnectionList(favouritesRequestDTOList);
        return manageFavouritesRequestDTO;
    }

    static VerificationStatusUpdateRequest getVerificationStatusUpdateRequest(String valueId, String verificationStatus,
                                                                              List<KeyValueData> keyValueDataList) {
        VerificationStatusUpdateRequest updateRequest = new VerificationStatusUpdateRequest();
        updateRequest.setValueId(valueId);
        updateRequest.setVerificationStatus(verificationStatus);
        updateRequest.setKeyValueDataList(keyValueDataList);
        return updateRequest;
    }

    static ChangePrivacyProfileRequestDTO getChangePrivacyProfileRequestDTO(String connectionId,
                                                                            String privacyProfileId,
                                                                            List<String> valueIds) {
        ChangePrivacyProfileRequestDTO requestDTO = new ChangePrivacyProfileRequestDTO();
        SharedProfileInformationData profileInformationData = new SharedProfileInformationData();
        profileInformationData.setValueIdList(valueIds);
        profileInformationData.setPrivacyProfileId(privacyProfileId);
        requestDTO.setSharedProfileInformationData(profileInformationData);
        requestDTO.setConnectionId(connectionId);
        return requestDTO;
    }

    static ReportUserRequest getReportUserRequest(String userId, String message) {
        ReportUserRequest reportUser = new ReportUserRequest();
        reportUser.setUserId(userId);
        reportUser.setReportMessage(message);
        return reportUser;
    }

    static UserContact getUserContact(String connectionId, ContactNumberDTO phoneNumber) {
        UserContact userContact = new UserContact();
        userContact.setConnectionId(connectionId);
        userContact.setContactNumber(phoneNumber);
        return userContact;
    }

    static SharedProfileInformationData getSharedProfileInformationData(String privacyProfileId, List<String> valueIdList) {
        SharedProfileInformationData sharedPrivacyProfileKey = new SharedProfileInformationData();
        sharedPrivacyProfileKey.setPrivacyProfileId(privacyProfileId);
        sharedPrivacyProfileKey.setIsCompanyShared(true);
        sharedPrivacyProfileKey.setIsPositionShared(true);
        sharedPrivacyProfileKey.setValueIdList(valueIdList);
        return sharedPrivacyProfileKey;
    }

    static BlockUserRequest getBlockUserRequest(String userIdToBeBlocked, boolean isUserBlocked) {
        BlockUserRequest blockUserRequest = new BlockUserRequest();
        blockUserRequest.setUserId(userIdToBeBlocked);
        blockUserRequest.setIsBlocked(isUserBlocked);
        return blockUserRequest;
    }

    static DeleteAccountRequest getDeleteAccountRequest() {
        DeleteAccountRequest deleteAccountRequest = new DeleteAccountRequest();
        deleteAccountRequest.setMessage("Please delete my account.");
        return deleteAccountRequest;
    }

    static UserSession getUserSession(String userId) {
        UserSession userSession = new UserSession();
        userSession.setUserId(userId);
        userSession.setSessionToken(TokenGenerator.generateSessionToken());
        userSession.setStatus(TokenStatus.ACTIVE);
        userSession.setCreatedTime(new DateTime());
        return userSession;
    }

    static UpdateNetworkFavouriteRequestDTO getNetworkFavouriteRequestDTO(String networkId, boolean updateFavouriteStatus) {
        UpdateNetworkFavouriteRequestDTO updateNetworkFavouriteRequestDTO = new UpdateNetworkFavouriteRequestDTO();
        updateNetworkFavouriteRequestDTO.setNetworkId(networkId);
        updateNetworkFavouriteRequestDTO.setNetworkFavorite(updateFavouriteStatus);
        return updateNetworkFavouriteRequestDTO;
    }

    static NetworkAdminPromoteDTO getNetworkAdminPromoteDTO(String networkId, List<String> memberIdList) {
        NetworkAdminPromoteDTO networkAdminPromoteDTO = new NetworkAdminPromoteDTO();
        networkAdminPromoteDTO.setNetworkId(networkId);
        networkAdminPromoteDTO.setMemberIdList(memberIdList);
        return networkAdminPromoteDTO;
    }

    static UserActivity getUserActivityForNetworkJoinRequest(String userId, String networkId, String activityForId) {
        UserActivity userActivity = new UserActivity();
        userActivity.setActivityById(userId);
        userActivity.setActivityForId(activityForId);
        ActivityType activityType = new ActivityType();
        activityType.setActionTaken(Action.INITIATED);
        userActivity.setOverallStatus(ActivityStatus.PENDING);
        activityType.setRequestType(RequestType.NETWORK_JOIN_REQUEST);
        userActivity.setActivityType(activityType);
        userActivity.setNetworkId(PeopleUtils.convertStringToObjectId(networkId));
        userActivity.setCreatedOn(new DateTime());
        userActivity.setLastUpdatedOn(new DateTime());

        return userActivity;
    }

    static RegisteredNumber getRegisteredNumber(List<String> numbers) {
        RegisteredNumber registeredNumber = new RegisteredNumber();
        registeredNumber.setRegisteredNumberList(numbers);
        return registeredNumber;
    }

    static NetworkPrimaryContactMethod getNetworkContactMethod() {
        NetworkPrimaryContactMethod networkPrimaryContactMethod = new NetworkPrimaryContactMethod();
        networkPrimaryContactMethod.setContactCategory("PhoneNumber");
        networkPrimaryContactMethod.setContactLabel("PL.00.00");
        return networkPrimaryContactMethod;
    }

    static DeleteContactRequest getDeleteContactRequest(List<String> connectionIds) {
        DeleteContactRequest deleteContactRequest = new DeleteContactRequest();
        deleteContactRequest.setConnectionIdList(connectionIds);
        return deleteContactRequest;
    }

    /* Stubs for ambassador test cases */
    static SendReferralRequestDTO getReferralRequest(String phoneNumber) {

        CanadaAndUSAContactNumberDTO contactNumber = new CanadaAndUSAContactNumberDTO();
        contactNumber.setCountryCode("+1");
        contactNumber.setPhoneNumber(phoneNumber);
        SendReferralRequestDTO referralRequest = new SendReferralRequestDTO();
        referralRequest.setLink("https://app-download.link");
        referralRequest.setReferredContactNumber(contactNumber);
        return referralRequest;
    }

    static VerifyOTPRequestDTO getVerifyOTPRequest(String referralCode, String otp) {
        VerifyOTPRequestDTO verifyOTPRequest = new VerifyOTPRequestDTO();
        verifyOTPRequest.setOtp(otp);
        verifyOTPRequest.setReferralCode(referralCode);
        return verifyOTPRequest;
    }

    static TemporarySession getTempSession(ContactNumberDTO contactNumber, String tempToken) {
        // persist otp and temp token
        TemporarySession temporarySession = new TemporarySession();
        temporarySession.setOtp("1111");
        temporarySession.setContactNumber(contactNumber);
        temporarySession.setTemporaryToken(tempToken);
        temporarySession.setTokenStatus(TokenStatus.ACTIVE);
        temporarySession.setCreatedOn(PeopleUtils.getCurrentTimeInUTC());
        return temporarySession;
    }


    /* General stubs */
    static PeopleUser getWatuUserAccount(String phoneNumber, String email, String firstName, String lastName,
                                         String company) {
        //User creation
        PeopleUser watuUser = new PeopleUser();
        ObjectId userId = new ObjectId();

        //primary contactNumber
        ContactNumberDTO verifiedContactNumber = new ContactNumberDTO();
        verifiedContactNumber.setPhoneNumber(phoneNumber);
        verifiedContactNumber.setCountryCode("+1");

        // primary Email mapping
        Map<Integer, UserInformationVerification> emailVerificationMap = new HashMap<>();
        emailVerificationMap.put(email.hashCode(), UserInformationVerification.VERIFIED);

        //Network setting
        List<String> networkDefaultValues = new ArrayList<>();


        //primary number tobe added to meta list
        UserProfileData primaryNumber = getUserProfileDataForGivenContactNumber("+1", phoneNumber);
        primaryNumber.setValueId(new ObjectId().toString());
        primaryNumber.setIsPrimary(true);
        primaryNumber.setVerification(UserInformationVerification.VERIFIED);
        networkDefaultValues.add(primaryNumber.getValueId());

        //primary email tobe added to meta list
        UserProfileData primaryEmail = getUserProfileDataForGivenEmail(email);
        primaryEmail.setValueId(new ObjectId().toString());
        primaryEmail.setVerification(UserInformationVerification.VERIFIED);
        primaryEmail.setIsPrimary(true);
        networkDefaultValues.add(primaryEmail.getValueId());

        //social profile
        UserProfileData socialProfile = getUserProfileDataForSocialProfile();
        socialProfile.setValueId(new ObjectId().toString());
        socialProfile.setVerification(UserInformationVerification.VERIFIED);

        Map<Integer, UserInformationVerification> socialHandleMap = new HashMap<>();
        socialHandleMap.put(socialProfile.getKeyValueDataList().get(0).getVal().hashCode(),
                UserInformationVerification.VERIFIED);

        // updating userMetaList
        List<UserProfileData> userMetaDataList = Arrays.asList(primaryNumber, primaryEmail, socialProfile);

        watuUser.setUserId(userId);
        watuUser.setFirstName(firstName);
        watuUser.setLastName(lastName);
        watuUser.setCompany(company);
        watuUser.setDefaultImageUrl("ImageURL");
        watuUser.setVerifiedContactNumber(verifiedContactNumber);
        watuUser.setPrimaryEmail(email);
        watuUser.setUserMetadataList(userMetaDataList);
        watuUser.setEmailAddressMap(emailVerificationMap);
        watuUser.setSocialHandleMap(socialHandleMap);
        watuUser.setNetworkSharedValueList(networkDefaultValues);
        watuUser.setReferralCode(RandomStringUtils.randomAlphanumeric(12));
        watuUser.setCreatedOn(PeopleUtils.getCurrentTimeInUTC());
        watuUser.setStatus(UserStatus.ACTIVE);


        return watuUser;
    }

    static UserProfileData getUserProfileDataForGivenContactNumber(String countryCode, String phoneNumber) {

        UserProfileData userProfileData = new UserProfileData();
        userProfileData.setCategory(UserInfoCategory.CONTACT_NUMBER.getValue());
        userProfileData.setLabel("PL.00.00");
        List<KeyValueData> keyValueDataList = new ArrayList<>();
        KeyValueData keyValueForCountryCode = new KeyValueData();
        keyValueForCountryCode.setKey("countryCode");
        keyValueForCountryCode.setVal(countryCode);
        keyValueDataList.add(keyValueForCountryCode);

        KeyValueData keyValueForPhoneNumber = new KeyValueData();
        keyValueForPhoneNumber.setKey("phoneNumber");
        keyValueForPhoneNumber.setVal(phoneNumber);
        keyValueDataList.add(keyValueForPhoneNumber);

        userProfileData.setKeyValueDataList(keyValueDataList);

        return userProfileData;
    }

    static UserProfileData getUserProfileDataForGivenEmail(String email) {

        UserProfileData userProfileData = new UserProfileData();
        userProfileData.setCategory(UserInfoCategory.EMAIL_ADDRESS.getValue());
        userProfileData.setLabel("PL.01.04");
        List<KeyValueData> keyValueDataList = new ArrayList<>();
        KeyValueData keyValueForEmail = new KeyValueData();
        keyValueForEmail.setKey("emailAddress");
        keyValueForEmail.setVal(email);
        keyValueDataList.add(keyValueForEmail);

        userProfileData.setKeyValueDataList(keyValueDataList);

        return userProfileData;
    }

    static UserConnection getUserStaticConnectionWithGivenContactNumber(String fromUserId, ContactNumberDTO staticContactNumber) {

        UserProfileData userProfileData = getUserProfileDataForGivenContactNumber("+1", staticContactNumber.getPhoneNumber());
        userProfileData.setVerification(UserInformationVerification.VERIFIED);
        // prepare static information
        UserInformationDTO userInformation = new UserInformationDTO();
        userInformation.setFirstName("staticContact");
        userInformation.setContactNumber(staticContactNumber);
        userInformation.setUserMetadataList(Arrays.asList(userProfileData));

        // create a userConnection
        UserConnection staticConnection = new UserConnection();
        staticConnection.setConnectionId(new ObjectId());
        staticConnection.setConnectionStatus(ConnectionStatus.NOT_CONNECTED);
        staticConnection.setConnectionFromId(fromUserId);
        staticConnection.setIsFavourite(false);
        staticConnection.setContactStaticData(userInformation);

        staticConnection.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
        return staticConnection;
    }

    static ContactSyncRequestDTO getContactSyncRequestDTO() {
        ContactSyncRequestDTO contactSyncRequestDTO = new ContactSyncRequestDTO();
        List<UserInformationDTO> userInformationDTOS = new ArrayList<>();
        userInformationDTOS.add(getUserInformationObject());
        contactSyncRequestDTO.setUserContactList(userInformationDTOS);
        return contactSyncRequestDTO;
    }

    static RequestMoreInfoDTO requestForMoreInfo(String connectionId) {
        RequestMoreInfoDTO moreInfo = new RequestMoreInfoDTO();
        moreInfo.setConnectionId(connectionId);
        moreInfo.setMessage("provide more info");
        return moreInfo;
    }

    static IgnoreRequestDTO getActivityIgnoreRequestDTO(List<String> activityIds, List<String> activitySubId) {
        IgnoreRequestDTO ignoreRequest = new IgnoreRequestDTO();
        ignoreRequest.setActivityIdList(activityIds);
        ignoreRequest.setActivitySubIdList(activitySubId);
        return ignoreRequest;
    }

    static CancelRequestDTO getActivityCancelRequestDTO(List<String> activityIds) {
        CancelRequestDTO cancelRequest = new CancelRequestDTO();
        cancelRequest.setActivityIdList(activityIds);
        return cancelRequest;
    }

    static SendConnectionRequest getConnectionRequestObjectWithUserIDFlow(String initiateId,
                                                                          UserPrivacyProfile sessionUserPrivacyProfile) {
        SharedProfileInformationData sharedProfileInformationData = new SharedProfileInformationData();
        sharedProfileInformationData.setIsPositionShared(false);
        sharedProfileInformationData.setIsCompanyShared(false);
        sharedProfileInformationData.setPrivacyProfileId(sessionUserPrivacyProfile.getPrivacyProfileId());
        sharedProfileInformationData.setValueIdList(sessionUserPrivacyProfile.getValueIdList());

        SendConnectionRequest connectionRequest = new SendConnectionRequest();
        connectionRequest.setMessage(" Lets get connected ");
        connectionRequest.setSharedPrivacyProfileKey(sharedProfileInformationData);
        connectionRequest.setInitiateUserIdList(Arrays.asList(initiateId));
        return connectionRequest;
    }

    static ShareContactRequest getShareContactRequestDTO(List<String> sharedWithContacts, List<String> sharedContacts) {
        ShareContactRequest shareContact = new ShareContactRequest();
        shareContact.setSharedWithConnectionIdList(sharedWithContacts);
        shareContact.setSharedContactIdList(sharedContacts);
        return shareContact;
    }

    static ShareLocationRequest getShareLocationRequestDTO(List<String> sharedWithConnectionIdList,
                                                           int timeInMinutes) {

        ShareLocationRequest request = new ShareLocationRequest();
        request.setSharedWithConnectionIdList(sharedWithConnectionIdList);
        request.setTimeInMinutes(timeInMinutes);
        return request;
    }

    static DeleteActivityRequest getDeleteActivityDTO(List<String> activityIds) {
        DeleteActivityRequest deleteActivity = new DeleteActivityRequest();
        deleteActivity.setActivityIdList(activityIds);
        return deleteActivity;
    }

    static JoinNetworkRequestDTO getJoinNetworkRequestDTO(String networkId) {
        JoinNetworkRequestDTO joinNetwork = new JoinNetworkRequestDTO();
        joinNetwork.setNetworkId(networkId);
        return joinNetwork;
    }

    static UpdateNetworkSettingDTO getUpdateNetworkSettingsDTO(List<String> networkSharedValueList) {
        UpdateNetworkSettingDTO updateNetworkSetting = new UpdateNetworkSettingDTO();
        updateNetworkSetting.setNetworkSharedValueList(networkSharedValueList);
        return updateNetworkSetting;
    }

    static EditSharedContactRequest getEditSharedContactDTO(List<String> activityIds, List<String> activitySubIds) {

        EditSharedContactRequest editSharedContact = new EditSharedContactRequest();
        editSharedContact.setActivityIdList(activityIds);
        editSharedContact.setActivitySubIdList(activitySubIds);
        return editSharedContact;
    }

    static ContactImage getContactImageObj(String connectionId, String imageUrl) {
        ContactImage contactImage = new ContactImage();
        contactImage.setConnectionId(connectionId);
        contactImage.setImageURL(imageUrl);
        return contactImage;
    }

    static ContactNumberDTO getContactNumberDTO(String countryCode, String phoneNumber) {
        ContactNumberDTO contactNumberDTO = new ContactNumberDTO();
        contactNumberDTO.setCountryCode(countryCode);
        contactNumberDTO.setPhoneNumber(phoneNumber);
        return contactNumberDTO;
    }

    static TemporarySession getTemporarySessionObj(String otp, ContactNumberDTO contactNumber) {
        TemporarySession temporarySession = new TemporarySession();
        temporarySession.setOtp(otp);
        temporarySession.setContactNumber(contactNumber);
        temporarySession.setTemporaryToken(TokenGenerator.generateTempToken());
        temporarySession.setTokenStatus(TokenStatus.ACTIVE);
        temporarySession.setCreatedOn(PeopleUtils.getCurrentTimeInUTC());
        return temporarySession;
    }

    static RemoveMemberFromNetworkDTO getRemoveMemberDTO(List<String> membersToBeRemoved, String networkId) {
        RemoveMemberFromNetworkDTO removeMember = new RemoveMemberFromNetworkDTO();
        removeMember.setMemberIdList(membersToBeRemoved);
        removeMember.setNetworkId(networkId);
        return removeMember;
    }

    static LeaveNetworkRequestDTO getLeaveNetworkDTO(String networkId) {
        LeaveNetworkRequestDTO leaveNetwork = new LeaveNetworkRequestDTO();
        leaveNetwork.setNetworkId(networkId);
        return leaveNetwork;
    }

    static DemoteAdminRequestDTO getDemoteAdminDTO(String networkId, List<String> adminsToBeDemoted) {
        DemoteAdminRequestDTO demoteAdmin = new DemoteAdminRequestDTO();
        demoteAdmin.setNetworkId(networkId);
        demoteAdmin.setMemberIdList(adminsToBeDemoted);
        return demoteAdmin;
    }

    static NetworkInviteRequestDTO getNetworkInvitationDTO(String networkId, List<String> connectionIds) {
        NetworkInviteRequestDTO networkInviteRequest = new NetworkInviteRequestDTO();
        List<NetworkInviteeContact> networkInviteeContacts = new ArrayList<>();

        for (String connectionId : PeopleUtils.emptyIfNull(connectionIds)) {
            NetworkInviteeContact networkInviteeContact = new NetworkInviteeContact();
            networkInviteeContact.setConnectionId(connectionId);
            networkInviteeContacts.add(networkInviteeContact);
        }

        networkInviteRequest.setNetworkId(networkId);
        networkInviteRequest.setNetworkInviteeList(networkInviteeContacts);

        return networkInviteRequest;
    }

    static TransferOwnerShipRequestDTO getOwnerShipTransferDTO(String networkId, String userId) {
        TransferOwnerShipRequestDTO transferOwnerShip = new TransferOwnerShipRequestDTO();
        transferOwnerShip.setNetworkId(networkId);
        transferOwnerShip.setMemberId(userId);
        return transferOwnerShip;
    }

    static ShareNetworkRequestDTO getNetworkShareDTO(String networkId, List<String> connectionIds) {
        ShareNetworkRequestDTO shareNetwork = new ShareNetworkRequestDTO();
        List<NetworkInviteeContact> shareWithContacts = new ArrayList<>();

        for (String connectionId : PeopleUtils.emptyIfNull(connectionIds)) {
            NetworkInviteeContact networkInviteeContact = new NetworkInviteeContact();
            networkInviteeContact.setConnectionId(connectionId);
            shareWithContacts.add(networkInviteeContact);
        }

        shareNetwork.setNetworkId(networkId);
        shareNetwork.setSharedWithContactList(shareWithContacts);
        return shareNetwork;
    }

    static UpdateFavouriteContact getFavouriteContact(String connectionId, boolean contactFavouriteStatus) {
        UpdateFavouriteContact favouriteContact = new UpdateFavouriteContact();
        favouriteContact.setConnectionId(connectionId);
        favouriteContact.setIsFavourite(contactFavouriteStatus);
        return favouriteContact;
    }

    static EditGroup getEditGroupDTO(String groupId, String title, String imageURL, List<String> contactsIds, String desc) {
        EditGroup editGroup = new EditGroup();
        editGroup.setGroupId(groupId);
        editGroup.setTitle(title);
        editGroup.setImageURL(imageURL);
        editGroup.setContactIdList(contactsIds);
        editGroup.setDesc(desc);
        return editGroup;
    }

    static UpdateFavouriteGroup getFavouriteGroupDTO(String groupId, Boolean status) {
        UpdateFavouriteGroup favouriteGroup = new UpdateFavouriteGroup();
        favouriteGroup.setGroupId(groupId);
        favouriteGroup.setIsFavourite(status);
        return favouriteGroup;
    }

    static DeleteUserGroupRequestDTO getDeleteGroupRequest(List<String> groupsToBeDeleted) {
        DeleteUserGroupRequestDTO deleteUserGroupRequest = new DeleteUserGroupRequestDTO();
        deleteUserGroupRequest.setGroupIdList(groupsToBeDeleted);
        return deleteUserGroupRequest;
    }

    static GroupImage getGroupImage(String groupId, String imageURL) {
        GroupImage groupImage = new GroupImage();
        groupImage.setGroupId(groupId);
        groupImage.setImageURL(imageURL);
        return groupImage;
    }

    static DeletePrivacyProfileRequestDTO getPrivacyProfileDeleteRequest(List<String> profileIds) {
        DeletePrivacyProfileRequestDTO deletePrivacyProfileRequestDTO = new DeletePrivacyProfileRequestDTO();
        deletePrivacyProfileRequestDTO.setProfileIdList(profileIds);
        return deletePrivacyProfileRequestDTO;
    }

    static EditPrivacyProfileRequestDTO getPrivacyProfileEditRequest(String profileName, String profileId) {
        EditPrivacyProfileRequestDTO editPrivacyProfileRequestDTO = new EditPrivacyProfileRequestDTO();
        PrivacyProfileData privacyProfileData = new PrivacyProfileData();
        privacyProfileData.setProfileName(profileName);
        privacyProfileData.setPrivacyProfileId(profileId);
        editPrivacyProfileRequestDTO.setPrivacyProfileData(privacyProfileData);
        return editPrivacyProfileRequestDTO;
    }

    static InviteByNumberRequest getInviteByNumberRequestDTO(String privacyProfileId) {
        UserInformationDTO inviteeInfo = getUserInformationObject();
        List<UserProfileData> userProfileDataList = new ArrayList<>();
        userProfileDataList.add(getUserProfileDataForContactNumber());
        inviteeInfo.setUserMetadataList(userProfileDataList);

        SharedProfileInformationData sharedProfileInformationData = new SharedProfileInformationData();
        sharedProfileInformationData.setPrivacyProfileId(privacyProfileId);

        InviteByNumberRequest invite = new InviteByNumberRequest();
        invite.setInviteeContactInformation(inviteeInfo);
        invite.setSharedPrivacyProfileKey(sharedProfileInformationData);

        return invite;
    }

    static SendConnectionRequest getSendConnectionRequestDTOWithUserIdAndUserInformation(String initiateUserId,
                                                                                         String initiatorProfileId,
                                                                                         UserInformationDTO userInformationDTO) {
        SendConnectionRequest requestDTO = new SendConnectionRequest();
        requestDTO.setInitiateUserIdList(Collections.singletonList(initiateUserId));
        requestDTO.setSharedPrivacyProfileKey(MethodStubs.getSharedProfileInformationData(
                initiatorProfileId, new ArrayList<>()));
        requestDTO.setInitiateUserInformation(userInformationDTO);
        requestDTO.setStaticContactToBeCreated(true);
        return requestDTO;
    }

    static SystemPrivacyProfile getSystemPrivacyProfile() {
        SystemPrivacyProfile systemPrivacyProfile = new SystemPrivacyProfile();
        systemPrivacyProfile.setIsDefault(true);
        systemPrivacyProfile.setIsPublic(true);
        systemPrivacyProfile.setProfileName("System default");
        List<ProfileKey> profileKeyList = new ArrayList<>();
        ProfileKey profileKey1 = new ProfileKey();
        profileKey1.setCategory(UserInfoCategory.CONTACT_NUMBER.getValue());
        profileKey1.setLabel("PL.00.00");
        ProfileKey profileKey2 = new ProfileKey();
        profileKey2.setCategory(UserInfoCategory.EMAIL_ADDRESS.getValue());
        profileKey2.setLabel("PL.00.01");
        systemPrivacyProfile.setProfileKeyList(profileKeyList);
        return systemPrivacyProfile;
    }

    static Coordinates getCoordinates(double latitude, double longitude) {
        Coordinates coordinates = new Coordinates();
        coordinates.setLongitude(longitude);
        coordinates.setLatitude(latitude);
        return coordinates;
    }

    static UserActivity getUserActivityBasedOnRequestType(String activityForId, String activityToId, RequestType requestType,ActivityStatus activityStatus,Action action) {
        UserActivity userActivity = new UserActivity();
        ActivityType activityType = new ActivityType();
        activityType.setRequestType(requestType);
        activityType.setActionTaken(action);
        userActivity.setActivityType(activityType);
        userActivity.setOverallStatus(activityStatus);
        userActivity.setIsCleared(Boolean.FALSE);
        userActivity.setActivityForId(activityForId);
        userActivity.setActivityById(activityToId);
        return userActivity;
    }

    static MergeContactsRequestDTO getMergeContactsRequestDTO(String masterID, List<String> mergedConnectionIds) {
        MergeContact mergeContact = new MergeContact();
        mergeContact.setMasterConnectionId(masterID);
        mergeContact.setMergedConnectionIds(mergedConnectionIds);

        MergeContactsRequestDTO mergeContactsRequestDTO = new MergeContactsRequestDTO();
        mergeContactsRequestDTO.setListOfContactsToBeMerged(Arrays.asList(mergeContact));
        return mergeContactsRequestDTO;
    }
}



