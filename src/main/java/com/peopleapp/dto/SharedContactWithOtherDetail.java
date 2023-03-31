package com.peopleapp.dto;

import lombok.Data;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;

@Data
public class SharedContactWithOtherDetail {

    private String activityId;

    private List<String> activityIds;

    private List<Map<String, String>> sharedConnectionIdMaps;

    private String sharedWithUserId;

    private UserContactData receiverDetails;

    private DateTime sharedOn;

    private List<String> sharedContactIdList;

    private int numberOfContactShared;
}
