package com.peopleapp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "verifyEmails")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VerifyEmail {

    @Id
    @Field(value = "_id")
    private ObjectId id;

    @Field(value = "peopleUserId")
    private String userId;

    private Boolean isEmailVerified = Boolean.FALSE;

    private Boolean isPrimary = Boolean.FALSE;

    private String email;

    private String verificationLink;

    @Field
    @Indexed(name="dateIndex", expireAfterSeconds=86400)
    private DateTime createdOn;


}
