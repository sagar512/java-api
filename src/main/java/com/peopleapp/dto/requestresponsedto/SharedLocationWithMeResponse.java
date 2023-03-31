package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.SharedLocationWithMeDetails;
import lombok.Data;

import java.util.List;

@Data
public class SharedLocationWithMeResponse {

    private List<SharedLocationWithMeDetails> sharedLocationDetailsList;

    private int totalNumberOfPages;

    private long totalElements;

    private String nextURL;
}
