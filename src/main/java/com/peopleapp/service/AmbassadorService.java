package com.peopleapp.service;

import com.peopleapp.dto.requestresponsedto.GetAmbassadorDetailsResponseDTO;
import com.peopleapp.dto.requestresponsedto.SendReferralRequestDTO;

public interface AmbassadorService {

    void sendReferralLink(SendReferralRequestDTO sendReferralRequestDTO);

    GetAmbassadorDetailsResponseDTO getAmbassadorDetails();
}
