package com.peopleapp.dto.requestresponsedto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class ShareLocationRequest {

    @NotNull
    private int timeInMinutes;

    /* connections with which location is shared */
    @NotEmpty
    private List<String> sharedWithConnectionIdList;

    @ApiModelProperty(hidden = true)
    @JsonIgnore
    private String initiatorId;

    @ApiModelProperty(hidden = true)
    @JsonIgnore
    private String initiatorName;

    @ApiModelProperty(hidden = true)
    @JsonIgnore
    private String receiverId;

    @ApiModelProperty(hidden = true)
    @JsonIgnore
    private String receiverConnectionId;




}