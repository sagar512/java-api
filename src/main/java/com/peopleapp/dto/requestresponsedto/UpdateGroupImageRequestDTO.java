package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.GroupImage;
import lombok.Data;

import javax.validation.Valid;
import java.util.List;

@Data
public class UpdateGroupImageRequestDTO {

    @Valid
    private List<GroupImage> listOfGroupImages;
}
