package com.peopleapp.service.impl;

import com.peopleapp.dto.ContactNumberDTO;
import com.peopleapp.dto.UserContact;
import com.peopleapp.dto.UserInformationDTO;
import com.peopleapp.dto.UserProfileData;
import com.peopleapp.dto.requestresponsedto.NetworkInviteeContact;
import com.peopleapp.enums.*;
import com.peopleapp.exception.BadRequestException;
import com.peopleapp.model.PeopleUser;
import com.peopleapp.model.RegisteredNumber;
import com.peopleapp.model.UserConnection;
import com.peopleapp.model.UserPrivacyProfile;
import com.peopleapp.repository.PeopleUserRepository;
import com.peopleapp.repository.RegisteredNumberRepository;
import com.peopleapp.repository.UserPrivacyProfileRepository;
import com.peopleapp.service.MasterService;
import com.peopleapp.util.PeopleUtils;

import org.apache.catalina.User;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MasterServiceImpl implements MasterService {

    @Inject
    private UserPrivacyProfileRepository userPrivacyProfileRepository;

    @Inject
    private RegisteredNumberRepository registeredNumberRepository;

    @Inject
    private PeopleUserRepository peopleUserRepository;

    @Override
    public UserInformationDTO prepareUserPublicData(PeopleUser peopleUser) {

       // UserPrivacyProfile publicProfile = userPrivacyProfileRepository.findPublicProfile(peopleUser.getUserId());
        UserInformationDTO publicData = new UserInformationDTO();
        //if (publicProfile != null) {
          //  publicData = prepareUserDataBasedOnPrivacyProfileNew(peopleUser, publicProfile);
        //} else {
        if(peopleUser!=null) {
            publicData.setUserId(peopleUser.getUserId());
            publicData.setFirstName(PeopleUtils.getDefaultOrEmpty(peopleUser.getFirstNameValue()));
            publicData.setLastName(PeopleUtils.getDefaultOrEmpty(peopleUser.getLastNameValue()));
            publicData.setName(PeopleUtils.getDefaultOrEmpty(peopleUser.getFullName()));
            publicData.setImageURL(PeopleUtils.getDefaultOrEmpty(peopleUser.getDefaultImageUrl()));
            List<UserProfileData> TempProfileDatas = new ArrayList<>();
            if(peopleUser.getUserMetadataList()!=null) {
        		for(UserProfileData up : PeopleUtils.emptyIfNull(peopleUser.getUserMetadataList())) {
        			if(up.getCategory().equalsIgnoreCase("PHONENUMBER")) {
        				TempProfileDatas.add(up);
        			}
        		}
        	}
            publicData.setUserMetadataList(TempProfileDatas);
        }
        //}
        return publicData;
    }

    @Override
    public UserInformationDTO prepareSharedData(UserConnection userContact) {

        PeopleUser userData = userContact.getUserData();
        UserPrivacyProfile privacyProfileData = userContact.getPrivacyProfileData();

        if (privacyProfileData == null || userData == null) {
            return null;
        }

        UserInformationDTO sharedData = new UserInformationDTO();
        List<UserProfileData> metadataList = new ArrayList<>();
        Map<String, UserProfileData> metadataMap = userData.getMetadataMap();

        Set<String> valueIdList = new HashSet<>(privacyProfileData.getValueIdList());

        List<String> profilePlusSharedList = userContact.getSharedProfile().getValueIdList();

        if (!PeopleUtils.isNullOrEmpty(profilePlusSharedList)) {
            valueIdList.addAll(profilePlusSharedList);
        }
        for (String valueId : PeopleUtils.emptyIfNull(valueIdList)) {
            if (!metadataMap.containsKey(valueId)) {
                continue;
            }
            UserProfileData userProfileData = metadataMap.get(valueId);
            metadataList.add(userProfileData);
        }

        sharedData.setUserMetadataList(userData, metadataList);
        sharedData.setName(userData.getNameValue());
        sharedData.setFirstName(userData.getFirstNameValue());
        sharedData.setLastName(userData.getLastNameValue());
        sharedData.setMiddleName(userData.getMiddleNameValue());
        sharedData.setNameSuffix(userData.getNameSuffix());
        sharedData.setNamePrefix(userData.getNamePrefix());

        String imageURL = privacyProfileData.getImageURL();
        if (PeopleUtils.isNullOrEmpty(imageURL)) {
            imageURL = userData.getDefaultImageUrl();
        }

        sharedData.setImageURL(imageURL);

        checkPrivacyProfileSharedValuesAndUpdateSharedData(userData, userContact, sharedData);

        return sharedData;
    }
    
    @Override
    public UserInformationDTO prepareSharedData1(UserConnection userContact) {

        PeopleUser userData = userContact.getUserData();
        UserInformationDTO sharedData = new UserInformationDTO();
        UserPrivacyProfile privacyProfileData = userContact.getPrivacyProfileData();
     	List<UserProfileData> TempProfileDatas = new ArrayList<>();
     	
        if (privacyProfileData == null || userData == null || privacyProfileData.getShareProfileData() == null) {
            return null;
        }
        
        sharedData.setUserId(userData.getUserId());
        
        if(privacyProfileData.getShareProfileData().getIsName()) {
        	sharedData.setName(PeopleUtils.getDefaultOrEmpty(userData.getNameValue()));
        	sharedData.setFullName(PeopleUtils.getDefaultOrEmpty(userData.getFullName()));
        	sharedData.setFirstName(PeopleUtils.getDefaultOrEmpty(userData.getFirstNameValue()));
        	sharedData.setLastName(PeopleUtils.getDefaultOrEmpty(userData.getLastNameValue()));
        	sharedData.setMiddleName(PeopleUtils.getDefaultOrEmpty(userData.getMiddleNameValue()));
        	sharedData.setPhoneticFirstName(PeopleUtils.getDefaultOrEmpty(userData.getPhoneticFirstName()));
        	sharedData.setPhoneticMiddleName(PeopleUtils.getDefaultOrEmpty(userData.getPhoneticMiddleName()));
        	sharedData.setPhoneticLastName(PeopleUtils.getDefaultOrEmpty(userData.getPhoneticLastName()));
        	sharedData.setNamePrefix(PeopleUtils.getDefaultOrEmpty(userData.getNamePrefix()));
        	sharedData.setNameSuffix(PeopleUtils.getDefaultOrEmpty(userData.getNameSuffix()));
        	sharedData.setMaidenName(PeopleUtils.getDefaultOrEmpty(userData.getMaidenNameValue()));
        	sharedData.setNickName(PeopleUtils.getDefaultOrEmpty(userData.getNickNameValue()));
        }else {
        	sharedData.setName(PeopleUtils.getDefaultOrEmpty(""));
        	sharedData.setFullName(PeopleUtils.getDefaultOrEmpty(""));
        	sharedData.setFirstName(PeopleUtils.getDefaultOrEmpty(""));
        	sharedData.setLastName(PeopleUtils.getDefaultOrEmpty(""));
        	sharedData.setMiddleName(PeopleUtils.getDefaultOrEmpty(""));
        	sharedData.setPhoneticFirstName(PeopleUtils.getDefaultOrEmpty(""));
        	sharedData.setPhoneticMiddleName(PeopleUtils.getDefaultOrEmpty(""));
        	sharedData.setPhoneticLastName(PeopleUtils.getDefaultOrEmpty(""));
        	sharedData.setNamePrefix(PeopleUtils.getDefaultOrEmpty(""));
        	sharedData.setNameSuffix(PeopleUtils.getDefaultOrEmpty(""));
        	sharedData.setMaidenName(PeopleUtils.getDefaultOrEmpty(""));
        	sharedData.setNickName(PeopleUtils.getDefaultOrEmpty(""));
        }
        
        if(privacyProfileData.getShareProfileData().getIsGender()) {
        	sharedData.setGender(userData.getGender());
        }else {
        	sharedData.setGender(PeopleUtils.getDefaultOrEmpty(""));
        }
        
        if(privacyProfileData.getShareProfileData().getIsCompany()) {
        	sharedData.setCompany(userData.getCompanyValue());
        }else {
        	sharedData.setCompany(PeopleUtils.getDefaultOrEmpty(""));
        }
        
        if(privacyProfileData.getShareProfileData().getIsDepartment()) {
        	sharedData.setDepartment(userData.getDepartment());
        }else {
        	sharedData.setDepartment(PeopleUtils.getDefaultOrEmpty(""));
        }
        
        if(privacyProfileData.getShareProfileData().getIsPosition()) {
        	sharedData.setPosition(userData.getPositionValue());
        }else {
        	sharedData.setPosition(PeopleUtils.getDefaultOrEmpty(""));
        }
        
        if(privacyProfileData.getShareProfileData().getIsImageURL()) {
        	sharedData.setImageURL(userData.getDefaultImageUrl());
        }else {
        	sharedData.setImageURL(PeopleUtils.getDefaultOrEmpty(""));
        }
        
        if(privacyProfileData.getShareProfileData().getIsNotes()) {
        	sharedData.setNotes(userData.getNotes());
        }else {
        	sharedData.setNotes(PeopleUtils.getDefaultOrEmpty(""));
        }
        
        if(privacyProfileData.getShareProfileData().getIsTagList()) {
        	sharedData.setTagList(userData.getProfileTags());
        }
        
        if(userData.getUserMetadataList()!=null) {
        	for(UserProfileData userProfileData : PeopleUtils.emptyIfNull(userData.getUserMetadataList())) {
        		if(privacyProfileData.getShareProfileData().getIsPhoneNumberMobile()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.00.00")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsPhoneNumberHome()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.00.01")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsPhoneNumberWork()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.00.02")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsPhoneNumberIphone()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.00.03")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsPhoneNumberMain()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.00.04")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsPhoneNumberHomeFax()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.00.05")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsPhoneNumberWorkFax()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.00.06")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsPhoneNumberPager()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.00.07")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsPhoneNumberOther()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.00.08")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsPhoneNumberPersonal()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.00.09")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsPhoneNumberLandLine()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.00.10")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsEmailHome()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.01.00")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsEmailWork()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.01.01")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsEmailiCloud()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.01.02")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsEmailOther()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.01.03")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsEmailPersonal()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.01.04")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsSocialPeofileTwitter()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.02.00")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsSocialPeofileLinkedIn()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.02.01")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsSocialPeofileFlickr()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.02.02")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsSocialPeofileFacebook()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.02.03")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsSocialPeofileMyspace()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.02.04")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsSocialPeofileSinaWeibo()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.02.05")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsSocialPeofileInstagram()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.02.06")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsSocialPeofileSnapchat()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.02.07")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsSocialPeofileReddit()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.02.08")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsSocialPeofileImgur()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.02.09")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsSocialPeofileGoogle()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.02.10")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsSocialPeofileSquareCash()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.02.11")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsSocialPeofileTiktok()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.02.12")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsSocialPeofileWhatsapp()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.02.13")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsSocialPeofileYoutube()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.02.14")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsSocialPeofileFacebookMessenger()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.02.15")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsAddressHome()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.03.00")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsAddressWork()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.03.01")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsAddressOther()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.03.02")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsEventBirthday()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.04.00")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsEventAnniversary()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.04.01")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsEventOther()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.04.02")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsWebsiteHomepage()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.05.00")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsWebsiteHome()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.05.01")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsWebsiteWork()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.05.02")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsWebsiteOther()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.05.03")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsWebsitePersonal()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.05.04")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsRelationshipParent()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.06.00")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsRelationshipMother()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.06.01")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsRelationshipFather()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.06.02")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsRelationshipBrother()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.06.03")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsRelationshipSister()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.06.04")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsRelationshipSpouse()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.06.05")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsRelationshipChild()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.06.06")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsRelationshipSon()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.06.07")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsRelationshipDaughter()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.06.08")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsRelationshipFriend()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.06.09")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsRelationshipRelative()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.06.10")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsRelationshipPartner()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.06.11")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsRelationshipDomesticPartner()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.06.12")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsRelationshipManager()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.06.13")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsRelationshipAssistant()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.06.14")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsRelationshipReference()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.06.15")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsRelationshipOther()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.06.16")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsInstantmessagingSkype()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.07.00")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsInstantmessagingWindowsLive()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.07.01")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsInstantmessagingGoogleHangouts()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.07.02")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsInstantmessagingFacebookMessenger()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.07.03")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsInstantmessagingAIM()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.07.04")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsInstantmessagingYahoo()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.07.05")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsInstantmessagingICQ()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.07.06")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsInstantmessagingJabber()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.07.07")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsInstantmessagingQQ()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.07.08")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsInstantmessagingGaduGadu()) {
        			if(userProfileData.getLabel().equalsIgnoreCase("PL.07.09")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        		if(privacyProfileData.getShareProfileData().getIsCustome()) {
        			if(userProfileData.getCategory().equalsIgnoreCase("OTHER")) {
        				TempProfileDatas.add(userProfileData);
        			}
        		}
        	}
        	sharedData.setUserMetadataList(TempProfileDatas);
        }
        
        return sharedData;
    }

    private void checkPrivacyProfileSharedValuesAndUpdateSharedData(PeopleUser userData, UserConnection userContact,
                                                                    UserInformationDTO sharedData) {
        UserPrivacyProfile privacyProfileData = userContact.getPrivacyProfileData();
        if (privacyProfileData.getIsTagShared()) {
            sharedData.setTagList(userData.getProfileTags());
        }
        if (privacyProfileData.getIsPositionShared() || userContact.getSharedProfile().getIsPositionShared()) {
            sharedData.setPosition(userData.getPositionValue());
        }
        if (privacyProfileData.getIsCompanyShared() || userContact.getSharedProfile().getIsCompanyShared()) {
            sharedData.setCompany(userData.getCompanyValue());
        }
        if (privacyProfileData.getIsNickNameShared() || userContact.getSharedProfile().getIsNickNameShared()) {
            sharedData.setNickName(userData.getNickNameValue());
        }
        if (privacyProfileData.getIsMaidenNameShared() || userContact.getSharedProfile().getIsMaidenNameShared()) {
            sharedData.setMaidenName(userData.getMaidenNameValue());
        }
    }

    @Override
    public UserInformationDTO prepareUserDataBasedOnPrivacyProfile(PeopleUser peopleUser,
                                                                   UserPrivacyProfile userPrivacyProfile) {

        UserInformationDTO userData = new UserInformationDTO();
        userData.setUserId(peopleUser.getUserId());
        
        userData.setName(Optional.ofNullable(peopleUser.getNameValue()).orElse(""));
        userData.setFirstName(Optional.ofNullable(peopleUser.getFirstNameValue()).orElse(""));
        userData.setLastName(Optional.ofNullable(peopleUser.getLastNameValue()).orElse(""));
        userData.setMiddleName(Optional.ofNullable(peopleUser.getMiddleNameValue()).orElse(""));
        userData.setNamePrefix(Optional.ofNullable(peopleUser.getNamePrefix()).orElse(""));
        userData.setNameSuffix(Optional.ofNullable(peopleUser.getNameSuffix()).orElse(""));
        userData.setImageURL(userPrivacyProfile.getImageURL() != null ?
                userPrivacyProfile.getImageURL() : peopleUser.getDefaultImageUrl());

        if (userPrivacyProfile.getIsCompanyShared()) {
            userData.setCompany(peopleUser.getCompanyValue());
        }
        if (userPrivacyProfile.getIsPositionShared()) {
            userData.setPosition(peopleUser.getPositionValue());
        }
        if (userPrivacyProfile.getIsNickNameShared()) {
            userData.setNickName(peopleUser.getNickNameValue());
        }
        if (userPrivacyProfile.getIsMaidenNameShared()) {
            userData.setMaidenName(peopleUser.getMaidenNameValue());
        }
        if (userPrivacyProfile.getIsTagShared()) {
            userData.setTagList(peopleUser.getProfileTags());
        }

        // prepare metadata
        if (!PeopleUtils.isNullOrEmpty(userPrivacyProfile.getValueIdList())) {
            userData.setUserMetadataList(peopleUser, prepareProfileDataFromValueId(peopleUser.getMetadataMap(),
                    userPrivacyProfile.getValueIdList()));
        }

        return userData;
    }


    @Override
    public void mergeSharedInfoToStaticInfo(UserInformationDTO sharedInformationDTO,
                                            UserInformationDTO staticInformationDTO) {

        // Name Fields
        PeopleUtils.setIfNotNullOrEmpty(staticInformationDTO::setName, sharedInformationDTO.getName());
        PeopleUtils.setIfNotNullOrEmpty(staticInformationDTO::setFirstName, sharedInformationDTO.getFirstName());
        PeopleUtils.setIfNotNullOrEmpty(staticInformationDTO::setMiddleName, sharedInformationDTO.getMiddleName());
        PeopleUtils.setIfNotNullOrEmpty(staticInformationDTO::setLastName, sharedInformationDTO.getLastName());
        PeopleUtils
                .setIfNotNullOrEmpty(staticInformationDTO::setPhoneticFirstName, sharedInformationDTO.getPhoneticFirstName());
        PeopleUtils.setIfNotNullOrEmpty(staticInformationDTO::setPhoneticMiddleName,
                sharedInformationDTO.getPhoneticMiddleName());
        PeopleUtils.setIfNotNullOrEmpty(staticInformationDTO::setPhoneticLastName, sharedInformationDTO.getPhoneticLastName());
        PeopleUtils.setIfNotNullOrEmpty(staticInformationDTO::setNamePrefix, sharedInformationDTO.getNamePrefix());
        PeopleUtils.setIfNotNullOrEmpty(staticInformationDTO::setNameSuffix, sharedInformationDTO.getNameSuffix());
        PeopleUtils.setIfNotNullOrEmpty(staticInformationDTO::setMaidenName, sharedInformationDTO.getMaidenName());
        PeopleUtils.setIfNotNullOrEmpty(staticInformationDTO::setNickName, sharedInformationDTO.getNickName());

        // Basic Profile Fields
        PeopleUtils.setIfNotNullOrEmpty(staticInformationDTO::setDeviceContactId, sharedInformationDTO.getDeviceContactId());
        PeopleUtils.setIfNotNullOrEmpty(staticInformationDTO::setGender, sharedInformationDTO.getGender());
        PeopleUtils.setIfNotNullOrEmpty(staticInformationDTO::setCompany, sharedInformationDTO.getCompany());
        PeopleUtils.setIfNotNullOrEmpty(staticInformationDTO::setPosition, sharedInformationDTO.getPosition());
        PeopleUtils.setIfNotNullOrEmpty(staticInformationDTO::setImageURL, sharedInformationDTO.getImageURL());
        PeopleUtils.setIfNotNullOrEmpty(staticInformationDTO::setNotes, sharedInformationDTO.getNotes());
        PeopleUtils.setIfNotNullOrEmpty(staticInformationDTO::setSip, sharedInformationDTO.getSip());

        // Tag List
        List<String> mergedTagList = new ArrayList<>();
        PeopleUtils.setIfNotNullOrEmpty(mergedTagList::addAll, staticInformationDTO.getTagList());
        PeopleUtils.setIfNotNullOrEmpty(mergedTagList::addAll, sharedInformationDTO.getTagList());

        staticInformationDTO.setTagList(PeopleUtils.removeDuplicates(mergedTagList));

        // Meta List
        List<UserProfileData> userStaticMetaList =
                mergeMetaList(sharedInformationDTO.getUserMetadataList(), staticInformationDTO.getUserMetadataList());

        staticInformationDTO.setUserMetadataList(userStaticMetaList);
    }

    @Override
    public Set<String> getRegisteredNumberList() {

        List<RegisteredNumber> registeredNumbersList = registeredNumberRepository.findAll();
        RegisteredNumber registeredNumber = (registeredNumbersList.isEmpty() ? null : registeredNumbersList.get(0));
        if (registeredNumber != null) {
            return (new HashSet<>(registeredNumber.getRegisteredNumberList()));
        }
        return (new HashSet<>());
    }

    @Override
    public boolean isPushNotificationEnabledForUser(String userId) {
        PeopleUser peopleUser = peopleUserRepository.findByUserIdAndStatus(userId, UserStatus.ACTIVE);
        if (peopleUser != null) {
            return peopleUser.getIsPushNotificationEnabled();
        }
        return false;
    }

    @Override
    public boolean isUserBlockedByContact(String contactUserId, String userId) {
        if (contactUserId == null) {
            return Boolean.FALSE;
        }

        PeopleUser peopleUser = peopleUserRepository.getUserWithBlockedId(contactUserId);

        Set<String> blockedIdList = peopleUser.getBlockedUserIdList();

        return blockedIdList.contains(userId);
    }

    @Override
    public Map<Integer, ContactNumberDTO> prepareContactNumberPriorityList(List<UserProfileData> userProfileData) {
        Set<String> numberList = getRegisteredNumberList();
        Map<Integer, ContactNumberDTO> priorityMap = new HashMap<>();
        boolean isPriorityLabelMobile = false;

        for (UserProfileData userProfile : PeopleUtils.emptyIfNull(userProfileData)) {
            if (userProfile.getCategory().equalsIgnoreCase(UserInfoCategory.CONTACT_NUMBER.getValue()) &&
                    numberList.contains(userProfile.getContactNumber().getMobileNumber())) {
                isPriorityLabelMobile = assignPriorityToUserprofile(userProfile, priorityMap);
                if (isPriorityLabelMobile) {
                    break;
                }
            }
        }
        return priorityMap;
    }

    @Override
    public boolean updateContactForStaticContactWithVerifiedNumber(List<UserProfileData> userProfileData, Object contact) {

        Map<Integer, ContactNumberDTO> priorityMap = prepareContactNumberPriorityList(userProfileData);
        return updateContactBasedOnPriority(priorityMap, contact);
    }

    @Override
    public List<UserProfileData> getDeletedMetaList(PeopleUser sessionUser, List<String> oldValueIds,
                                                    List<String> newValueIds) {
        List<UserProfileData> deletedMetaList = new ArrayList<>();
        List<String> deletedValueIds;

        if (PeopleUtils.isNullOrEmpty(oldValueIds)) {
            return deletedMetaList;
        } else if (PeopleUtils.isNullOrEmpty(newValueIds)) {
            deletedValueIds = oldValueIds;
        } else {
            deletedValueIds =
                    oldValueIds.stream().filter(oldValue -> !newValueIds.contains(oldValue)).collect(Collectors.toList());
        }

        for (UserProfileData profileData : sessionUser.getUserMetadataList()) {
            if (deletedValueIds.contains(profileData.getValueId())) {
                deletedMetaList.add(profileData);
            }
        }
        return deletedMetaList;
    }

    public boolean updateContactForNonWatuRegisteredNumbers(List<UserProfileData> userProfileData, UserContact contact) {

        Map<Integer, ContactNumberDTO> priorityMap = prepareContactNumberPriorityListForNonWatuNumbers(userProfileData);
        return updateContactBasedOnPriorityForNonWatuNumbers(priorityMap, contact);
    }

    @Override
    public boolean isValidCanadaOrUSANumber(ContactNumberDTO contactNumber) {
        boolean isValidNumber = false;
        /*
         * For a number to be valid USA or Canada number country code must be "+1"
         * and number must be of 10 digits
         */
        if (contactNumber.getCountryCode().equals(CountryCode.US_CANADA_COUNTRY_CODE.getValue())
                && (contactNumber.getPhoneNumber().length() == 10)) {
            isValidNumber = true;
        }
        return isValidNumber;
    }

    private Map<Integer, ContactNumberDTO> prepareContactNumberPriorityListForNonWatuNumbers(List<UserProfileData> contactProfileData) {
        Map<Integer, ContactNumberDTO> priorityMap = new HashMap<>();
        boolean isPriorityLabelMobile = false;
        for (UserProfileData userProfile : contactProfileData) {
            if (userProfile.getCategory().equalsIgnoreCase(UserInfoCategory.CONTACT_NUMBER.getValue())
                    && isValidCanadaOrUSANumber(userProfile.getContactNumber())) {
                isPriorityLabelMobile = assignPriorityToUserprofile(userProfile, priorityMap);
                if (isPriorityLabelMobile) {
                    break;
                }
            }
        }

        return priorityMap;
    }

    private boolean updateContactBasedOnPriority(Map<Integer, ContactNumberDTO> priorityMap, Object contact) {
        boolean isUserContactUpdated = false;
        if (priorityMap.get(1) != null) {
            isUserContactUpdated = updateContact(priorityMap.get(1), contact);
        } else if (priorityMap.get(2) != null) {
            isUserContactUpdated = updateContact(priorityMap.get(2), contact);
        } else if (priorityMap.get(3) != null) {
            isUserContactUpdated = updateContact(priorityMap.get(3), contact);
        } else if (priorityMap.get(4) != null) {
            isUserContactUpdated = updateContact(priorityMap.get(4), contact);
        } else if (priorityMap.get(5) != null) {
            isUserContactUpdated = updateContact(priorityMap.get(5), contact);
        } else if (priorityMap.get(6) != null) {
            isUserContactUpdated = updateContact(priorityMap.get(6), contact);
        }
        return isUserContactUpdated;
    }

    private boolean updateContactBasedOnPriorityForNonWatuNumbers(Map<Integer, ContactNumberDTO> priorityMap, UserContact contact) {
        boolean isUserContactUpdated = false;
        if (priorityMap.get(1) != null) {
            isUserContactUpdated = updateContactForNonWatuNumbers(priorityMap.get(1), contact);
        } else if (priorityMap.get(2) != null) {
            isUserContactUpdated = updateContactForNonWatuNumbers(priorityMap.get(2), contact);
        } else if (priorityMap.get(3) != null) {
            isUserContactUpdated = updateContactForNonWatuNumbers(priorityMap.get(3), contact);
        } else if (priorityMap.get(4) != null) {
            isUserContactUpdated = updateContactForNonWatuNumbers(priorityMap.get(4), contact);
        } else if (priorityMap.get(5) != null) {
            isUserContactUpdated = updateContactForNonWatuNumbers(priorityMap.get(5), contact);
        } else if (priorityMap.get(6) != null) {
            isUserContactUpdated = updateContactForNonWatuNumbers(priorityMap.get(6), contact);
        }
        return isUserContactUpdated;
    }

    private boolean updateContactForNonWatuNumbers(ContactNumberDTO userContactNumber, UserContact contact) {
        boolean isUserContactUpdated = false;
        if (userContactNumber != null && contact != null) {
            contact.setContactNumber(userContactNumber.getContactNumberWithDefaultCountryCode(
                    CountryCode.US_CANADA_COUNTRY_CODE.getValue()));
            isUserContactUpdated = true;
        }
        return isUserContactUpdated;
    }

    private boolean updateContact(ContactNumberDTO userContactNumber, Object contact) {
        boolean isUserContactUpdated = false;
        PeopleUser verifiedUser = peopleUserRepository.findByCodeAndNumber(
                userContactNumber.getCountryCode(), userContactNumber.getPhoneNumber());
        if (verifiedUser != null) {
            if (contact instanceof UserContact) {
                UserContact userContact = (UserContact) contact;
                userContact.setUserId(verifiedUser.getUserId());
                userContact.setContactNumber(verifiedUser.getVerifiedContactNumber());
                isUserContactUpdated = true;
            } else if (contact instanceof NetworkInviteeContact) {
                NetworkInviteeContact inviteeContact = (NetworkInviteeContact) contact;
                inviteeContact.setInviteeUserId(verifiedUser.getUserId());
                inviteeContact.setContactNumber(verifiedUser.getVerifiedContactNumber());
                isUserContactUpdated = true;
            } else {
                throw new BadRequestException(MessageCodes.INVALID_PROPERTY.getValue());
            }

        }
        return isUserContactUpdated;
    }

    /**
     * Merge metaList1 into metaList2, which does not exists in metaList2 (to avoid duplicacy)
     *
     * @param metaList1
     * @param metaList2 Used in merging SharedMetaList into StaticMetaList
     *                  Also used in merging deletedMetaList into StaticMetaList
     */
    @Override
    public List<UserProfileData> mergeMetaList(List<UserProfileData> metaList1,
                                               List<UserProfileData> metaList2) {
        List<UserProfileData> metaListToBeAdded = new ArrayList<>();
        if (metaList2 == null) {
            metaList2 = new ArrayList<>();
        }
        for (UserProfileData sharedProfileData : metaList1) {
            boolean isProfileDataExisting = false;
            for (UserProfileData staticProfileData : metaList2) {
                if (sharedProfileData.getLabel().equalsIgnoreCase(staticProfileData.getLabel()) &&
                        sharedProfileData.getKeyValueDataList().equals(staticProfileData.getKeyValueDataList())) {
                    isProfileDataExisting = true;
                    break;
                }
            }
            // add profile data if not exists
            if (!isProfileDataExisting) {
                metaListToBeAdded.add(sharedProfileData);
            }
        }
        metaList2.addAll(metaListToBeAdded);
        return metaList2;
    }

    private List<UserProfileData> prepareProfileDataFromValueId(Map<String, UserProfileData> metadataMap,
                                                                List<String> valueIdList) {

        List<UserProfileData> userProfileDataList = new ArrayList<>();
        Set<String> valueIdSet = new HashSet<>(valueIdList);
        for (String valueId : PeopleUtils.emptyIfNull(valueIdSet)) {
            UserProfileData userProfileData = metadataMap.getOrDefault(valueId, null);
            if (userProfileData != null) {
                userProfileDataList.add(userProfileData);
            }
        }

        return userProfileDataList;
    }

    private boolean assignPriorityToUserprofile(UserProfileData userProfile, Map<Integer, ContactNumberDTO> priorityMap) {
        if (userProfile.getLabel().equalsIgnoreCase(ContactPriority.PRIORITY_LABEL_MOBILE.getValue())) {
            priorityMap.put(1, userProfile.getContactNumber());
            return true;
        } else if (userProfile.getLabel().equalsIgnoreCase(ContactPriority.PRIORITY_LABEL_IPHONE.getValue())) {
            priorityMap.put(2, userProfile.getContactNumber());
        } else if (userProfile.getLabel().equalsIgnoreCase(ContactPriority.PRIORITY_LABEL_MAIN.getValue())) {
            priorityMap.put(3, userProfile.getContactNumber());
        } else if (userProfile.getLabel().equalsIgnoreCase(ContactPriority.PRIORITY_LABEL_HOME.getValue())) {
            priorityMap.put(4, userProfile.getContactNumber());
        } else if (userProfile.getLabel().equalsIgnoreCase(ContactPriority.PRIORITY_LABEL_WORK.getValue())) {
            priorityMap.put(5, userProfile.getContactNumber());
        } else {
            priorityMap.put(6, userProfile.getContactNumber());
        }
        return false;
    }

}
