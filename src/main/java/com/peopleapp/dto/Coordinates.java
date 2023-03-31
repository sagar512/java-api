package com.peopleapp.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class Coordinates {

    @NotNull
    private double latitude;

    @NotNull
    private double longitude;
}
