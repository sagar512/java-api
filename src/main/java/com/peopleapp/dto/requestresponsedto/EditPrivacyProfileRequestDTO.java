package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.PrivacyProfileData;
import com.peopleapp.dto.UserProfileData;
import lombok.Data;

import java.util.List;

@Data
public class EditPrivacyProfileRequestDTO {

    private PrivacyProfileData privacyProfileData;

    private List<UserProfileData> userMetadataList;
}
