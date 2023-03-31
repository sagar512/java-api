package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.NetworkPendingRequest;
import lombok.Data;

import java.util.List;

@Data
public class NetworkPendingRequestDetailsDTO {

    private String networkId;

    private List<NetworkPendingRequest> requestDetailsList;

    private int totalNumberOfPages;

    private long totalElements;

    private String nextURL;
}
