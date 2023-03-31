package com.peopleapp.dto.requestresponsedto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.peopleapp.dto.CanadaAndUSAContactNumberDTO;
import com.peopleapp.enums.MessageCodes;
import com.peopleapp.exception.BadRequestException;
import com.peopleapp.util.PeopleUtils;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Email;

@Data
public class ConnectRequestDTO {

    @Valid
	private CanadaAndUSAContactNumberDTO contactNumber;

    @Email(message = "Email should be valid")
    private String emailId;

    @JsonIgnore
    public void checkIfValidConnectRequest() {

        if((this.contactNumber == null && PeopleUtils.isNullOrEmpty(this.emailId)) ||
                (this.contactNumber != null && this.emailId != null)) {
            throw new BadRequestException(MessageCodes.INVALID_PROPERTY.getValue());
        }
    }

}


