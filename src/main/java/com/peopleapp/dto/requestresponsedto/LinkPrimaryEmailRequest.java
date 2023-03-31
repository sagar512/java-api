package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;

@Data
public class LinkPrimaryEmailRequest {

    @Email(message = "Email should be valid")
    @NotEmpty
    private String emailId;
}
