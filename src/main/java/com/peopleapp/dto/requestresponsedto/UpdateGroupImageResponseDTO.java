package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

import java.util.Set;

@Data
public class UpdateGroupImageResponseDTO {

    private Set<String> listOfGroupsWithUpdatedImages;

}
