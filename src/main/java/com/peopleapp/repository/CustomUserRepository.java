package com.peopleapp.repository;

import com.peopleapp.dto.ContactNumberDTO;
import com.peopleapp.enums.UserStatus;
import com.peopleapp.model.PeopleUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.util.List;

public interface CustomUserRepository {

    PeopleUser findByCodeAndNumber(String code, String number);

    PeopleUser findByUserIdAndStatus(String userId, UserStatus status);

    List<PeopleUser> findByUserIdsAndStatus(List<String> userId, UserStatus status);

    Page<PeopleUser> getUserDetailsForSelectedUserIds(List<String> userIdList, String searchString, Pageable pageable);

    void flagUserAccount(String userId, String reason);

    PeopleUser findByPrimaryEmail(String email);

    PeopleUser findByContactNumberWithLimitedFields(ContactNumberDTO contactNumber);

    List<PeopleUser> findByContactNumberWithLimitedFields(List<ContactNumberDTO> contactNumberList);

    PeopleUser getUserWithBlockedId(String userId);


}

