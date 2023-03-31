package com.peopleapp.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.peopleapp.configuration.LocaleMessageReader;
import com.peopleapp.constant.MessageConstant;
import com.peopleapp.dto.ActivityType;
import com.peopleapp.dto.PrivacyProfileData;
import com.peopleapp.dto.ProfileKey;
import com.peopleapp.dto.PushNotificationDTO;
import com.peopleapp.dto.SQSPayload;
import com.peopleapp.dto.SharedProfileInformationData;
import com.peopleapp.dto.UserInformationDTO;
import com.peopleapp.dto.UserProfileData;
import com.peopleapp.dto.requestresponsedto.CreateCustomProfileRequestDTO;
import com.peopleapp.dto.requestresponsedto.CreateOrEditPrivacyProfileResponse;
import com.peopleapp.dto.requestresponsedto.DeletePrivacyProfileRequestDTO;
import com.peopleapp.dto.requestresponsedto.DeletePrivacyProfileResponseDTO;
import com.peopleapp.dto.requestresponsedto.EditPrivacyProfileRequestDTO;
import com.peopleapp.dto.requestresponsedto.GetListOfPrivacyProfilesResponseDTO;
import com.peopleapp.dto.requestresponsedto.SetDefaultProfileRequestDTO;
import com.peopleapp.dto.requestresponsedto.ShareProfileData;
import com.peopleapp.dto.requestresponsedto.ShareProfileResponse;
import com.peopleapp.dto.requestresponsedto.ShareTagRequest;
import com.peopleapp.enums.Action;
import com.peopleapp.enums.ActivityStatus;
import com.peopleapp.enums.MessageCodes;
import com.peopleapp.enums.RequestType;
import com.peopleapp.exception.BadRequestException;
import com.peopleapp.model.PeopleUser;
import com.peopleapp.model.SystemPrivacyProfile;
import com.peopleapp.model.UserActivity;
import com.peopleapp.model.UserConnection;
import com.peopleapp.model.UserPrivacyProfile;
import com.peopleapp.repository.PeopleUserRepository;
import com.peopleapp.repository.SystemPrivacyProfileRepository;
import com.peopleapp.repository.UserActivityRepository;
import com.peopleapp.repository.UserConnectionRepository;
import com.peopleapp.repository.UserPrivacyProfileRepository;
import com.peopleapp.security.TokenAuthService;
import com.peopleapp.service.MasterService;
import com.peopleapp.service.NotificationService;
import com.peopleapp.service.PrivacyProfileService;
import com.peopleapp.service.QueueService;
import com.peopleapp.util.PeopleUtils;

/*
This service class has all the methods related to user privacy profiles
 */
@Service
public class PrivacyProfileServiceImpl implements PrivacyProfileService {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Inject
	private PeopleUserRepository peopleUserRepository;

	@Inject
	private SystemPrivacyProfileRepository systemProfileRepository;

	@Inject
	private UserPrivacyProfileRepository userPrivacyProfileRepository;

	@Inject
	private UserConnectionRepository userConnectionRepository;

	@Inject
	private UserActivityRepository userActivityRepository;

	@Inject
	private LocaleMessageReader message;

	@Inject
	private TokenAuthService tokenAuthService;

	@Inject
	private MasterService masterService;

	@Inject
	private NotificationService notificationService;

	@Inject
	private QueueService queueService;

	@Override
	public GetListOfPrivacyProfilesResponseDTO getListOfUserPrivacyProfiles() {

		logger.info("Inside PeopleUserServiceImpl -> getListOfUserPrivacyProfiles");

		PeopleUser sessionUser = tokenAuthService.getSessionUser();
		String userId = sessionUser.getUserId();
		List<UserPrivacyProfile> userPrivacyProfilesList;
		GetListOfPrivacyProfilesResponseDTO response;

		userPrivacyProfilesList = userPrivacyProfileRepository.findAllByUserId(userId);

		if (PeopleUtils.isNullOrEmpty(userPrivacyProfilesList)) {
			userPrivacyProfilesList = createSystemProfileDataForUser(sessionUser);
		}

		response = new GetListOfPrivacyProfilesResponseDTO();
		response.setUserPrivacyProfileList(
				populatePrivacyProfileData(userPrivacyProfilesList, sessionUser.getDefaultImageUrl()));
		return response;
	}

	@Override
	public DeletePrivacyProfileResponseDTO deleteUserPrivacyProfile(
			DeletePrivacyProfileRequestDTO deletePrivacyProfileRequest) {

		logger.info("Inside PeopleUserServiceImpl->deleteUserPrivacyProfile");

		PeopleUser sessionUser = tokenAuthService.getSessionUser();
		String userId = sessionUser.getUserId();
		List<String> profileIdList = deletePrivacyProfileRequest.getProfileIdList();

		// get user default profile
		UserPrivacyProfile defaultProfile = userPrivacyProfileRepository.findDefaultUserProfile(userId, Boolean.TRUE);
		String defaultProfileId = defaultProfile.getPrivacyProfileId();
		for (String profileId : profileIdList) {
			if (defaultProfileId.equalsIgnoreCase(profileId)) {
				throw new BadRequestException(MessageCodes.DEFAULT_PRIVACY_PROFILE_CANNOT_BE_DELETED.getValue());
			}
			UserPrivacyProfile publicProfile = userPrivacyProfileRepository.findPublicProfile(userId);
			if (publicProfile.getPrivacyProfileId().equalsIgnoreCase(profileId)) {
				throw new BadRequestException(MessageCodes.PUBLIC_PRIVACY_PROFILE_CANNOT_BE_DELETED.getValue());
			}
		}

		List<UserPrivacyProfile> profilesToBeDeleted = userPrivacyProfileRepository.deleteValidPrivacyProfiles(userId,
				profileIdList);

		// change connection and connection request to default profile for deleted
		// profile
		List<String> deletedProfilesList = new ArrayList<>();
		Set<String> blockedUsers = sessionUser.getBlockedUserIdList();
		for (UserPrivacyProfile profile : profilesToBeDeleted) {
			deletedProfilesList.add(profile.getPrivacyProfileId());

			List<UserConnection> userConnectionList = userConnectionRepository
					.findConnectionByUserIdAndPrivacyProfileId(sessionUser.getUserId(),
							Collections.singletonList(profile.getPrivacyProfileId()));

			if (userConnectionList != null) {
				// get deleted Meta List
				List<UserProfileData> deletedUserMetaList = masterService.getDeletedMetaList(sessionUser,
						profile.getValueIdList(), defaultProfile.getValueIdList());
				for (UserConnection connection : userConnectionList) {
					if (!blockedUsers.contains(connection.getConnectionFromId())
							&& !PeopleUtils.isNullOrEmpty(deletedUserMetaList)) {
						UserInformationDTO informationDTO = Optional.ofNullable(connection.getContactStaticData())
								.orElse(new UserInformationDTO());
						informationDTO.setUserMetadataList(
								masterService.mergeMetaList(deletedUserMetaList, informationDTO.getUserMetadataList()));
						connection.setContactStaticData(informationDTO);
					}
					SharedProfileInformationData realTimeSharedData = connection.getRealTimeSharedData();
					realTimeSharedData.setPrivacyProfileId(defaultProfileId);
					connection.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
				}
				userConnectionRepository.saveAll(userConnectionList);
			}
		}

		// assigning default profile to connection requests
		userActivityRepository.updateRequestWithDefaultProfile(defaultProfileId, deletedProfilesList);

		DeletePrivacyProfileResponseDTO deletePrivacyProfileResponse = new DeletePrivacyProfileResponseDTO();

		deletePrivacyProfileResponse.setDeletedprofilesList(deletedProfilesList);
		return deletePrivacyProfileResponse;

	}

	@Override
	public CreateOrEditPrivacyProfileResponse createCustomProfile(
			CreateCustomProfileRequestDTO createCustomProfileRequestDTO) {

		PeopleUser sessionUser = tokenAuthService.getSessionUser();

		PrivacyProfileData privacyProfileData = createCustomProfileRequestDTO.getPrivacyProfileData();
		List<UserProfileData> userMetadataList = createCustomProfileRequestDTO.getUserMetadataList();

		checkIfPrivacyProfileNameIsInUse(sessionUser, privacyProfileData);

		// create userPrivacyProfile object
		UserPrivacyProfile userPrivacyProfile = new UserPrivacyProfile();
		userPrivacyProfile.setIsPublic(Boolean.FALSE);
		userPrivacyProfile.setIsSystem(Boolean.FALSE);
		userPrivacyProfile.setIsDefault(Boolean.FALSE);
		userPrivacyProfile.setProfileName(privacyProfileData.getProfileName());
		userPrivacyProfile.setUserId(sessionUser.getUserId());
		userPrivacyProfile.setCreatedOn(PeopleUtils.getCurrentTimeInUTC());
		userPrivacyProfile.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
		// updating ImageURL
		String imageURL = privacyProfileData.getImageURL();
		if (imageURL != null && !imageURL.isEmpty()) {
			userPrivacyProfile.setUseDefaultImage(Boolean.FALSE);
			userPrivacyProfile.setImageURL(imageURL);
		} else {
			userPrivacyProfile.setImageURL(sessionUser.getDefaultImageUrl());
		}

		PeopleUtils.setIfNotNullOrEmpty(userPrivacyProfile::setIsTagShared, privacyProfileData.getIsTagShared());
		PeopleUtils.setIfNotNullOrEmpty(userPrivacyProfile::setIsCompanyShared,
				privacyProfileData.getIsCompanyShared());
		PeopleUtils.setIfNotNullOrEmpty(userPrivacyProfile::setIsPositionShared,
				privacyProfileData.getIsPositionShared());
		PeopleUtils.setIfNotNullOrEmpty(userPrivacyProfile::setIsNickNameShared,
				privacyProfileData.getIsNickNameShared());
		PeopleUtils.setIfNotNullOrEmpty(userPrivacyProfile::setIsMaidenNameShared,
				privacyProfileData.getIsMaidenNameShared());

		List<String> valueIdList = privacyProfileData.getValueIdList();
		if (PeopleUtils.isNullOrEmpty(valueIdList)) {
			valueIdList = new ArrayList<>();
		}

		// create metadata and update valueId
		List<UserProfileData> newDataList = new ArrayList<>();
		if (!PeopleUtils.isNullOrEmpty(userMetadataList)) {
			Map<String, UserProfileData> idMap = createUserMetaData(sessionUser, userMetadataList);
			for (Map.Entry<String, UserProfileData> entry : idMap.entrySet()) {
				valueIdList.add(entry.getKey());
				newDataList.add(entry.getValue());
			}
		}
		userPrivacyProfile.setValueIdList(valueIdList);
		userPrivacyProfile.setShareProfileData(privacyProfileData.getSharePrivacyData());

		UserPrivacyProfile createdPrivacyProfile = userPrivacyProfileRepository.insert(userPrivacyProfile);

		// populate response object
		PrivacyProfileData createdProfileData = new PrivacyProfileData();
		createdProfileData.setProfileName(createdPrivacyProfile.getProfileName());
//        createdProfileData.setImageURL(createdPrivacyProfile.getImageURL());
		createdProfileData.setPrivacyProfileId(createdPrivacyProfile.getPrivacyProfileId());
		createdProfileData.setIsDefault(createdPrivacyProfile.getIsDefault());
		createdProfileData.setIsPublic(createdPrivacyProfile.getIsPublic());
		createdProfileData.setIsSystem(createdPrivacyProfile.getIsSystem());
//        createdProfileData.setIsTagShared(createdPrivacyProfile.getIsTagShared());
//        createdProfileData.setIsCompanyShared(createdPrivacyProfile.getIsCompanyShared());
//        createdProfileData.setIsPositionShared(createdPrivacyProfile.getIsPositionShared());
//        createdProfileData.setIsNameShared(createdPrivacyProfile.getIsNameShared());
//        createdProfileData.setIsNickNameShared(createdPrivacyProfile.getIsNickNameShared());
//        createdProfileData.setIsMaidenNameShared(createdPrivacyProfile.getIsMaidenNameShared());
//        createdProfileData.setValueIdList(createdPrivacyProfile.getValueIdList());

		List<ShareProfileResponse> responses = new ArrayList<>();
		if (createdPrivacyProfile.getShareProfileData().getIsImageURL()) {
			ShareProfileResponse profileResponse1 = new ShareProfileResponse();
			profileResponse1.setLabel("Profile Picture");
			profileResponse1.setKey("isImageURL");
			profileResponse1.setValue(createdPrivacyProfile.getShareProfileData().getIsImageURL());
			responses.add(profileResponse1);
		} else {
			ShareProfileResponse profileResponse1 = new ShareProfileResponse();
			profileResponse1.setLabel("Profile Picture");
			profileResponse1.setKey("isImageURL");
			profileResponse1.setValue(createdPrivacyProfile.getShareProfileData().getIsImageURL());
			responses.add(profileResponse1);
		}

		if (createdPrivacyProfile.getShareProfileData().getIsName()) {
			ShareProfileResponse profileResponse2 = new ShareProfileResponse();
			profileResponse2.setLabel("Name");
			profileResponse2.setKey("isName");
			profileResponse2.setValue(createdPrivacyProfile.getShareProfileData().getIsName());
			responses.add(profileResponse2);
		} else {
			ShareProfileResponse profileResponse2 = new ShareProfileResponse();
			profileResponse2.setLabel("Name");
			profileResponse2.setKey("isName");
			profileResponse2.setValue(createdPrivacyProfile.getShareProfileData().getIsName());
			responses.add(profileResponse2);
		}

		if (createdPrivacyProfile.getShareProfileData().getIsPhoneNumberMobile()) {
			ShareProfileResponse profileResponse3 = new ShareProfileResponse();
			profileResponse3.setLabel("Mobile Phone");
			profileResponse3.setKey("isPhoneNumberMobile");
			profileResponse3.setValue(createdPrivacyProfile.getShareProfileData().getIsPhoneNumberMobile());
			responses.add(profileResponse3);
		} else {
			ShareProfileResponse profileResponse3 = new ShareProfileResponse();
			profileResponse3.setLabel("Mobile Phone");
			profileResponse3.setKey("isPhoneNumberMobile");
			profileResponse3.setValue(createdPrivacyProfile.getShareProfileData().getIsPhoneNumberMobile());
			responses.add(profileResponse3);
		}

		if (createdPrivacyProfile.getShareProfileData().getIsEmailHome()) {
			ShareProfileResponse profileResponse4 = new ShareProfileResponse();
			profileResponse4.setLabel("Mail");
			profileResponse4.setKey("isEmailHome");
			profileResponse4.setValue(createdPrivacyProfile.getShareProfileData().getIsEmailHome());
			responses.add(profileResponse4);
		} else {
			ShareProfileResponse profileResponse4 = new ShareProfileResponse();
			profileResponse4.setLabel("Mail");
			profileResponse4.setKey("isEmailHome");
			profileResponse4.setValue(createdPrivacyProfile.getShareProfileData().getIsEmailHome());
			responses.add(profileResponse4);
		}

		if (createdPrivacyProfile.getShareProfileData().getIsAddressHome()) {
			ShareProfileResponse profileResponse5 = new ShareProfileResponse();
			profileResponse5.setLabel("Address");
			profileResponse5.setKey("isAddressHome");
			profileResponse5.setValue(createdPrivacyProfile.getShareProfileData().getIsAddressHome());
			responses.add(profileResponse5);
		} else {
			ShareProfileResponse profileResponse5 = new ShareProfileResponse();
			profileResponse5.setLabel("Address");
			profileResponse5.setKey("isAddressHome");
			profileResponse5.setValue(createdPrivacyProfile.getShareProfileData().getIsAddressHome());
			responses.add(profileResponse5);
		}

		if (createdPrivacyProfile.getShareProfileData().getIsSocialPeofileTwitter()) {
			ShareProfileResponse profileResponse6 = new ShareProfileResponse();
			profileResponse6.setLabel("Social Media");
			profileResponse6.setKey("isSocialPeofileTwitter");
			profileResponse6.setValue(createdPrivacyProfile.getShareProfileData().getIsSocialPeofileTwitter());
			responses.add(profileResponse6);
		} else {
			ShareProfileResponse profileResponse6 = new ShareProfileResponse();
			profileResponse6.setLabel("Social Media");
			profileResponse6.setKey("isSocialPeofileTwitter");
			profileResponse6.setValue(createdPrivacyProfile.getShareProfileData().getIsSocialPeofileTwitter());
			responses.add(profileResponse6);
		}

		if (createdPrivacyProfile.getShareProfileData().getIsEventBirthday()) {
			ShareProfileResponse profileResponse7 = new ShareProfileResponse();
			profileResponse7.setLabel("Event");
			profileResponse7.setKey("isEventBirthday");
			profileResponse7.setValue(createdPrivacyProfile.getShareProfileData().getIsEventBirthday());
			responses.add(profileResponse7);
		} else {
			ShareProfileResponse profileResponse7 = new ShareProfileResponse();
			profileResponse7.setLabel("Event");
			profileResponse7.setKey("isEventBirthday");
			profileResponse7.setValue(createdPrivacyProfile.getShareProfileData().getIsEventBirthday());
			responses.add(profileResponse7);
		}

		if (createdPrivacyProfile.getShareProfileData().getIsWebsiteHomepage()) {
			ShareProfileResponse profileResponse8 = new ShareProfileResponse();
			profileResponse8.setLabel("Website");
			profileResponse8.setKey("isWebsiteHomepage");
			profileResponse8.setValue(createdPrivacyProfile.getShareProfileData().getIsWebsiteHomepage());
			responses.add(profileResponse8);
		} else {
			ShareProfileResponse profileResponse8 = new ShareProfileResponse();
			profileResponse8.setLabel("Website");
			profileResponse8.setKey("isWebsiteHomepage");
			profileResponse8.setValue(createdPrivacyProfile.getShareProfileData().getIsWebsiteHomepage());
			responses.add(profileResponse8);
		}

		if (createdPrivacyProfile.getShareProfileData().getIsPosition()) {
			ShareProfileResponse profileResponse9 = new ShareProfileResponse();
			profileResponse9.setLabel("Job Title");
			profileResponse9.setKey("isPosition");
			profileResponse9.setValue(createdPrivacyProfile.getShareProfileData().getIsPosition());
			responses.add(profileResponse9);
		} else {
			ShareProfileResponse profileResponse9 = new ShareProfileResponse();
			profileResponse9.setLabel("Job Title");
			profileResponse9.setKey("isPosition");
			profileResponse9.setValue(createdPrivacyProfile.getShareProfileData().getIsPosition());
			responses.add(profileResponse9);
		}

		if (createdPrivacyProfile.getShareProfileData().getIsRelationshipParent()) {
			ShareProfileResponse profileResponse10 = new ShareProfileResponse();
			profileResponse10.setLabel("Relation");
			profileResponse10.setKey("isRelationshipParent");
			profileResponse10.setValue(createdPrivacyProfile.getShareProfileData().getIsRelationshipParent());
			responses.add(profileResponse10);
		} else {
			ShareProfileResponse profileResponse10 = new ShareProfileResponse();
			profileResponse10.setLabel("Relation");
			profileResponse10.setKey("isRelationshipParent");
			profileResponse10.setValue(createdPrivacyProfile.getShareProfileData().getIsRelationshipParent());
			responses.add(profileResponse10);
		}

		if (createdPrivacyProfile.getShareProfileData().getIsCompany()) {
			ShareProfileResponse profileResponse12 = new ShareProfileResponse();
			profileResponse12.setLabel("Company");
			profileResponse12.setKey("isCompany");
			profileResponse12.setValue(createdPrivacyProfile.getShareProfileData().getIsCompany());
			responses.add(profileResponse12);
		} else {
			ShareProfileResponse profileResponse12 = new ShareProfileResponse();
			profileResponse12.setLabel("Company");
			profileResponse12.setKey("isCompany");
			profileResponse12.setValue(createdPrivacyProfile.getShareProfileData().getIsCompany());
			responses.add(profileResponse12);
		}

		createdProfileData.setShareProfileData(responses);
		createdProfileData.setLastUpdatedOn(createdPrivacyProfile.getLastUpdatedOn());
		createdProfileData.setCreatedOn(createdPrivacyProfile.getCreatedOn());

		CreateOrEditPrivacyProfileResponse response = new CreateOrEditPrivacyProfileResponse();

		response.setPrivacyProfileData(createdProfileData);
		response.setUserMetadataList(newDataList);

		return response;
	}

