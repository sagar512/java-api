package com.peopleapp.dto.requestresponsedto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peopleapp.dto.PrivacyProfileData;
import com.peopleapp.dto.UserProfileData;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CreateOrEditPrivacyProfileResponse {

    private PrivacyProfileData privacyProfileData;

    private List<UserProfileData> userMetadataList;

}
