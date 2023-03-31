package com.peopleapp.repository;

import com.mongodb.client.result.UpdateResult;
import com.peopleapp.constant.PeopleCollectionKeys;
import com.peopleapp.enums.RequestType;
import com.peopleapp.model.ActivityContact;
import com.peopleapp.util.PeopleUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
public class CustomActivityContactRepositoryImpl implements CustomActivityContactRepository {

    @Inject
    private MongoOperations mongoOperations;

    private String collectionName = PeopleCollectionKeys.Collection.ACTIVITY_CONTACTS.getCollectionName();

    private static final String ACTIVITY_ID = "activityId";
    private static final String IS_ACTIVE = "isActive";
    private static final String INITIATOR_ID = "initiatorId";
    private static final String RECEIVER_ID = "receiverId";
    private static final String UNIQUE_ID = "_id";
    private static final String CONNECTION_TO_ID = "contactsConnection.peopleUserToId";
    private static final String CONNECTION_BASE_PROFILE = "contactsConnection.realTimeSharedData.privacyProfileId";
    private static final String PRIVACY_PROFILE_DATA = "privacyProfileData";
    private static final String USER_DATA = "userData";
    private static final String CONNECTION_ID = "connectionId";
    private static final String CONNECTION_FNAME = "contactsConnection.contactStaticData.firstName";
    private static final String CONNECTION_LNAME = "contactsConnection.contactStaticData.lastName";
    private static final String CONNECTION_FULL_NAME = "contactsConnection.contactStaticData.fullName";
    private static final String CONNECTION_COMPANY_NAME = "contactsConnection.contactStaticData.company";
    private static final String PEOPLE_USER_FNAME = "userData.firstName.value";
    private static final String PEOPLE_USER_LNAME = "userData.lastName.value";
    private static final String PEOPLE_USER_FULL_NAME = "userData.fullName";
    private static final String PEOPLE_USER_COMPANY_NAME = "userData.company.value";
    private static final String CONTACTS_CONNECTION = "contactsConnection";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final String FULL_NAME = "fullName";
    private static final String COMPANY = "company";
    private static final String REQUEST_TYPE = "requestType";
    private static final String INTRODUCED_CONTACT_NUMBER = "introducedContactNumber";


    @Override
    public Page<ActivityContact> getActivityContactsByActivityId(String activityId, String searchString, Pageable pageable) {

        MatchOperation matchOnActivityId = Aggregation.match(Criteria
                .where(ACTIVITY_ID).is(PeopleUtils.convertStringToObjectId(activityId))
                .and(IS_ACTIVE).is(Boolean.TRUE)
        );
        return getListOfActivityContact(matchOnActivityId, null, searchString, pageable);
    }

    @Override
    public Page<ActivityContact> getShareActivityContactsByInitiatorIdAndReceiverId(String initiatorId, String receiverId,
                                                                                    String searchString, Pageable pageable) {

        MatchOperation matchOnInitiatorAndReceiver = Aggregation.match(Criteria
                .where(INITIATOR_ID).is(PeopleUtils.convertStringToObjectId(initiatorId))
                .and(RECEIVER_ID).is(PeopleUtils.convertStringToObjectId(receiverId))
                .and(REQUEST_TYPE).is(RequestType.SHARE_CONTACT_ACTIVITY.getValue())
                .and(IS_ACTIVE).is(Boolean.TRUE)
        );


        GroupOperation groupOperation = group(CONNECTION_ID).push(CONNECTION_ID).as(CONNECTION_ID)
                .first(ACTIVITY_ID).as(ACTIVITY_ID)
                .first(INITIATOR_ID).as(INITIATOR_ID)
                .first(RECEIVER_ID).as(RECEIVER_ID)
                .first(IS_ACTIVE).as(IS_ACTIVE)
                .first(REQUEST_TYPE).as(REQUEST_TYPE)
                .first(INTRODUCED_CONTACT_NUMBER).as(INTRODUCED_CONTACT_NUMBER)
                .first(FIRST_NAME).as(FIRST_NAME)
                .first(LAST_NAME).as(LAST_NAME)
                .first(FULL_NAME).as(FULL_NAME)
                .first(COMPANY).as(COMPANY)
                .addToSet(UNIQUE_ID).as("subIdList");

        return getListOfActivityContact(matchOnInitiatorAndReceiver, groupOperation, searchString, pageable);

    }

