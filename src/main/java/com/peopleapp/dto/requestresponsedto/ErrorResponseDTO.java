package com.peopleapp.dto.requestresponsedto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDTO {

    private String message;

    private String statusCode;

    public ErrorResponseDTO() {

    }

    public ErrorResponseDTO(String statusCode, String message) {
        this.message = message;
        this.statusCode = statusCode;
    }

    public ErrorResponseDTO(String message) {
        this.message = message;
    }

    public String convertToJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

}

