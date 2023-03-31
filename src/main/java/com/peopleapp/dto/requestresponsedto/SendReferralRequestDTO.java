package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.CanadaAndUSAContactNumberDTO;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class SendReferralRequestDTO {

    @NotEmpty
    private String link;

    @Valid
    @NotNull
    private CanadaAndUSAContactNumberDTO referredContactNumber;
}
