package com.peopleapp.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class GroupImage {

    @NotEmpty
    private String groupId;

    @NotEmpty
    private String imageURL;
}
