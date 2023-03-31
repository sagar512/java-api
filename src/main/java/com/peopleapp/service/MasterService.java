package com.peopleapp.service;

import com.peopleapp.dto.ContactNumberDTO;
import com.peopleapp.dto.UserContact;
import com.peopleapp.dto.UserInformationDTO;
import com.peopleapp.dto.UserProfileData;
import com.peopleapp.model.PeopleUser;
import com.peopleapp.model.UserConnection;
import com.peopleapp.model.UserPrivacyProfile;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface MasterService {

    UserInformationDTO prepareUserDataBasedOnPrivacyProfile(PeopleUser peopleUser, UserPrivacyProfile userPrivacyProfile);
    
    UserInformationDTO prepareUserPublicData(PeopleUser peopleUser);

    UserInformationDTO prepareSharedData(UserConnection userConnection);
    
    UserInformationDTO prepareSharedData1(UserConnection userConnection);

    /* Returns registered numbers list */
    Set<String> getRegisteredNumberList();

    /**
     * Merges the shared information into the static information and remove duplicates
     * Priority is given to the shared information object
     */
    void mergeSharedInfoToStaticInfo(UserInformationDTO sharedInformationDTO, UserInformationDTO staticInformationDTO);

    boolean isPushNotificationEnabledForUser(String userId);

    boolean isUserBlockedByContact(String contactUserId, String userId);

    Map<Integer, ContactNumberDTO> prepareContactNumberPriorityList(List<UserProfileData> userProfileData);

    boolean updateContactForStaticContactWithVerifiedNumber(List<UserProfileData> userProfileData, Object contact);

    boolean updateContactForNonWatuRegisteredNumbers(List<UserProfileData> userProfileData, UserContact contact);

    List<UserProfileData> mergeMetaList(List<UserProfileData> metaList1, List<UserProfileData> metaList2);

    List<UserProfileData> getDeletedMetaList(PeopleUser sessionUser, List<String> oldValueIds,
                                             List<String> newValueIds);

    boolean isValidCanadaOrUSANumber(ContactNumberDTO contactNumber);

}
