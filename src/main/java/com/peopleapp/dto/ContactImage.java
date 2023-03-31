package com.peopleapp.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class ContactImage {

    @NotEmpty
    private String connectionId;

    @NotNull
    private String imageURL;
}
