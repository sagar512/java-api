package com.peopleapp.dto;

import com.peopleapp.model.PeopleUser;
import lombok.Data;

import java.util.Map;

@Data
public class OneToOneIntroduction {

    private UserContact receiver;

    private UserContact introducedContact;

    private Map<ContactNumberDTO, PeopleUser> contactNumberToUserMap;

    private String initiatorId;

    private String initiatorName;

    private String receiverId;

    private String introductionMessage;
}
