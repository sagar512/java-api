package com.peopleapp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peopleapp.enums.TokenStatus;
import lombok.Data;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "userSessions")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSession {

    @Id
    @Field(value = "_id")
    private ObjectId id;

    @Field(value = "peopleUserId")
    private ObjectId userId;

    private String sessionToken;

    private TokenStatus status;

    private Integer deviceTypeId;

    private String deviceToken;

    private String appVersion;

    private String endPointARN;

    private DateTime createdTime;

    private DateTime modifiedTime;

    public String getUserId() {
        return this.userId != null ? this.userId.toString() : null;
    }

    public void setUserId(String userId) {
        if (userId != null) {
            this.userId = new ObjectId(userId);
        }
    }


}