	@Override
	public CreateOrEditPrivacyProfileResponse createCustomProfileNew(
			CreateCustomProfileRequestDTO createCustomProfileRequestDTO) {

		PeopleUser sessionUser = tokenAuthService.getSessionUser();

		PrivacyProfileData privacyProfileData = createCustomProfileRequestDTO.getPrivacyProfileData();
		List<UserProfileData> userMetadataList = createCustomProfileRequestDTO.getUserMetadataList();

		checkIfPrivacyProfileNameIsInUse(sessionUser, privacyProfileData);

		// create userPrivacyProfile object
		UserPrivacyProfile userPrivacyProfile = new UserPrivacyProfile();
		userPrivacyProfile.setIsPublic(Boolean.FALSE);
		userPrivacyProfile.setIsSystem(Boolean.FALSE);
		userPrivacyProfile.setIsDefault(Boolean.FALSE);
		userPrivacyProfile.setProfileName(privacyProfileData.getProfileName());
		userPrivacyProfile.setUserId(sessionUser.getUserId());
		userPrivacyProfile.setCreatedOn(PeopleUtils.getCurrentTimeInUTC());
		userPrivacyProfile.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
		// updating ImageURL
		String imageURL = privacyProfileData.getImageURL();
		if (imageURL != null && !imageURL.isEmpty()) {
			userPrivacyProfile.setUseDefaultImage(Boolean.FALSE);
			userPrivacyProfile.setImageURL(imageURL);
		} else {
			userPrivacyProfile.setImageURL(sessionUser.getDefaultImageUrl());
		}

		PeopleUtils.setIfNotNullOrEmpty(userPrivacyProfile::setIsTagShared, privacyProfileData.getIsTagShared());
		PeopleUtils.setIfNotNullOrEmpty(userPrivacyProfile::setIsCompanyShared,
				privacyProfileData.getIsCompanyShared());
		PeopleUtils.setIfNotNullOrEmpty(userPrivacyProfile::setIsPositionShared,
				privacyProfileData.getIsPositionShared());
		PeopleUtils.setIfNotNullOrEmpty(userPrivacyProfile::setIsNickNameShared,
				privacyProfileData.getIsNickNameShared());
		PeopleUtils.setIfNotNullOrEmpty(userPrivacyProfile::setIsMaidenNameShared,
				privacyProfileData.getIsMaidenNameShared());

		List<String> valueIdList = privacyProfileData.getValueIdList();
		if (PeopleUtils.isNullOrEmpty(valueIdList)) {
			valueIdList = new ArrayList<>();
		}

		// create metadata and update valueId
		List<UserProfileData> newDataList = new ArrayList<>();
		if (!PeopleUtils.isNullOrEmpty(userMetadataList)) {
			Map<String, UserProfileData> idMap = createUserMetaData(sessionUser, userMetadataList);
			for (Map.Entry<String, UserProfileData> entry : idMap.entrySet()) {
				valueIdList.add(entry.getKey());
				newDataList.add(entry.getValue());
			}
		}
		userPrivacyProfile.setValueIdList(valueIdList);
		userPrivacyProfile.setShareProfileData(saveOrEditPrivacySettings(privacyProfileData));
		UserPrivacyProfile createdPrivacyProfile = userPrivacyProfileRepository.insert(userPrivacyProfile);

		// populate response object
		PrivacyProfileData createdProfileData = new PrivacyProfileData();
		createdProfileData.setProfileName(createdPrivacyProfile.getProfileName());
//        createdProfileData.setImageURL(createdPrivacyProfile.getImageURL());
		createdProfileData.setPrivacyProfileId(createdPrivacyProfile.getPrivacyProfileId());
		createdProfileData.setIsDefault(createdPrivacyProfile.getIsDefault());
		createdProfileData.setIsPublic(createdPrivacyProfile.getIsPublic());
		createdProfileData.setIsSystem(createdPrivacyProfile.getIsSystem());
//        createdProfileData.setIsTagShared(createdPrivacyProfile.getIsTagShared());
//        createdProfileData.setIsCompanyShared(createdPrivacyProfile.getIsCompanyShared());
//        createdProfileData.setIsPositionShared(createdPrivacyProfile.getIsPositionShared());
//        createdProfileData.setIsNameShared(createdPrivacyProfile.getIsNameShared());
//        createdProfileData.setIsNickNameShared(createdPrivacyProfile.getIsNickNameShared());
//        createdProfileData.setIsMaidenNameShared(createdPrivacyProfile.getIsMaidenNameShared());
//        createdProfileData.setValueIdList(createdPrivacyProfile.getValueIdList());

		PeopleUser peopleUser = tokenAuthService.getSessionUser();
		createdProfileData.setShareProfileData(userPrivacyResponse(peopleUser, createdPrivacyProfile));
		createdProfileData.setLastUpdatedOn(createdPrivacyProfile.getLastUpdatedOn());
		createdProfileData.setCreatedOn(createdPrivacyProfile.getCreatedOn());

		CreateOrEditPrivacyProfileResponse response = new CreateOrEditPrivacyProfileResponse();

		response.setPrivacyProfileData(createdProfileData);
		response.setUserMetadataList(newDataList);

		return response;
	}

	@Override
	public String setDefaultProfile(SetDefaultProfileRequestDTO setDefaultProfileRequestDTO) {

		UserPrivacyProfile oldDefaultProfile;
		List<UserPrivacyProfile> userPrivacyProfilesList;

		PeopleUser sessionUser = tokenAuthService.getSessionUser();
		String userId = sessionUser.getUserId();

		UserPrivacyProfile profileToSetDefault = userPrivacyProfileRepository
				.findByProfileIdAndUserId(setDefaultProfileRequestDTO.getProfileId(), userId);

		if (profileToSetDefault == null) {
			throw new BadRequestException(MessageCodes.INVALID_PROFILEID.getValue());
		}

		userPrivacyProfilesList = new ArrayList<>();

		oldDefaultProfile = userPrivacyProfileRepository.findDefaultUserProfile(userId, Boolean.TRUE);
		oldDefaultProfile.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
		oldDefaultProfile.setIsDefault(Boolean.FALSE);
		userPrivacyProfilesList.add(oldDefaultProfile);

		profileToSetDefault.setIsDefault(Boolean.TRUE);
		profileToSetDefault.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
		profileToSetDefault.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
		userPrivacyProfilesList.add(profileToSetDefault);

		userPrivacyProfileRepository.saveAll(userPrivacyProfilesList);
		return message.get(MessageConstant.PROFILE_DEFAULT_SET);

	}

	@Override
	public CreateOrEditPrivacyProfileResponse editPrivacyProfile(
			EditPrivacyProfileRequestDTO editPrivacyProfileRequestDTO) {

		logger.info("Inside PeopleUserServiceImpl->editPrivacyProfile");

		PeopleUser sessionUser = tokenAuthService.getSessionUser();

		PrivacyProfileData privacyProfileData = editPrivacyProfileRequestDTO.getPrivacyProfileData();

		List<UserProfileData> userMetadataList = editPrivacyProfileRequestDTO.getUserMetadataList();

		UserPrivacyProfile userPrivacyProfile = userPrivacyProfileRepository
				.findByProfileIdAndUserId(privacyProfileData.getPrivacyProfileId(), sessionUser.getUserId());
		if (userPrivacyProfile == null) {
			throw new BadRequestException(MessageCodes.INVALID_PROFILEID.getValue());
		}

		// check to have unique names for privacy profiles
		if (!userPrivacyProfile.getProfileName().equalsIgnoreCase(privacyProfileData.getProfileName())) {
			checkIfPrivacyProfileNameIsInUse(sessionUser, privacyProfileData);
		}

		// edit userPrivacyProfile object
		if (!userPrivacyProfile.getIsPublic()) {
			userPrivacyProfile.setProfileName(privacyProfileData.getProfileName());
		}
		// updating Default Image Flag if the imageURL is different from the existing
		// one
		if ((privacyProfileData.getImageURL() != null
				&& !privacyProfileData.getImageURL().equals(sessionUser.getDefaultImageUrl()))
				|| (privacyProfileData.getImageURL() == null && sessionUser.getDefaultImageUrl() != null)) {
			userPrivacyProfile.setUseDefaultImage(Boolean.FALSE);
		}

		userPrivacyProfile.setImageURL(privacyProfileData.getImageURL());
		userPrivacyProfile.setUserId(sessionUser.getUserId());
		userPrivacyProfile.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());

		PeopleUtils.setIfNotNullOrEmpty(userPrivacyProfile::setIsTagShared, privacyProfileData.getIsTagShared());
		PeopleUtils.setIfNotNullOrEmpty(userPrivacyProfile::setIsCompanyShared,
				privacyProfileData.getIsCompanyShared());
		PeopleUtils.setIfNotNullOrEmpty(userPrivacyProfile::setIsPositionShared,
				privacyProfileData.getIsPositionShared());
		PeopleUtils.setIfNotNullOrEmpty(userPrivacyProfile::setIsNickNameShared,
				privacyProfileData.getIsNickNameShared());
		PeopleUtils.setIfNotNullOrEmpty(userPrivacyProfile::setIsMaidenNameShared,
				privacyProfileData.getIsMaidenNameShared());

		List<String> valueIdList = privacyProfileData.getValueIdList();

		if (PeopleUtils.isNullOrEmpty(valueIdList)) {
			valueIdList = new ArrayList<>();
		}

		// get deleted Meta List
		List<UserProfileData> deletedUserMetaList = masterService.getDeletedMetaList(sessionUser,
				userPrivacyProfile.getValueIdList(),
				editPrivacyProfileRequestDTO.getPrivacyProfileData().getValueIdList());

		// create metadata and update valueId
		List<UserProfileData> categoryInfoList = new ArrayList<>();
		if (!PeopleUtils.isNullOrEmpty(userMetadataList)) {
			Map<String, UserProfileData> idMap = createUserMetaData(sessionUser, userMetadataList);
			for (Map.Entry<String, UserProfileData> entry : idMap.entrySet()) {
				valueIdList.add(entry.getKey());
				categoryInfoList.add(entry.getValue());
			}
		}
		userPrivacyProfile.setValueIdList(valueIdList);
		userPrivacyProfile.setIsSystem(false);
		userPrivacyProfile.setShareProfileData(privacyProfileData.getSharePrivacyData());
		UserPrivacyProfile editedPrivacyProfile = userPrivacyProfileRepository.save(userPrivacyProfile);

		// populate response object

		PrivacyProfileData editedProfileData = new PrivacyProfileData();
		editedProfileData.setProfileName(editedPrivacyProfile.getProfileName());
//        editedProfileData.setImageURL(editedPrivacyProfile.getImageURL());
		editedProfileData.setPrivacyProfileId(editedPrivacyProfile.getPrivacyProfileId());
		editedProfileData.setIsDefault(editedPrivacyProfile.getIsDefault());
		editedProfileData.setIsPublic(editedPrivacyProfile.getIsPublic());
		editedProfileData.setIsSystem(editedPrivacyProfile.getIsSystem());
//        editedProfileData.setIsTagShared(editedPrivacyProfile.getIsTagShared());
//        editedProfileData.setIsCompanyShared(editedPrivacyProfile.getIsCompanyShared());
//        editedProfileData.setIsPositionShared(editedPrivacyProfile.getIsPositionShared());
//        editedProfileData.setIsNameShared(editedPrivacyProfile.getIsNameShared());
//        editedProfileData.setIsNickNameShared(editedPrivacyProfile.getIsNickNameShared());
//        editedProfileData.setIsMaidenNameShared(editedPrivacyProfile.getIsMaidenNameShared());
//        editedProfileData.setValueIdList(editedPrivacyProfile.getValueIdList());
		editedProfileData.setLastUpdatedOn(editedPrivacyProfile.getLastUpdatedOn());
		editedProfileData.setCreatedOn(editedPrivacyProfile.getCreatedOn());

		List<ShareProfileResponse> responses = new ArrayList<>();
		if (editedPrivacyProfile.getShareProfileData().getIsImageURL()) {
			ShareProfileResponse profileResponse1 = new ShareProfileResponse();
			profileResponse1.setLabel("Profile Picture");
			profileResponse1.setKey("isImageURL");
			profileResponse1.setValue(editedPrivacyProfile.getShareProfileData().getIsImageURL());
			responses.add(profileResponse1);
		} else {
			ShareProfileResponse profileResponse1 = new ShareProfileResponse();
			profileResponse1.setLabel("Profile Picture");
			profileResponse1.setKey("isImageURL");
			profileResponse1.setValue(editedPrivacyProfile.getShareProfileData().getIsImageURL());
			responses.add(profileResponse1);
		}

		if (editedPrivacyProfile.getShareProfileData().getIsName()) {
			ShareProfileResponse profileResponse2 = new ShareProfileResponse();
			profileResponse2.setLabel("Name");
			profileResponse2.setKey("isName");
			profileResponse2.setValue(editedPrivacyProfile.getShareProfileData().getIsName());
			responses.add(profileResponse2);
		} else {
			ShareProfileResponse profileResponse2 = new ShareProfileResponse();
			profileResponse2.setLabel("Name");
			profileResponse2.setKey("isName");
			profileResponse2.setValue(editedPrivacyProfile.getShareProfileData().getIsName());
			responses.add(profileResponse2);
		}

		if (editedPrivacyProfile.getShareProfileData().getIsPhoneNumberMobile()) {
			ShareProfileResponse profileResponse3 = new ShareProfileResponse();
			profileResponse3.setLabel("Mobile Phone");
			profileResponse3.setKey("isPhoneNumberMobile");
			profileResponse3.setValue(editedPrivacyProfile.getShareProfileData().getIsPhoneNumberMobile());
			responses.add(profileResponse3);
		} else {
			ShareProfileResponse profileResponse3 = new ShareProfileResponse();
			profileResponse3.setLabel("Mobile Phone");
			profileResponse3.setKey("isPhoneNumberMobile");
			profileResponse3.setValue(editedPrivacyProfile.getShareProfileData().getIsPhoneNumberMobile());
			responses.add(profileResponse3);
		}

		if (editedPrivacyProfile.getShareProfileData().getIsEmailHome()) {
			ShareProfileResponse profileResponse4 = new ShareProfileResponse();
			profileResponse4.setLabel("Mail");
			profileResponse4.setKey("isEmailHome");
			profileResponse4.setValue(editedPrivacyProfile.getShareProfileData().getIsEmailHome());
			responses.add(profileResponse4);
		} else {
			ShareProfileResponse profileResponse4 = new ShareProfileResponse();
			profileResponse4.setLabel("Mail");
			profileResponse4.setKey("isEmailHome");
			profileResponse4.setValue(editedPrivacyProfile.getShareProfileData().getIsEmailHome());
			responses.add(profileResponse4);
		}

