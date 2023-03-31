package com.peopleapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UserContactData {

    private String connectionId;

    @JsonProperty("contactId")
    private String deviceContactId;

    private String toUserId;

    private String connectionStatus;

    private Boolean isBlocked = Boolean.FALSE;

    private Boolean isFavourite = Boolean.FALSE;

    private Integer sequenceNumber;

    private List<String> groupIdList;

    private PrivacyProfileData sharedPrivacyProfileWithContact;

    private PrivacyProfileData sharedPrivacyProfileByContact;

    private UserInformationDTO staticProfileData;

    private UserInformationDTO sharedProfileData;

    private UserInformationDTO publicProfileData;

    private UserInformationDTO deleteProfileData;

    private NetworkSharedInformationDTO networkSharedInformationData;

    private String activitySubId;

    private List<String> activitySubIdList;

    public String getActivitySubId() {
        return this.activitySubId != null ? this.activitySubId : null;
    }

    public void setActivitySubId(String activitySubId) {
        if (activitySubId != null) {
            this.activitySubId = activitySubId;
        }
    }

}
