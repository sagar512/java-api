package com.peopleapp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document(collection = "userGroups")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserGroup {

    @Id
    @Field(value = "_id")
    private ObjectId groupId;

    @Field(value = "peopleUserId")
    private ObjectId ownerId;

    private String title;

    private String desc;

    private String imageURL;

    private Integer orderNumber;

    private Boolean isFavourite = Boolean.FALSE;

    private List<String> contactIdList;

    private DateTime lastUpdatedOn;

    /**
     * Used by Front end team to map localId and groupId in offline mode
     */
    private String localId;

    public String getGroupId() {
        return this.groupId.toString();
    }

    public String getOwnerId() {
        return this.ownerId != null ? this.ownerId.toString() : null;
    }

    public void setOwnerId(String ownerId) {
        if (ownerId != null) {
            this.ownerId = new ObjectId(ownerId);
        }
    }

}
