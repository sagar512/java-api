package com.peopleapp.dto;

import com.peopleapp.model.UserActivity;
import com.peopleapp.model.UserConnection;
import lombok.Data;

@Data
public class UserActivityAndConnectionData {

    private UserActivity userActivity;

    private UserConnection userConnection;
}
