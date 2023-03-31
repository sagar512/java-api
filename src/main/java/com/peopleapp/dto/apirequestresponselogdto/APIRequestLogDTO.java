package com.peopleapp.dto.apirequestresponselogdto;

import lombok.Data;

import java.util.Map;

@Data
public class APIRequestLogDTO {

    private String contentType;

    private Map<String, String> headers;

    private String queryString;

    private String requestBody;
}
