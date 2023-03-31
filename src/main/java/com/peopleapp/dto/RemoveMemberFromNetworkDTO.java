package com.peopleapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RemoveMemberFromNetworkDTO {

    @NotNull
    private String networkId;

    @NotEmpty
    private List<String> memberIdList;

}
