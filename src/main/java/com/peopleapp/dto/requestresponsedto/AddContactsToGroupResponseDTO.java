package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

import java.util.List;

@Data
public class AddContactsToGroupResponseDTO {

    private List<String> contactIdList;
}
