package com.peopleapp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peopleapp.dto.requestresponsedto.ShareProfileData;

import lombok.Data;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document(collection = "userPrivacyProfiles")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@TypeAlias("userPrivacyProfiles")
public class UserPrivacyProfile {

    @Id
    @Field(value = "_id")
    private ObjectId privacyProfileId;

    @Field(value = "peopleUserId")
    private ObjectId userId;

    private String profileName;

    private String profileDesc;

    private String imageURL;

    private Boolean isSystem;

    private Boolean isDefault;

    private Boolean isPublic;

    private Boolean isCompanyShared = Boolean.FALSE;

    private Boolean isPositionShared = Boolean.FALSE;

    private Boolean isTagShared = Boolean.FALSE;

    private Boolean isNameShared = Boolean.TRUE;

    private Boolean isNickNameShared = Boolean.FALSE;

    private Boolean isMaidenNameShared = Boolean.FALSE;

    private List<String> valueIdList;

    private DateTime lastUpdatedOn;

    private DateTime createdOn;

    private Boolean useDefaultImage = Boolean.TRUE;
    
    private ShareProfileData shareProfileData;
    
    public String getPrivacyProfileId() {
        return this.privacyProfileId.toString();
    }

    public String getUserId() {
        return this.userId != null ? this.userId.toString() : null;
    }

    public void setUserId(String userId) {
        if (userId != null) {
            this.userId = new ObjectId(userId);
        }
    }


}
