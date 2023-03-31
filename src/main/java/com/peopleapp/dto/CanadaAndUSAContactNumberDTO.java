package com.peopleapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class CanadaAndUSAContactNumberDTO {

    private String countryCode;

    @Pattern(regexp = "[\\d]*")
    @Size(min = 10, max = 10, message = "PhoneNumber must be of 10 digit")
    @NotEmpty
    private String phoneNumber;

    @JsonIgnore
    public ContactNumberDTO getContactNumberDTO() {
        return new ContactNumberDTO(this.countryCode, this.phoneNumber);
    }
}
