package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.model.UserGroup;
import lombok.Data;

import java.util.List;

@Data
public class EditGroupResponse {

    private List<UserGroup> groupDetails;
}
