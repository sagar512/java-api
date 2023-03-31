package com.peopleapp.service;

import com.peopleapp.dto.ContactNumberDTO;
import com.peopleapp.dto.requestresponsedto.ResendOTPResponseDTO;
import com.peopleapp.model.TemporarySession;

public interface OTPService {

    String generateOtpForUser();

    void sendOTP(String otp, ContactNumberDTO contactNumber);

    void checkIfOTPValid(String otp, TemporarySession temporarySession);

    ResendOTPResponseDTO resendOTP();


}
