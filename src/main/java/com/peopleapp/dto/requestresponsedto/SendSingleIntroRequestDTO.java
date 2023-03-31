package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.UserContact;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class SendSingleIntroRequestDTO {

    @NotNull
    private UserContact introducedContact;

    @NotEmpty
    private List<UserContact> initiateContactDetailsList;

    private String message;
}