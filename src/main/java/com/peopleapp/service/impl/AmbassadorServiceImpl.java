package com.peopleapp.service.impl;

import com.peopleapp.configuration.LocaleMessageReader;
import com.peopleapp.constant.SMSTemplateKeys;
import com.peopleapp.dto.ContactNumberDTO;
import com.peopleapp.dto.requestresponsedto.GetAmbassadorDetailsResponseDTO;
import com.peopleapp.dto.requestresponsedto.SendReferralRequestDTO;
import com.peopleapp.enums.MessageCodes;
import com.peopleapp.enums.ReferralStatus;
import com.peopleapp.exception.BadRequestException;
import com.peopleapp.model.PeopleAmbassador;
import com.peopleapp.model.PeopleUser;
import com.peopleapp.repository.AmbassadorRepository;
import com.peopleapp.repository.PeopleUserRepository;
import com.peopleapp.security.TokenAuthService;
import com.peopleapp.service.AmbassadorService;
import com.peopleapp.service.NotificationService;
import com.peopleapp.util.PeopleUtils;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

@Service
public class AmbassadorServiceImpl implements AmbassadorService {

    @Inject
    private PeopleUserRepository peopleUserRepository;

    @Inject
    private AmbassadorRepository ambassadorRepository;

    @Inject
    private LocaleMessageReader messages;

    @Inject
    private TokenAuthService tokenAuthService;

    @Inject
    private NotificationService notificationService;


    @Override
    public void sendReferralLink(SendReferralRequestDTO sendReferralRequestDTO) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();

        //validate the number to which the sms has to be sent is not already signed up
        PeopleUser existingUser = peopleUserRepository.findByCodeAndNumber(
                sendReferralRequestDTO.getReferredContactNumber().getCountryCode(),
                sendReferralRequestDTO.getReferredContactNumber().getPhoneNumber());

        if (existingUser != null) {
            throw new BadRequestException(MessageCodes.PHONE_NUMBER_ALREADY_EXISTS.getValue());
        }

        ContactNumberDTO referredContactNumber = sendReferralRequestDTO.getReferredContactNumber().getContactNumberDTO();

        //validate if the request is already sent to the contact
        PeopleAmbassador ambassador = ambassadorRepository.findByAmbassadorIdAndReferredContactNumber(
                PeopleUtils.convertStringToObjectId(sessionUser.getUserId()), referredContactNumber);

        if (ambassador != null) {
            throw new BadRequestException(MessageCodes.REFERRAL_ALREADY_SENT.getValue());
        }

        //create PeopleAmbassador and populate all details
        PeopleAmbassador peopleAmbassador = new PeopleAmbassador();
        peopleAmbassador.setAmbassadorID(sessionUser.getUserId());
        peopleAmbassador.setReferralCode(sessionUser.getReferralCode());
        peopleAmbassador.setReferralLink(sendReferralRequestDTO.getLink());
        peopleAmbassador.setReferralStatus(ReferralStatus.PENDING);
        peopleAmbassador.setReferralInitiatedOn(new DateTime());
        peopleAmbassador.setReferredContactNumber(referredContactNumber);
        ambassadorRepository.save(peopleAmbassador);


        //prepare SMS template and send sms
        Object[] messageParam = new Object[]{sendReferralRequestDTO.getLink()};
        notificationService.prepareSMSPayloadAndSendToQueue(referredContactNumber,
                SMSTemplateKeys.AMBASSADOR_REFERRAL, messageParam);

    }


    @Override
    public GetAmbassadorDetailsResponseDTO getAmbassadorDetails() {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();

        int completedReferral = 0;
        int totalRewardPoints = 0;

        GetAmbassadorDetailsResponseDTO ambassadorDetails = new GetAmbassadorDetailsResponseDTO();

        //fetching all the referrals sent by user
        List<PeopleAmbassador> usersAmbassadorDetails = ambassadorRepository.findByAmbassadorId(PeopleUtils.convertStringToObjectId(sessionUser.getUserId()));
        for (PeopleAmbassador peopleAmbassador : usersAmbassadorDetails) {
            if (peopleAmbassador.getReferralStatus().equals(ReferralStatus.COMPLETED)) {
                completedReferral++;
                totalRewardPoints += peopleAmbassador.getRewardPoints();
            }
        }

        //Prepare response
        ambassadorDetails.setNumberOfReferralSent(usersAmbassadorDetails.size());
        ambassadorDetails.setNumberOfReferralCompleted(completedReferral);
        ambassadorDetails.setTotalRewardPoints(totalRewardPoints);

        return ambassadorDetails;
    }
}
