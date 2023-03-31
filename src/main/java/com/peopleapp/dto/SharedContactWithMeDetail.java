package com.peopleapp.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SharedContactWithMeDetail {

    private String activityId;

    private List<String> activityIds;

    private List<Map<String, String>> sharedConnectionIdMaps;

    private UserContactData initiatorDetails;

    private String sharedByUserId;

    private List<String> sharedContactId;

    private int numberOfContactShared;

}
