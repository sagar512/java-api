package com.peopleapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NetworkSharedInformationDTO {

    private List<UserProfileData> userNetworkSharedMetadataList;

    private String memberRole;

}
