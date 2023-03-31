package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class ShareNetworkRequestDTO {

    @NotNull
    private String networkId;

    @Valid
    private List<NetworkInviteeContact> sharedWithContactList;

}
