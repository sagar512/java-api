package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.UserInformationDTO;
import lombok.Data;

import java.util.List;

@Data
public class UpdateUserInfoResponseDTO {

    private UserInformationDTO userDetails;

    private List<String> networkSharedValues;
}
