package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.UserContactData;
import lombok.Data;

@Data
public class SearchByNumberResponseDTO {

    private UserContactData searchedContactDetails;
    
    private UserContactData searchedWatuContactDetails;

    private boolean searchedContactExist;
}
