package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.FavouriteContactsSequenceNumber;
import lombok.Data;

import java.util.List;

@Data
public class FavouriteContactsResponseDTO {

    private List<FavouriteContactsSequenceNumber> favouriteContacts;

}
