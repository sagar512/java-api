package com.peopleapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peopleapp.dto.requestresponsedto.ShareProfileResponse;
import com.peopleapp.dto.requestresponsedto.ShareProfileData;

import lombok.Data;
import org.joda.time.DateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrivacyProfileData {

    private String privacyProfileId;

    private String profileName;

    private String imageURL;

    private Boolean isDefault;

    private Boolean isPublic;

    private Boolean isSystem;

    private List<String> valueIdList;

    private Boolean isCompanyShared;

    private Boolean isPositionShared;

    private Boolean isTagShared;

    private Boolean isNameShared;

    private Boolean isNickNameShared;

    private Boolean isMaidenNameShared;
    
    private List<ShareProfileResponse> shareProfileData;
    
    private ShareProfileData sharePrivacyData;
    
    private DateTime createdOn;

    private DateTime lastUpdatedOn;

}
