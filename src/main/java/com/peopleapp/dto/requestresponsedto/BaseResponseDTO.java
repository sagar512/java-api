package com.peopleapp.dto.requestresponsedto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponseDTO<T> {

    private T data;
    private String message;
}
