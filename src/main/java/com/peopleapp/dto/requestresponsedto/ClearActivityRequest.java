package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

import java.util.List;

@Data
public class ClearActivityRequest {

    private List<String> activityIdList;

    private Boolean isAllActivityCleared = Boolean.FALSE;
}
