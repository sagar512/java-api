package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.EditGroup;
import lombok.Data;

import javax.validation.Valid;
import java.util.List;

@Data
public class EditUserGroupRequestDTO {

    @Valid
    private List<EditGroup> userGroupsToBeEdited;

}
