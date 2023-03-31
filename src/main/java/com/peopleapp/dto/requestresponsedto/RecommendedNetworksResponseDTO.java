package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.UserNetworkDetails;
import lombok.Data;

import java.util.List;

@Data
public class RecommendedNetworksResponseDTO {

    List<UserNetworkDetails> mostPopular;

    List<UserNetworkDetails> local;

    List<UserNetworkDetails> suggestion;
}
