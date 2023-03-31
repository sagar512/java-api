package com.peopleapp.service;

public interface EmailService {
	
	void sendEmail(String subject, String mailBody, String emailId);

	void sendTemplatedEmail(String templateName, String templateData, String emailId);

}
