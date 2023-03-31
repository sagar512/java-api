package com.peopleapp.service.impl;

import com.peopleapp.configuration.LocaleMessageReader;
import com.peopleapp.constant.SMSTemplateKeys;
import com.peopleapp.dto.ContactNumberDTO;
import com.peopleapp.dto.requestresponsedto.ResendOTPResponseDTO;
import com.peopleapp.enums.MessageCodes;
import com.peopleapp.enums.TokenStatus;
import com.peopleapp.exception.BadRequestException;
import com.peopleapp.model.TemporarySession;
import com.peopleapp.repository.TemporarySessionRepository;
import com.peopleapp.security.TokenAuthService;
import com.peopleapp.service.NotificationService;
import com.peopleapp.service.OTPService;
import com.peopleapp.util.PeopleUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.security.SecureRandom;

import static java.lang.Long.parseLong;

@Service
public class OTPServiceImpl implements OTPService {

    private final SecureRandom random = new SecureRandom();
    private Long startBound;
    private Long endBound;

    @Inject
    private ConfigurableEnvironment environment;

    @Inject
    private NotificationService notificationService;

    @Inject
    private TokenAuthService tokenAuthService;

    @Inject
    private TemporarySessionRepository temporarySessionRepository;

    @Inject
    private LocaleMessageReader messages;

    @Value("${otp.digits}")
    private Integer otpDigits;

    @Value("${otp.default:}")
    private String defaultOtp;

    @Value("${otp.retry-count}")
    private int retryCount;

    @Override
    public String generateOtpForUser() {
        return generateRandomOtp().toString();
    }

    @Override
    public void sendOTP(String otp, ContactNumberDTO contactNumber) {

        // send sms
        Object[] messageParam = new Object[]{otp};
        if (PeopleUtils.isNullOrEmpty(defaultOtp)) {
            notificationService.prepareSMSPayloadAndSendToQueue(contactNumber, SMSTemplateKeys.PHONE_NUMBER_OTP_VERIFICATION, messageParam);
        }
    }

    @Override
    public void checkIfOTPValid(String otp, TemporarySession temporarySession) {

        if (!otp.equals(temporarySession.getOtp())) {
            updateRetryCount(temporarySession);
            throw new BadRequestException(MessageCodes.OTP_INVALID.getValue());
        }
    }

    @Override
    public ResendOTPResponseDTO resendOTP() {

        String tempToken = tokenAuthService.getTempToken();
        TemporarySession temporarySession = tokenAuthService.getTempSessionByTempToken(tempToken);

        ResendOTPResponseDTO resendOTPResponseDTO = new ResendOTPResponseDTO();
        resendOTPResponseDTO.setTempToken(tempToken);
        resendOTPResponseDTO.setIsPhoneNumberVerified(Boolean.FALSE);
        ContactNumberDTO contactNumber = temporarySession.getContactNumber();

        // check if otp exists 
        Object[] messageParam = new Object[]{temporarySession.getOtp()};
        if (PeopleUtils.isNullOrEmpty(defaultOtp)) {
            notificationService.prepareSMSPayloadAndSendToQueue(contactNumber, SMSTemplateKeys.PHONE_NUMBER_OTP_VERIFICATION, messageParam);
        }

        return resendOTPResponseDTO;
    }

    private Long generateRandomOtp() {
        // this condition is not then defaultOtp trigger otherwise real otp trigger
        if (PeopleUtils.isNullOrEmpty(defaultOtp)) {
            return Long.parseLong(defaultOtp);
        }

        setStartAndEndBounds(otpDigits);
        return startBound + ((long) ((endBound - startBound) * random.nextDouble()));
    }

    private void setStartAndEndBounds(Integer noOfDigits) {
        char[] start = new char[noOfDigits];
        char[] end = new char[noOfDigits];
        for (int i = 0; i < noOfDigits; i++) {
            start[i] = '1';
            end[i] = '9';
        }
        startBound = parseLong(String.valueOf(start));
        endBound = parseLong(String.valueOf(end));
    }

    private void updateRetryCount(TemporarySession temporarySession) {

        int previousCount = temporarySession.getOtpRetryCount();
        int newCount = previousCount + 1;
        temporarySession.setOtpRetryCount(newCount);
        if (newCount == retryCount) {
            temporarySession.setTokenStatus(TokenStatus.EXPIRED);
        }
        temporarySessionRepository.save(temporarySession);
    }

}
