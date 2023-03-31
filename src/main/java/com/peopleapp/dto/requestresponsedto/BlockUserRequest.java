package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class BlockUserRequest {

    @NotNull
    private String userId;

    @NotNull
    private Boolean isBlocked;
}
