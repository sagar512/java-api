package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class ShareTagRequest {

    @NotEmpty
    private String privacyProfileId;

    @NotNull
    private Boolean isTagShared;
}
