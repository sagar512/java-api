package com.peopleapp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peopleapp.dto.ContactNumberDTO;
import com.peopleapp.enums.ReferralStatus;
import lombok.Data;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "peopleAmbassadors")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@TypeAlias("peopleAmbassadors")
public class PeopleAmbassador {

    @Id
    @Field(value = "_id")
    private ObjectId uniqueId;

    private ObjectId ambassadorId;

    private ObjectId referredUserId;

    private ContactNumberDTO referredContactNumber;

    private String referralLink;

    private String referralCode;

    private ReferralStatus referralStatus;

    private int rewardPoints;

    private DateTime referralInitiatedOn;

    private DateTime referralCompletedOn;


    public String getAmbassadorID() {
        return this.ambassadorId.toString();
    }

    public void setAmbassadorID(String ambassadorId) {
        if (ambassadorId != null) {
            this.ambassadorId = new ObjectId(ambassadorId);
        }
    }

    public void setReferredUserId(String referredUserId) {
        if (referredUserId != null) {
            this.referredUserId = new ObjectId(referredUserId);
        }
    }

}
