package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.UpdateFavouriteContact;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class UpdateFavouriteRequestDTO {

    @NotEmpty
    @Valid
    private List<UpdateFavouriteContact> favouriteContactList;
}
