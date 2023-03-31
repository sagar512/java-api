package com.peopleapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.peopleapp.util.PeopleUtils;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContactNumberDTO {
    public ContactNumberDTO(String countryCode, String phoneNumber) {
        this.countryCode = countryCode;
        this.phoneNumber = phoneNumber;
    }

    public ContactNumberDTO() {
    }

    private String countryCode;

    @NotEmpty
    private String phoneNumber;

    @JsonIgnore
    public String getMobileNumber() {

        String contactNumber = "";

        if (!PeopleUtils.isNullOrEmpty(this.countryCode) && !PeopleUtils.isNullOrEmpty(this.phoneNumber)) {
            contactNumber = this.countryCode.concat(this.phoneNumber);
        } else if (!PeopleUtils.isNullOrEmpty(countryCode)) {
            contactNumber = this.countryCode;
        } else if (!PeopleUtils.isNullOrEmpty(this.phoneNumber)) {
            contactNumber = this.phoneNumber;
        }
        return contactNumber;
    }

    @JsonIgnore
    public String getMobileNumberWithDefaultCountryCode(String defaultCountryCode) {

        if (PeopleUtils.isNullOrEmpty(this.countryCode)) {
            this.countryCode = defaultCountryCode;
        }
        return this.countryCode.concat(this.phoneNumber);
    }

    @JsonIgnore
    public ContactNumberDTO getContactNumberWithDefaultCountryCode(String defaultCountryCode) {

        ContactNumberDTO contactNumber = new ContactNumberDTO();
        contactNumber.setPhoneNumber(this.phoneNumber);
        contactNumber.setCountryCode(this.countryCode);
        if (PeopleUtils.isNullOrEmpty(this.countryCode)) {
            contactNumber.setCountryCode(defaultCountryCode);
        }
        return contactNumber;
    }
}
