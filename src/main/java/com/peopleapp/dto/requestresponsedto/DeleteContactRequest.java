package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class DeleteContactRequest {

    @NotEmpty
    private List<String> connectionIdList;
}
