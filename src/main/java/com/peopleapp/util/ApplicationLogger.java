package com.peopleapp.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Component
@Aspect
public class ApplicationLogger {

    public ApplicationLogger() {
        // application logger
    }

    @AfterReturning(value = "execution(* com.peopleapp.service..*.*(..))")
    public void logMethodAccessAfter(JoinPoint joinPoint) {

        Log logger = LogFactory.getLog(joinPoint.getTarget().getClass().getCanonicalName());
        logger.info("Outside " + joinPoint.getSignature().getName());

    }

    @Before("execution(* com.peopleapp.service..*.*(..))")
    public void logMethodAccessBefore(JoinPoint joinPoint) {
        Log logger = LogFactory.getLog(joinPoint.getTarget().getClass().getCanonicalName());
        logger.info("Inside " + joinPoint.getSignature().getName());
    }

    @Before(value = "execution(* com.peopleapp.controller..*.*(..)) && !execution(* com.peopleapp.controller.ApplicationController.*(..))")
    public void logControllerMethodAccessBefore(JoinPoint joinPoint) {

        Log logger = LogFactory.getLog(joinPoint.getTarget().getClass().getCanonicalName());

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();

        String sessionToken = request.getHeader("sessionToken");
        StringBuffer reqInfo = new StringBuffer();
        if (sessionToken != null) {
            reqInfo.append("[sessionToken: ").append(sessionToken).append("]");
        }
        reqInfo.append(request.getMethod()).append(" ").append(request.getRequestURL());

        String queryString = request.getQueryString();
        if (queryString != null) {
            reqInfo.append("?").append(queryString);
        }

        if (logger.isDebugEnabled()) {
            Object[] signatureArgs = joinPoint.getArgs();
            ObjectMapper mapper = new ObjectMapper();
            try {

                if (signatureArgs.length != 0 && signatureArgs[0] != null) {
                    reqInfo.append(" [").append(mapper.writeValueAsString(signatureArgs[0]));
                }
            } catch (JsonProcessingException e) {
                logger.error("json parsing exception");
            }
        }

        logger.info(reqInfo);
        logger.info("Inside " + joinPoint.getSignature().getName());

    }

    @AfterReturning(value = "execution(* com.peopleapp.controller..*.*(..)) && !execution(* com.peopleapp.controller.ApplicationController.*(..))", returning = "returnValue")
    public void logMethodAccessAfter(JoinPoint joinPoint, Object returnValue) {

        Log logger = LogFactory.getLog(joinPoint.getTarget().getClass().getCanonicalName());
        logger.info("Outside " + joinPoint.getSignature().getName());

        if (logger.isDebugEnabled()) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                logger.info("[" + mapper.writeValueAsString(returnValue) + "]");
            } catch (JsonProcessingException e) {
                logger.error("json parsing exception");
            }
        }

    }

}
