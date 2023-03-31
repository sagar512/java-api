package com.peopleapp.dto;

import com.peopleapp.enums.UserInfoCategory;
import com.peopleapp.validator.EnumValidator;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class ProfileKey {

    @NotEmpty
    @EnumValidator(enumClazz = UserInfoCategory.class)
    private String category;

    private String label;

}
