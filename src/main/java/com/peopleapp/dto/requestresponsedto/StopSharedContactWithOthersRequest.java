package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

import java.util.List;

@Data
public class StopSharedContactWithOthersRequest {

    private String sharedWithConnectionId;

    private List<String> sharedConnectionIdList;
}
