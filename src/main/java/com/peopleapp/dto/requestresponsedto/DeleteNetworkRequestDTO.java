package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class DeleteNetworkRequestDTO {

    @NotNull
    private String networkId;
}
