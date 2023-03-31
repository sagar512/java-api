package com.peopleapp.dto;

import com.peopleapp.model.PeopleUser;
import com.peopleapp.model.UserConnection;
import com.peopleapp.model.UserPrivacyProfile;
import lombok.Data;

@Data
public class SharedData {

    private String connectionId;

    private UserConnection connectionData;

    private UserPrivacyProfile privacyProfileData;

    private PeopleUser userData;
}
