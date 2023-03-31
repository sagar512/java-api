package com.peopleapp.repository;

import com.peopleapp.model.UserPrivacyProfile;

import java.util.List;

public interface CustomPrivacyProfileRepository {

    List<UserPrivacyProfile> deleteValidPrivacyProfiles(String userId, List<String> profileIdList);

    List<UserPrivacyProfile> findAllByUserId(String userId);

    List<UserPrivacyProfile> findAllByUserIdAndValueIds(String userId, List<String> valueIds);

    UserPrivacyProfile findByProfileIdAndUserId(String profileId, String userId);

    UserPrivacyProfile findDefaultUserProfile(String userId, Boolean isDefault);

    UserPrivacyProfile findPublicProfile(String userId);

    List<UserPrivacyProfile> findSystemProfilesForUser(String userId);

    void updatePrivacyProfileDefaultImage(String userId, String imageURL);

}
