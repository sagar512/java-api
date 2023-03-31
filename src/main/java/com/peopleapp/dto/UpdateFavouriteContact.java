package com.peopleapp.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class UpdateFavouriteContact {

    @NotEmpty
    private String connectionId;

    @NotNull
    private Boolean isFavourite;
}
