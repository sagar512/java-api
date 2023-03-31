package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.SearchedNetworkDetails;
import lombok.Data;

import java.util.List;

@Data
public class SearchedNetworkResponseDTO {

    private List<SearchedNetworkDetails> matchedNetworkList;

    private int totalNumberOfPages;

    private long totalElements;

    private String nextURL;
}
