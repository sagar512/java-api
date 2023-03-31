package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.NetworkDetails;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class EditNetworkRequestDTO {

    private NetworkDetails networkDetails;

    @NotEmpty
    private String networkId;
}
