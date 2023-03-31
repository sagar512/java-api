package com.peopleapp.model;

import lombok.Data;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document(collection = "mergedConnections")
@Data
public class MergedConnection {

    @Id
    @Field(value = "_id")
    private ObjectId mergeId;

    @Field(value = "peopleUserId")
    private String userId;

    private String masterConnectionId;

    private List<String> mergedConnectionIds;

    private List<String> groupsUpdatedWithMasterConnectionId;

    private DateTime mergedOn;

}
