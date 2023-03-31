package com.peopleapp.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import com.peopleapp.configuration.SecurityConfiguration;
import com.peopleapp.constant.APIParamKeys;
import com.peopleapp.dto.requestresponsedto.ErrorResponseDTO;
import com.peopleapp.exception.UnAuthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter to extract temp-token and session token from the header param. This
 * filter has been configured in the WebSecurityConfiguration to be called
 * before processing any API request (except though that are ignored).
 * To keep things cleaner, we are not checking the validity of the token by looking in
 * our DB. This is done in TokenAuthProvider class. So essentially this class
 * just takes the Google Auth token from the header and sets it in the Spring
 * Security context.
 *
 * @see SecurityConfiguration - to see the wiring of the various Spring
 * Security pieces
 * @see TokenAuthProvider - to see how the token is processed for validity.
 */
public class TokenAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String sessionToken = request.getHeader(APIParamKeys.SESSION_TOKEN);
        String tempToken = request.getHeader(APIParamKeys.TEMP_TOKEN);
        Map<String, String> authToken = new HashMap<>();

        try {
            // Check which token is being sent
            if (!Strings.isNullOrEmpty(sessionToken)) {
                authToken.put(APIParamKeys.SESSION_TOKEN, sessionToken);
            } else if (!Strings.isNullOrEmpty(tempToken)) {
                authToken.put(APIParamKeys.TEMP_TOKEN, tempToken);
            } else {
			/*	 If control has reached here, then there is no valid token available
				 NOTE: Do not throw an exception from this class - it will not be translated
				 as a 40x HTTP Status code. This is the correct way to set the status code.*/
                throw new UnAuthorizedException("Invalid security token");
            }
            // Set the sessionToken Security Context
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            SecurityContextHolder.setContext(securityContext);
            SecurityContextHolder.getContext().setAuthentication(new SpringSecurityAuthToken(new ArrayList<>(), authToken));

            // Move on to the next filter
            filterChain.doFilter(request, response);

        } catch (UnAuthorizedException e) {
            setErrorResponse(HttpStatus.UNAUTHORIZED, response, e);
        } catch (RuntimeException e) {
            logger.error("RunTimeException : " + e.getLocalizedMessage());
            setErrorResponse(HttpStatus.BAD_REQUEST, response, e);
        }


    }

    private void setErrorResponse(HttpStatus status, HttpServletResponse response, Throwable e) {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(e.getMessage());
        try {
            String json = errorResponse.convertToJson();
            response.getWriter().write(json);
        } catch (JsonProcessingException ex) {
            logger.error("JsonProcessingException : " + e.getMessage());
        } catch (IOException ex) {
            logger.error("IOException  : " + e.getMessage());
        }
    }
}