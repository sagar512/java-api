package com.peopleapp.dto.requestresponsedto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peopleapp.dto.SharedLocationWithOtherDetails;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SharedLocationWithOthersResponse {

    private List<SharedLocationWithOtherDetails> sharedLocationDetailsList;

    private int totalNumberOfPages;

    private long totalElements;

    private String nextURL;
}
