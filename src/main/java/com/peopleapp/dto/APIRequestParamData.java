package com.peopleapp.dto;

import lombok.Data;

@Data
public class APIRequestParamData {

    private String searchString;

    private Integer pageNumber;

    private Integer pageSize;

    private Integer sortByRole;

    private Integer fNameOrder;

    private Integer lNameOrder;

    private Boolean lastNamePreferred;
}
