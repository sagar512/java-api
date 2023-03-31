package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class JoinNetworkRequestDTO {

    @NotNull
    private String networkId;

}
