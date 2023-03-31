package com.peopleapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.peopleapp.dto.ActivityType;
import com.peopleapp.dto.SharedProfileInformationData;
import com.peopleapp.dto.UserContact;
import com.peopleapp.enums.ActivityStatus;
import com.peopleapp.util.PeopleUtils;
import lombok.Data;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.*;

@Document(collection = "userActivities")
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UserActivity {

    @Id
    @Field(value = "_id")
    private ObjectId activityId;

    /*
     *
     * this field is used when multiple activities are created through one single request
     *
     */
    @JsonIgnore
    private String requestId;

    @Field(value = "peopleUserById")
    private ObjectId activityById;

    @Field(value = "peopleUserToId")
    private ObjectId activityForId;

    /*
     *
     * if activity is initiated to a contact
     */
    private UserContact initiateDetails;

    private ActivityType activityType;

    private ObjectId actionTakenById;

    private String message;

    private List<String> introducedContactNumber;

    private Boolean isInitiatorBlocked = Boolean.FALSE;

    @JsonIgnore
    private SharedProfileInformationData sharedProfileInformationData;

    @JsonIgnore
    private ObjectId networkId;

    /* Time in minutes */
    private Integer locationSharedForTime;

    /* map of shared connectionId -> generated unique id for each shared contact */
    private Set<String> sharedConnectionIdList;

    private List<Set<String>> listOfSharedConnectionIds;

    private ActivityStatus overallStatus;

    private Boolean isCleared = Boolean.FALSE;

    private Boolean isRead = Boolean.FALSE;

    private DateTime createdOn;

    private DateTime lastUpdatedOn;

    @Indexed(name = "expire_activity_index", expireAfterSeconds = 30)
    private DateTime expireAt;

    private List<ObjectId> activityIds;

    // Will use this field to return the associated "connectionId"
    // during "/connection-request/send" and "/connection-request/accept" API response generation
    @JsonIgnore
    private String connectionId;

    public String getActivityId() {
        return this.activityId.toString();
    }

    public String getActivityById() {
        return this.activityById != null ? this.activityById.toString() : null;
    }

    public String getActivityForId() {
        return this.activityForId != null ? this.activityForId.toString() : null;
    }

    public String getNetworkId() {
        return this.networkId != null ? this.networkId.toString() : null;
    }

    public void setActionTakenById(String id) {
        if (id != null) {
            this.actionTakenById = new ObjectId(id);
        }
    }

    public void setActivityById(String userId) {
        if (userId != null) {
            this.activityById = new ObjectId(userId);
        }
    }

    public void setActivityForId(String userId) {
        if (userId != null) {
            this.activityForId = new ObjectId(userId);
        }
    }

    public Set<String> getSharedConnectionIdList() {
        return sharedConnectionIdList != null ? sharedConnectionIdList : new HashSet<>();
    }

    public  List<String> getActivityIds(){
        List<String> activityIdsList = new ArrayList<>();

        for(ObjectId id : this.activityIds){
            activityIdsList.add(PeopleUtils.convertObjectIdToString(id));
        }

        return  activityIdsList;
    }

}
