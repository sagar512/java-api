package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.UpdateFavouriteGroup;
import lombok.Data;

import javax.validation.Valid;
import java.util.List;

@Data
public class UpdateGroupFavouriteRequestDTO {

    @Valid
    private List<UpdateFavouriteGroup> favouriteGroups;
}
