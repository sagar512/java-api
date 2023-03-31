package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.UserContact;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class SendMultiIntroRequestDTO {

    @NotEmpty
    @Size(min = 2, max = 50)
    @Valid
    private List<UserContact> contactDetailsList;

    private String message;

}
