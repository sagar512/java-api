package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class UpdateNetworkFavouriteRequestDTO {

    @NotBlank
    private String networkId;

    @NotNull
    private boolean isNetworkFavorite;
}
