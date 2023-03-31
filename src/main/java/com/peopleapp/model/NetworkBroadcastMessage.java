package com.peopleapp.model;

import lombok.Data;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "networkMessages")
public class NetworkBroadcastMessage {
    @Id
    @Field(value = "_id")
    private ObjectId messageId;

    private ObjectId networkId;

    @Field(value = "networkMemberId")
    private ObjectId broadcasterId;

    @Field(value = "networkMemberRole")
    private String broadcasterRole;

    private String message;

    private long sentCount;

    private DateTime messageBroadcastTime;

}
