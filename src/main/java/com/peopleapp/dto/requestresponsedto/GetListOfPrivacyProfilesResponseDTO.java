package com.peopleapp.dto.requestresponsedto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peopleapp.dto.PrivacyProfileData;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetListOfPrivacyProfilesResponseDTO {

    private List<PrivacyProfileData> userPrivacyProfileList;

}
