package com.peopleapp.security;

import com.peopleapp.model.PeopleUser;
import com.peopleapp.model.TemporarySession;

public interface TokenAuthService {
    PeopleUser getUserFromSessionToken(String sessionToken);

    TemporarySession getTempSessionByTempToken(String tempToken);

    String getSessionToken();

    String getTempToken();

    PeopleUser getSessionUser();
}
