package com.peopleapp.dto;

import lombok.Data;

import java.util.List;

@Data
public class Address {

    private String street;
    private String city;
    private String country;
    private String state;
    private String pincode;
    private String label;
    private List<Double> coordinates;
    private String type;
}
