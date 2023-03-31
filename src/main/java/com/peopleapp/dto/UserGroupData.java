package com.peopleapp.dto;

import lombok.Data;
import org.springframework.format.annotation.NumberFormat;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class UserGroupData {

    @NotEmpty
    private String title;

    private String desc;

    @NotEmpty
    private String imageURL;

    @NumberFormat
    private Integer orderNumber;

    private List<String> contactIdList;

    private String localId;

    private Boolean isFavourite;

}
