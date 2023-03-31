package com.peopleapp.dto.apirequestresponselogdto;

import lombok.Data;

@Data
public class APILogDTO {

    private String userIdentity;

    private String method;

    private String uri;

    private String host;

    private APIRequestLogDTO apiRequest;

    private APIResponseLogDTO apiResponse;

    private long apiExecutionTime;

    public APILogDTO(String userIdentity, String method, String uri, String host){
        this.userIdentity = userIdentity;
        this.method = method;
        this.uri = uri;
        this.host = host;
    }

}
