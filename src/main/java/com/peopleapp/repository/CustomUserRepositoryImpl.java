package com.peopleapp.repository;

import com.peopleapp.constant.PeopleCollectionKeys;
import com.peopleapp.dto.ContactNumberDTO;
import com.peopleapp.enums.UserStatus;
import com.peopleapp.model.PeopleUser;
import com.peopleapp.util.PeopleUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CustomUserRepositoryImpl implements CustomUserRepository {

    private static final String PEOPLE_USER_COLLECTION = PeopleCollectionKeys.Collection.PEOPLE_USER.getCollectionName();

    @Autowired
    private MongoOperations mongoOperations;

    private static final String UNIQUE_ID = "_id";
    private static final String CONTACT_NUMBER = "verifiedContactNumber";
    private static final String STATUS = "status";
    private static final String FIRST_NAME = "firstName.value";
    private static final String LAST_NAME = "lastName.value";
    private static final String FULL_NAME = "fullName";
    private static final String COMPANY_NAME = "company.value";

    @Override
    public PeopleUser findByCodeAndNumber(String code, String number) {

        Query query = new Query(Criteria
                .where("verifiedContactNumber.countryCode").is(code)
                .and("verifiedContactNumber.phoneNumber").is(number)
                .and(STATUS).ne(UserStatus.DELETED.getValue()));
        return mongoOperations.findOne(query, PeopleUser.class, PEOPLE_USER_COLLECTION);

    }

    @Override
    public PeopleUser findByUserIdAndStatus(String userId, UserStatus status) {
        Query query = new Query(Criteria
                .where(UNIQUE_ID).is(userId)
                .and(STATUS).is(status));
        return mongoOperations.findOne(query, PeopleUser.class, PEOPLE_USER_COLLECTION);
    }

    @Override
    public List<PeopleUser> findByUserIdsAndStatus(List<String> userIds, UserStatus status) {

        Query query = new Query(Criteria
                .where(UNIQUE_ID).in(PeopleUtils.convertStringToObjectId(userIds))
                .and(STATUS).is(status)
        );
        return mongoOperations.find(query, PeopleUser.class, PEOPLE_USER_COLLECTION);
    }

    @Override
    public Page<PeopleUser> getUserDetailsForSelectedUserIds(List<String> userIdList, String searchString, Pageable pageable) {
        Query query;
        if (searchString.isEmpty()) {
            query = new Query(Criteria
                    .where(UNIQUE_ID).in(PeopleUtils.convertStringToObjectId(userIdList))
                    .and(STATUS).is(UserStatus.ACTIVE.getValue()))
                    .with(pageable);
        } else {
            query = new Query(Criteria
                    .where(UNIQUE_ID).in(PeopleUtils.convertStringToObjectId(userIdList))
                    .and(STATUS).is(UserStatus.ACTIVE.getValue())
                    .orOperator(
                            Criteria
                                    .where(FIRST_NAME).regex(("^").concat(searchString), "i"),
                            Criteria
                                    .where(LAST_NAME).regex(("^").concat(searchString), "i"),
                            Criteria
                                    .where(FULL_NAME).regex(("^").concat(searchString), "i"),
                            Criteria
                                    .where(COMPANY_NAME).regex(("^").concat(searchString), "i")))
                    .with(pageable);
        }

        return new PageImpl<>(mongoOperations.find(query, PeopleUser.class, PEOPLE_USER_COLLECTION), pageable,
                mongoOperations.count(query, PEOPLE_USER_COLLECTION));
    }

    @Override
    public void flagUserAccount(String userId, String reason) {
        Query query = new Query(Criteria
                .where(UNIQUE_ID).is(userId));
        Update update = new Update();
        update.set("isFlagged", true);
        update.set("flaggedReason", reason);
        mongoOperations.updateFirst(query, update, PeopleUser.class, PEOPLE_USER_COLLECTION);
    }

    @Override
    public PeopleUser findByPrimaryEmail(String email) {

        Query query = new Query(Criteria
                .where("primaryEmail").is(email)
        .and(STATUS).ne(UserStatus.DELETED.getValue()));
        return mongoOperations.findOne(query, PeopleUser.class, PEOPLE_USER_COLLECTION);
    }

    @Override
    public PeopleUser findByContactNumberWithLimitedFields(ContactNumberDTO contactNumber) {

        Query query = new Query(Criteria
                .where("verifiedContactNumber.countryCode").is(contactNumber.getCountryCode())
                .and("verifiedContactNumber.phoneNumber").is(contactNumber.getPhoneNumber())
                .and(STATUS).ne(UserStatus.DELETED.getValue()));

        query.fields().include(UNIQUE_ID)
                .include(CONTACT_NUMBER)
                .include("firstName")
                .include("lastName");


        return mongoOperations.findOne(query, PeopleUser.class, PEOPLE_USER_COLLECTION);
    }

    @Override
    public List<PeopleUser> findByContactNumberWithLimitedFields(List<ContactNumberDTO> contactNumberList) {

        Query query = new Query(Criteria
                .where(CONTACT_NUMBER).in(contactNumberList)
                .and(STATUS).ne(UserStatus.DELETED.getValue()));

        query.fields().include(UNIQUE_ID)
                .include(CONTACT_NUMBER)
                .include("firstName")
                .include("lastName");



        return mongoOperations.find(query, PeopleUser.class, PEOPLE_USER_COLLECTION);
    }


    @Override
    public PeopleUser getUserWithBlockedId(String userId) {

        Query query = new Query(Criteria
                .where(UNIQUE_ID).is(userId)
                .and(STATUS).is(UserStatus.ACTIVE));

        query.fields().include("blockedUserIdList");
        query.fields().include(UNIQUE_ID);
        return mongoOperations.findOne(query, PeopleUser.class, PEOPLE_USER_COLLECTION);

    }
}
