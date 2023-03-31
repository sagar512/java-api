package com.peopleapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.peopleapp.dto.requestresponsedto.GroupNameResponse;
import com.peopleapp.dto.requestresponsedto.UserPrivacyProfileDTO;
import com.peopleapp.enums.UserInfoCategory;
import com.peopleapp.enums.UserInformationVerification;
import com.peopleapp.model.PeopleUser;
import com.peopleapp.util.PeopleUtils;
import lombok.Data;
import org.springframework.data.annotation.Transient;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserInformationDTO {

    @Transient
    private String userId;

    private ContactNumberDTO contactNumber;

    @JsonProperty("contactId")
    private String deviceContactId;

    private String name;

    private String firstName;

    private String middleName;

    private String lastName;

    private String fullName;

    private String phoneticFirstName;

    private String phoneticMiddleName;

    private String phoneticLastName;

    private String namePrefix;

    private String nameSuffix;

    private String maidenName;

    private String nickName;

    private String gender;

    private String company;
    
    private String department;

    private String position;

    private String imageURL;

    private String notes;

    private String sip;

    private List<GroupNameResponse> groupNameList = new ArrayList<>();

    private List<String> tagList = new ArrayList<>();

    private Boolean isFavourite;

    private Integer sequenceNumber;
    
    private String birthday;
    
    private String accountName;
    
    private String accountType;
    
    private Boolean isIdentical = Boolean.FALSE;
    
    private UserPrivacyProfileDTO userPrivacyProfile;

    @Valid
    private List<UserProfileData> userMetadataList = new ArrayList<>();


    public void setUserMetadataList(List<UserProfileData> userMetadataList) {

        for(UserProfileData userProfileData : PeopleUtils.emptyIfNull(userMetadataList)) {
            userProfileData.setIsPrimary(Boolean.FALSE);
        }
        this.userMetadataList = userMetadataList;
    }

    public void setUserMetadataList(PeopleUser peopleUser, List<UserProfileData> userMetadataList) {

        Map<Integer, UserInformationVerification> emailMap = peopleUser.getEmailAddressMap();
        Map<Integer, UserInformationVerification> socialMap = peopleUser.getSocialHandleMap();

        for(UserProfileData userProfileData : PeopleUtils.emptyIfNull(userMetadataList)) {

            UserInfoCategory category = UserInfoCategory.getByValue(userProfileData.getCategory());

            switch (category) {

                case CONTACT_NUMBER:
                    if(userProfileData.getIsPrimary()) {
                        userProfileData.setVerification(UserInformationVerification.VERIFIED);
                    }
                    break;
                case EMAIL_ADDRESS:
                    String email = userProfileData.getSingleValueData();
                    userProfileData.setVerification(emailMap.getOrDefault(email.hashCode(), UserInformationVerification.NOT_VERIFIED));
                    break;
                case SOCIAL_PROFILE:
                case INSTANT_MESSAGING:
                    String socialHandle = userProfileData.getSingleValueData();
                    userProfileData.setVerification(socialMap.getOrDefault(socialHandle.hashCode(), UserInformationVerification.NOT_VERIFIED));
                    break;
                default:
                    break;
            }
        }

        this.userMetadataList = userMetadataList;
    }




    @JsonIgnore
    public List<String> getAllMobileNumbers() {

        List<String> mobileNumberList = new ArrayList<>();
        for (UserProfileData profileData : PeopleUtils.emptyIfNull(this.userMetadataList)) {
            if (UserInfoCategory.CONTACT_NUMBER.getValue().equalsIgnoreCase(profileData.getCategory())) {
                mobileNumberList.add(profileData.getContactNumber().getMobileNumber());
            }
        }
        return mobileNumberList;
    }

}
