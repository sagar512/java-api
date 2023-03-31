package com.peopleapp.service.impl;

import com.peopleapp.model.VerifyEmail;
import com.peopleapp.repository.VerifyEmailRepository;
import com.peopleapp.service.VerifyEmailService;
import com.peopleapp.util.TokenGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import javax.inject.Inject;

@Service
public class VerifyEmailServiceImpl implements VerifyEmailService {

    @Inject
    private VerifyEmailRepository verifyEmailRepository;

    @Value("${server.base-path}")
    private String serverBasePath;

	@Override
	public VerifyEmail generateAndPersistEmailVerificationLink(String userId, String email) {

		VerifyEmail verifyEmail = new VerifyEmail();
		verifyEmail.setEmail(email);
		verifyEmail.setUserId(userId);
		verifyEmail.setIsEmailVerified(Boolean.FALSE);
		verifyEmail.setVerificationLink(generateEmailVerificationLink());

		return verifyEmailRepository.save(verifyEmail);
	}

    @Override
    public VerifyEmail generateAndPersistLinkForPrimaryEmail(String userId, String email) {

        VerifyEmail verifyEmail = new VerifyEmail();

        verifyEmail.setEmail(email);
        verifyEmail.setIsPrimary(Boolean.TRUE);
        verifyEmail.setUserId(userId);
        verifyEmail.setIsEmailVerified(Boolean.FALSE);
        verifyEmail.setVerificationLink(generateEmailVerificationLink());

        return verifyEmailRepository.save(verifyEmail);
    }

    @Override
    public String generateEmailVerificationLink() {

        String token = TokenGenerator.generateTempToken();

        String verificationLink = serverBasePath.concat("v1.0/webview/api/email/verify");
        return UriComponentsBuilder.fromUriString(verificationLink).queryParam("token", token).build().toUriString();
    }

    @Override
    public String generateEmailVerificationLink(String token) {

        String verificationLink = serverBasePath.concat("v1.0/webview/api/email/verify");
        return UriComponentsBuilder.fromUriString(verificationLink).queryParam("token", token).build().toUriString();
    }

}
