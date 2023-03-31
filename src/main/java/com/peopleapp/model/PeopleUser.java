package com.peopleapp.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.peopleapp.constant.PeopleCollectionKeys;
import com.peopleapp.dto.ContactNumberDTO;
import com.peopleapp.dto.Coordinates;
import com.peopleapp.dto.KeyValueData;
import com.peopleapp.dto.NetworkCommunicationSettingStatus;
import com.peopleapp.dto.ProfileKey;
import com.peopleapp.dto.TagData;
import com.peopleapp.dto.TimestampData;
import com.peopleapp.dto.UserProfileData;
import com.peopleapp.dto.requestresponsedto.SocialMediaCategoryResponse;
import com.peopleapp.enums.UserInfoCategory;
import com.peopleapp.enums.UserInformationVerification;
import com.peopleapp.enums.UserStatus;
import com.peopleapp.util.PeopleUtils;

import lombok.Data;
import lombok.Getter;

@Document(collection = "peopleUsers")
@CompoundIndexes({
        @CompoundIndex(name = "id_status", def = "{'_id': 1, 'status': -1}")
})
@Data
@TypeAlias("peopleUsers")
public class PeopleUser {

    private static String primaryNumberLabel = "PL.00.00";

    private static String primaryEmailLabel = "PL.01.04";

    @Id
    @Field(value = "_id")
    private ObjectId userId;

    @Getter
    private ContactNumberDTO verifiedContactNumber;

    private Map<Integer, UserInformationVerification> emailAddressMap;

    private Map<Integer, UserInformationVerification> socialHandleMap;

    private String primaryEmail;

    private TimestampData name;

    private  String fullName;

    private TimestampData company;

    private TimestampData position;

    private SortedMap<String, TagData> tagMap;

    private String namePrefix;

    private TimestampData firstName;

    private TimestampData middleName;

    private TimestampData lastName;

    private TimestampData nickName;

    private String phoneticFirstName;

    private String phoneticMiddleName;

    private String phoneticLastName;

    private TimestampData maidenName;

    private String nameSuffix;

    private String gender;

    private String notes;

    private DateTime lastUpdatedOn;

    private List<UserProfileData> userMetadataList;

    private UserStatus status;

    private Boolean isFlagged;

    private String flaggedReason;

    private String defaultImageUrl;

    private List<String> networkSharedValueList;

    private NetworkCommunicationSettingStatus networkCommunicationTypesSelected = new NetworkCommunicationSettingStatus();

    private String deleteAccountReason;

    private Set<String> blockedUserIdList;

    private Boolean isPushNotificationEnabled = Boolean.TRUE;

    private Coordinates deviceLocation;

    private String referralCode;

    private String bluetoothToken;

    private DateTime createdOn;
    
    private String birthday;
    
    private String city;
    
    private String designation;
    
    private String department;
    
    private String website;
    
    private List<SocialMediaCategoryResponse> socialMediaCategories=new ArrayList<>();
    
   public String getUserId() {
        return this.userId.toString();
    }

    public Set<String> getBlockedUserIdList() {
        return !PeopleUtils.isNullOrEmpty(this.blockedUserIdList) ? this.blockedUserIdList : new HashSet<>();
    }

    public List<String> getProfileTags() {
        List<String> profileTags = new ArrayList<>();
        if (this.tagMap == null) {
            return profileTags;
        }
        for (Map.Entry<String, TagData> entry : this.tagMap.entrySet()) {
            if (entry.getValue().getIsProfileTag()) {
                profileTags.add(entry.getKey());
            }
        }
        return profileTags;
    }

    public Map<String, UserProfileData> getMetadataMap() {
        Map<String, UserProfileData> metaDataMap = new HashMap<>();
        for (UserProfileData userProfileData : PeopleUtils.emptyIfNull(this.userMetadataList)) {
            metaDataMap.put(userProfileData.getValueId(), userProfileData);
        }
        return metaDataMap;
    }

