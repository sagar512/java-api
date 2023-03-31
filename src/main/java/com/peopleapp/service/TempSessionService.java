package com.peopleapp.service;

import com.peopleapp.model.TemporarySession;

public interface TempSessionService {

    void persistOTPWithToken(TemporarySession temporarySession);

}
