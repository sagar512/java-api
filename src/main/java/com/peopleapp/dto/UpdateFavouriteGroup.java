package com.peopleapp.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class UpdateFavouriteGroup {

    @NotEmpty
    private String groupId;

    @NotNull
    private Boolean isFavourite;
}