    private String prepareName() {
        String first = getFirstNameValue();
        String last = getLastNameValue();
        String companyName = getCompanyValue();
        String fullname = null;
        if (!PeopleUtils.isNullOrEmpty(first) && !PeopleUtils.isNullOrEmpty(last)) {
            fullname = StringUtils.capitalize(first.toLowerCase()).concat(" ").concat(StringUtils.capitalize(last.toLowerCase()));

        } else if (PeopleUtils.isNullOrEmpty(first) && !PeopleUtils.isNullOrEmpty(last)) {
            fullname = StringUtils.capitalize(last.toLowerCase());
        } else if (PeopleUtils.isNullOrEmpty(last) && !PeopleUtils.isNullOrEmpty(first)) {
            fullname = StringUtils.capitalize(first.toLowerCase());
        } else if (PeopleUtils.isNullOrEmpty(first) && PeopleUtils.isNullOrEmpty(last) &&
                !PeopleUtils.isNullOrEmpty(companyName)){
            fullname = StringUtils.capitalize(companyName.toLowerCase());
        }
        return fullname != null ? fullname : "Watu user";
    }


    public String getNameValue() {

        return this.name != null && this.name.getValue() != null ? this.name.getValue() : prepareName();
    }

    public void setName(String name) {
        TimestampData nameData = new TimestampData();
        nameData.setValue(name);
        nameData.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
        this.name = nameData;
    }

    public String getFirstNameValue() {
        return this.firstName != null ? this.firstName.getValue() : null;
    }

    public String getMiddleNameValue() {
        return this.middleName != null ? this.middleName.getValue() : null;
    }

    public String getLastNameValue() {
        return this.lastName != null ? this.lastName.getValue() : null;
    }

    public String getCompanyValue() {
        return this.company != null ? this.company.getValue() : null;
    }

    public String getPositionValue() {
        return this.position != null ? this.position.getValue() : null;
    }

    public String getNickNameValue() {
        return this.nickName != null ? this.nickName.getValue() : null;
    }

    public String getMaidenNameValue() {
        return this.maidenName != null ? this.maidenName.getValue() : null;
    }

    public void setFirstName(String firstName) {
        TimestampData nameData = new TimestampData();
        nameData.setValue(firstName);
        nameData.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
        this.firstName = nameData;
    }

    public void setMiddleName(String middleName) {
        TimestampData nameData = new TimestampData();
        nameData.setValue(middleName);
        nameData.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
        this.middleName = nameData;
    }

    public void setLastName(String lastName) {
        TimestampData nameData = new TimestampData();
        nameData.setValue(lastName);
        nameData.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
        this.lastName = nameData;
    }

    public void setCompany(String company) {
        TimestampData data = new TimestampData();
        data.setValue(company);
        data.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
        this.company = data;
    }

    public void setPosition(String position) {
        TimestampData data = new TimestampData();
        data.setValue(position);
        data.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
        this.position = data;
    }

    public void setNickName(String nickName) {
        TimestampData data = new TimestampData();
        data.setValue(nickName);
        data.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
        this.nickName = data;
    }

    public void setMaidenName(String maidenName) {
        TimestampData data = new TimestampData();
        data.setValue(maidenName);
        data.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
        this.maidenName = data;
    }

    public void setVerifiedContactNumber(ContactNumberDTO contactNumber) {
        this.verifiedContactNumber = contactNumber;

        // set in metadata
        List<UserProfileData> userProfileDataList;
        if (this.userMetadataList == null) {
            userProfileDataList = new ArrayList<>();
            UserProfileData userProfileData = new UserProfileData();
            userProfileData.setCategory(UserInfoCategory.CONTACT_NUMBER.getValue());
            userProfileData.setLabel(primaryNumberLabel);
            userProfileData.setValueId(new ObjectId().toString());
            userProfileData.setIsPrimary(Boolean.TRUE);
            userProfileDataList.add(userProfileData);

            // adding primary number as network default value
            this.networkSharedValueList = new ArrayList<>();
            this.networkSharedValueList.add(userProfileData.getValueId());
            // setting status of phone number(primary) which is added as default network setting value
            this.networkCommunicationTypesSelected.setDefaultPhoneNumberAdded(Boolean.TRUE);
        } else {
            userProfileDataList = this.userMetadataList;
        }

        // search and update value
        updateVerifiedNumberInMetadata(userProfileDataList, contactNumber);
    }

