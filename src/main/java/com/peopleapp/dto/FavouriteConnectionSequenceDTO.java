package com.peopleapp.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class FavouriteConnectionSequenceDTO {

    @NotBlank
    private String connectionId;

    @NotNull
    private Integer sequenceNumber;
}
