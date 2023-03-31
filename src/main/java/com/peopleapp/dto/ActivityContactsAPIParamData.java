package com.peopleapp.dto;

import lombok.Data;

@Data
public class ActivityContactsAPIParamData {

    private String searchString;

    private String activityId;

    private String initiatorId;

    private String receiverId;

    private Integer fNameOrder;

    private Integer lNameOrder;

    private Boolean lNamePreferred;

    private Integer pageNumber;

    private Integer pageSize;

}
