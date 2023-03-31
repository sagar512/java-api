package com.peopleapp.constant;

/*
This class contains all pre defined profile keys defined in people system
 */
public class PeopleCollectionKeys {

    private PeopleCollectionKeys() {

    }

    /*
    Collection names
     */

    public enum Collection {

        NETWORK("networks"),
        NETWORK_MEMBER("networkMembers"),
        USER_CONNECTION("userConnections"),
        USER_ACTIVITY("userActivities"),
        PEOPLE_USER("peopleUsers"),
        USER_GROUP("userGroups"),
        USER_PRIVACY_PROFILE("userPrivacyProfiles"),
        SYSTEM_TAG("systemTags"),
        RECENT_ACTIVE_NETWORK("recentActiveNetworks"),
        ACTIVITY_CONTACTS("activityContacts"),
        PEOPLE_AMBASSADORS("peopleAmbassadors");

        private String collectionName;

        Collection(String collectionName) {
            this.collectionName = collectionName;
        }

        public String getCollectionName() {
            return collectionName;
        }

    }

    // general

    public static final String USER_ID = "_id";
    public static final String USER_REFERENCE_ID = "peopleUserId";
    public static final String CREATED_ON = "createdOn";

    /* profile collection*/
    public static final String ACTIVITY_TYPE = "activityType";

    /*
     *
     * category 'phoneNumber' keys
     *
     */
    public static final String COUNTRY_CODE_KEY = "countryCode";
    public static final String PHONE_NUMBER_KEY = "phoneNumber";
    public static final String EMAIL_ADDRESS_KEY = "emailAddress";
    public static final String PUBLIC_PRIVACY_PUBLIC = "Public";

}
