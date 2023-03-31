package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.util.List;

@Data
public class RemoveTagRequest {

    @NotEmpty
    @Pattern(regexp = "^(CONTACT|PROFILE)$", message = "invalid tag type.")
    private String tagType;

    private String connectionId;

    private List<String> removedTagList;
}