    @Override
    public List<ActivityContact> getActivityContactsByIdsAndUserId(List<String> activitySubIdList, String userId) {
        Query query = new Query(
                Criteria.where(UNIQUE_ID).in(activitySubIdList)
                        .orOperator(Criteria.where(INITIATOR_ID).is(PeopleUtils.convertStringToObjectId(userId)),
                                Criteria.where(RECEIVER_ID).is(PeopleUtils.convertStringToObjectId(userId)))
        );

        return mongoOperations.find(query, ActivityContact.class, collectionName);
    }

    @Override
    public List<ActivityContact> getActivityContactsByActivityIdsAndUserId(List<String> activityIdList, String userId) {
        Query query = new Query(
                Criteria.where(ACTIVITY_ID).in(PeopleUtils.convertStringToObjectId(activityIdList))
                        .orOperator(Criteria.where(INITIATOR_ID).is(PeopleUtils.convertStringToObjectId(userId)),
                                Criteria.where(RECEIVER_ID).is(PeopleUtils.convertStringToObjectId(userId)))
        );

        return mongoOperations.find(query, ActivityContact.class, collectionName);
    }

    @Override
    public List<ActivityContact> getActivityContactsByIdsAndReceiverId(List<String> activitySubIdList, String userId) {
        Query query = new Query(
                Criteria.where(UNIQUE_ID).in(activitySubIdList)
                        .and(RECEIVER_ID).is(PeopleUtils.convertStringToObjectId(userId))
        );

        return mongoOperations.find(query, ActivityContact.class, collectionName);
    }

    @Override
    public List<ActivityContact> getActivityContactsByInitiatorIdAndConnectionId(String initiatorId, String connectionId) {
        Query query = new Query(
                Criteria.where(INITIATOR_ID).is(PeopleUtils.convertStringToObjectId(initiatorId))
                        .and(CONNECTION_ID).is(PeopleUtils.convertStringToObjectId(connectionId))
        );

        return mongoOperations.find(query, ActivityContact.class, collectionName);
    }

    private LookupOperation getLookupOperation(String fromCollection, String localField, String foreignField, String as) {

        return LookupOperation.newLookup()
                .from(fromCollection)
                .localField(localField)
                .foreignField(foreignField)
                .as(as);
    }

