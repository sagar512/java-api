package com.peopleapp.exception;

import com.peopleapp.configuration.LocaleMessageReader;
import com.peopleapp.dto.requestresponsedto.ErrorResponseDTO;
import com.peopleapp.enums.MessageCodes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class PeopleAppExceptionHandler extends ResponseEntityExceptionHandler {

    @Autowired
    private LocaleMessageReader messages;

    private ErrorResponseDTO response;

    // 400

    @ExceptionHandler({BadRequestException.class})
    public ResponseEntity<Object> handleBadRequestException(final BadRequestException ex,
                                                            final WebRequest request) {
        String code = ex.getMessage();
        logger.error(ex.getMessage(), ex);
        try {
            response = new ErrorResponseDTO(ex.getMessage(), messages.get(code));
        } catch (NoSuchMessageException e) {
            // if message against the code is not found, return the code
            // (used in case of customised error messages, when message string is passed as code)
            response = new ErrorResponseDTO(MessageCodes.CUSTOMISED_MESSAGE.getValue(), code);
        }
        return handleExceptionInternal(ex, response, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    // 401

    @ExceptionHandler({UnAuthorizedException.class})
    public ResponseEntity<Object> handleUnAuthorizeRequestException(final UnAuthorizedException ex,
                                                                    final WebRequest request) {
        String code = ex.getMessage();
        logger.error(ex.getMessage(), ex);
        response = new ErrorResponseDTO(ex.getMessage(), messages.get(code));
        return handleExceptionInternal(ex, response, new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @Override
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        List<String> errors = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            if(error.getField().equalsIgnoreCase("networkSharedValueList")){
                errors.add(error.getDefaultMessage());
            }
            errors.add(error.getField() + ": " + error.getDefaultMessage());
        }
        for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            errors.add(error.getObjectName() + ": " + error.getDefaultMessage());
        }
        logger.error(ex.getMessage(), ex);
        response = new ErrorResponseDTO(MessageCodes.INVALID_PROPERTY.getValue(), errors.get(0));

        return handleExceptionInternal(ex, response, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<Object> handleAll(Exception ex, WebRequest request) {

        String code = MessageCodes.INTERNAL_SERVER_ERROR.getValue();
        logger.error(ex.getMessage(), ex);
        response = new ErrorResponseDTO(code, messages.get(code));

        return handleExceptionInternal(ex, response, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }


}
