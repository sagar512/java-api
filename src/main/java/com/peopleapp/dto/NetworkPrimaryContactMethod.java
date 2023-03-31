package com.peopleapp.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class NetworkPrimaryContactMethod {

    @NotEmpty
    private String contactCategory;

    @NotEmpty
    private String contactLabel;

}
