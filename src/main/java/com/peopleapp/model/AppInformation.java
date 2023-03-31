package com.peopleapp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "appInformation")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@TypeAlias("appInformation")
public class AppInformation {

    @Id
    @Field(value = "_id")
    private ObjectId uniqueId;

    private Integer deviceTypeId;

    private String buildVersion;

    private Integer buildNumber;

    private Boolean forceUpdateRequired = Boolean.FALSE;
}
