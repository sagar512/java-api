package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.MergeContact;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class MergeContactsRequestDTO {

    @NotEmpty
    @Valid
    private List<MergeContact> listOfContactsToBeMerged;
}
