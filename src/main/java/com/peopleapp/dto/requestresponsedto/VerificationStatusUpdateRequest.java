package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.KeyValueData;
import com.peopleapp.enums.UserInformationVerification;
import com.peopleapp.validator.EnumValidator;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class VerificationStatusUpdateRequest {

    @NotNull
    private String valueId;

    @NotNull
    @EnumValidator(enumClazz = UserInformationVerification.class)
    private String verificationStatus;

    private String socialProfileId;

    private String socialProfileImageURL;

    @Valid
    @NotEmpty
    private List<KeyValueData> keyValueDataList;
}
