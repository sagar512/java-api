package com.peopleapp.dto.requestresponsedto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RemoveTagResponse {

    private List<String> updatedUserTagList;

    private List<String> updatedContactTagList;
}
