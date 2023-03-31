package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.CanadaAndUSAContactNumberDTO;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
public class ChangeMobileNumberRequest {

    @NotNull
    @Valid
    private CanadaAndUSAContactNumberDTO newContactNumber;
}
