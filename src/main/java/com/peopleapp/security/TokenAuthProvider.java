package com.peopleapp.security;

import com.google.common.base.Strings;
import com.peopleapp.constant.APIParamKeys;
import com.peopleapp.model.PeopleUser;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Map;

@Component
public class TokenAuthProvider implements AuthenticationProvider {

    @Inject
    private TokenAuthService tokenAuthService;

    @Override
    public Authentication authenticate(Authentication authentication) {
        Map<String, String> tokenMap = (Map<String, String>) ((SpringSecurityAuthToken) authentication).getCredentials();

        PeopleUser peopleUser = null;
        // Check if the user and session is valid
        if (!Strings.isNullOrEmpty(tokenMap.get(APIParamKeys.SESSION_TOKEN))) {
            peopleUser = tokenAuthService.getUserFromSessionToken(tokenMap.get(APIParamKeys.SESSION_TOKEN));
            if (peopleUser == null) {
                return null;
            }
        } else {
            // Check temporary session
            tokenAuthService.getTempSessionByTempToken(tokenMap.get(APIParamKeys.TEMP_TOKEN));
        }

        // This new copy of the token will be set as fully authenticated.
        return new SpringSecurityAuthToken(((SpringSecurityAuthToken) authentication), peopleUser);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return SpringSecurityAuthToken.class.isAssignableFrom(authentication);
    }

}
