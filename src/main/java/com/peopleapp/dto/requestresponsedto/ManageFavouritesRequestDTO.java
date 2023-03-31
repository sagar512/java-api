package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.FavouriteConnectionSequenceDTO;
import lombok.Data;

import javax.validation.Valid;
import java.util.List;

@Data
public class ManageFavouritesRequestDTO {

    @Valid
    List<FavouriteConnectionSequenceDTO> favouriteConnectionList;
}
