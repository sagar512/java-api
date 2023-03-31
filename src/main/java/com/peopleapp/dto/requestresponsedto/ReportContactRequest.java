package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class ReportContactRequest {

    @NotEmpty
    private String connectionId;

    private String reportMessage;
}
