package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class AddContactsToGroupRequestDTO {

    @NotEmpty
    private String groupId;

    @NotEmpty
    private List<String> contactIdList;
}
