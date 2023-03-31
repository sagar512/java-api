package com.peopleapp.dto;

import com.peopleapp.model.PeopleUser;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class OneToManyIntroduction {

    private UserContact receiver;

    private List<UserContact> introducedContactList;

    private Map<ContactNumberDTO, PeopleUser> contactNumberToUserMap;

    private String initiatorId;

    private String initiatorName;

    private String receiverId;

    private String introductionMessage;
}
