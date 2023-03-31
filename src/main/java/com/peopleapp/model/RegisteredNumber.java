package com.peopleapp.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Data
@Document(collection = "registeredNumber")
public class RegisteredNumber {

    @Id
    @Field(value = "_id")
    private String uniqueId;

    private List<String> registeredNumberList;
}
