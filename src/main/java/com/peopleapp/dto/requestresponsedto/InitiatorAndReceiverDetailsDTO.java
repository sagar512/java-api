package com.peopleapp.dto.requestresponsedto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class InitiatorAndReceiverDetailsDTO {

    @ApiModelProperty(hidden = true)
    @JsonIgnore
    private String receiverUserId;

    @ApiModelProperty(hidden = true)
    @JsonIgnore
    private String receiverConnectionId;

    @ApiModelProperty(hidden = true)
    @JsonIgnore
    private String initiatorUserId;

    @ApiModelProperty(hidden = true)
    @JsonIgnore
    private String initiatorName;
}
