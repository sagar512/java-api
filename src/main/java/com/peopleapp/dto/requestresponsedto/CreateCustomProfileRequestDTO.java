package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.PrivacyProfileData;
import com.peopleapp.dto.UserProfileData;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class CreateCustomProfileRequestDTO {

    @NotNull
    @Valid
    private PrivacyProfileData privacyProfileData;

    @Valid
    private List<UserProfileData> userMetadataList;

}
