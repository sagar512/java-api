package com.peopleapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.peopleapp.enums.CountryCode;
import com.peopleapp.enums.UserInfoCategory;
import com.peopleapp.enums.UserInformationVerification;
import com.peopleapp.util.PeopleUtils;
import com.peopleapp.validator.EnumValidator;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.joda.time.DateTime;
import org.springframework.data.annotation.Transient;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileData {

    private static final String COUNTRY_CODE_KEY = "countryCode";
    private static final String PHONE_NUMBER_KEY = "phoneNumber";

    private String valueId;

    @NotEmpty
    @EnumValidator(enumClazz = UserInfoCategory.class)
    private String category;

    private String label;

    @NotEmpty
    @Valid
    private List<KeyValueData> keyValueDataList;

    @Transient
    private UserInformationVerification verification;

    private Boolean isPrimary = Boolean.FALSE;

    @ApiModelProperty(hidden = true)
    @JsonIgnore
    private DateTime lastUpdatedOn;

    private String socialProfileId;

    private String socialProfileImageURL;

    @JsonIgnore
    public String getSingleValueData() {
        return this.keyValueDataList.size() == 1 ? this.keyValueDataList.get(0).getVal() : null;
    }

    @JsonIgnore
    public ContactNumberDTO getContactNumber() {

        ContactNumberDTO contactNumber = new ContactNumberDTO();
        for (KeyValueData keyValueData : PeopleUtils.emptyIfNull(this.keyValueDataList)) {
            if (COUNTRY_CODE_KEY.equalsIgnoreCase(keyValueData.getKey())) {
                contactNumber.setCountryCode(keyValueData.getVal());
            } else if (PHONE_NUMBER_KEY.equalsIgnoreCase(keyValueData.getKey())) {
                contactNumber.setPhoneNumber(keyValueData.getVal());
            }
        }

        if (PeopleUtils.isNullOrEmpty(contactNumber.getCountryCode())) {
            // setting the default country code
            contactNumber.setCountryCode(CountryCode.US_CANADA_COUNTRY_CODE.getValue());
        }

        return contactNumber;
    }

    public void setCategory(String category) {
        this.category = category.toUpperCase();
    }
}
