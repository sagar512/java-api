package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

import java.util.List;

@Data
public class StopSharedLocationWithOthersRequest {

    private List<String> sharedWithConnectionIdList;
}
