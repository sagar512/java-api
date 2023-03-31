package com.peopleapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.peopleapp.dto.ContactNumberDTO;
import com.peopleapp.enums.RequestType;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document(collection = "activityContacts")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@TypeAlias("activityContacts")
public class ActivityContact {

    @Id
    @Field(value = "_id")
    private ObjectId uniqueId;

    private ObjectId activityId;

    private ObjectId initiatorId;

    private ObjectId receiverId;

    private ObjectId connectionId;

    private Boolean isActive = Boolean.TRUE;

    private RequestType requestType;

    private ContactNumberDTO introducedContactNumber;

    @JsonIgnore
    private List<String> subIdList;

    public String getActivityId() {
        return this.activityId != null ? this.activityId.toString() : null;
    }

    public String getConnectionId() {
        return this.connectionId != null ? this.connectionId.toString() : null;
    }

    public String getInitiatorId() {
        return this.initiatorId != null ? this.initiatorId.toString() : null;
    }

    public String getReceiverId() {
        return this.receiverId != null ? this.receiverId.toString() : null;
    }

    public void setActivityId(String activityId) {
        if (activityId != null) {
            this.activityId = new ObjectId(activityId);
        }
    }

    public void setConnectionId(String connectionId) {
        if (connectionId != null) {
            this.connectionId = new ObjectId(connectionId);
        }
    }

    public void setInitiatorId(String initiatorId) {
        if (initiatorId != null) {
            this.initiatorId = new ObjectId(initiatorId);
        }
    }

    public void setReceiverId(String receiverId) {
        if (receiverId != null) {
            this.receiverId = new ObjectId(receiverId);
        }
    }

}
