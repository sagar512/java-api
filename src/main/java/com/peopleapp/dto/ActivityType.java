package com.peopleapp.dto;

import com.peopleapp.enums.Action;
import com.peopleapp.enums.RequestType;
import lombok.Data;

@Data
public class ActivityType {

    private RequestType requestType;

    private Action actionTaken;
}