		if (editedPrivacyProfile.getShareProfileData().getIsAddressHome()) {
			ShareProfileResponse profileResponse5 = new ShareProfileResponse();
			profileResponse5.setLabel("Address");
			profileResponse5.setKey("isAddressHome");
			profileResponse5.setValue(editedPrivacyProfile.getShareProfileData().getIsAddressHome());
			responses.add(profileResponse5);
		} else {
			ShareProfileResponse profileResponse5 = new ShareProfileResponse();
			profileResponse5.setLabel("Address");
			profileResponse5.setKey("isAddressHome");
			profileResponse5.setValue(editedPrivacyProfile.getShareProfileData().getIsAddressHome());
			responses.add(profileResponse5);
		}

		if (editedPrivacyProfile.getShareProfileData().getIsSocialPeofileTwitter()) {
			ShareProfileResponse profileResponse6 = new ShareProfileResponse();
			profileResponse6.setLabel("Social Media");
			profileResponse6.setKey("isSocialPeofileTwitter");
			profileResponse6.setValue(editedPrivacyProfile.getShareProfileData().getIsSocialPeofileTwitter());
			responses.add(profileResponse6);
		} else {
			ShareProfileResponse profileResponse6 = new ShareProfileResponse();
			profileResponse6.setLabel("Social Media");
			profileResponse6.setKey("isSocialPeofileTwitter");
			profileResponse6.setValue(editedPrivacyProfile.getShareProfileData().getIsSocialPeofileTwitter());
			responses.add(profileResponse6);
		}

		if (editedPrivacyProfile.getShareProfileData().getIsEventBirthday()) {
			ShareProfileResponse profileResponse7 = new ShareProfileResponse();
			profileResponse7.setLabel("Event");
			profileResponse7.setKey("isEventBirthday");
			profileResponse7.setValue(editedPrivacyProfile.getShareProfileData().getIsEventBirthday());
			responses.add(profileResponse7);
		} else {
			ShareProfileResponse profileResponse7 = new ShareProfileResponse();
			profileResponse7.setLabel("Event");
			profileResponse7.setKey("isEventBirthday");
			profileResponse7.setValue(editedPrivacyProfile.getShareProfileData().getIsEventBirthday());
			responses.add(profileResponse7);
		}

		if (editedPrivacyProfile.getShareProfileData().getIsWebsiteHomepage()) {
			ShareProfileResponse profileResponse8 = new ShareProfileResponse();
			profileResponse8.setLabel("Website");
			profileResponse8.setKey("isWebsiteHomepage");
			profileResponse8.setValue(editedPrivacyProfile.getShareProfileData().getIsWebsiteHomepage());
			responses.add(profileResponse8);
		} else {
			ShareProfileResponse profileResponse8 = new ShareProfileResponse();
			profileResponse8.setLabel("Website");
			profileResponse8.setKey("isWebsiteHomepage");
			profileResponse8.setValue(editedPrivacyProfile.getShareProfileData().getIsWebsiteHomepage());
			responses.add(profileResponse8);
		}

		if (editedPrivacyProfile.getShareProfileData().getIsPosition()) {
			ShareProfileResponse profileResponse9 = new ShareProfileResponse();
			profileResponse9.setLabel("Job Title");
			profileResponse9.setKey("isPosition");
			profileResponse9.setValue(editedPrivacyProfile.getShareProfileData().getIsPosition());
			responses.add(profileResponse9);
		} else {
			ShareProfileResponse profileResponse9 = new ShareProfileResponse();
			profileResponse9.setLabel("Job Title");
			profileResponse9.setKey("isPosition");
			profileResponse9.setValue(editedPrivacyProfile.getShareProfileData().getIsPosition());
			responses.add(profileResponse9);
		}

		if (editedPrivacyProfile.getShareProfileData().getIsRelationshipParent()) {
			ShareProfileResponse profileResponse10 = new ShareProfileResponse();
			profileResponse10.setLabel("Relation");
			profileResponse10.setKey("isRelationshipParent");
			profileResponse10.setValue(editedPrivacyProfile.getShareProfileData().getIsRelationshipParent());
			responses.add(profileResponse10);
		} else {
			ShareProfileResponse profileResponse10 = new ShareProfileResponse();
			profileResponse10.setLabel("Relation");
			profileResponse10.setKey("isRelationshipParent");
			profileResponse10.setValue(editedPrivacyProfile.getShareProfileData().getIsRelationshipParent());
			responses.add(profileResponse10);
		}

		if (editedPrivacyProfile.getShareProfileData().getIsCompany()) {
			ShareProfileResponse profileResponse12 = new ShareProfileResponse();
			profileResponse12.setLabel("Company");
			profileResponse12.setKey("isCompany");
			profileResponse12.setValue(editedPrivacyProfile.getShareProfileData().getIsCompany());
			responses.add(profileResponse12);
		} else {
			ShareProfileResponse profileResponse12 = new ShareProfileResponse();
			profileResponse12.setLabel("Company");
			profileResponse12.setKey("isCompany");
			profileResponse12.setValue(editedPrivacyProfile.getShareProfileData().getIsCompany());
			responses.add(profileResponse12);
		}

		editedProfileData.setShareProfileData(responses);

		CreateOrEditPrivacyProfileResponse response = new CreateOrEditPrivacyProfileResponse();
		response.setPrivacyProfileData(editedProfileData);
		response.setUserMetadataList(categoryInfoList);

		List<SQSPayload> sqsPayloadList = new ArrayList<>();
		List<UserConnection> userConnections = userConnectionRepository.findConnectionByUserIdAndPrivacyProfileId(
				sessionUser.getUserId(), Arrays.asList(editedPrivacyProfile.getPrivacyProfileId()));
		Set<String> blockedUsers = sessionUser.getBlockedUserIdList();
		for (UserConnection userConnection : userConnections) {

			/* Blocked users should not be notified about any profile changes */
			if (blockedUsers.contains(userConnection.getConnectionFromId())) {
				continue;
			} else if (!PeopleUtils.isNullOrEmpty(deletedUserMetaList)) {
				UserInformationDTO staticInformationDTO = Optional.ofNullable(userConnection.getContactStaticData())
						.orElse(new UserInformationDTO());
				staticInformationDTO.setUserMetadataList(
						masterService.mergeMetaList(deletedUserMetaList, staticInformationDTO.getUserMetadataList()));
				userConnection.setContactStaticData(staticInformationDTO);
			}
			// Create activity for a particular contact
			UserActivity userActivity = new UserActivity();
			userActivity.setActivityForId(userConnection.getConnectionFromId());
			userActivity.setActivityById(userConnection.getConnectionToId());
			ActivityType activityType = new ActivityType();
			activityType.setRequestType(RequestType.UPDATE_CONTACT_ACTIVITY);
			activityType.setActionTaken(Action.INITIATED);
			userActivity.setActivityType(activityType);
			userActivity.setOverallStatus(ActivityStatus.ACTIVE);

			DateTime currentDateTime = PeopleUtils.getCurrentTimeInUTC();
			userActivity.setCreatedOn(currentDateTime);
			userActivity.setLastUpdatedOn(currentDateTime);

			List<UserActivity> userActivities = userActivityRepository
					.getPendingActivitiesByInitiatedByIdAndRequestType(userConnection.getConnectionFromId(),
							RequestType.UPDATE_CONTACT_ACTIVITY);

			if (userActivities != null) {
				for (UserActivity activity : PeopleUtils.emptyIfNull(userActivities)) {
					userActivityRepository.deleteById(activity.getActivityId());
				}
			}

			userActivityRepository.save(userActivity);

			sqsPayloadList.add(prepareSQSPayloadForUpdateContactActivity(userActivity, sessionUser,
					userConnection.getConnectionId()));
		}

		userConnectionRepository.saveAll(userConnections);
		queueService.sendPayloadToSQS(sqsPayloadList);
		return response;

	}

	@Override
	public CreateOrEditPrivacyProfileResponse editPrivacyProfileNew(
			EditPrivacyProfileRequestDTO editPrivacyProfileRequestDTO) {

		logger.info("Inside PeopleUserServiceImpl->editPrivacyProfile");

		PeopleUser sessionUser = tokenAuthService.getSessionUser();

		PrivacyProfileData privacyProfileData = editPrivacyProfileRequestDTO.getPrivacyProfileData();

		List<UserProfileData> userMetadataList = editPrivacyProfileRequestDTO.getUserMetadataList();

		UserPrivacyProfile userPrivacyProfile = userPrivacyProfileRepository
				.findByProfileIdAndUserId(privacyProfileData.getPrivacyProfileId(), sessionUser.getUserId());
		if (userPrivacyProfile == null) {
			throw new BadRequestException(MessageCodes.INVALID_PROFILEID.getValue());
		}

		// check to have unique names for privacy profiles
		if (!userPrivacyProfile.getProfileName().equalsIgnoreCase(privacyProfileData.getProfileName())) {
			checkIfPrivacyProfileNameIsInUse(sessionUser, privacyProfileData);
		}

		// edit userPrivacyProfile object
		if (!userPrivacyProfile.getIsPublic()) {
			userPrivacyProfile.setProfileName(privacyProfileData.getProfileName());
		}
		// updating Default Image Flag if the imageURL is different from the existing
		// one
		if ((privacyProfileData.getImageURL() != null
				&& !privacyProfileData.getImageURL().equals(sessionUser.getDefaultImageUrl()))
				|| (privacyProfileData.getImageURL() == null && sessionUser.getDefaultImageUrl() != null)) {
			userPrivacyProfile.setUseDefaultImage(Boolean.FALSE);
		}

		userPrivacyProfile.setImageURL(privacyProfileData.getImageURL());
		userPrivacyProfile.setUserId(sessionUser.getUserId());
		userPrivacyProfile.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());

		PeopleUtils.setIfNotNullOrEmpty(userPrivacyProfile::setIsTagShared, privacyProfileData.getIsTagShared());
		PeopleUtils.setIfNotNullOrEmpty(userPrivacyProfile::setIsCompanyShared,
				privacyProfileData.getIsCompanyShared());
		PeopleUtils.setIfNotNullOrEmpty(userPrivacyProfile::setIsPositionShared,
				privacyProfileData.getIsPositionShared());
		PeopleUtils.setIfNotNullOrEmpty(userPrivacyProfile::setIsNickNameShared,
				privacyProfileData.getIsNickNameShared());
		PeopleUtils.setIfNotNullOrEmpty(userPrivacyProfile::setIsMaidenNameShared,
				privacyProfileData.getIsMaidenNameShared());

		List<String> valueIdList = privacyProfileData.getValueIdList();

		if (PeopleUtils.isNullOrEmpty(valueIdList)) {
			valueIdList = new ArrayList<>();
		}

		// get deleted Meta List
		List<UserProfileData> deletedUserMetaList = masterService.getDeletedMetaList(sessionUser,
				userPrivacyProfile.getValueIdList(),
				editPrivacyProfileRequestDTO.getPrivacyProfileData().getValueIdList());

		// create metadata and update valueId
		List<UserProfileData> categoryInfoList = new ArrayList<>();
		if (!PeopleUtils.isNullOrEmpty(userMetadataList)) {
			Map<String, UserProfileData> idMap = createUserMetaData(sessionUser, userMetadataList);
			for (Map.Entry<String, UserProfileData> entry : idMap.entrySet()) {
				valueIdList.add(entry.getKey());
				categoryInfoList.add(entry.getValue());
			}
		}
		userPrivacyProfile.setValueIdList(valueIdList);
		userPrivacyProfile.setIsSystem(false);
		userPrivacyProfile.setShareProfileData(saveOrEditPrivacySettings(privacyProfileData));
		UserPrivacyProfile editedPrivacyProfile = userPrivacyProfileRepository.save(userPrivacyProfile);

		// populate response object

		PrivacyProfileData editedProfileData = new PrivacyProfileData();
		editedProfileData.setProfileName(editedPrivacyProfile.getProfileName());
//        editedProfileData.setImageURL(editedPrivacyProfile.getImageURL());
		editedProfileData.setPrivacyProfileId(editedPrivacyProfile.getPrivacyProfileId());
		editedProfileData.setIsDefault(editedPrivacyProfile.getIsDefault());
		editedProfileData.setIsPublic(editedPrivacyProfile.getIsPublic());
		editedProfileData.setIsSystem(editedPrivacyProfile.getIsSystem());
