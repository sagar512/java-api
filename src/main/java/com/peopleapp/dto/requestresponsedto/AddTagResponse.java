package com.peopleapp.dto.requestresponsedto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AddTagResponse {

    private List<String> updatedUserTagList;

    private List<String> updatedContactTagList;

}
