package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.UserActivityData;
import lombok.Data;

import java.util.List;

@Data
public class ActivityListResponse {

    private List<UserActivityData> userActivityList;

    private int totalNumberOfPages;

    private long totalElements;

    private String nextURL;
}
