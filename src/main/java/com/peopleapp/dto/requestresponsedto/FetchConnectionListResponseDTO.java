package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.UserContactData;
import lombok.Data;
import org.joda.time.DateTime;

import java.util.List;

@Data
public class FetchConnectionListResponseDTO {

    private List<UserContactData> contactList;

    private DateTime lastSyncedTime;

    private long totalNumberOfPages;

    private long totalElements;

    private String nextURL;
}
