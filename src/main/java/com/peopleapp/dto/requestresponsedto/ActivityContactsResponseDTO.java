package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.UserContactData;
import lombok.Data;

import java.util.List;

@Data
public class ActivityContactsResponseDTO {

    private List<UserContactData> activityContactsList;

    private int totalNumberOfPages;

    private long totalElements;

    private String nextURL;
}
