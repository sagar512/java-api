package com.peopleapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.peopleapp.dto.SharedProfileInformationData;
import com.peopleapp.dto.StaticSharedData;
import com.peopleapp.dto.UserInformationDTO;
import com.peopleapp.enums.ConnectionStatus;
import lombok.Data;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;


/*
 *
 * User restore connection/s or contact/s.
 * Includes shared data information with contact/connection.
 * A people user has three type of contacts:
 * static contact -a contact which is not a registered user and hence not real time connected
 * non static contact - a contact which is a registered user , but not real time connected
 * connection - a contact to which it is connected in real time
 *
 */
@Document(collection = "userRestoreConnection")
@Data
public class UserRestoreConnection {

    @Id
    @Field(value = "_id")
    private ObjectId connectionId;

    @Field(value = "peopleUserFromId")
    private ObjectId connectionFromId;

    private String deviceContactId;

    @Field(value = "peopleUserToId")
    private ObjectId connectionToId;

    private ConnectionStatus connectionStatus;

    private Boolean isFavourite = Boolean.FALSE;

    private Integer sequenceNumber;

    /* privacy profile id shared by user with this contact */
    private String sharedPrivacyProfileId;

    /*
     *
     * All kind of data which is real time shared by 'toUserId' for 'fromUserId', till this contact is a connection.
     * Makes sense only for connections and not contacts/static contacts.
     * Includes - shared profile data, connection/contact data, location data
     *
     */
    private SharedProfileInformationData realTimeSharedData;

    /*
     *
     * All kind of data which is shared by this contact, but is static and no longer updates in real time
     *
     */
    private StaticSharedData staticSharedData;

    /*
     *
     * Profile data which is added for this connection/contact and is not at all real time.
     *
     */
    private UserInformationDTO staticProfileData;

    /*
     *
     * Store the fields deleted by the connection.
     *
     */
    private UserInformationDTO connectionDeletedData;

    @Transient
    private List<String> groupIdList;

    @Transient
    private Boolean isBlocked = Boolean.FALSE;

    private DateTime lastUpdatedOn;

    /*
     *
     * Below two properties are only used by mongodb aggregation framework and is not directly persisted
     *
     */
    @JsonIgnore
    private UserPrivacyProfile privacyProfileData;

    @JsonIgnore
    private PeopleUser userData;

    public String getConnectionId() {
        return this.connectionId.toString();
    }

    public String getConnectionFromId() {

        return this.connectionFromId != null ? this.connectionFromId.toString() : null;
    }

    public void setConnectionFromId(String fromId) {
        if (fromId != null) {
            this.connectionFromId = new ObjectId(fromId);
        }
    }

    public String getConnectionToId() {
        return this.connectionToId != null ? this.connectionToId.toString() : null;
    }

    public void setConnectionToId(String toId) {
        if (toId != null) {
            this.connectionToId = new ObjectId(toId);
        }
    }

    public SharedProfileInformationData getSharedProfile() {
        if (this.realTimeSharedData != null) {
            return this.realTimeSharedData;
        } else {
            return new SharedProfileInformationData();
        }
    }

    public SharedProfileInformationData getRealTimeSharedData() {
        return this.realTimeSharedData != null ? this.realTimeSharedData : new SharedProfileInformationData();
    }

    public StaticSharedData getStaticSharedData() {
        return this.staticSharedData != null ? this.staticSharedData : new StaticSharedData();
    }

    public void setStaticSharedProfileData(UserInformationDTO staticSharedProfileData) {

        StaticSharedData staticData = getStaticSharedData();
        staticData.setProfileData(staticSharedProfileData);
        this.staticSharedData = staticData;
    }
}