//        editedProfileData.setIsTagShared(editedPrivacyProfile.getIsTagShared());
//        editedProfileData.setIsCompanyShared(editedPrivacyProfile.getIsCompanyShared());
//        editedProfileData.setIsPositionShared(editedPrivacyProfile.getIsPositionShared());
//        editedProfileData.setIsNameShared(editedPrivacyProfile.getIsNameShared());
//        editedProfileData.setIsNickNameShared(editedPrivacyProfile.getIsNickNameShared());
//        editedProfileData.setIsMaidenNameShared(editedPrivacyProfile.getIsMaidenNameShared());
//        editedProfileData.setValueIdList(editedPrivacyProfile.getValueIdList());
		editedProfileData.setLastUpdatedOn(editedPrivacyProfile.getLastUpdatedOn());
		editedProfileData.setCreatedOn(editedPrivacyProfile.getCreatedOn());

		PeopleUser peopleUser = tokenAuthService.getSessionUser();
		editedProfileData.setShareProfileData(userPrivacyResponse(peopleUser, editedPrivacyProfile));
		CreateOrEditPrivacyProfileResponse response = new CreateOrEditPrivacyProfileResponse();
		response.setPrivacyProfileData(editedProfileData);
		response.setUserMetadataList(categoryInfoList);

		List<SQSPayload> sqsPayloadList = new ArrayList<>();
		List<UserConnection> userConnections = userConnectionRepository.findConnectionByUserIdAndPrivacyProfileId(
				sessionUser.getUserId(), Arrays.asList(editedPrivacyProfile.getPrivacyProfileId()));
		Set<String> blockedUsers = sessionUser.getBlockedUserIdList();
		for (UserConnection userConnection : userConnections) {

			/* Blocked users should not be notified about any profile changes */
			if (blockedUsers.contains(userConnection.getConnectionFromId())) {
				continue;
			} else if (!PeopleUtils.isNullOrEmpty(deletedUserMetaList)) {
				UserInformationDTO staticInformationDTO = Optional.ofNullable(userConnection.getContactStaticData())
						.orElse(new UserInformationDTO());
				staticInformationDTO.setUserMetadataList(
						masterService.mergeMetaList(deletedUserMetaList, staticInformationDTO.getUserMetadataList()));
				userConnection.setContactStaticData(staticInformationDTO);
			}
			// Create activity for a particular contact
			UserActivity userActivity = new UserActivity();
			userActivity.setActivityForId(userConnection.getConnectionFromId());
			userActivity.setActivityById(userConnection.getConnectionToId());
			ActivityType activityType = new ActivityType();
			activityType.setRequestType(RequestType.UPDATE_CONTACT_ACTIVITY);
			activityType.setActionTaken(Action.INITIATED);
			userActivity.setActivityType(activityType);
			userActivity.setOverallStatus(ActivityStatus.ACTIVE);

			DateTime currentDateTime = PeopleUtils.getCurrentTimeInUTC();
			userActivity.setCreatedOn(currentDateTime);
			userActivity.setLastUpdatedOn(currentDateTime);

			List<UserActivity> userActivities = userActivityRepository
					.getPendingActivitiesByInitiatedByIdAndRequestType(userConnection.getConnectionFromId(),
							RequestType.UPDATE_CONTACT_ACTIVITY);

			if (userActivities != null) {
				for (UserActivity activity : PeopleUtils.emptyIfNull(userActivities)) {
					userActivityRepository.deleteById(activity.getActivityId());
				}
			}

			userActivityRepository.save(userActivity);
			sqsPayloadList.add(prepareSQSPayloadForUpdateContactActivity(userActivity, sessionUser,
					userConnection.getConnectionId()));
		}

		userConnectionRepository.saveAll(userConnections);
		queueService.sendPayloadToSQS(sqsPayloadList);
		return response;

	}

	@Override
	public UserPrivacyProfile getPrivacyProfileById(String privacyProfileId) {
		UserPrivacyProfile userPrivacyProfile = null;
		Optional<UserPrivacyProfile> privacyProfile = userPrivacyProfileRepository.findById(privacyProfileId);
		if (privacyProfile.isPresent()) {
			userPrivacyProfile = privacyProfile.get();
		}
		return userPrivacyProfile;
	}

	@Override
	public PrivacyProfileData populatePrivacyProfileData(UserPrivacyProfile userProfile, String defaultImageURL) {

		String imageURL;

		PrivacyProfileData privacyProfileData = new PrivacyProfileData();
		imageURL = userProfile.getImageURL();
		if (PeopleUtils.isNullOrEmpty(imageURL)) {
			imageURL = defaultImageURL;
		}
//        privacyProfileData.setImageURL(imageURL);
		privacyProfileData.setPrivacyProfileId(userProfile.getPrivacyProfileId());
		privacyProfileData.setProfileName(userProfile.getProfileName());
		privacyProfileData.setIsDefault(userProfile.getIsDefault());
		privacyProfileData.setIsPublic(userProfile.getIsPublic());
		privacyProfileData.setIsSystem(userProfile.getIsSystem());

//        privacyProfileData.setValueIdList(userProfile.getValueIdList());
//        privacyProfileData.setIsNameShared(userProfile.getIsNameShared());
//        privacyProfileData.setIsNickNameShared(userProfile.getIsNickNameShared());
//        privacyProfileData.setIsMaidenNameShared(userProfile.getIsMaidenNameShared());
//        privacyProfileData.setIsCompanyShared(userProfile.getIsCompanyShared());
//        privacyProfileData.setIsPositionShared(userProfile.getIsPositionShared());
//        privacyProfileData.setIsTagShared(userProfile.getIsTagShared());

		PeopleUser peopleUser = tokenAuthService.getSessionUser();
		privacyProfileData.setShareProfileData(userPrivacyResponse(peopleUser, userProfile));
		privacyProfileData.setCreatedOn(userProfile.getCreatedOn());

		return privacyProfileData;
	}

	@Override
	public void shareTag(ShareTagRequest shareTagRequest) {

		PeopleUser sessionUser = tokenAuthService.getSessionUser();

		UserPrivacyProfile userPrivacyProfile = getPrivacyProfileById(shareTagRequest.getPrivacyProfileId());
		if (!userPrivacyProfile.getUserId().equals(sessionUser.getUserId())) {
			throw new BadRequestException(MessageCodes.INVALID_PROFILEID.getValue());
		}

		userPrivacyProfile.setIsTagShared(Boolean.TRUE);
		userPrivacyProfileRepository.save(userPrivacyProfile);
	}

	@Override
	public SQSPayload prepareSQSPayloadForUpdateContactActivity(UserActivity userActivity, PeopleUser sessionUser,
			String connectionId) {
		if (!masterService.isPushNotificationEnabledForUser(userActivity.getActivityForId())) {
			return null;
		}
		PushNotificationDTO pushNotificationDTO = new PushNotificationDTO();
		pushNotificationDTO.setActivityId(userActivity.getActivityId());
		pushNotificationDTO.setActivityRequestType(userActivity.getActivityType().getRequestType());
		pushNotificationDTO.setInitiatorName(PeopleUtils.getDefaultOrEmpty(sessionUser.getFullName()));
		pushNotificationDTO.setReceiverUserId(userActivity.getActivityForId());
		pushNotificationDTO.setActivityMessage(userActivity.getMessage());
		pushNotificationDTO.setConnectionId(connectionId);

		return notificationService.prepareSQSPayloadForNotification(userActivity.getActivityForId(),
				pushNotificationDTO);

	}

	private Map<String, UserProfileData> createUserMetaData(PeopleUser peopleUser, List<UserProfileData> newDataList) {

		Map<String, UserProfileData> valueIdMap = new HashMap<>();
		List<UserProfileData> existingMetadata = peopleUser.getUserMetadataList();
		for (UserProfileData newData : PeopleUtils.emptyIfNull(newDataList)) {

			String valueId = new ObjectId().toString();
			newData.setValueId(valueId);
			newData.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
			existingMetadata.add(newData);
			valueIdMap.put(valueId, newData);
		}
		peopleUser.setUserMetadataList(existingMetadata);
		peopleUserRepository.save(peopleUser);

		return valueIdMap;
	}

	/* This method populates the predefined system profiles for a particular user */
	private List<UserPrivacyProfile> createSystemProfileDataForUser(PeopleUser peopleUser) {

		logger.info("Inside PrivacyProfileServiceImpl->createSystemProfileDataForUser ");

		List<UserPrivacyProfile> userPrivacyProfileList = new ArrayList<>();

		// fetch all pre defined system profiles
		List<SystemPrivacyProfile> systemProfilesList = systemProfileRepository.findAll();

		// fetch each system profile and create user privacy profile object
		systemProfilesList.forEach(systemProfile -> userPrivacyProfileList
				.add(createUserProfileFromSystemProfile(systemProfile, peopleUser)));

		return userPrivacyProfileRepository.saveAll(userPrivacyProfileList);
	}

	private UserPrivacyProfile createUserProfileFromSystemProfile(SystemPrivacyProfile systemProfile,
			PeopleUser peopleUser) {

		logger.info("Inside PrivacyProfileServiceImpl->createUserPrivacyProfileObj");

		UserPrivacyProfile userPrivacyProfile = new UserPrivacyProfile();
		userPrivacyProfile.setUserId(peopleUser.getUserId());
		userPrivacyProfile.setProfileDesc(systemProfile.getProfileName());
		userPrivacyProfile.setImageURL(null);
		userPrivacyProfile.setProfileName(systemProfile.getProfileName());
		userPrivacyProfile.setIsDefault(systemProfile.getIsDefault());
		userPrivacyProfile.setIsSystem(Boolean.TRUE);
		userPrivacyProfile.setIsPublic(systemProfile.getIsPublic());

		// Ghanshyam Panchal Code Start
		ShareProfileData shareProfileData = new ShareProfileData();

		if (systemProfile.getProfileName().equalsIgnoreCase("Acquaintance")) {
			if (peopleUser != null) {
				if (peopleUser.getFullName() != null && !peopleUser.getFullName().trim().isEmpty()) {
					shareProfileData.setIsName(true);
				}
				if (peopleUser.getDefaultImageUrl() != null && !peopleUser.getDefaultImageUrl().trim().isEmpty()) {
					shareProfileData.setIsImageURL(true);
				}
				if (peopleUser.getUserMetadataList() != null) {
					for (UserProfileData profileData : PeopleUtils.emptyIfNull(peopleUser.getUserMetadataList())) {
						if (profileData.getLabel().equalsIgnoreCase("PL.00.00")) {
							shareProfileData.setIsPhoneNumberMobile(true);
						}
					}
				}
			}
		}

		if (systemProfile.getProfileName().equalsIgnoreCase("Professional")) {
			if (peopleUser != null) {
				if (peopleUser.getFullName() != null && !peopleUser.getFullName().trim().isEmpty()) {
					shareProfileData.setIsName(true);
				}
				if (peopleUser.getDefaultImageUrl() != null && !peopleUser.getDefaultImageUrl().trim().isEmpty()) {
					shareProfileData.setIsImageURL(true);
				}
				if (peopleUser.getPositionValue() != null && !peopleUser.getPositionValue().trim().isEmpty()) {
					shareProfileData.setIsPosition(true);
				}
				if (peopleUser.getCompanyValue() != null && !peopleUser.getCompanyValue().trim().isEmpty()) {
					shareProfileData.setIsCompany(true);
				}
				if (peopleUser.getDepartment() != null && !peopleUser.getDepartment().trim().isEmpty()) {
					shareProfileData.setIsDepartment(true);
				}
				if (peopleUser.getUserMetadataList() != null) {
					for (UserProfileData profileData : PeopleUtils.emptyIfNull(peopleUser.getUserMetadataList())) {
						if (profileData.getLabel().equalsIgnoreCase("PL.00.02")) {
							shareProfileData.setIsPhoneNumberWork(true);
						}
						if (profileData.getLabel().equalsIgnoreCase("PL.01.01")) {
							shareProfileData.setIsEmailWork(true);
						}
						if (profileData.getLabel().equalsIgnoreCase("PL.05.00")) {
							shareProfileData.setIsWebsiteHomepage(true);
						}
					}
				}
			}
		}

		if (systemProfile.getProfileName().equalsIgnoreCase("Friends")) {
			if (peopleUser != null) {
				if (peopleUser.getFullName() != null && !peopleUser.getFullName().trim().isEmpty()) {
					shareProfileData.setIsName(true);
				}
				if (peopleUser.getDefaultImageUrl() != null && !peopleUser.getDefaultImageUrl().trim().isEmpty()) {
					shareProfileData.setIsImageURL(true);
				}
				if (peopleUser.getUserMetadataList() != null) {
					for (UserProfileData profileData : PeopleUtils.emptyIfNull(peopleUser.getUserMetadataList())) {
						if (profileData.getLabel().equalsIgnoreCase("PL.00.00")) {
							shareProfileData.setIsPhoneNumberMobile(true);
						}
						if (profileData.getLabel().equalsIgnoreCase("PL.03.00")) {
							shareProfileData.setIsAddressHome(true);
						}
						if (profileData.getLabel().equalsIgnoreCase("PL.02.00")) {
							shareProfileData.setIsSocialPeofileTwitter(true);
						}
					}
				}
			}
		}

		if (systemProfile.getProfileName().equalsIgnoreCase("Family")) {
			if (peopleUser != null) {
				if (peopleUser.getFullName() != null && !peopleUser.getFullName().trim().isEmpty()) {
					shareProfileData.setIsName(true);
				}
				if (peopleUser.getDefaultImageUrl() != null && !peopleUser.getDefaultImageUrl().trim().isEmpty()) {
					shareProfileData.setIsImageURL(true);
				}
				if (peopleUser.getUserMetadataList() != null) {
					for (UserProfileData profileData : PeopleUtils.emptyIfNull(peopleUser.getUserMetadataList())) {
						if (profileData.getLabel().equalsIgnoreCase("PL.00.00")) {
							shareProfileData.setIsPhoneNumberMobile(true);
						}
						if (profileData.getLabel().equalsIgnoreCase("PL.03.00")) {
							shareProfileData.setIsAddressHome(true);
						}
						if (profileData.getLabel().equalsIgnoreCase("PL.06.00")) {
							shareProfileData.setIsRelationshipParent(true);
						}
						if (profileData.getLabel().equalsIgnoreCase("PL.02.00")) {
							shareProfileData.setIsSocialPeofileTwitter(true);
						}
					}
				}
			}
		}

		if (systemProfile.getProfileName().equalsIgnoreCase("Public")) {
			if (peopleUser != null) {
				if (peopleUser.getFullName() != null && !peopleUser.getFullName().trim().isEmpty()) {
					shareProfileData.setIsName(true);
				}
				if (peopleUser.getUserMetadataList() != null) {
					for (UserProfileData profileData : PeopleUtils.emptyIfNull(peopleUser.getUserMetadataList())) {
						if (profileData.getLabel().equalsIgnoreCase("PL.00.00")) {
							shareProfileData.setIsPhoneNumberMobile(true);
						}
					}
				}
			}
		}
		userPrivacyProfile.setShareProfileData(shareProfileData);
		// Ghanshyam Panchal Code End

		userPrivacyProfile.setCreatedOn(PeopleUtils.getCurrentTimeInUTC());
		userPrivacyProfile.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());

		if (systemProfile.getProfileName().equalsIgnoreCase("Professional")) {
			userPrivacyProfile.setIsCompanyShared(true);
			userPrivacyProfile.setIsPositionShared(true);
		}

		Map<ProfileKey, List<String>> profileKeyListMap = peopleUser.getProfileKeyMap();
		List<String> valueIdList = new ArrayList<>();
		for (ProfileKey profileKey : PeopleUtils.emptyIfNull(systemProfile.getProfileKeyList())) {
			if (profileKeyListMap.containsKey(profileKey)) {
				valueIdList.addAll(profileKeyListMap.get(profileKey));
			}
		}

		userPrivacyProfile.setValueIdList(valueIdList);

		return userPrivacyProfile;
	}

	private List<PrivacyProfileData> populatePrivacyProfileData(List<UserPrivacyProfile> userPrivacyProfileList,
			String defaultImageURL) {

		List<PrivacyProfileData> privacyProfileDataList = new ArrayList<>();
		for (UserPrivacyProfile userProfile : PeopleUtils.emptyIfNull(userPrivacyProfileList)) {
			privacyProfileDataList.add(populatePrivacyProfileData(userProfile, defaultImageURL));
		}

		return privacyProfileDataList;
	}

	private void checkIfPrivacyProfileNameIsInUse(PeopleUser sessionUser, PrivacyProfileData privacyProfileData) {
		// Adding the check to have unique names for privacy profiles
		List<UserPrivacyProfile> listOfCustomProfiles = userPrivacyProfileRepository
				.findAllByUserId(sessionUser.getUserId());
		Set<String> profileNames = new HashSet<>();
		for (UserPrivacyProfile userPrivacyProfile : listOfCustomProfiles) {
			profileNames.add(userPrivacyProfile.getProfileName());
		}
		if (profileNames.contains(privacyProfileData.getProfileName())) {
			throw new BadRequestException(MessageCodes.CUSTOM_PRIVACY_PROFILE_NAME_ALREADY_EXISTS.getValue());
		}
	}

	private List<ShareProfileResponse> userPrivacyResponse(PeopleUser peopleUser, UserPrivacyProfile userProfile) {

		List<ShareProfileResponse> responses = new ArrayList<>();
		List<ShareProfileResponse> phoneNumberCatgoryResponse = new ArrayList<>();
		List<ShareProfileResponse> emailCatgoryResponse = new ArrayList<>();
		List<ShareProfileResponse> socialMediaCatgoryResponse = new ArrayList<>();
		List<ShareProfileResponse> instantMessagingCatgoryResponse = new ArrayList<>();
		List<ShareProfileResponse> addressCatgoryResponse = new ArrayList<>();
		List<ShareProfileResponse> eventCatgoryResponse = new ArrayList<>();
		List<ShareProfileResponse> websiteCatgoryResponse = new ArrayList<>();
		List<ShareProfileResponse> relationshipCatgoryResponse = new ArrayList<>();
		List<ShareProfileResponse> otherResponse = new ArrayList<>();

		if (peopleUser != null) {
			if (peopleUser.getFullName() != null && !peopleUser.getFullName().trim().isEmpty()) {
				if (userProfile.getShareProfileData().getIsName()) {
					ShareProfileResponse profileResponse1 = new ShareProfileResponse();
					profileResponse1.setLabel("Name");
					profileResponse1.setKey("isName");
					profileResponse1.setView_detail(peopleUser.getNamePrefix() + " " + peopleUser.getFirstNameValue()
							+ " " + peopleUser.getMiddleNameValue() + " " + peopleUser.getLastNameValue() + " "
							+ peopleUser.getNameSuffix());
					profileResponse1.setValue(userProfile.getShareProfileData().getIsName());
					responses.add(profileResponse1);
				} else {
					ShareProfileResponse profileResponse1 = new ShareProfileResponse();
					profileResponse1.setLabel("Name");
					profileResponse1.setKey("isName");
					profileResponse1.setView_detail(peopleUser.getNamePrefix() + " " + peopleUser.getFirstNameValue()
							+ " " + peopleUser.getMiddleNameValue() + " " + peopleUser.getLastNameValue() + " "
							+ peopleUser.getNameSuffix());
					profileResponse1.setValue(userProfile.getShareProfileData().getIsName());
					responses.add(profileResponse1);
				}
			}
			if (peopleUser.getGender() != null && !peopleUser.getGender().trim().isEmpty()) {
				if (userProfile.getShareProfileData().getIsGender()) {
					ShareProfileResponse profileResponse2 = new ShareProfileResponse();
					profileResponse2.setLabel("Gender");
					profileResponse2.setKey("isGender");
					profileResponse2.setView_detail(PeopleUtils.getDefaultOrEmpty(peopleUser.getGender()));
					profileResponse2.setValue(userProfile.getShareProfileData().getIsGender());
					responses.add(profileResponse2);
				} else {
					ShareProfileResponse profileResponse2 = new ShareProfileResponse();
					profileResponse2.setLabel("Gender");
					profileResponse2.setKey("isGender");
					profileResponse2.setView_detail(PeopleUtils.getDefaultOrEmpty(peopleUser.getGender()));
					profileResponse2.setValue(userProfile.getShareProfileData().getIsGender());
					responses.add(profileResponse2);
				}
			}
			if (peopleUser.getCompanyValue() != null && !peopleUser.getCompanyValue().trim().isEmpty()) {
				if (userProfile.getShareProfileData().getIsCompany()) {
					ShareProfileResponse profileResponse3 = new ShareProfileResponse();
					profileResponse3.setLabel("Company");
					profileResponse3.setKey("isCompany");
					profileResponse3.setView_detail(PeopleUtils.getDefaultOrEmpty(peopleUser.getCompanyValue()));
					profileResponse3.setValue(userProfile.getShareProfileData().getIsCompany());
					responses.add(profileResponse3);
				} else {
					ShareProfileResponse profileResponse3 = new ShareProfileResponse();
					profileResponse3.setLabel("Company");
					profileResponse3.setKey("isCompany");
					profileResponse3.setView_detail(PeopleUtils.getDefaultOrEmpty(peopleUser.getCompanyValue()));
					profileResponse3.setValue(userProfile.getShareProfileData().getIsCompany());
					responses.add(profileResponse3);
				}
			}
			if (peopleUser.getDepartment() != null && !peopleUser.getDepartment().trim().isEmpty()) {
				if (userProfile.getShareProfileData().getIsDepartment()) {
					ShareProfileResponse profileResponse4 = new ShareProfileResponse();
					profileResponse4.setLabel("Department");
					profileResponse4.setKey("isDepartment");
					profileResponse4.setView_detail(PeopleUtils.getDefaultOrEmpty(peopleUser.getDepartment()));
					profileResponse4.setValue(userProfile.getShareProfileData().getIsDepartment());
					responses.add(profileResponse4);
				} else {
					ShareProfileResponse profileResponse4 = new ShareProfileResponse();
					profileResponse4.setLabel("Department");
					profileResponse4.setKey("isDepartment");
					profileResponse4.setView_detail(PeopleUtils.getDefaultOrEmpty(peopleUser.getDepartment()));
					profileResponse4.setValue(userProfile.getShareProfileData().getIsDepartment());
					responses.add(profileResponse4);
				}
			}
			if (peopleUser.getPositionValue() != null && !peopleUser.getPositionValue().trim().isEmpty()) {
				if (userProfile.getShareProfileData().getIsPosition()) {
					ShareProfileResponse profileResponse5 = new ShareProfileResponse();
					profileResponse5.setLabel("Job Title");
					profileResponse5.setKey("isPosition");
					profileResponse5.setView_detail(PeopleUtils.getDefaultOrEmpty(peopleUser.getPositionValue()));
					profileResponse5.setValue(userProfile.getShareProfileData().getIsPosition());
					responses.add(profileResponse5);
				} else {
					ShareProfileResponse profileResponse5 = new ShareProfileResponse();
					profileResponse5.setLabel("Job Title");
					profileResponse5.setKey("isPosition");
					profileResponse5.setView_detail(PeopleUtils.getDefaultOrEmpty(peopleUser.getPositionValue()));
					profileResponse5.setValue(userProfile.getShareProfileData().getIsPosition());
					responses.add(profileResponse5);
				}
			}
			if (peopleUser.getDefaultImageUrl() != null && !peopleUser.getDefaultImageUrl().trim().isEmpty()) {
				if (userProfile.getShareProfileData().getIsImageURL()) {
					ShareProfileResponse profileResponse6 = new ShareProfileResponse();
					profileResponse6.setLabel("Profile Picture");
					profileResponse6.setKey("isImageURL");
					profileResponse6.setView_detail(PeopleUtils.getDefaultOrEmpty(peopleUser.getDefaultImageUrl()));
					profileResponse6.setValue(userProfile.getShareProfileData().getIsImageURL());
					responses.add(profileResponse6);
				} else {
					ShareProfileResponse profileResponse6 = new ShareProfileResponse();
					profileResponse6.setLabel("Profile Picture");
					profileResponse6.setKey("isImageURL");
					profileResponse6.setView_detail(PeopleUtils.getDefaultOrEmpty(peopleUser.getDefaultImageUrl()));
					profileResponse6.setValue(userProfile.getShareProfileData().getIsImageURL());
					responses.add(profileResponse6);
				}
			}
			if (peopleUser.getNotes() != null && !peopleUser.getNotes().trim().isEmpty()) {
				if (userProfile.getShareProfileData().getIsNotes()) {
					ShareProfileResponse profileResponse7 = new ShareProfileResponse();
					profileResponse7.setLabel("Notes");
					profileResponse7.setKey("isNotes");
					profileResponse7.setView_detail(PeopleUtils.getDefaultOrEmpty(peopleUser.getNotes()));
					profileResponse7.setValue(userProfile.getShareProfileData().getIsNotes());
					responses.add(profileResponse7);
				} else {
					ShareProfileResponse profileResponse7 = new ShareProfileResponse();
					profileResponse7.setLabel("Notes");
					profileResponse7.setKey("isNotes");
					profileResponse7.setView_detail(PeopleUtils.getDefaultOrEmpty(peopleUser.getNotes()));
					profileResponse7.setValue(userProfile.getShareProfileData().getIsNotes());
					responses.add(profileResponse7);
				}
			}
			if (!peopleUser.getTagMap().isEmpty()) {
				if (userProfile.getShareProfileData().getIsTagList()) {
					ShareProfileResponse profileResponse8 = new ShareProfileResponse();
					profileResponse8.setLabel("Tags");
					profileResponse8.setKey("isTagList");
					profileResponse8.setView_detail(
							PeopleUtils.getDefaultOrEmpty(peopleUser.getProfileTags().toString().replace("[", "") // remove
																													// the
																													// right
																													// bracket
									.replace("]", "") // remove the left bracket
									.trim()));
					profileResponse8.setValue(userProfile.getShareProfileData().getIsTagList());
					responses.add(profileResponse8);
				} else {
					ShareProfileResponse profileResponse8 = new ShareProfileResponse();
					profileResponse8.setLabel("Tags");
					profileResponse8.setKey("isTagList");
					profileResponse8.setView_detail(
							PeopleUtils.getDefaultOrEmpty(peopleUser.getProfileTags().toString().replace("[", "") // remove
																													// the
																													// right
																													// bracket
									.replace("]", "") // remove the left bracket
									.trim()));
					profileResponse8.setValue(userProfile.getShareProfileData().getIsTagList());
					responses.add(profileResponse8);
				}
			}
			if (peopleUser.getUserMetadataList() != null) {
				for (UserProfileData profileData : PeopleUtils.emptyIfNull(peopleUser.getUserMetadataList())) {
					if (profileData.getLabel().equalsIgnoreCase("PL.00.00")) {
						if (userProfile.getShareProfileData().getIsPhoneNumberMobile()) {
							ShareProfileResponse profileResponse9 = new ShareProfileResponse();
							profileResponse9.setLabel("Mobile Phone");
							profileResponse9.setKey("isPhoneNumberMobile");
							profileResponse9.setValue(userProfile.getShareProfileData().getIsPhoneNumberMobile());
							profileResponse9.setView_detail(
									PeopleUtils.convertMobileListToString(profileData.getKeyValueDataList()));
							phoneNumberCatgoryResponse.add(profileResponse9);
						} else {
							ShareProfileResponse profileResponse9 = new ShareProfileResponse();
							profileResponse9.setLabel("Mobile Phone");
							profileResponse9.setKey("isPhoneNumberMobile");
							profileResponse9.setValue(userProfile.getShareProfileData().getIsPhoneNumberMobile());
							profileResponse9.setView_detail(
									PeopleUtils.convertMobileListToString(profileData.getKeyValueDataList()));
							phoneNumberCatgoryResponse.add(profileResponse9);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.00.01")) {
						if (userProfile.getShareProfileData().getIsPhoneNumberHome()) {
							ShareProfileResponse profileResponse10 = new ShareProfileResponse();
							profileResponse10.setLabel("Home Mobile Phone");
							profileResponse10.setKey("isPhoneNumberHome");
							profileResponse10.setView_detail(
									PeopleUtils.convertMobileListToString(profileData.getKeyValueDataList()));
							profileResponse10.setValue(userProfile.getShareProfileData().getIsPhoneNumberHome());
							phoneNumberCatgoryResponse.add(profileResponse10);
						} else {
							ShareProfileResponse profileResponse10 = new ShareProfileResponse();
							profileResponse10.setLabel("Home Mobile Phone");
							profileResponse10.setKey("isPhoneNumberHome");
							profileResponse10.setView_detail(
									PeopleUtils.convertMobileListToString(profileData.getKeyValueDataList()));
							profileResponse10.setValue(userProfile.getShareProfileData().getIsPhoneNumberHome());
							phoneNumberCatgoryResponse.add(profileResponse10);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.00.02")) {
						if (userProfile.getShareProfileData().getIsPhoneNumberWork()) {
							ShareProfileResponse profileResponse11 = new ShareProfileResponse();
							profileResponse11.setLabel("Work Mobile Phone");
							profileResponse11.setKey("isPhoneNumberWork");
							profileResponse11.setView_detail(
									PeopleUtils.convertMobileListToString(profileData.getKeyValueDataList()));
							profileResponse11.setValue(userProfile.getShareProfileData().getIsPhoneNumberWork());
							phoneNumberCatgoryResponse.add(profileResponse11);
						} else {
							ShareProfileResponse profileResponse11 = new ShareProfileResponse();
							profileResponse11.setLabel("Work Mobile Phone");
							profileResponse11.setKey("isPhoneNumberWork");
							profileResponse11.setView_detail(
									PeopleUtils.convertMobileListToString(profileData.getKeyValueDataList()));
							profileResponse11.setValue(userProfile.getShareProfileData().getIsPhoneNumberWork());
							phoneNumberCatgoryResponse.add(profileResponse11);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.00.03")) {
						if (userProfile.getShareProfileData().getIsPhoneNumberIphone()) {
							ShareProfileResponse profileResponse12 = new ShareProfileResponse();
							profileResponse12.setLabel("iPhone Mobile Phone");
							profileResponse12.setKey("isPhoneNumberIphone");
							profileResponse12.setView_detail(
									PeopleUtils.convertMobileListToString(profileData.getKeyValueDataList()));
							profileResponse12.setValue(userProfile.getShareProfileData().getIsPhoneNumberIphone());
							phoneNumberCatgoryResponse.add(profileResponse12);
						} else {
							ShareProfileResponse profileResponse12 = new ShareProfileResponse();
							profileResponse12.setLabel("iPhone Mobile Phone");
							profileResponse12.setKey("isPhoneNumberIphone");
							profileResponse12.setView_detail(
									PeopleUtils.convertMobileListToString(profileData.getKeyValueDataList()));
							profileResponse12.setValue(userProfile.getShareProfileData().getIsPhoneNumberIphone());
							phoneNumberCatgoryResponse.add(profileResponse12);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.00.04")) {
						if (userProfile.getShareProfileData().getIsPhoneNumberMain()) {
							ShareProfileResponse profileResponse13 = new ShareProfileResponse();
							profileResponse13.setLabel("Main Mobile Phone");
							profileResponse13.setKey("isPhoneNumberMain");
							profileResponse13.setView_detail(
									PeopleUtils.convertMobileListToString(profileData.getKeyValueDataList()));
							profileResponse13.setValue(userProfile.getShareProfileData().getIsPhoneNumberMain());
							phoneNumberCatgoryResponse.add(profileResponse13);
						} else {
							ShareProfileResponse profileResponse13 = new ShareProfileResponse();
							profileResponse13.setLabel("Main Mobile Phone");
							profileResponse13.setKey("isPhoneNumberMain");
							profileResponse13.setView_detail(
									PeopleUtils.convertMobileListToString(profileData.getKeyValueDataList()));
							profileResponse13.setValue(userProfile.getShareProfileData().getIsPhoneNumberMain());
							phoneNumberCatgoryResponse.add(profileResponse13);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.00.05")) {
						if (userProfile.getShareProfileData().getIsPhoneNumberHomeFax()) {
							ShareProfileResponse profileResponse14 = new ShareProfileResponse();
							profileResponse14.setLabel("Home Fax Mobile Phone");
							profileResponse14.setKey("isPhoneNumberHomeFax");
							profileResponse14.setView_detail(
									PeopleUtils.convertMobileListToString(profileData.getKeyValueDataList()));
							profileResponse14.setValue(userProfile.getShareProfileData().getIsPhoneNumberHomeFax());
							phoneNumberCatgoryResponse.add(profileResponse14);
						} else {
							ShareProfileResponse profileResponse14 = new ShareProfileResponse();
							profileResponse14.setLabel("Home Fax Mobile Phone");
							profileResponse14.setKey("isPhoneNumberHomeFax");
							profileResponse14.setView_detail(
									PeopleUtils.convertMobileListToString(profileData.getKeyValueDataList()));
							profileResponse14.setValue(userProfile.getShareProfileData().getIsPhoneNumberHomeFax());
							phoneNumberCatgoryResponse.add(profileResponse14);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.00.06")) {
						if (userProfile.getShareProfileData().getIsPhoneNumberWorkFax()) {
							ShareProfileResponse profileResponse15 = new ShareProfileResponse();
							profileResponse15.setLabel("Work Fax Mobile Phone");
							profileResponse15.setKey("isPhoneNumberWorkFax");
							profileResponse15.setView_detail(
									PeopleUtils.convertMobileListToString(profileData.getKeyValueDataList()));
							profileResponse15.setValue(userProfile.getShareProfileData().getIsPhoneNumberWorkFax());
							phoneNumberCatgoryResponse.add(profileResponse15);
						} else {
							ShareProfileResponse profileResponse15 = new ShareProfileResponse();
							profileResponse15.setLabel("Work Fax Mobile Phone");
							profileResponse15.setKey("isPhoneNumberWorkFax");
							profileResponse15.setView_detail(
									PeopleUtils.convertMobileListToString(profileData.getKeyValueDataList()));
							profileResponse15.setValue(userProfile.getShareProfileData().getIsPhoneNumberWorkFax());
							phoneNumberCatgoryResponse.add(profileResponse15);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.00.07")) {
						if (userProfile.getShareProfileData().getIsPhoneNumberPager()) {
							ShareProfileResponse profileResponse16 = new ShareProfileResponse();
							profileResponse16.setLabel("Pager Mobile Phone");
							profileResponse16.setKey("isPhoneNumberPager");
							profileResponse16.setView_detail(
									PeopleUtils.convertMobileListToString(profileData.getKeyValueDataList()));
							profileResponse16.setValue(userProfile.getShareProfileData().getIsPhoneNumberPager());
							phoneNumberCatgoryResponse.add(profileResponse16);
						} else {
							ShareProfileResponse profileResponse16 = new ShareProfileResponse();
							profileResponse16.setLabel("Pager Mobile Phone");
							profileResponse16.setKey("isPhoneNumberPager");
							profileResponse16.setView_detail(
									PeopleUtils.convertMobileListToString(profileData.getKeyValueDataList()));
							profileResponse16.setValue(userProfile.getShareProfileData().getIsPhoneNumberPager());
							phoneNumberCatgoryResponse.add(profileResponse16);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.00.08")) {
						if (userProfile.getShareProfileData().getIsPhoneNumberOther()) {
							ShareProfileResponse profileResponse17 = new ShareProfileResponse();
							profileResponse17.setLabel("Other Mobile Phone");
							profileResponse17.setKey("isPhoneNumberOther");
							profileResponse17.setView_detail(
									PeopleUtils.convertMobileListToString(profileData.getKeyValueDataList()));
							profileResponse17.setValue(userProfile.getShareProfileData().getIsPhoneNumberOther());
							phoneNumberCatgoryResponse.add(profileResponse17);
						} else {
							ShareProfileResponse profileResponse17 = new ShareProfileResponse();
							profileResponse17.setLabel("Other Mobile Phone");
							profileResponse17.setKey("isPhoneNumberOther");
							profileResponse17.setView_detail(
									PeopleUtils.convertMobileListToString(profileData.getKeyValueDataList()));
							profileResponse17.setValue(userProfile.getShareProfileData().getIsPhoneNumberOther());
							phoneNumberCatgoryResponse.add(profileResponse17);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.00.09")) {
						if (userProfile.getShareProfileData().getIsPhoneNumberPersonal()) {
							ShareProfileResponse profileResponse18 = new ShareProfileResponse();
							profileResponse18.setLabel("Personal Mobile Phone");
							profileResponse18.setKey("isPhoneNumberPersonal");
							profileResponse18.setView_detail(
									PeopleUtils.convertMobileListToString(profileData.getKeyValueDataList()));
							profileResponse18.setValue(userProfile.getShareProfileData().getIsPhoneNumberPersonal());
							phoneNumberCatgoryResponse.add(profileResponse18);
						} else {
							ShareProfileResponse profileResponse18 = new ShareProfileResponse();
							profileResponse18.setLabel("Personal Mobile Phone");
							profileResponse18.setKey("isPhoneNumberPersonal");
							profileResponse18.setView_detail(
									PeopleUtils.convertMobileListToString(profileData.getKeyValueDataList()));
							profileResponse18.setValue(userProfile.getShareProfileData().getIsPhoneNumberPersonal());
							phoneNumberCatgoryResponse.add(profileResponse18);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.00.10")) {
						if (userProfile.getShareProfileData().getIsPhoneNumberLandLine()) {
							ShareProfileResponse profileResponse19 = new ShareProfileResponse();
							profileResponse19.setLabel("Land Line Mobile Phone");
							profileResponse19.setKey("isPhoneNumberLandLine");
							profileResponse19.setView_detail(
									PeopleUtils.convertMobileListToString(profileData.getKeyValueDataList()));
							profileResponse19.setValue(userProfile.getShareProfileData().getIsPhoneNumberLandLine());
							phoneNumberCatgoryResponse.add(profileResponse19);
						} else {
							ShareProfileResponse profileResponse19 = new ShareProfileResponse();
							profileResponse19.setLabel("Land Line Mobile Phone");
							profileResponse19.setKey("isPhoneNumberLandLine");
							profileResponse19.setView_detail(
									PeopleUtils.convertMobileListToString(profileData.getKeyValueDataList()));
							profileResponse19.setValue(userProfile.getShareProfileData().getIsPhoneNumberLandLine());
							phoneNumberCatgoryResponse.add(profileResponse19);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.01.00")) {
						if (userProfile.getShareProfileData().getIsEmailHome()) {
							ShareProfileResponse profileResponse20 = new ShareProfileResponse();
							profileResponse20.setLabel("Home Email");
							profileResponse20.setKey("isEmailHome");
							profileResponse20.setValue(userProfile.getShareProfileData().getIsEmailHome());
							profileResponse20.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							emailCatgoryResponse.add(profileResponse20);
						} else {
							ShareProfileResponse profileResponse20 = new ShareProfileResponse();
							profileResponse20.setLabel("Home Email");
							profileResponse20.setKey("isEmailHome");
							profileResponse20.setValue(userProfile.getShareProfileData().getIsEmailHome());
							profileResponse20.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							emailCatgoryResponse.add(profileResponse20);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.01.01")) {
						if (userProfile.getShareProfileData().getIsEmailWork()) {
							ShareProfileResponse profileResponse21 = new ShareProfileResponse();
							profileResponse21.setLabel("Work Email");
							profileResponse21.setKey("isEmailWork");
							profileResponse21.setValue(userProfile.getShareProfileData().getIsEmailWork());
							profileResponse21.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							emailCatgoryResponse.add(profileResponse21);
						} else {
							ShareProfileResponse profileResponse21 = new ShareProfileResponse();
							profileResponse21.setLabel("Work Email");
							profileResponse21.setKey("isEmailWork");
							profileResponse21.setValue(userProfile.getShareProfileData().getIsEmailWork());
							profileResponse21.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							emailCatgoryResponse.add(profileResponse21);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.01.02")) {
						if (userProfile.getShareProfileData().getIsEmailiCloud()) {
							ShareProfileResponse profileResponse22 = new ShareProfileResponse();
							profileResponse22.setLabel("iCloud Email");
							profileResponse22.setKey("isEmailiCloud");
							profileResponse22.setValue(userProfile.getShareProfileData().getIsEmailiCloud());
							profileResponse22.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							emailCatgoryResponse.add(profileResponse22);
						} else {
							ShareProfileResponse profileResponse22 = new ShareProfileResponse();
							profileResponse22.setLabel("iCloud Email");
							profileResponse22.setKey("isEmailiCloud");
							profileResponse22.setValue(userProfile.getShareProfileData().getIsEmailiCloud());
							profileResponse22.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							emailCatgoryResponse.add(profileResponse22);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.01.03")) {
						if (userProfile.getShareProfileData().getIsEmailOther()) {
							ShareProfileResponse profileResponse23 = new ShareProfileResponse();
							profileResponse23.setLabel("Other Email");
							profileResponse23.setKey("isEmailOther");
							profileResponse23.setValue(userProfile.getShareProfileData().getIsEmailOther());
							profileResponse23.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							emailCatgoryResponse.add(profileResponse23);
						} else {
							ShareProfileResponse profileResponse23 = new ShareProfileResponse();
							profileResponse23.setLabel("Other Email");
							profileResponse23.setKey("isEmailOther");
							profileResponse23.setValue(userProfile.getShareProfileData().getIsEmailOther());
							profileResponse23.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							emailCatgoryResponse.add(profileResponse23);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.01.04")) {
						if (userProfile.getShareProfileData().getIsEmailPersonal()) {
							ShareProfileResponse profileResponse24 = new ShareProfileResponse();
							profileResponse24.setLabel("Personal Email");
							profileResponse24.setKey("isEmailPersonal");
							profileResponse24.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							profileResponse24.setValue(userProfile.getShareProfileData().getIsEmailPersonal());
							emailCatgoryResponse.add(profileResponse24);
						} else {
							ShareProfileResponse profileResponse24 = new ShareProfileResponse();
							profileResponse24.setLabel("Personal Email");
							profileResponse24.setKey("isEmailPersonal");
							profileResponse24.setValue(userProfile.getShareProfileData().getIsEmailPersonal());
							profileResponse24.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							emailCatgoryResponse.add(profileResponse24);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.02.00")) {
						if (userProfile.getShareProfileData().getIsSocialPeofileTwitter()) {
							ShareProfileResponse profileResponse25 = new ShareProfileResponse();
							profileResponse25.setLabel("Twitter");
							profileResponse25.setKey("isSocialPeofileTwitter");
							profileResponse25.setValue(userProfile.getShareProfileData().getIsSocialPeofileTwitter());
							profileResponse25.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse25);
						} else {
							ShareProfileResponse profileResponse25 = new ShareProfileResponse();
							profileResponse25.setLabel("Twitter");
							profileResponse25.setKey("isSocialPeofileTwitter");
							profileResponse25.setValue(userProfile.getShareProfileData().getIsSocialPeofileTwitter());
							profileResponse25.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse25);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.02.01")) {
						if (userProfile.getShareProfileData().getIsSocialPeofileLinkedIn()) {
							ShareProfileResponse profileResponse26 = new ShareProfileResponse();
							profileResponse26.setLabel("LinkedIn");
							profileResponse26.setKey("isSocialPeofileLinkedIn");
							profileResponse26.setValue(userProfile.getShareProfileData().getIsSocialPeofileLinkedIn());
							profileResponse26.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse26);
						} else {
							ShareProfileResponse profileResponse26 = new ShareProfileResponse();
							profileResponse26.setLabel("LinkedIn");
							profileResponse26.setKey("isSocialPeofileLinkedIn");
							profileResponse26.setValue(userProfile.getShareProfileData().getIsSocialPeofileLinkedIn());
							profileResponse26.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse26);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.02.02")) {
						if (userProfile.getShareProfileData().getIsSocialPeofileFlickr()) {
							ShareProfileResponse profileResponse27 = new ShareProfileResponse();
							profileResponse27.setLabel("Flickr");
							profileResponse27.setKey("isSocialPeofileFlickr");
							profileResponse27.setValue(userProfile.getShareProfileData().getIsSocialPeofileFlickr());
							profileResponse27.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse27);
						} else {
							ShareProfileResponse profileResponse27 = new ShareProfileResponse();
							profileResponse27.setLabel("Flickr");
							profileResponse27.setKey("isSocialPeofileFlickr");
							profileResponse27.setValue(userProfile.getShareProfileData().getIsSocialPeofileFlickr());
							profileResponse27.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse27);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.02.03")) {
						if (userProfile.getShareProfileData().getIsSocialPeofileFacebook()) {
							ShareProfileResponse profileResponse28 = new ShareProfileResponse();
							profileResponse28.setLabel("Facebook");
							profileResponse28.setKey("isSocialPeofileFacebook");
							profileResponse28.setValue(userProfile.getShareProfileData().getIsSocialPeofileFacebook());
							profileResponse28.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse28);
						} else {
							ShareProfileResponse profileResponse28 = new ShareProfileResponse();
							profileResponse28.setLabel("Facebook");
							profileResponse28.setKey("isSocialPeofileFacebook");
							profileResponse28.setValue(userProfile.getShareProfileData().getIsSocialPeofileFacebook());
							profileResponse28.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse28);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.02.04")) {
						if (userProfile.getShareProfileData().getIsSocialPeofileMyspace()) {
							ShareProfileResponse profileResponse29 = new ShareProfileResponse();
							profileResponse29.setLabel("Myspace");
							profileResponse29.setKey("isSocialPeofileMyspace");
							profileResponse29.setValue(userProfile.getShareProfileData().getIsSocialPeofileMyspace());
							profileResponse29.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse29);
						} else {
							ShareProfileResponse profileResponse29 = new ShareProfileResponse();
							profileResponse29.setLabel("Myspace");
							profileResponse29.setKey("isSocialPeofileMyspace");
							profileResponse29.setValue(userProfile.getShareProfileData().getIsSocialPeofileMyspace());
							profileResponse29.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse29);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.02.05")) {
						if (userProfile.getShareProfileData().getIsSocialPeofileSinaWeibo()) {
							ShareProfileResponse profileResponse30 = new ShareProfileResponse();
							profileResponse30.setLabel("Sina Weibo");
							profileResponse30.setKey("isSocialPeofileSinaWeibo");
							profileResponse30.setValue(userProfile.getShareProfileData().getIsSocialPeofileSinaWeibo());
							profileResponse30.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse30);
						} else {
							ShareProfileResponse profileResponse30 = new ShareProfileResponse();
							profileResponse30.setLabel("Sina Weibo");
							profileResponse30.setKey("isSocialPeofileSinaWeibo");
							profileResponse30.setValue(userProfile.getShareProfileData().getIsSocialPeofileSinaWeibo());
							profileResponse30.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse30);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.02.06")) {
						if (userProfile.getShareProfileData().getIsSocialPeofileInstagram()) {
							ShareProfileResponse profileResponse31 = new ShareProfileResponse();
							profileResponse31.setLabel("Instagram");
							profileResponse31.setKey("isSocialPeofileInstagram");
							profileResponse31.setValue(userProfile.getShareProfileData().getIsSocialPeofileInstagram());
							profileResponse31.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse31);
						} else {
							ShareProfileResponse profileResponse31 = new ShareProfileResponse();
							profileResponse31.setLabel("Instagram");
							profileResponse31.setKey("isSocialPeofileInstagram");
							profileResponse31.setValue(userProfile.getShareProfileData().getIsSocialPeofileInstagram());
							profileResponse31.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse31);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.02.07")) {
						if (userProfile.getShareProfileData().getIsSocialPeofileSnapchat()) {
							ShareProfileResponse profileResponse32 = new ShareProfileResponse();
							profileResponse32.setLabel("Snapchat");
							profileResponse32.setKey("isSocialPeofileSnapchat");
							profileResponse32.setValue(userProfile.getShareProfileData().getIsSocialPeofileSnapchat());
							profileResponse32.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse32);
						} else {
							ShareProfileResponse profileResponse32 = new ShareProfileResponse();
							profileResponse32.setLabel("Snapchat");
							profileResponse32.setKey("isSocialPeofileSnapchat");
							profileResponse32.setValue(userProfile.getShareProfileData().getIsSocialPeofileSnapchat());
							profileResponse32.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse32);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.02.08")) {
						if (userProfile.getShareProfileData().getIsSocialPeofileReddit()) {
							ShareProfileResponse profileResponse33 = new ShareProfileResponse();
							profileResponse33.setLabel("Reddit");
							profileResponse33.setKey("isSocialPeofileReddit");
							profileResponse33.setValue(userProfile.getShareProfileData().getIsSocialPeofileReddit());
							profileResponse33.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse33);
						} else {
							ShareProfileResponse profileResponse33 = new ShareProfileResponse();
							profileResponse33.setLabel("Reddit");
							profileResponse33.setKey("isSocialPeofileReddit");
							profileResponse33.setValue(userProfile.getShareProfileData().getIsSocialPeofileReddit());
							profileResponse33.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse33);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.02.09")) {
						if (userProfile.getShareProfileData().getIsSocialPeofileImgur()) {
							ShareProfileResponse profileResponse34 = new ShareProfileResponse();
							profileResponse34.setLabel("Imgur");
							profileResponse34.setKey("isSocialPeofileImgur");
							profileResponse34.setValue(userProfile.getShareProfileData().getIsSocialPeofileImgur());
							profileResponse34.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse34);
						} else {
							ShareProfileResponse profileResponse34 = new ShareProfileResponse();
							profileResponse34.setLabel("Imgur");
							profileResponse34.setKey("isSocialPeofileImgur");
							profileResponse34.setValue(userProfile.getShareProfileData().getIsSocialPeofileImgur());
							profileResponse34.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse34);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.02.10")) {
						if (userProfile.getShareProfileData().getIsSocialPeofileGoogle()) {
							ShareProfileResponse profileResponse35 = new ShareProfileResponse();
							profileResponse35.setLabel("Google");
							profileResponse35.setKey("isSocialPeofileGoogle");
							profileResponse35.setValue(userProfile.getShareProfileData().getIsSocialPeofileGoogle());
							profileResponse35.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse35);
						} else {
							ShareProfileResponse profileResponse35 = new ShareProfileResponse();
							profileResponse35.setLabel("Google");
							profileResponse35.setKey("isSocialPeofileGoogle");
							profileResponse35.setValue(userProfile.getShareProfileData().getIsSocialPeofileGoogle());
							profileResponse35.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse35);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.02.11")) {
						if (userProfile.getShareProfileData().getIsSocialPeofileSquareCash()) {
							ShareProfileResponse profileResponse36 = new ShareProfileResponse();
							profileResponse36.setLabel("SquareCash");
							profileResponse36.setKey("isSocialPeofileSquareCash");
							profileResponse36
									.setValue(userProfile.getShareProfileData().getIsSocialPeofileSquareCash());
							profileResponse36.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse36);
						} else {
							ShareProfileResponse profileResponse36 = new ShareProfileResponse();
							profileResponse36.setLabel("SquareCash");
							profileResponse36.setKey("isSocialPeofileSquareCash");
							profileResponse36
									.setValue(userProfile.getShareProfileData().getIsSocialPeofileSquareCash());
							profileResponse36.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse36);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.02.12")) {
						if (userProfile.getShareProfileData().getIsSocialPeofileTiktok()) {
							ShareProfileResponse profileResponse37 = new ShareProfileResponse();
							profileResponse37.setLabel("Tiktok");
							profileResponse37.setKey("isSocialPeofileTiktok");
							profileResponse37.setValue(userProfile.getShareProfileData().getIsSocialPeofileTiktok());
							profileResponse37.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse37);
						} else {
							ShareProfileResponse profileResponse37 = new ShareProfileResponse();
							profileResponse37.setLabel("Tiktok");
							profileResponse37.setKey("isSocialPeofileTiktok");
							profileResponse37.setValue(userProfile.getShareProfileData().getIsSocialPeofileTiktok());
							profileResponse37.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse37);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.02.13")) {
						if (userProfile.getShareProfileData().getIsSocialPeofileWhatsapp()) {
							ShareProfileResponse profileResponse38 = new ShareProfileResponse();
							profileResponse38.setLabel("Whatsapp");
							profileResponse38.setKey("isSocialPeofileWhatsapp");
							profileResponse38.setValue(userProfile.getShareProfileData().getIsSocialPeofileWhatsapp());
							profileResponse38.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse38);
						} else {
							ShareProfileResponse profileResponse38 = new ShareProfileResponse();
							profileResponse38.setLabel("Whatsapp");
							profileResponse38.setKey("isSocialPeofileWhatsapp");
							profileResponse38.setValue(userProfile.getShareProfileData().getIsSocialPeofileWhatsapp());
							profileResponse38.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse38);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.02.14")) {
						if (userProfile.getShareProfileData().getIsSocialPeofileYoutube()) {
							ShareProfileResponse profileResponse39 = new ShareProfileResponse();
							profileResponse39.setLabel("Youtube");
							profileResponse39.setKey("isSocialPeofileYoutube");
							profileResponse39.setValue(userProfile.getShareProfileData().getIsSocialPeofileYoutube());
							profileResponse39.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse39);
						} else {
							ShareProfileResponse profileResponse39 = new ShareProfileResponse();
							profileResponse39.setLabel("Youtube");
							profileResponse39.setKey("isSocialPeofileYoutube");
							profileResponse39.setValue(userProfile.getShareProfileData().getIsSocialPeofileYoutube());
							profileResponse39.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse39);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.02.15")) {
						if (userProfile.getShareProfileData().getIsSocialPeofileFacebookMessenger()) {
							ShareProfileResponse profileResponse40 = new ShareProfileResponse();
							profileResponse40.setLabel("Facebook Messenger");
							profileResponse40.setKey("isSocialPeofileFacebookMessenger");
							profileResponse40
									.setValue(userProfile.getShareProfileData().getIsSocialPeofileFacebookMessenger());
							profileResponse40.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse40);
						} else {
							ShareProfileResponse profileResponse40 = new ShareProfileResponse();
							profileResponse40.setLabel("Facebook Messenger");
							profileResponse40.setKey("isSocialPeofileFacebookMessenger");
							profileResponse40
									.setValue(userProfile.getShareProfileData().getIsSocialPeofileFacebookMessenger());
							profileResponse40.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							socialMediaCatgoryResponse.add(profileResponse40);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.03.00")) {
						if (userProfile.getShareProfileData().getIsAddressHome()) {
							ShareProfileResponse profileResponse41 = new ShareProfileResponse();
							profileResponse41.setLabel("Home Address");
							profileResponse41.setKey("isAddressHome");
							profileResponse41.setValue(userProfile.getShareProfileData().getIsAddressHome());
							profileResponse41.setView_detail(
									PeopleUtils.convertAddressListToString(profileData.getKeyValueDataList()));
							addressCatgoryResponse.add(profileResponse41);
						} else {
							ShareProfileResponse profileResponse41 = new ShareProfileResponse();
							profileResponse41.setLabel("Home Address");
							profileResponse41.setKey("isAddressHome");
							profileResponse41.setValue(userProfile.getShareProfileData().getIsAddressHome());
							profileResponse41.setView_detail(
									PeopleUtils.convertAddressListToString(profileData.getKeyValueDataList()));
							addressCatgoryResponse.add(profileResponse41);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.03.01")) {
						if (userProfile.getShareProfileData().getIsAddressWork()) {
							ShareProfileResponse profileResponse42 = new ShareProfileResponse();
							profileResponse42.setLabel("Work Address");
							profileResponse42.setKey("isAddressWork");
							profileResponse42.setValue(userProfile.getShareProfileData().getIsAddressWork());
							profileResponse42.setView_detail(
									PeopleUtils.convertAddressListToString(profileData.getKeyValueDataList()));
							addressCatgoryResponse.add(profileResponse42);
						} else {
							ShareProfileResponse profileResponse42 = new ShareProfileResponse();
							profileResponse42.setLabel("Work Address");
							profileResponse42.setKey("isAddressWork");
							profileResponse42.setValue(userProfile.getShareProfileData().getIsAddressWork());
							profileResponse42.setView_detail(
									PeopleUtils.convertAddressListToString(profileData.getKeyValueDataList()));
							addressCatgoryResponse.add(profileResponse42);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.03.02")) {
						if (userProfile.getShareProfileData().getIsAddressOther()) {
							ShareProfileResponse profileResponse43 = new ShareProfileResponse();
							profileResponse43.setLabel("Other Address");
							profileResponse43.setKey("isAddressOther");
							profileResponse43.setValue(userProfile.getShareProfileData().getIsAddressOther());
							profileResponse43.setView_detail(
									PeopleUtils.convertAddressListToString(profileData.getKeyValueDataList()));
							addressCatgoryResponse.add(profileResponse43);
						} else {
							ShareProfileResponse profileResponse43 = new ShareProfileResponse();
							profileResponse43.setLabel("Other Address");
							profileResponse43.setKey("isAddressOther");
							profileResponse43.setValue(userProfile.getShareProfileData().getIsAddressOther());
							profileResponse43.setView_detail(
									PeopleUtils.convertAddressListToString(profileData.getKeyValueDataList()));
							addressCatgoryResponse.add(profileResponse43);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.04.00")) {
						if (userProfile.getShareProfileData().getIsEventBirthday()) {
							ShareProfileResponse profileResponse44 = new ShareProfileResponse();
							profileResponse44.setLabel("Birthday Date");
							profileResponse44.setKey("isEventBirthday");
							profileResponse44.setValue(userProfile.getShareProfileData().getIsEventBirthday());
							profileResponse44.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							eventCatgoryResponse.add(profileResponse44);
						} else {
							ShareProfileResponse profileResponse44 = new ShareProfileResponse();
							profileResponse44.setLabel("Birthday Date");
							profileResponse44.setKey("isEventBirthday");
							profileResponse44.setValue(userProfile.getShareProfileData().getIsEventBirthday());
							profileResponse44.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							eventCatgoryResponse.add(profileResponse44);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.04.01")) {
						if (userProfile.getShareProfileData().getIsEventAnniversary()) {
							ShareProfileResponse profileResponse45 = new ShareProfileResponse();
							profileResponse45.setLabel("Anniversary Date");
							profileResponse45.setKey("isEventAnniversary");
							profileResponse45.setValue(userProfile.getShareProfileData().getIsEventAnniversary());
							profileResponse45.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							eventCatgoryResponse.add(profileResponse45);
						} else {
							ShareProfileResponse profileResponse45 = new ShareProfileResponse();
							profileResponse45.setLabel("Anniversary Date");
							profileResponse45.setKey("isEventAnniversary");
							profileResponse45.setValue(userProfile.getShareProfileData().getIsEventAnniversary());
							profileResponse45.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							eventCatgoryResponse.add(profileResponse45);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.04.02")) {
						if (userProfile.getShareProfileData().getIsEventOther()) {
							ShareProfileResponse profileResponse46 = new ShareProfileResponse();
							profileResponse46.setLabel("Other Date");
							profileResponse46.setKey("isEventOther");
							profileResponse46.setValue(userProfile.getShareProfileData().getIsEventOther());
							profileResponse46.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							eventCatgoryResponse.add(profileResponse46);
						} else {
							ShareProfileResponse profileResponse46 = new ShareProfileResponse();
							profileResponse46.setLabel("Other Date");
							profileResponse46.setKey("isEventOther");
							profileResponse46.setValue(userProfile.getShareProfileData().getIsEventOther());
							profileResponse46.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							eventCatgoryResponse.add(profileResponse46);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.05.00")) {
						if (userProfile.getShareProfileData().getIsWebsiteHomepage()) {
							ShareProfileResponse profileResponse47 = new ShareProfileResponse();
							profileResponse47.setLabel("Homepage Website");
							profileResponse47.setKey("isWebsiteHomepage");
							profileResponse47.setValue(userProfile.getShareProfileData().getIsWebsiteHomepage());
							profileResponse47.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							websiteCatgoryResponse.add(profileResponse47);
						} else {
							ShareProfileResponse profileResponse47 = new ShareProfileResponse();
							profileResponse47.setLabel("Homepage Website");
							profileResponse47.setKey("isWebsiteHomepage");
							profileResponse47.setValue(userProfile.getShareProfileData().getIsWebsiteHomepage());
							profileResponse47.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							websiteCatgoryResponse.add(profileResponse47);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.05.01")) {
						if (userProfile.getShareProfileData().getIsWebsiteHome()) {
							ShareProfileResponse profileResponse48 = new ShareProfileResponse();
							profileResponse48.setLabel("Home Website");
							profileResponse48.setKey("isWebsiteHome");
							profileResponse48.setValue(userProfile.getShareProfileData().getIsWebsiteHome());
							profileResponse48.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							websiteCatgoryResponse.add(profileResponse48);
						} else {
							ShareProfileResponse profileResponse48 = new ShareProfileResponse();
							profileResponse48.setLabel("Home Website");
							profileResponse48.setKey("isWebsiteHome");
							profileResponse48.setValue(userProfile.getShareProfileData().getIsWebsiteHome());
							profileResponse48.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							websiteCatgoryResponse.add(profileResponse48);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.05.02")) {
						if (userProfile.getShareProfileData().getIsWebsiteWork()) {
							ShareProfileResponse profileResponse49 = new ShareProfileResponse();
							profileResponse49.setLabel("Work Website");
							profileResponse49.setKey("isWebsiteWork");
							profileResponse49.setValue(userProfile.getShareProfileData().getIsWebsiteWork());
							profileResponse49.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							websiteCatgoryResponse.add(profileResponse49);
						} else {
							ShareProfileResponse profileResponse49 = new ShareProfileResponse();
							profileResponse49.setLabel("Work Website");
							profileResponse49.setKey("isWebsiteWork");
							profileResponse49.setValue(userProfile.getShareProfileData().getIsWebsiteWork());
							profileResponse49.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							websiteCatgoryResponse.add(profileResponse49);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.05.03")) {
						if (userProfile.getShareProfileData().getIsWebsiteOther()) {
							ShareProfileResponse profileResponse50 = new ShareProfileResponse();
							profileResponse50.setLabel("Other Website");
							profileResponse50.setKey("isWebsiteOther");
							profileResponse50.setValue(userProfile.getShareProfileData().getIsWebsiteOther());
							profileResponse50.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							websiteCatgoryResponse.add(profileResponse50);
						} else {
							ShareProfileResponse profileResponse50 = new ShareProfileResponse();
							profileResponse50.setLabel("Other Website");
							profileResponse50.setKey("isWebsiteOther");
							profileResponse50.setValue(userProfile.getShareProfileData().getIsWebsiteOther());
							profileResponse50.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							websiteCatgoryResponse.add(profileResponse50);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.05.04")) {
						if (userProfile.getShareProfileData().getIsWebsitePersonal()) {
							ShareProfileResponse profileResponse51 = new ShareProfileResponse();
							profileResponse51.setLabel("Personal Website");
							profileResponse51.setKey("isWebsitePersonal");
							profileResponse51.setValue(userProfile.getShareProfileData().getIsWebsitePersonal());
							profileResponse51.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							websiteCatgoryResponse.add(profileResponse51);
						} else {
							ShareProfileResponse profileResponse51 = new ShareProfileResponse();
							profileResponse51.setLabel("Personal Website");
							profileResponse51.setKey("isWebsitePersonal");
							profileResponse51.setValue(userProfile.getShareProfileData().getIsWebsitePersonal());
							profileResponse51.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							websiteCatgoryResponse.add(profileResponse51);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.06.00")) {
						if (userProfile.getShareProfileData().getIsRelationshipParent()) {
							ShareProfileResponse profileResponse52 = new ShareProfileResponse();
							profileResponse52.setLabel("Parent");
							profileResponse52.setKey("isRelationshipParent");
							profileResponse52.setValue(userProfile.getShareProfileData().getIsRelationshipParent());
							profileResponse52.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse52);
						} else {
							ShareProfileResponse profileResponse52 = new ShareProfileResponse();
							profileResponse52.setLabel("Parent");
							profileResponse52.setKey("isRelationshipParent");
							profileResponse52.setValue(userProfile.getShareProfileData().getIsRelationshipParent());
							profileResponse52.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse52);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.06.01")) {
						if (userProfile.getShareProfileData().getIsRelationshipMother()) {
							ShareProfileResponse profileResponse53 = new ShareProfileResponse();
							profileResponse53.setLabel("Mother");
							profileResponse53.setKey("isRelationshipMother");
							profileResponse53.setValue(userProfile.getShareProfileData().getIsRelationshipMother());
							profileResponse53.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse53);
						} else {
							ShareProfileResponse profileResponse53 = new ShareProfileResponse();
							profileResponse53.setLabel("Mother");
							profileResponse53.setKey("isRelationshipMother");
							profileResponse53.setValue(userProfile.getShareProfileData().getIsRelationshipMother());
							profileResponse53.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse53);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.06.02")) {
						if (userProfile.getShareProfileData().getIsRelationshipFather()) {
							ShareProfileResponse profileResponse54 = new ShareProfileResponse();
							profileResponse54.setLabel("Father");
							profileResponse54.setKey("isRelationshipFather");
							profileResponse54.setValue(userProfile.getShareProfileData().getIsRelationshipFather());
							profileResponse54.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse54);
						} else {
							ShareProfileResponse profileResponse54 = new ShareProfileResponse();
							profileResponse54.setLabel("Father");
							profileResponse54.setKey("isRelationshipFather");
							profileResponse54.setValue(userProfile.getShareProfileData().getIsRelationshipFather());
							profileResponse54.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse54);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.06.03")) {
						if (userProfile.getShareProfileData().getIsRelationshipBrother()) {
							ShareProfileResponse profileResponse55 = new ShareProfileResponse();
							profileResponse55.setLabel("Brother");
							profileResponse55.setKey("isRelationshipBrother");
							profileResponse55.setValue(userProfile.getShareProfileData().getIsRelationshipBrother());
							profileResponse55.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse55);
						} else {
							ShareProfileResponse profileResponse55 = new ShareProfileResponse();
							profileResponse55.setLabel("Brother");
							profileResponse55.setKey("isRelationshipBrother");
							profileResponse55.setValue(userProfile.getShareProfileData().getIsRelationshipBrother());
							profileResponse55.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse55);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.06.04")) {
						if (userProfile.getShareProfileData().getIsRelationshipSister()) {
							ShareProfileResponse profileResponse56 = new ShareProfileResponse();
							profileResponse56.setLabel("Sister");
							profileResponse56.setKey("isRelationshipSister");
							profileResponse56.setValue(userProfile.getShareProfileData().getIsRelationshipSister());
							profileResponse56.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse56);
						} else {
							ShareProfileResponse profileResponse56 = new ShareProfileResponse();
							profileResponse56.setLabel("Sister");
							profileResponse56.setKey("isRelationshipSister");
							profileResponse56.setValue(userProfile.getShareProfileData().getIsRelationshipSister());
							profileResponse56.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse56);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.06.05")) {
						if (userProfile.getShareProfileData().getIsRelationshipSpouse()) {
							ShareProfileResponse profileResponse57 = new ShareProfileResponse();
							profileResponse57.setLabel("Spouse");
							profileResponse57.setKey("isRelationshipSpouse");
							profileResponse57.setValue(userProfile.getShareProfileData().getIsRelationshipSpouse());
							profileResponse57.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse57);
						} else {
							ShareProfileResponse profileResponse57 = new ShareProfileResponse();
							profileResponse57.setLabel("Spouse");
							profileResponse57.setKey("isRelationshipSpouse");
							profileResponse57.setValue(userProfile.getShareProfileData().getIsRelationshipSpouse());
							profileResponse57.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse57);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.06.06")) {
						if (userProfile.getShareProfileData().getIsRelationshipChild()) {
							ShareProfileResponse profileResponse58 = new ShareProfileResponse();
							profileResponse58.setLabel("Child");
							profileResponse58.setKey("isRelationshipChild");
							profileResponse58.setValue(userProfile.getShareProfileData().getIsRelationshipChild());
							profileResponse58.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse58);
						} else {
							ShareProfileResponse profileResponse58 = new ShareProfileResponse();
							profileResponse58.setLabel("Child");
							profileResponse58.setKey("isRelationshipChild");
							profileResponse58.setValue(userProfile.getShareProfileData().getIsRelationshipChild());
							profileResponse58.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse58);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.06.07")) {
						if (userProfile.getShareProfileData().getIsRelationshipSon()) {
							ShareProfileResponse profileResponse59 = new ShareProfileResponse();
							profileResponse59.setLabel("Son");
							profileResponse59.setKey("isRelationshipSon");
							profileResponse59.setValue(userProfile.getShareProfileData().getIsRelationshipSon());
							profileResponse59.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse59);
						} else {
							ShareProfileResponse profileResponse59 = new ShareProfileResponse();
							profileResponse59.setLabel("Son");
							profileResponse59.setKey("isRelationshipSon");
							profileResponse59.setValue(userProfile.getShareProfileData().getIsRelationshipSon());
							profileResponse59.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse59);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.06.08")) {
						if (userProfile.getShareProfileData().getIsRelationshipDaughter()) {
							ShareProfileResponse profileResponse60 = new ShareProfileResponse();
							profileResponse60.setLabel("Daughter");
							profileResponse60.setKey("isRelationshipDaughter");
							profileResponse60.setValue(userProfile.getShareProfileData().getIsRelationshipDaughter());
							profileResponse60.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse60);
						} else {
							ShareProfileResponse profileResponse60 = new ShareProfileResponse();
							profileResponse60.setLabel("Daughter");
							profileResponse60.setKey("isRelationshipDaughter");
							profileResponse60.setValue(userProfile.getShareProfileData().getIsRelationshipDaughter());
							profileResponse60.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse60);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.06.09")) {
						if (userProfile.getShareProfileData().getIsRelationshipFriend()) {
							ShareProfileResponse profileResponse61 = new ShareProfileResponse();
							profileResponse61.setLabel("Friend");
							profileResponse61.setKey("isRelationshipFriend");
							profileResponse61.setValue(userProfile.getShareProfileData().getIsRelationshipFriend());
							profileResponse61.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse61);
						} else {
							ShareProfileResponse profileResponse61 = new ShareProfileResponse();
							profileResponse61.setLabel("Friend");
							profileResponse61.setKey("isRelationshipFriend");
							profileResponse61.setValue(userProfile.getShareProfileData().getIsRelationshipFriend());
							profileResponse61.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse61);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.06.10")) {
						if (userProfile.getShareProfileData().getIsRelationshipRelative()) {
							ShareProfileResponse profileResponse62 = new ShareProfileResponse();
							profileResponse62.setLabel("Relative");
							profileResponse62.setKey("isRelationshipRelative");
							profileResponse62.setValue(userProfile.getShareProfileData().getIsRelationshipRelative());
							profileResponse62.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse62);
						} else {
							ShareProfileResponse profileResponse62 = new ShareProfileResponse();
							profileResponse62.setLabel("Relative");
							profileResponse62.setKey("isRelationshipRelative");
							profileResponse62.setValue(userProfile.getShareProfileData().getIsRelationshipRelative());
							profileResponse62.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse62);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.06.11")) {
						if (userProfile.getShareProfileData().getIsRelationshipPartner()) {
							ShareProfileResponse profileResponse63 = new ShareProfileResponse();
							profileResponse63.setLabel("Partner");
							profileResponse63.setKey("isRelationshipPartner");
							profileResponse63.setValue(userProfile.getShareProfileData().getIsRelationshipPartner());
							profileResponse63.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse63);
						} else {
							ShareProfileResponse profileResponse63 = new ShareProfileResponse();
							profileResponse63.setLabel("Parent");
							profileResponse63.setKey("isRelationshipPartner");
							profileResponse63.setValue(userProfile.getShareProfileData().getIsRelationshipPartner());
							profileResponse63.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse63);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.06.12")) {
						if (userProfile.getShareProfileData().getIsRelationshipDomesticPartner()) {
							ShareProfileResponse profileResponse64 = new ShareProfileResponse();
							profileResponse64.setLabel("Domestic Partner");
							profileResponse64.setKey("isRelationshipDomesticPartner");
							profileResponse64
									.setValue(userProfile.getShareProfileData().getIsRelationshipDomesticPartner());
							profileResponse64.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse64);
						} else {
							ShareProfileResponse profileResponse64 = new ShareProfileResponse();
							profileResponse64.setLabel("Domestic Partner");
							profileResponse64.setKey("isRelationshipDomesticPartner");
							profileResponse64
									.setValue(userProfile.getShareProfileData().getIsRelationshipDomesticPartner());
							profileResponse64.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse64);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.06.13")) {
						if (userProfile.getShareProfileData().getIsRelationshipManager()) {
							ShareProfileResponse profileResponse65 = new ShareProfileResponse();
							profileResponse65.setLabel("Manager");
							profileResponse65.setKey("isRelationshipManager");
							profileResponse65.setValue(userProfile.getShareProfileData().getIsRelationshipManager());
							profileResponse65.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse65);
						} else {
							ShareProfileResponse profileResponse65 = new ShareProfileResponse();
							profileResponse65.setLabel("Manager");
							profileResponse65.setKey("isRelationshipManager");
							profileResponse65.setValue(userProfile.getShareProfileData().getIsRelationshipManager());
							profileResponse65.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse65);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.06.14")) {
						if (userProfile.getShareProfileData().getIsRelationshipAssistant()) {
							ShareProfileResponse profileResponse66 = new ShareProfileResponse();
							profileResponse66.setLabel("Assistant");
							profileResponse66.setKey("isRelationshipAssistant");
							profileResponse66.setValue(userProfile.getShareProfileData().getIsRelationshipAssistant());
							profileResponse66.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse66);
						} else {
							ShareProfileResponse profileResponse66 = new ShareProfileResponse();
							profileResponse66.setLabel("Assistant");
							profileResponse66.setKey("isRelationshipAssistant");
							profileResponse66.setValue(userProfile.getShareProfileData().getIsRelationshipAssistant());
							profileResponse66.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse66);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.06.15")) {
						if (userProfile.getShareProfileData().getIsRelationshipReference()) {
							ShareProfileResponse profileResponse67 = new ShareProfileResponse();
							profileResponse67.setLabel("Reference");
							profileResponse67.setKey("isRelationshipReference");
							profileResponse67.setValue(userProfile.getShareProfileData().getIsRelationshipReference());
							profileResponse67.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse67);
						} else {
							ShareProfileResponse profileResponse67 = new ShareProfileResponse();
							profileResponse67.setLabel("Reference");
							profileResponse67.setKey("isRelationshipReference");
							profileResponse67.setValue(userProfile.getShareProfileData().getIsRelationshipReference());
							profileResponse67.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse67);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.06.16")) {
						if (userProfile.getShareProfileData().getIsRelationshipOther()) {
							ShareProfileResponse profileResponse68 = new ShareProfileResponse();
							profileResponse68.setLabel("Other Relationship");
							profileResponse68.setKey("isRelationshipOther");
							profileResponse68.setValue(userProfile.getShareProfileData().getIsRelationshipOther());
							profileResponse68.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse68);
						} else {
							ShareProfileResponse profileResponse68 = new ShareProfileResponse();
							profileResponse68.setLabel("Other Relationship");
							profileResponse68.setKey("isRelationshipOther");
							profileResponse68.setValue(userProfile.getShareProfileData().getIsRelationshipOther());
							profileResponse68.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							relationshipCatgoryResponse.add(profileResponse68);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.07.00")) {
						if (userProfile.getShareProfileData().getIsInstantmessagingSkype()) {
							ShareProfileResponse profileResponse69 = new ShareProfileResponse();
							profileResponse69.setLabel("Skype");
							profileResponse69.setKey("isInstantmessagingSkype");
							profileResponse69.setValue(userProfile.getShareProfileData().getIsInstantmessagingSkype());
							profileResponse69.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							instantMessagingCatgoryResponse.add(profileResponse69);
						} else {
							ShareProfileResponse profileResponse69 = new ShareProfileResponse();
							profileResponse69.setLabel("Skype");
							profileResponse69.setKey("isInstantmessagingSkype");
							profileResponse69.setValue(userProfile.getShareProfileData().getIsInstantmessagingSkype());
							profileResponse69.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							instantMessagingCatgoryResponse.add(profileResponse69);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.07.01")) {
						if (userProfile.getShareProfileData().getIsInstantmessagingWindowsLive()) {
							ShareProfileResponse profileResponse70 = new ShareProfileResponse();
							profileResponse70.setLabel("Windows Live");
							profileResponse70.setKey("isInstantmessagingWindowsLive");
							profileResponse70
									.setValue(userProfile.getShareProfileData().getIsInstantmessagingWindowsLive());
							profileResponse70.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							instantMessagingCatgoryResponse.add(profileResponse70);
						} else {
							ShareProfileResponse profileResponse70 = new ShareProfileResponse();
							profileResponse70.setLabel("Windows Live");
							profileResponse70.setKey("isInstantmessagingWindowsLive");
							profileResponse70
									.setValue(userProfile.getShareProfileData().getIsInstantmessagingWindowsLive());
							profileResponse70.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							instantMessagingCatgoryResponse.add(profileResponse70);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.07.02")) {
						if (userProfile.getShareProfileData().getIsInstantmessagingGoogleHangouts()) {
							ShareProfileResponse profileResponse71 = new ShareProfileResponse();
							profileResponse71.setLabel("Google Hangouts");
							profileResponse71.setKey("isInstantmessagingGoogleHangouts");
							profileResponse71
									.setValue(userProfile.getShareProfileData().getIsInstantmessagingGoogleHangouts());
							profileResponse71.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							instantMessagingCatgoryResponse.add(profileResponse71);
						} else {
							ShareProfileResponse profileResponse71 = new ShareProfileResponse();
							profileResponse71.setLabel("Google Hangouts");
							profileResponse71.setKey("isInstantmessagingGoogleHangouts");
							profileResponse71
									.setValue(userProfile.getShareProfileData().getIsInstantmessagingGoogleHangouts());
							profileResponse71.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							instantMessagingCatgoryResponse.add(profileResponse71);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.07.03")) {
						if (userProfile.getShareProfileData().getIsInstantmessagingFacebookMessenger()) {
							ShareProfileResponse profileResponse72 = new ShareProfileResponse();
							profileResponse72.setLabel("Facebook Messenger");
							profileResponse72.setKey("isInstantmessagingFacebookMessenger");
							profileResponse72.setValue(
									userProfile.getShareProfileData().getIsInstantmessagingFacebookMessenger());
							profileResponse72.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							instantMessagingCatgoryResponse.add(profileResponse72);
						} else {
							ShareProfileResponse profileResponse72 = new ShareProfileResponse();
							profileResponse72.setLabel("Facebook Messenger");
							profileResponse72.setKey("isInstantmessagingFacebookMessenger");
							profileResponse72.setValue(
									userProfile.getShareProfileData().getIsInstantmessagingFacebookMessenger());
							profileResponse72.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							instantMessagingCatgoryResponse.add(profileResponse72);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.07.04")) {
						if (userProfile.getShareProfileData().getIsInstantmessagingAIM()) {
							ShareProfileResponse profileResponse73 = new ShareProfileResponse();
							profileResponse73.setLabel("AIM");
							profileResponse73.setKey("isInstantmessagingAIM");
							profileResponse73.setValue(userProfile.getShareProfileData().getIsInstantmessagingAIM());
							profileResponse73.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							instantMessagingCatgoryResponse.add(profileResponse73);
						} else {
							ShareProfileResponse profileResponse73 = new ShareProfileResponse();
							profileResponse73.setLabel("AIM");
							profileResponse73.setKey("isInstantmessagingAIM");
							profileResponse73.setValue(userProfile.getShareProfileData().getIsInstantmessagingAIM());
							profileResponse73.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							instantMessagingCatgoryResponse.add(profileResponse73);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.07.05")) {
						if (userProfile.getShareProfileData().getIsInstantmessagingYahoo()) {
							ShareProfileResponse profileResponse74 = new ShareProfileResponse();
							profileResponse74.setLabel("Yahoo");
							profileResponse74.setKey("isInstantmessagingYahoo");
							profileResponse74.setValue(userProfile.getShareProfileData().getIsInstantmessagingYahoo());
							profileResponse74.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							instantMessagingCatgoryResponse.add(profileResponse74);
						} else {
							ShareProfileResponse profileResponse74 = new ShareProfileResponse();
							profileResponse74.setLabel("Yahoo");
							profileResponse74.setKey("isInstantmessagingYahoo");
							profileResponse74.setValue(userProfile.getShareProfileData().getIsInstantmessagingYahoo());
							profileResponse74.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							instantMessagingCatgoryResponse.add(profileResponse74);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.07.06")) {
						if (userProfile.getShareProfileData().getIsInstantmessagingICQ()) {
							ShareProfileResponse profileResponse75 = new ShareProfileResponse();
							profileResponse75.setLabel("ICQ");
							profileResponse75.setKey("isInstantmessagingICQ");
							profileResponse75.setValue(userProfile.getShareProfileData().getIsInstantmessagingICQ());
							profileResponse75.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							instantMessagingCatgoryResponse.add(profileResponse75);
						} else {
							ShareProfileResponse profileResponse75 = new ShareProfileResponse();
							profileResponse75.setLabel("ICQ");
							profileResponse75.setKey("isInstantmessagingICQ");
							profileResponse75.setValue(userProfile.getShareProfileData().getIsInstantmessagingICQ());
							profileResponse75.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							instantMessagingCatgoryResponse.add(profileResponse75);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.07.07")) {
						if (userProfile.getShareProfileData().getIsInstantmessagingJabber()) {
							ShareProfileResponse profileResponse76 = new ShareProfileResponse();
							profileResponse76.setLabel("Jabber");
							profileResponse76.setKey("isInstantmessagingJabber");
							profileResponse76.setValue(userProfile.getShareProfileData().getIsInstantmessagingJabber());
							profileResponse76.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							instantMessagingCatgoryResponse.add(profileResponse76);
						} else {
							ShareProfileResponse profileResponse76 = new ShareProfileResponse();
							profileResponse76.setLabel("Jabber");
							profileResponse76.setKey("isInstantmessagingJabber");
							profileResponse76.setValue(userProfile.getShareProfileData().getIsInstantmessagingJabber());
							profileResponse76.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							instantMessagingCatgoryResponse.add(profileResponse76);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.07.08")) {
						if (userProfile.getShareProfileData().getIsInstantmessagingQQ()) {
							ShareProfileResponse profileResponse77 = new ShareProfileResponse();
							profileResponse77.setLabel("QQ");
							profileResponse77.setKey("isInstantmessagingQQ");
							profileResponse77.setValue(userProfile.getShareProfileData().getIsInstantmessagingQQ());
							profileResponse77.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							instantMessagingCatgoryResponse.add(profileResponse77);
						} else {
							ShareProfileResponse profileResponse77 = new ShareProfileResponse();
							profileResponse77.setLabel("QQ");
							profileResponse77.setKey("isInstantmessagingQQ");
							profileResponse77.setValue(userProfile.getShareProfileData().getIsInstantmessagingQQ());
							profileResponse77.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							instantMessagingCatgoryResponse.add(profileResponse77);
						}
					}
					if (profileData.getLabel().equalsIgnoreCase("PL.07.09")) {
						if (userProfile.getShareProfileData().getIsInstantmessagingGaduGadu()) {
							ShareProfileResponse profileResponse78 = new ShareProfileResponse();
							profileResponse78.setLabel("Gadu-Gadu");
							profileResponse78.setKey("isInstantmessagingGaduGadu");
							profileResponse78
									.setValue(userProfile.getShareProfileData().getIsInstantmessagingGaduGadu());
							profileResponse78.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							instantMessagingCatgoryResponse.add(profileResponse78);
						} else {
							ShareProfileResponse profileResponse78 = new ShareProfileResponse();
							profileResponse78.setLabel("Gadu-Gadu");
							profileResponse78.setKey("isInstantmessagingGaduGadu");
							profileResponse78
									.setValue(userProfile.getShareProfileData().getIsInstantmessagingGaduGadu());
							profileResponse78.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							instantMessagingCatgoryResponse.add(profileResponse78);
						}
					}
					if (profileData.getCategory().equalsIgnoreCase("OTHER")) {
						if (userProfile.getShareProfileData().getIsCustome()) {
							ShareProfileResponse profileResponse79 = new ShareProfileResponse();
							profileResponse79.setLabel(profileData.getLabel());
							profileResponse79.setKey("isCustome");
							profileResponse79.setValue(userProfile.getShareProfileData().getIsCustome());
							profileResponse79.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							otherResponse.add(profileResponse79);
						} else {
							ShareProfileResponse profileResponse79 = new ShareProfileResponse();
							profileResponse79.setLabel(profileData.getLabel());
							profileResponse79.setKey("isCustome");
							profileResponse79.setValue(userProfile.getShareProfileData().getIsCustome());
							profileResponse79.setView_detail(
									PeopleUtils.convertOtherKeyValueToString(profileData.getKeyValueDataList()));
							otherResponse.add(profileResponse79);
						}
					}
				}
			}
		}
		responses.addAll(phoneNumberCatgoryResponse);
		responses.addAll(emailCatgoryResponse);
		responses.addAll(socialMediaCatgoryResponse);
		responses.addAll(instantMessagingCatgoryResponse);
		responses.addAll(addressCatgoryResponse);
		responses.addAll(eventCatgoryResponse);
		responses.addAll(websiteCatgoryResponse);
		responses.addAll(otherResponse);

		return responses;
	}

	private List<ShareProfileResponse> removeDuplicate(List<ShareProfileResponse> profileResponse) {
		for (int i = 0; i < profileResponse.size(); i++) {
			for (int j = i + 1; j < profileResponse.size(); j++) {
				if (profileResponse.get(i).getKey().equalsIgnoreCase(profileResponse.get(j).getKey())) {
					profileResponse.remove(i);
				}
			}
		}
		return profileResponse;
	}

	private ShareProfileData saveOrEditPrivacySettings(PrivacyProfileData privacyProfileData) {
		ShareProfileData shareProfileData = new ShareProfileData();
		if (privacyProfileData.getShareProfileData() != null) {
			for (ShareProfileResponse shareData : PeopleUtils.emptyIfNull(privacyProfileData.getShareProfileData())) {
				if (shareData.getKey().equalsIgnoreCase("isName")) {
					shareProfileData.setIsName(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isGender")) {
					shareProfileData.setIsGender(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isCompany")) {
					shareProfileData.setIsCompany(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isDepartment")) {
					shareProfileData.setIsDepartment(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isPosition")) {
					shareProfileData.setIsPosition(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isImageURL")) {
					shareProfileData.setIsImageURL(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isNotes")) {
					shareProfileData.setIsNotes(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isTagList")) {
					shareProfileData.setIsTagList(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isPhoneNumberMobile")) {
					shareProfileData.setIsPhoneNumberMobile(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isPhoneNumberHome")) {
					shareProfileData.setIsPhoneNumberHome(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isPhoneNumberWork")) {
					shareProfileData.setIsPhoneNumberWork(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isPhoneNumberIphone")) {
					shareProfileData.setIsPhoneNumberIphone(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isPhoneNumberMain")) {
					shareProfileData.setIsPhoneNumberMain(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isPhoneNumberHomeFax")) {
					shareProfileData.setIsPhoneNumberHomeFax(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isPhoneNumberWorkFax")) {
					shareProfileData.setIsPhoneNumberWorkFax(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isPhoneNumberPager")) {
					shareProfileData.setIsPhoneNumberPager(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isPhoneNumberOther")) {
					shareProfileData.setIsPhoneNumberOther(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isPhoneNumberPersonal")) {
					shareProfileData.setIsPhoneNumberPersonal(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isPhoneNumberLandLine")) {
					shareProfileData.setIsPhoneNumberLandLine(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isEmailHome")) {
					shareProfileData.setIsEmailHome(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isEmailWork")) {
					shareProfileData.setIsEmailWork(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isEmailiCloud")) {
					shareProfileData.setIsEmailiCloud(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isEmailOther")) {
					shareProfileData.setIsEmailOther(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isEmailPersonal")) {
					shareProfileData.setIsEmailPersonal(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isSocialPeofileTwitter")) {
					shareProfileData.setIsSocialPeofileTwitter(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isSocialPeofileLinkedIn")) {
					shareProfileData.setIsSocialPeofileLinkedIn(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isSocialPeofileFlickr")) {
					shareProfileData.setIsSocialPeofileFlickr(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isSocialPeofileFacebook")) {
					shareProfileData.setIsSocialPeofileFacebook(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isSocialPeofileMyspace")) {
					shareProfileData.setIsSocialPeofileMyspace(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isSocialPeofileSinaWeibo")) {
					shareProfileData.setIsSocialPeofileSinaWeibo(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isSocialPeofileInstagram")) {
					shareProfileData.setIsSocialPeofileInstagram(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isSocialPeofileSnapchat")) {
					shareProfileData.setIsSocialPeofileSnapchat(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isSocialPeofileReddit")) {
					shareProfileData.setIsSocialPeofileReddit(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isSocialPeofileImgur")) {
					shareProfileData.setIsSocialPeofileImgur(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isSocialPeofileGoogle")) {
					shareProfileData.setIsSocialPeofileGoogle(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isSocialPeofileSquareCash")) {
					shareProfileData.setIsSocialPeofileSquareCash(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isSocialPeofileTiktok")) {
					shareProfileData.setIsSocialPeofileTiktok(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isSocialPeofileWhatsapp")) {
					shareProfileData.setIsSocialPeofileWhatsapp(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isSocialPeofileYoutube")) {
					shareProfileData.setIsSocialPeofileYoutube(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isSocialPeofileFacebookMessenger")) {
					shareProfileData.setIsSocialPeofileFacebookMessenger(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isAddressHome")) {
					shareProfileData.setIsAddressHome(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isAddressWork")) {
					shareProfileData.setIsAddressWork(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isAddressOther")) {
					shareProfileData.setIsAddressOther(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isEventBirthday")) {
					shareProfileData.setIsEventBirthday(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isEventAnniversary")) {
					shareProfileData.setIsEventAnniversary(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isEventOther")) {
					shareProfileData.setIsEventOther(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isWebsiteHomepage")) {
					shareProfileData.setIsWebsiteHomepage(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isWebsiteHome")) {
					shareProfileData.setIsWebsiteHome(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isWebsiteWork")) {
					shareProfileData.setIsWebsiteWork(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isWebsiteOther")) {
					shareProfileData.setIsWebsiteOther(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isWebsitePersonal")) {
					shareProfileData.setIsWebsitePersonal(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isRelationshipParent")) {
					shareProfileData.setIsRelationshipParent(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isRelationshipMother")) {
					shareProfileData.setIsRelationshipMother(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isRelationshipFather")) {
					shareProfileData.setIsRelationshipFather(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isRelationshipBrother")) {
					shareProfileData.setIsRelationshipBrother(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isRelationshipSister")) {
					shareProfileData.setIsRelationshipSister(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isRelationshipSpouse")) {
					shareProfileData.setIsRelationshipSpouse(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isRelationshipChild")) {
					shareProfileData.setIsRelationshipChild(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isRelationshipSon")) {
					shareProfileData.setIsRelationshipSon(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isRelationshipDaughter")) {
					shareProfileData.setIsRelationshipDaughter(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isRelationshipFriend")) {
					shareProfileData.setIsRelationshipFriend(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isRelationshipRelative")) {
					shareProfileData.setIsRelationshipRelative(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isRelationshipPartner")) {
					shareProfileData.setIsRelationshipPartner(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isRelationshipDomesticPartner")) {
					shareProfileData.setIsRelationshipDomesticPartner(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isRelationshipManager")) {
					shareProfileData.setIsRelationshipManager(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isRelationshipAssistant")) {
					shareProfileData.setIsRelationshipAssistant(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isRelationshipReference")) {
					shareProfileData.setIsRelationshipReference(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isRelationshipOther")) {
					shareProfileData.setIsRelationshipOther(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isInstantmessagingSkype")) {
					shareProfileData.setIsInstantmessagingSkype(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isInstantmessagingWindowsLive")) {
					shareProfileData.setIsInstantmessagingWindowsLive(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isInstantmessagingGoogleHangouts")) {
					shareProfileData.setIsInstantmessagingGoogleHangouts(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isInstantmessagingFacebookMessenger")) {
					shareProfileData.setIsInstantmessagingFacebookMessenger(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isInstantmessagingAIM")) {
					shareProfileData.setIsInstantmessagingAIM(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isInstantmessagingYahoo")) {
					shareProfileData.setIsInstantmessagingYahoo(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isInstantmessagingICQ")) {
					shareProfileData.setIsInstantmessagingICQ(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isInstantmessagingJabber")) {
					shareProfileData.setIsInstantmessagingJabber(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isInstantmessagingQQ")) {
					shareProfileData.setIsInstantmessagingQQ(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isInstantmessagingGaduGadu")) {
					shareProfileData.setIsInstantmessagingGaduGadu(shareData.getValue());
				}
				if (shareData.getKey().equalsIgnoreCase("isCustome")) {
					shareProfileData.setIsCustome(shareData.getValue());
				}
			}
		}
		return shareProfileData;
	}
}
