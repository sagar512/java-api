package com.peopleapp.service;

import com.peopleapp.dto.PrivacyProfileData;
import com.peopleapp.dto.SQSPayload;
import com.peopleapp.dto.requestresponsedto.*;
import com.peopleapp.model.PeopleUser;
import com.peopleapp.model.UserActivity;
import com.peopleapp.model.UserPrivacyProfile;

public interface PrivacyProfileService {

    GetListOfPrivacyProfilesResponseDTO getListOfUserPrivacyProfiles();

    DeletePrivacyProfileResponseDTO deleteUserPrivacyProfile(DeletePrivacyProfileRequestDTO deletePrivacyProfileRequest);

    CreateOrEditPrivacyProfileResponse createCustomProfile(CreateCustomProfileRequestDTO createCustomProfileRequestDTO);

    CreateOrEditPrivacyProfileResponse createCustomProfileNew(CreateCustomProfileRequestDTO createCustomProfileRequestDTO);

    CreateOrEditPrivacyProfileResponse editPrivacyProfile(EditPrivacyProfileRequestDTO editPrivacyProfileRequestDTO);

    CreateOrEditPrivacyProfileResponse editPrivacyProfileNew(EditPrivacyProfileRequestDTO editPrivacyProfileRequestDTO);

    String setDefaultProfile(SetDefaultProfileRequestDTO setDefaultProfileRequestDTO);

    UserPrivacyProfile getPrivacyProfileById(String privacyProfileId);

    void shareTag(ShareTagRequest shareTagRequest);

    PrivacyProfileData populatePrivacyProfileData(UserPrivacyProfile userProfile, String defaultImageURL);

    SQSPayload prepareSQSPayloadForUpdateContactActivity(UserActivity userActivity, PeopleUser sessionUser,
                                                         String connectionId);
}
