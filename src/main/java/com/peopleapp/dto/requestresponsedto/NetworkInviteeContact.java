package com.peopleapp.dto.requestresponsedto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.peopleapp.dto.ContactNumberDTO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.annotation.Transient;

import javax.validation.constraints.NotEmpty;

@Data
public class NetworkInviteeContact {
    @NotEmpty
    private String connectionId;

    @ApiModelProperty(hidden = true)
    @JsonIgnore
    @Transient
    private String inviteeUserId;

    @ApiModelProperty(hidden = true)
    @JsonIgnore
    @Transient
    private Boolean isInviteeAPeopleUser;

    @ApiModelProperty(hidden = true)
    @JsonIgnore
    @Transient
    private Boolean isNetworkSharedOrInviteSent;

    @ApiModelProperty(hidden = true)
    @JsonIgnore
    @Transient
    private Boolean isAlreadyANetworkMember;

    private ContactNumberDTO contactNumber;
}
