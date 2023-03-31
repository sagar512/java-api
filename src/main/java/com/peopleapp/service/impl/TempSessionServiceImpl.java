package com.peopleapp.service.impl;

import com.peopleapp.model.TemporarySession;
import com.peopleapp.repository.TemporarySessionRepository;
import com.peopleapp.security.TokenAuthService;
import com.peopleapp.service.TempSessionService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class TempSessionServiceImpl implements TempSessionService {

    @Inject
    private TokenAuthService tokenAuthService;

    @Inject
    private TemporarySessionRepository temporarySessionRepositary;

    @Override
    public void persistOTPWithToken(TemporarySession temporarySession) {
        temporarySessionRepositary.save(temporarySession);
    }

}
