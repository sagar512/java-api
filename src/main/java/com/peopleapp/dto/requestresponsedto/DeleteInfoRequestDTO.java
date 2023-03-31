package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class DeleteInfoRequestDTO {

    @NotEmpty
    private String connectionId;

    @NotNull 
    private boolean retrieveDeletedInfo;
}
