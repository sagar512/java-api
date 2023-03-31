package com.peopleapp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peopleapp.dto.ContactNumberDTO;
import com.peopleapp.enums.TokenStatus;
import lombok.Data;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "temporarySessions")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TemporarySession {

    @Id
    @Field(value = "_id")
    private ObjectId id;

    private String otp;

    private int otpRetryCount = 0;

    private ContactNumberDTO contactNumber;

    private String temporaryToken;

    private TokenStatus tokenStatus;

    private String userId;

    @Field
    @Indexed(name = "dateIndex", expireAfterSeconds = 900)
    private DateTime createdOn;

    private String operation;

}
