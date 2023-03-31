package com.peopleapp.dto.requestresponsedto;


import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class ShareContactRequest extends InitiatorAndReceiverDetailsDTO{

    /* can be real time connections */
    @NotEmpty
    private List<String> sharedWithConnectionIdList;

    /* can be any contact */
    @NotEmpty
    private List<String> sharedContactIdList;

}
