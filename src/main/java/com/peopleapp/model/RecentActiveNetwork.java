package com.peopleapp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "recentActiveNetworks")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecentActiveNetwork {

    @Id
    @Field(value = "_id")
    private ObjectId id;

    private String networkId;

    private String networkCategory;

    private boolean newNetwork;

    private boolean newMember;

    // Keeping expiration for 1 week (604800 sec = 60*60*24*7)
    @Indexed(name = "dateIndex", expireAfterSeconds = 604800)
    private DateTime createdOn;

    // Used to calculate the network weightage while fetching suggested networks
    private String totalNetworkWeightage;
}


