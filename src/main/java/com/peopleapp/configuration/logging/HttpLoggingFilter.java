package com.peopleapp.configuration.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peopleapp.dto.ContactNumberDTO;
import com.peopleapp.dto.apirequestresponselogdto.APILogDTO;
import com.peopleapp.dto.apirequestresponselogdto.APIRequestLogDTO;
import com.peopleapp.dto.apirequestresponselogdto.APIResponseLogDTO;
import com.peopleapp.enums.APIPrivacyType;
import com.peopleapp.enums.TokenStatus;
import com.peopleapp.enums.UserStatus;
import com.peopleapp.model.PeopleUser;
import com.peopleapp.model.TemporarySession;
import com.peopleapp.model.UserSession;
import com.peopleapp.repository.PeopleUserRepository;
import com.peopleapp.repository.TemporarySessionRepository;
import com.peopleapp.repository.UserSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Component
public class HttpLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(HttpLoggingFilter.class);
    private static final String SESSION_TOKEN = "sessionToken";
    private static final String TEMP_TOKEN = "tempToken";

    @Inject
    private UserSessionRepository userSessionRepository;

    @Inject
    private TemporarySessionRepository temporarySessionRepository;

    @Inject
    private PeopleUserRepository peopleUserRepository;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // abstract method overridden from parent class
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        try {
            long startTime = new Date().getTime();
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            HttpServletResponse httpServletResponse = (HttpServletResponse) response;

            BufferedRequestWrapper bufferedRequest = new BufferedRequestWrapper(
                    httpServletRequest);
            BufferedResponseWrapper bufferedResponse = new BufferedResponseWrapper(
                    httpServletResponse);

            chain.doFilter(bufferedRequest, bufferedResponse);

            long endTime = new Date().getTime();

            long apiExecutionTime = (endTime - startTime);

            logAPIRequestDetail(bufferedRequest, bufferedResponse, log, apiExecutionTime);

        } catch (Exception a) {
            log.error(a.getMessage());
        }
    }

    @Override
    public void destroy() {
        // abstract method overridden from parent class
    }

    private void logAPIRequestDetail(BufferedRequestWrapper bufferedRequest, BufferedResponseWrapper bufferedResponse,
                                     Logger log, long apiExecutionTime) {
        APIPrivacyType apiPrivacyType = getAPIPrivacyType(bufferedRequest);
        String contactNumber = getUserIdentity(bufferedRequest);

        switch (apiPrivacyType) {
            case PRIVATE:
            case TEMP_PRIVATE:
            case PUBLIC:
                try {
                    logJsonifyAPIRequestWithUserIdentity(log,bufferedRequest,
                            bufferedResponse, contactNumber, apiExecutionTime);
                } catch (JsonProcessingException e) {
                    log.error("json parsing exception");
                }
                break;
            default:
                break;
        }
    }

    private APIPrivacyType getAPIPrivacyType(BufferedRequestWrapper bufferedRequest){
        APIPrivacyType apiPrivacyType = null;
        if(bufferedRequest.getHeader(SESSION_TOKEN) != null){
            apiPrivacyType = APIPrivacyType.PRIVATE;
        } else if(bufferedRequest.getHeader(TEMP_TOKEN) != null){
            apiPrivacyType = APIPrivacyType.TEMP_PRIVATE;
        } else {
            apiPrivacyType = APIPrivacyType.PUBLIC;
        }
        return apiPrivacyType;
    }

    private String getUserIdentity(BufferedRequestWrapper bufferedRequest){
        ContactNumberDTO contactNumberDTO = null;
        if(bufferedRequest.getHeader(SESSION_TOKEN) != null){
            UserSession userSession =
                    userSessionRepository.findBySessionToken(bufferedRequest.getHeader(SESSION_TOKEN));
            String userId = userSession.getUserId();
            PeopleUser peopleUser = peopleUserRepository.findByuserId(userId, UserStatus.ACTIVE.getValue());
            contactNumberDTO = peopleUser.getVerifiedContactNumber();
        } else if(bufferedRequest.getHeader(TEMP_TOKEN) != null){
            TemporarySession temporarySession =
                    temporarySessionRepository.findByTempTokenAndStatus(bufferedRequest.getHeader(TEMP_TOKEN), TokenStatus.EXPIRED);
            contactNumberDTO = temporarySession.getContactNumber();
        } else {
            // Need to prepare user contact number value for public API
        }
        return (contactNumberDTO!= null)?
                contactNumberDTO.getMobileNumberWithDefaultCountryCode(contactNumberDTO.getCountryCode()) : null;
    }

    private void logJsonifyAPIRequestWithUserIdentity(
            Logger logger, BufferedRequestWrapper bufferedRequest, BufferedResponseWrapper bufferedResponse,
            String contactNumber, long apiExecutionTime) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        if (logger.isDebugEnabled()) {
            APILogDTO apiLogDTO =
                    new APILogDTO(contactNumber, bufferedRequest.getMethod(),
                            bufferedRequest.getRequestURI(), bufferedRequest.getLocalName());

            APIRequestLogDTO apiRequestLogDTO = new APIRequestLogDTO();

            apiRequestLogDTO.setContentType(bufferedRequest.getContentType());

            // Capture request headers and set in Request DTO
            captureRequestHeaders(bufferedRequest, apiRequestLogDTO);

            // Capture query param and set in Request DTO
            apiRequestLogDTO.setQueryString(bufferedRequest.getQueryString());

            // Capture JSON request body
            try {
                apiRequestLogDTO.setRequestBody(bufferedRequest.getRequestBody());
            } catch (IOException e) {
                log.error(e.getMessage());
            }

            // Set Request details
            apiLogDTO.setApiRequest(apiRequestLogDTO);

            APIResponseLogDTO apiResponseLogDTO = new APIResponseLogDTO();

            // Capture JSON request body
            apiResponseLogDTO.setResponseBody(bufferedResponse.getContent());
            apiResponseLogDTO.setResponseStatus(bufferedResponse.getStatus());

            // Set Response details
            apiLogDTO.setApiResponse(apiResponseLogDTO);

            // set api execution time
            apiLogDTO.setApiExecutionTime(apiExecutionTime);

            String jsonStr = objectMapper.writeValueAsString(apiLogDTO);
            logger.info(jsonStr);
        }
    }

    private void captureRequestHeaders(BufferedRequestWrapper bufferedRequest, APIRequestLogDTO apiRequestLogDTO) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = bufferedRequest.getHeaderNames();

        while(headerNames.hasMoreElements()){
            String headerName = headerNames.nextElement();
            headers.put(headerName, bufferedRequest.getHeader(headerName));
        }

        apiRequestLogDTO.setHeaders(headers);
    }
}

