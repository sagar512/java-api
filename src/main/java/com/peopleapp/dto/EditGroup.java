package com.peopleapp.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class EditGroup {

    @NotEmpty
    private String groupId;

    @NotEmpty
    private String title;

    private String desc;

    private String imageURL;

    private List<String> contactIdList;

    private String localId;

    private Boolean isFavourite;
}
