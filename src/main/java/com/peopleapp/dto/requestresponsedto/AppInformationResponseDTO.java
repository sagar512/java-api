package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

@Data
public class AppInformationResponseDTO {

    private Integer deviceTypeId;

    private String buildVersion;

    private Integer buildNumber;

    private Boolean forceUpdateRequired;
}
