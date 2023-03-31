package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class DemoteAdminRequestDTO {

    @NotNull
    private String networkId;

    @NotEmpty
    private List<String> memberIdList;
}
