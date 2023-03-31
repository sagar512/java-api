package com.peopleapp.security;

import com.peopleapp.constant.APIParamKeys;
import com.peopleapp.constant.PeopleConstants;
import com.peopleapp.enums.TokenStatus;
import com.peopleapp.enums.UserStatus;
import com.peopleapp.exception.UnAuthorizedException;
import com.peopleapp.model.PeopleUser;
import com.peopleapp.model.TemporarySession;
import com.peopleapp.model.UserSession;
import com.peopleapp.repository.PeopleUserRepository;
import com.peopleapp.repository.TemporarySessionRepository;
import com.peopleapp.repository.UserSessionRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Map;

@Service
public class TokenAuthServiceImpl implements TokenAuthService {

    @Inject
    private UserSessionRepository userSessionRepository;

    @Inject
    private PeopleUserRepository peopleUserRepository;

    @Inject
    private TemporarySessionRepository temporarySessionRepository;

    @Override
    public PeopleUser getUserFromSessionToken(String sessionToken) {
        PeopleUser peopleUser = null;

        // check for user session
        UserSession session = userSessionRepository.findBySessionTokenAndStatus(sessionToken, PeopleConstants.ACTIVE);
        if (session == null) {
            throw new UnAuthorizedException("This security token could not be verified, either the token is invalid or has expired. Please login again.");

        } else {
            peopleUser = peopleUserRepository.findByuserId(session.getUserId(), UserStatus.ACTIVE.getValue());
            if (peopleUser == null) {
                throw new UnAuthorizedException("The user does not exist as an active user.");
            }
        }
        return peopleUser;
    }

    @Override
    public TemporarySession getTempSessionByTempToken(String tempToken) {
        // Check temporary session
        TemporarySession temporarySession = temporarySessionRepository.findByTempTokenAndStatus(tempToken, TokenStatus.ACTIVE);
        if (temporarySession == null) {
            throw new UnAuthorizedException("This security temporary token could not be verified, either the token is invalid or has expired.");
        }
        return temporarySession;
    }


    @Override
    public String getSessionToken() {
        Map<String, String> authToken = (Map<String, String>) SecurityContextHolder.getContext().getAuthentication().getCredentials();
        return authToken.get(APIParamKeys.SESSION_TOKEN);
    }

    @Override
    public String getTempToken() {
        Map<String, String> authToken = (Map<String, String>) SecurityContextHolder.getContext().getAuthentication().getCredentials();
        return authToken.get(APIParamKeys.TEMP_TOKEN);
    }

    @Override
    public PeopleUser getSessionUser() {
        return (PeopleUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

}
