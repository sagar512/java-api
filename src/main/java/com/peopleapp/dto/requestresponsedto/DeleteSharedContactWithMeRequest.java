package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class DeleteSharedContactWithMeRequest {

    @NotEmpty
    private String sharedByConnectionId;

    @NotEmpty
    private List<String> sharedConnectionIdList;
}
