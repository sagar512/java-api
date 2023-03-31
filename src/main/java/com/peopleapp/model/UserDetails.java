package com.peopleapp.model;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Map;


@Data
public class UserDetails {

    @NotNull
    private String categoryName;

    private String categoryLabel;

    private Map<String, String> categoryKeys;

}
