package com.peopleapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.peopleapp.enums.NetworkMemberRole;
import com.peopleapp.enums.NetworkMemberStatus;
import com.peopleapp.validator.EnumValidator;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "networkMembers")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@TypeAlias("networkMembers")
public class NetworkMember {

    @Id
    @Field(value = "_id")
    private ObjectId uniqueId;

    private ObjectId networkId;

    @Field(value = "peopleUserId")
    private ObjectId memberId;

    private Boolean isFavourite = Boolean.FALSE;

    @EnumValidator(enumClazz = NetworkMemberRole.class)
    private String memberRole;

    private String memberStatus = NetworkMemberStatus.ACTIVE.getValue();

    @JsonIgnore
    private PeopleUser networkMemberDetails;

    public String getNetworkId() {
        return this.networkId != null ? this.networkId.toString() : null;
    }

    public String getMemberId() {
        return this.memberId != null ? this.memberId.toString() : null;
    }

    public void setMemberId(String memberId) {
        if (memberId != null) {
            this.memberId = new ObjectId(memberId);
        }
    }

    public void setNetworkId(String networkId) {
        if (networkId != null) {
            this.networkId = new ObjectId(networkId);
        }
    }

}
