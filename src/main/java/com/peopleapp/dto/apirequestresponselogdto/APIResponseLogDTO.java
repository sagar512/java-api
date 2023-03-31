package com.peopleapp.dto.apirequestresponselogdto;

import lombok.Data;

@Data
public class APIResponseLogDTO {

    private String responseBody;

    private int responseStatus;
}
