package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class ReportUserRequest {

    @NotNull
    private String userId;

    private String reportMessage;
}
