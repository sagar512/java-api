package com.peopleapp.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class MergeContact {

    @NotEmpty
    private String masterConnectionId;

    @NotEmpty
    private List<String> mergedConnectionIds;
}
