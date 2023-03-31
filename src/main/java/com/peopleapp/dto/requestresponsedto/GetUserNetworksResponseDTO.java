package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.UserNetworkDetails;
import lombok.Data;

import java.util.List;

@Data
public class GetUserNetworksResponseDTO {

    private List<UserNetworkDetails> userNetworkDetailsList;

    private int totalNumberOfPages;

    private long totalElements;

    private String nextURL;


}
