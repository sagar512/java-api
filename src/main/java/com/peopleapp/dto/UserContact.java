package com.peopleapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.annotation.Transient;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserContact {

    @NotEmpty
    private String connectionId;

    @ApiModelProperty(hidden = true)
    @JsonIgnore
    @Transient
    private String userId;

    @ApiModelProperty(hidden = true)
    @JsonIgnore
    @Transient
    private Boolean isPeopleUser;

    @Valid
    private ContactNumberDTO contactNumber;

}
