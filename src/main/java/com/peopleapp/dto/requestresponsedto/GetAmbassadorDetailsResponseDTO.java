package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

@Data
public class GetAmbassadorDetailsResponseDTO {

    private int numberOfReferralSent;

    private int numberOfReferralCompleted;

    private int totalRewardPoints;

}
