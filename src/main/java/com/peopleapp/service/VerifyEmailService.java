package com.peopleapp.service;

import com.peopleapp.model.VerifyEmail;

public interface VerifyEmailService {
	
	VerifyEmail generateAndPersistEmailVerificationLink(String userId, String email);

	VerifyEmail generateAndPersistLinkForPrimaryEmail(String userId, String email);

	String generateEmailVerificationLink();

	String generateEmailVerificationLink(String token);
}
