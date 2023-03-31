package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.UserGroupData;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class UserGroupRequestDTO {

    @NotEmpty
    private List<UserGroupData> userGroupList;
}