    private Page<ActivityContact> getListOfActivityContact(MatchOperation matchOperation,
                                                           GroupOperation groupOperation, String searchString,
                                                           Pageable pageable) {
        long totalElementsCount;

        LookupOperation lookupOperationForConnection = getLookupOperation(PeopleCollectionKeys
                .Collection.USER_CONNECTION.getCollectionName(), CONNECTION_ID, UNIQUE_ID, CONTACTS_CONNECTION);

        LookupOperation lookupPrivacyProfile = getLookupOperation(PeopleCollectionKeys
                        .Collection.USER_PRIVACY_PROFILE.getCollectionName(), CONNECTION_BASE_PROFILE, UNIQUE_ID,
                PRIVACY_PROFILE_DATA);

        LookupOperation lookupUserData = getLookupOperation(PeopleCollectionKeys
                .Collection.PEOPLE_USER.getCollectionName(), CONNECTION_TO_ID, UNIQUE_ID, USER_DATA);

        SortOperation sortOnFNameAndLName = sort(pageable.getSort());

        ProjectionOperation projectActivityContactWithName = project(UNIQUE_ID, ACTIVITY_ID, INITIATOR_ID,
                RECEIVER_ID, CONNECTION_ID, IS_ACTIVE, REQUEST_TYPE, INTRODUCED_CONTACT_NUMBER)
                .and(ConditionalOperators.ifNull(PEOPLE_USER_FNAME)
                        .thenValueOf(CONNECTION_FNAME))
                .as(FIRST_NAME)
                .and(ConditionalOperators.ifNull(PEOPLE_USER_LNAME)
                        .thenValueOf(CONNECTION_LNAME))
                .as(LAST_NAME)
                .and(ConditionalOperators.ifNull(PEOPLE_USER_FULL_NAME)
                        .thenValueOf(CONNECTION_FULL_NAME))
                .as(FULL_NAME)
                .and(ConditionalOperators.ifNull(PEOPLE_USER_COMPANY_NAME)
                        .thenValueOf(CONNECTION_COMPANY_NAME))
                .as(COMPANY);

        AggregationOptions collation = AggregationOptions.builder().collation(Collation.of("en")
                .strength(Collation.ComparisonLevel.primary())).build();

        Aggregation aggregation;
        Aggregation aggregationElementCount;
        /**
         * 1. All activity contacts for give activityId or Initiator-ReceiverId is fetched
         * 2. For each activity contact respective connection, peopleUser details and privacy profiles are fetched
         * 3. Projection is used to perform sorting on firstName and lastName
         *     preference is given for realTime data, if not available then contactStaticData values will be considered
         */
        if (searchString.isEmpty()) {

            if (groupOperation != null) {
                aggregation = Aggregation.newAggregation(
                        matchOperation,
                        lookupOperationForConnection,
                        unwind(CONTACTS_CONNECTION, true),
                        lookupPrivacyProfile,
                        lookupUserData,
                        unwind(USER_DATA, true),
                        projectActivityContactWithName,
                        groupOperation,
                        sortOnFNameAndLName,
                        skip((long) (pageable.getPageNumber()) * (pageable.getPageSize())),
                        limit(pageable.getPageSize())
                ).withOptions(collation);

                aggregationElementCount = Aggregation.newAggregation(
                        matchOperation,
                        lookupOperationForConnection,
                        lookupPrivacyProfile,
                        lookupUserData,
                        groupOperation
                ).withOptions(collation);
            } else {
                aggregation = Aggregation.newAggregation(
                        matchOperation,
                        lookupOperationForConnection,
                        unwind(CONTACTS_CONNECTION, true),
                        lookupPrivacyProfile,
                        lookupUserData,
                        unwind(USER_DATA, true),
                        projectActivityContactWithName,
                        sortOnFNameAndLName,
                        skip((long) (pageable.getPageNumber()) * (pageable.getPageSize())),
                        limit(pageable.getPageSize())
                ).withOptions(collation);

                aggregationElementCount = Aggregation.newAggregation(
                        matchOperation,
                        lookupOperationForConnection,
                        lookupPrivacyProfile,
                        lookupUserData
                ).withOptions(collation);
            }
        } else {

            MatchOperation regexOperation = Aggregation.match(new Criteria().orOperator(
                    Criteria
                            .where(FIRST_NAME).regex(("^").concat(searchString), "i"),
                    Criteria
                            .where(LAST_NAME).regex(("^").concat(searchString), "i"),
                    Criteria
                            .where(FULL_NAME).regex(("^").concat(searchString), "i"),
                    Criteria
                            .where(COMPANY).regex(("^").concat(searchString), "i")
                    )
            );

            if (groupOperation != null) {
                aggregation = Aggregation.newAggregation(
                        matchOperation,
                        lookupOperationForConnection,
                        unwind(CONTACTS_CONNECTION, true),
                        lookupPrivacyProfile,
                        lookupUserData,
                        unwind(USER_DATA, true),
                        projectActivityContactWithName,
                        regexOperation,
                        groupOperation,
                        sortOnFNameAndLName,
                        skip((long) (pageable.getPageNumber()) * (pageable.getPageSize())),
                        limit(pageable.getPageSize())
                ).withOptions(collation);

                aggregationElementCount = Aggregation.newAggregation(
                        matchOperation,
                        lookupOperationForConnection,
                        unwind(CONTACTS_CONNECTION, true),
                        lookupPrivacyProfile,
                        lookupUserData,
                        unwind(USER_DATA, true),
                        projectActivityContactWithName,
                        regexOperation,
                        groupOperation
                ).withOptions(collation);
            } else {
                aggregation = Aggregation.newAggregation(
                        matchOperation,
                        lookupOperationForConnection,
                        unwind(CONTACTS_CONNECTION, true),
                        lookupPrivacyProfile,
                        lookupUserData,
                        unwind(USER_DATA, true),
                        projectActivityContactWithName,
                        regexOperation,
                        sortOnFNameAndLName,
                        skip((long) (pageable.getPageNumber()) * (pageable.getPageSize())),
                        limit(pageable.getPageSize())
                ).withOptions(collation);

                aggregationElementCount = Aggregation.newAggregation(
                        matchOperation,
                        lookupOperationForConnection,
                        unwind(CONTACTS_CONNECTION, true),
                        lookupPrivacyProfile,
                        lookupUserData,
                        unwind(USER_DATA, true),
                        projectActivityContactWithName,
                        regexOperation
                ).withOptions(collation);
            }
        }

        AggregationResults<ActivityContact> results = mongoOperations.aggregate(aggregation, collectionName,
                ActivityContact.class);

        totalElementsCount = mongoOperations.aggregate(aggregationElementCount, collectionName, ActivityContact.class)
                .getMappedResults().size();

        return new PageImpl<>(results.getMappedResults(), pageable, totalElementsCount);

    }

