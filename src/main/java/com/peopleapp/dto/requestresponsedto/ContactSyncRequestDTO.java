package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.UserInformationDTO;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class ContactSyncRequestDTO {

    @NotEmpty
    @Valid
    private List<UserInformationDTO> userContactList;

    private Boolean isOnBoard;
}
