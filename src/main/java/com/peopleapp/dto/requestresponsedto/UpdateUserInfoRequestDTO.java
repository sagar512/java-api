package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.UserInformationDTO;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
public class UpdateUserInfoRequestDTO {

    @NotNull
    @Valid
    private UserInformationDTO userDetails;
}
