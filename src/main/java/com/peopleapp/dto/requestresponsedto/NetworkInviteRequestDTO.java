package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class NetworkInviteRequestDTO {

    private String networkId;

    @NotEmpty
    @Valid
    private List<NetworkInviteeContact> networkInviteeList;

    private String message;
}
