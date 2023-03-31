package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class RequestMoreInfoDTO {

    @NotEmpty
    private String connectionId;

    @NotNull
    @Size(max = 300)
    private String message;

}
