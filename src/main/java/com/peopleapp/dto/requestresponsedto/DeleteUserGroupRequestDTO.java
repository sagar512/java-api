package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class DeleteUserGroupRequestDTO {

    @NotEmpty
    private List<String> groupIdList;
}