    @Override
    public UpdateResult markActivityContactsInActiveByActivityId(List<String> activityIds) {
        Query query = new Query(
                Criteria.where(ACTIVITY_ID).in(PeopleUtils.convertStringToObjectId(activityIds)));
        Update update = new Update();
        update.set(IS_ACTIVE, Boolean.FALSE);

        return mongoOperations.updateMulti(query, update, ActivityContact.class, collectionName);
    }

    @Override
    public void expireActivityContactsByInitiatorIdAndReceiverId(String sessionUserId,
                                                                 List<String> connectedContactsUserId,
                                                                 boolean receivedActivityContacts) {
        Criteria preferredCriteria = null;

        if (receivedActivityContacts) {

            preferredCriteria = Criteria
                    .where(RECEIVER_ID).is(PeopleUtils.convertStringToObjectId(sessionUserId))
                    .and(INITIATOR_ID).in(PeopleUtils.convertStringToObjectId(connectedContactsUserId));
        } else {

            preferredCriteria = Criteria
                    .where(INITIATOR_ID).is(PeopleUtils.convertStringToObjectId(sessionUserId))
                    .and(RECEIVER_ID).in(PeopleUtils.convertStringToObjectId(connectedContactsUserId));
        }

        Query query = new Query(preferredCriteria);

        Update update = new Update();
        update.set(IS_ACTIVE, Boolean.FALSE);

        mongoOperations.updateMulti(query, update, ActivityContact.class, collectionName);
    }

    @Override
    public void updateConnectionIdForActivityContacts(String initiatorId, String masterConnectionId,
                                                      List<String> connectionIdsToBeReplaced) {
        Query query = new Query(
                Criteria.where(INITIATOR_ID).is(PeopleUtils.convertStringToObjectId(initiatorId))
                        .and(CONNECTION_ID).in(PeopleUtils.convertStringToObjectId(connectionIdsToBeReplaced))
        );

        Update updateConnectionId = new Update();
        updateConnectionId.set(CONNECTION_ID, PeopleUtils.convertStringToObjectId(masterConnectionId));

        mongoOperations.updateMulti(query, updateConnectionId, ActivityContact.class, collectionName);
    }

}
