package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.NetworkDetails;
import lombok.Data;

@Data
public class CreateOrEditNetworkResponseDTO {

    private NetworkDetails networkDetails;

    private String networkId;
}