    private void updateVerifiedNumberInMetadata(List<UserProfileData> userProfileDataList, ContactNumberDTO contactNumber) {

        for (UserProfileData userProfileData : PeopleUtils.emptyIfNull(userProfileDataList)) {
            if (UserInfoCategory.CONTACT_NUMBER.getValue().equalsIgnoreCase(userProfileData.getCategory()) &&
                    userProfileData.getIsPrimary()) {
                // update verified number
                List<KeyValueData> keyValueDataList = new ArrayList<>();
                KeyValueData countryCode = new KeyValueData();
                countryCode.setKey(PeopleCollectionKeys.COUNTRY_CODE_KEY);
                countryCode.setVal(contactNumber.getCountryCode());
                keyValueDataList.add(countryCode);
                KeyValueData number = new KeyValueData();
                number.setKey(PeopleCollectionKeys.PHONE_NUMBER_KEY);
                number.setVal(contactNumber.getPhoneNumber());
                keyValueDataList.add(number);
                userProfileData.setKeyValueDataList(keyValueDataList);
                userProfileData.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
                break;
            }
        }

        this.userMetadataList = userProfileDataList;

    }

    public Map<ProfileKey, List<String>> getProfileKeyMap() {

        Map<ProfileKey, List<String>> profileKeyListMap = new HashMap<>();
        for (UserProfileData userProfileData : PeopleUtils.emptyIfNull(this.userMetadataList)) {
            ProfileKey profileKey = new ProfileKey();
            profileKey.setCategory(userProfileData.getCategory());
            profileKey.setLabel(userProfileData.getLabel());
            List<String> valueIdList;
            if (profileKeyListMap.containsKey(profileKey)) {
                valueIdList = profileKeyListMap.get(profileKey);
                valueIdList.add(userProfileData.getValueId());
            } else {
                valueIdList = new ArrayList<>();
                valueIdList.add(userProfileData.getValueId());
                profileKeyListMap.put(profileKey, valueIdList);
            }
        }
        return profileKeyListMap;
    }

    public SortedMap<String, TagData> getTagMap() {
        return this.tagMap != null ? this.tagMap : new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }


    public Map<Integer, UserInformationVerification> getSocialHandleMap() {
        return socialHandleMap != null ? socialHandleMap : new HashMap<>();
    }

    public Map<Integer, UserInformationVerification> getEmailAddressMap() {
        return emailAddressMap != null ? emailAddressMap : new HashMap<>();
    }

    // update primary email
    public void updatePrimaryEmailInMetadata(String email) {

        List<UserProfileData> userProfileDataList = this.userMetadataList;
        UserProfileData primaryEmailData = null;
        Boolean isPrimaryEmailLinked = Boolean.FALSE;

        if (PeopleUtils.isNullOrEmpty(userProfileDataList)) {
            userProfileDataList = new ArrayList<>();
        }

        for (UserProfileData userProfileData : PeopleUtils.emptyIfNull(userProfileDataList)) {

            if (UserInfoCategory.EMAIL_ADDRESS.getValue().equals(userProfileData.getCategory()) && userProfileData.getIsPrimary()) {
                primaryEmailData = userProfileData;
                isPrimaryEmailLinked = Boolean.TRUE;
                break;
            }
        }
        if (primaryEmailData == null) {

            // create primary email data
            primaryEmailData = new UserProfileData();
            primaryEmailData.setValueId(new ObjectId().toString());
            primaryEmailData.setIsPrimary(Boolean.TRUE);
            primaryEmailData.setCategory(UserInfoCategory.EMAIL_ADDRESS.getValue());
            primaryEmailData.setLabel(primaryEmailLabel);

        }

        KeyValueData emailData = new KeyValueData();
        emailData.setKey(PeopleCollectionKeys.EMAIL_ADDRESS_KEY);
        emailData.setVal(email);
        List<KeyValueData> keyValueDataList = Collections.singletonList(emailData);
        primaryEmailData.setKeyValueDataList(keyValueDataList);

        if (!isPrimaryEmailLinked) {
            userProfileDataList.add(primaryEmailData);
        }

        this.userMetadataList = userProfileDataList;
    }


}
