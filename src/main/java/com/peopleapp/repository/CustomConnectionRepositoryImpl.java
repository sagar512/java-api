package com.peopleapp.repository;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.limit;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.skip;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.unwind;

import java.util.List;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import com.peopleapp.constant.PeopleCollectionKeys;
import com.peopleapp.dto.FavouriteConnectionSequenceDTO;
import com.peopleapp.dto.requestresponsedto.ManageFavouritesRequestDTO;
import com.peopleapp.enums.ConnectionStatus;
import com.peopleapp.model.UserConnection;
import com.peopleapp.util.PeopleUtils;

@Repository
public class CustomConnectionRepositoryImpl implements CustomConnectionRepository {

    private static final String COLLECTION_NAME = PeopleCollectionKeys.Collection.USER_CONNECTION.getCollectionName();

    private static final String UNIQUE_ID = "_id";
    private static final String CONNECTION_FROM_ID = "peopleUserFromId";
    private static final String CONNECTION_STATUS = "connectionStatus";
    private static final String CONNECTION_TO_ID = "peopleUserToId";
    private static final String IS_FAVOURITE = "isFavourite";
    private static final String SEQUENCE_NUMBER = "sequenceNumber";
    private static final String CONNECTION_BASE_PROFILE = "realTimeSharedData.privacyProfileId";
    private static final String PRIVACY_PROFILE_DATA = "privacyProfileData";
    private static final String USER_DATA = "userData";
    private static final String LAST_UPDATED_ON = "lastUpdatedOn";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final String CONNECTION_FNAME = "contactStaticData.firstName";
    private static final String CONNECTION_LNAME = "contactStaticData.lastName";
    private static final String PEOPLE_USER_FNAME = "userData.firstName.value";
    private static final String PEOPLE_USER_LNAME = "userData.lastName.value";
    private static final String DEVICE_CONTACT_ID = "deviceContactId";
    private static final String REAL_TIME_SHARED_DATA = "realTimeSharedData";
    private static final String SHARED_PRIVACY_PROFILE_ID = "sharedPrivacyProfileId";
    private static final String IS_BLOCKED = "isBlocked";
    private static final String GROUP_ID_LIST = "groupIdList";
    private static final String CONTACT_STATIC_DATA = "contactStaticData";
    private static final String STATIC_SHARED_DATA = "staticSharedData";

    @Autowired
    private MongoOperations mongoOperations;

    @Override
    public List<UserConnection> findConnectionByConnectionId(List<String> connectionIdList) {
        Query query = new Query(Criteria
                .where(UNIQUE_ID).is(connectionIdList));

        return mongoOperations.find(query, UserConnection.class, COLLECTION_NAME);
    }

    @Override
    public UserConnection findConnectionByConnectionId(String userId, String connectionId) {
        Query query = new Query(Criteria
                .where(UNIQUE_ID).is(connectionId)
                .and(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(userId))
                .and(CONNECTION_STATUS).is(ConnectionStatus.CONNECTED.getValue()));

        return mongoOperations.findOne(query, UserConnection.class, COLLECTION_NAME);
    }
    
    @Override
    public UserConnection findConnectionByConnectionIdnnNew(String userId, String connectionId) {
        Query query = new Query(Criteria
                .where(UNIQUE_ID).is(connectionId)
                .and(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(userId)));
        return mongoOperations.findOne(query, UserConnection.class, COLLECTION_NAME);
    }

    @Override
    public UserConnection findConnectionByConnectionIdAndInitiatorId(String userId, String connectionId) {
        Query query = new Query(Criteria
                .where(UNIQUE_ID).is(connectionId)
                .and(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(userId)));

        return mongoOperations.findOne(query, UserConnection.class, COLLECTION_NAME);
    }

    @Override
    public List<UserConnection> findConnectionByConnectionId(String userId, List<String> connectionIdList) {
        Query query = new Query(Criteria
                .where(UNIQUE_ID).in(connectionIdList)
                .and(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(userId))
                .and(CONNECTION_STATUS).is(ConnectionStatus.CONNECTED.getValue()));

        return mongoOperations.find(query, UserConnection.class, COLLECTION_NAME);

    }

    @Override
    public List<UserConnection> findConnectionByConnectionIdWithLimitedFields(String userId, List<String> connectionIdList) {

        Query query = new Query(Criteria
                .where(UNIQUE_ID).in(connectionIdList)
                .and(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(userId))
                .and(CONNECTION_STATUS).is(ConnectionStatus.CONNECTED.getValue()));

        query.fields().include(UNIQUE_ID)
                .include(CONNECTION_FROM_ID)
                .include(CONNECTION_TO_ID)
                .include(CONNECTION_STATUS);

        return mongoOperations.find(query, UserConnection.class, COLLECTION_NAME);
    }

    @Override
    public UserConnection findContactByConnectionId(String userId, String connectionId) {
        Query query = new Query(Criteria
                .where(UNIQUE_ID).is(connectionId)
                .and(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(userId))
                .and(CONNECTION_STATUS).ne(ConnectionStatus.DELETED.getValue()));

        return mongoOperations.findOne(query, UserConnection.class, COLLECTION_NAME);
    }

    @Override
    public List<UserConnection> findContactByConnectionId(String userId, List<String> connectionIdList) {
        Query query = new Query(Criteria
                .where(UNIQUE_ID).in(connectionIdList)
                .and(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(userId))
                .and(CONNECTION_STATUS).ne(ConnectionStatus.DELETED.getValue()));

        return mongoOperations.find(query, UserConnection.class, COLLECTION_NAME);
    }

    @Override
    public UserConnection findConnectionByFromIdAndToId(String fromUserId, String toUserId) {

        Query query = new Query(Criteria
                .where(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(fromUserId))
                .and(CONNECTION_TO_ID).is(PeopleUtils.convertStringToObjectId(toUserId))
                .and(CONNECTION_STATUS).is(ConnectionStatus.CONNECTED.getValue()));

        return mongoOperations.findOne(query, UserConnection.class, COLLECTION_NAME);
    }

    @Override
    public UserConnection findContactByFromIdAndToId(String fromUserId, String toUserId) {

        Query query = new Query(Criteria
                .where(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(fromUserId))
                .and(CONNECTION_TO_ID).is(PeopleUtils.convertStringToObjectId(toUserId)));

        return mongoOperations.findOne(query, UserConnection.class, COLLECTION_NAME);
    }

    @Override
    public List<UserConnection> findAllContact(String userId) {

        Query query = new Query(Criteria
                .where(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(userId))
                .and(CONNECTION_STATUS).ne(ConnectionStatus.DELETED.getValue()));

        return mongoOperations.find(query, UserConnection.class, COLLECTION_NAME);
    }
    
    @Override
    public List<UserConnection> findAllContactID(String userId) {

        Query query = new Query(Criteria
                .where(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(userId))
                .and(CONNECTION_STATUS).ne(ConnectionStatus.DELETED.getValue()));
        query.fields().include(UNIQUE_ID);
        return mongoOperations.find(query, UserConnection.class, COLLECTION_NAME);
    }

    @Override
    public List<UserConnection> getConnectionsByPeopleUserToIdAndSharedProfileIds(String userId, List<String> profileIds) {

        Query query = new Query(Criteria
                .where(CONNECTION_TO_ID).is(PeopleUtils.convertStringToObjectId(userId))
                .and(CONNECTION_STATUS).is(ConnectionStatus.CONNECTED.getValue())
                .and(CONNECTION_BASE_PROFILE).in(PeopleUtils.convertStringToObjectId(profileIds))
        );

        return mongoOperations.find(query, UserConnection.class, COLLECTION_NAME);
    }

    @Override
    public List<UserConnection> getConnectionDataWithProfileModifiedAfterLastSyncTime(String userId, DateTime lastSyncedTime, Pageable pageable) {

        java.util.Date utilDate = lastSyncedTime.toDate();
        LookupOperation lookupPrivacyProfile = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.USER_PRIVACY_PROFILE.getCollectionName())
                .localField(CONNECTION_BASE_PROFILE)
                .foreignField(UNIQUE_ID)
                .as(PRIVACY_PROFILE_DATA);

        LookupOperation lookupUserData = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.PEOPLE_USER.getCollectionName())
                .localField(CONNECTION_TO_ID)
                .foreignField(UNIQUE_ID)
                .as(USER_DATA);

        MatchOperation filterbyLastSyncedTime =
                match(new Criteria().orOperator(
                        Criteria.where(LAST_UPDATED_ON).gte(utilDate),
                        Criteria.where("userData.lastUpdatedOn").gte(utilDate),
                        Criteria.where("privacyProfileData.lastUpdatedOn").gte(utilDate)));

        Aggregation aggregation = Aggregation.newAggregation(
                match(Criteria
                        .where(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(userId))
                        .and(CONNECTION_STATUS).ne(ConnectionStatus.DELETED.getValue()))
                , lookupPrivacyProfile
                , lookupUserData
                , sort(pageable.getSort())
                , filterbyLastSyncedTime
        ).withOptions(new AggregationOptions(false, false, null,Collation.of("en").strength(Collation.ComparisonLevel.secondary())));

        AggregationResults<UserConnection> results = mongoOperations.aggregate(aggregation,
                COLLECTION_NAME, UserConnection.class);
        return results.getMappedResults();
    }

    @Override
    public Page<UserConnection> getConnectionDataWithProfilePaginated(String userId, Pageable pageable) {

        Query query = new Query(
                Criteria.where(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(userId))
                        .and(CONNECTION_STATUS).ne(ConnectionStatus.DELETED.getValue())
        );
        
        LookupOperation lookupPrivacyProfile = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.USER_PRIVACY_PROFILE.getCollectionName())
                .localField(CONNECTION_BASE_PROFILE)
                .foreignField(UNIQUE_ID)
                .as(PRIVACY_PROFILE_DATA);

        LookupOperation lookupUserData = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.PEOPLE_USER.getCollectionName())
                .localField(CONNECTION_TO_ID)
                .foreignField(UNIQUE_ID)
                .as(USER_DATA);

        Aggregation aggregation = Aggregation.newAggregation(
                match(Criteria
                        .where(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(userId))
                        .and(CONNECTION_STATUS).ne(ConnectionStatus.DELETED.getValue()))
                , lookupPrivacyProfile
                , lookupUserData
                , sort(pageable.getSort())
                , skip((long) (pageable.getPageNumber()) * (pageable.getPageSize())),
                limit(pageable.getPageSize())
        ).withOptions(new AggregationOptions(false, false, null,Collation.of("en").strength(Collation.ComparisonLevel.secondary())));

        AggregationResults<UserConnection> results = mongoOperations.aggregate(aggregation,
                COLLECTION_NAME, UserConnection.class);

        return new PageImpl<>(results.getMappedResults(), pageable, mongoOperations.count(query, COLLECTION_NAME));
    }

    @Override
    public List<UserConnection> getConnectionDataWithProfileForSelectedContact(String userId, List<String> connectionIdList) {

        LookupOperation lookupPrivacyProfile = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.USER_PRIVACY_PROFILE.getCollectionName())
                .localField(CONNECTION_BASE_PROFILE)
                .foreignField(UNIQUE_ID)
                .as(PRIVACY_PROFILE_DATA);

        LookupOperation lookupUserData = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.PEOPLE_USER.getCollectionName())
                .localField(CONNECTION_TO_ID)
                .foreignField(UNIQUE_ID)
                .as(USER_DATA);

        Aggregation aggregation = Aggregation.newAggregation(
                match(Criteria
                        .where(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(userId))
                        .and(UNIQUE_ID).in(PeopleUtils.convertStringToObjectId(connectionIdList))
                        .and(CONNECTION_STATUS).ne(ConnectionStatus.DELETED.getValue()))
                , lookupPrivacyProfile
                , lookupUserData
        );


        AggregationResults<UserConnection> results = mongoOperations.aggregate(aggregation,
                COLLECTION_NAME, UserConnection.class);
        return results.getMappedResults();
    }

    @Override
    public List<UserConnection> getConnectionDataWithProfileForSelectedToUserIds(String userId, List<String> toUserIds) {
        LookupOperation lookupPrivacyProfile = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.USER_PRIVACY_PROFILE.getCollectionName())
                .localField(CONNECTION_BASE_PROFILE)
                .foreignField(UNIQUE_ID)
                .as(PRIVACY_PROFILE_DATA);

        LookupOperation lookupUserData = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.PEOPLE_USER.getCollectionName())
                .localField(CONNECTION_TO_ID)
                .foreignField(UNIQUE_ID)
                .as(USER_DATA);

        Aggregation aggregation = Aggregation.newAggregation(
                match(Criteria
                        .where(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(userId))
                        .and(CONNECTION_TO_ID).in(PeopleUtils.convertStringToObjectId(toUserIds))
                        .and(CONNECTION_STATUS).ne(ConnectionStatus.DELETED.getValue()))
                , lookupPrivacyProfile
                , lookupUserData
        );

        AggregationResults<UserConnection> results = mongoOperations.aggregate(aggregation,
                PeopleCollectionKeys.Collection.USER_CONNECTION.getCollectionName(), UserConnection.class);
        return results.getMappedResults();

    }

    @Override
    public List<UserConnection> getSharedProfileDataForSelectedContact(List<String> connectionIdList) {

        LookupOperation lookupPrivacyProfile = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.USER_PRIVACY_PROFILE.getCollectionName())
                .localField(CONNECTION_BASE_PROFILE)
                .foreignField(UNIQUE_ID)
                .as(PRIVACY_PROFILE_DATA);

        LookupOperation lookupUserData = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.PEOPLE_USER.getCollectionName())
                .localField(CONNECTION_TO_ID)
                .foreignField(UNIQUE_ID)
                .as(USER_DATA);

        Aggregation aggregation = Aggregation.newAggregation(
                match(Criteria
                        .where(UNIQUE_ID).in(PeopleUtils.convertStringToObjectId(connectionIdList))
                        .and(CONNECTION_STATUS).ne(ConnectionStatus.DELETED.getValue()))
                , lookupPrivacyProfile
                , lookupUserData
        );


        AggregationResults<UserConnection> results = mongoOperations.aggregate(aggregation, COLLECTION_NAME,
                UserConnection.class);
        return results.getMappedResults();
    }

    @Override
    public List<UserConnection> findAllConnectionConnectedToGivenUserWithProfile(String toUserId) {
        LookupOperation lookupPrivacyProfile = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.USER_PRIVACY_PROFILE.getCollectionName())
                .localField(CONNECTION_BASE_PROFILE)
                .foreignField(UNIQUE_ID)
                .as(PRIVACY_PROFILE_DATA);

        LookupOperation lookupUserData = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.PEOPLE_USER.getCollectionName())
                .localField(CONNECTION_TO_ID)
                .foreignField(UNIQUE_ID)
                .as(USER_DATA);

        Aggregation aggregation = Aggregation.newAggregation(
                match(Criteria
                        .where(CONNECTION_TO_ID).is(PeopleUtils.convertStringToObjectId(toUserId))
                        .and(CONNECTION_STATUS).ne(ConnectionStatus.DELETED.getValue()))
                , lookupPrivacyProfile
                , lookupUserData
        );

        AggregationResults<UserConnection> results = mongoOperations.aggregate(aggregation, COLLECTION_NAME,
                UserConnection.class);
        return results.getMappedResults();
    }

    @Override
    public List<UserConnection> getPeopleUserDataForConnectionList(String userId, List<String> connectionIdList) {
        LookupOperation lookupUserData = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.PEOPLE_USER.getCollectionName())
                .localField(CONNECTION_TO_ID)
                .foreignField(UNIQUE_ID)
                .as(USER_DATA);

        Aggregation aggregation = Aggregation.newAggregation(
                match(Criteria
                        .where(UNIQUE_ID).in(PeopleUtils.convertStringToObjectId(connectionIdList))
                        .and(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(userId)))
                , lookupUserData
        );

        AggregationResults<UserConnection> results = mongoOperations.aggregate(aggregation, COLLECTION_NAME,
                UserConnection.class);

        return results.getMappedResults();
    }

    @Override
    public void removeFavouritesForGivenUser(String userId) {
        Query query = new Query(Criteria
                .where(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(userId))
                .and(CONNECTION_STATUS).ne(ConnectionStatus.DELETED.getValue()));

        Update update = new Update();
        update.set(IS_FAVOURITE, Boolean.FALSE);
        update.unset(SEQUENCE_NUMBER);

        mongoOperations.updateMulti(query, update, UserConnection.class, COLLECTION_NAME);
    }

    @Override
    public UserConnection getMaxSequenceNumberConnectionForGivenUser(String userId, List<String> connectionId) {
        Query query = new Query(Criteria
                .where(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(userId))
                .and(IS_FAVOURITE).is(Boolean.TRUE)
                .and(UNIQUE_ID).nin(PeopleUtils.convertStringToObjectId(connectionId))
                .and(CONNECTION_STATUS).ne(ConnectionStatus.DELETED.getValue()))
                .with(new Sort(
                        Sort.Direction.DESC, SEQUENCE_NUMBER)).limit(1);

        return mongoOperations.findOne(query, UserConnection.class, COLLECTION_NAME);

    }

    @Override
    public void updateFavouritesForGivenUser(String userId, ManageFavouritesRequestDTO favouritesList) {
        BulkOperations bulkOps = mongoOperations.bulkOps(BulkOperations.BulkMode.UNORDERED, UserConnection.class);
        for (FavouriteConnectionSequenceDTO requestDTO : favouritesList.getFavouriteConnectionList()) {
            Query query = new Query(Criteria
                    .where(UNIQUE_ID).is(PeopleUtils.convertStringToObjectId(requestDTO.getConnectionId()))
                    .and(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(userId))
                    .and(CONNECTION_STATUS).ne(ConnectionStatus.DELETED.getValue()));

            Update update = new Update();
            update.set(IS_FAVOURITE, Boolean.TRUE);
            update.set(SEQUENCE_NUMBER, requestDTO.getSequenceNumber());

            bulkOps.updateOne(query, update);
        }
        bulkOps.execute();
    }

    @Override
    public Page<UserConnection> getFavouritesForGivenUser(String userId, Pageable pageable) {

        LookupOperation lookupPrivacyProfile = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.USER_PRIVACY_PROFILE.getCollectionName())
                .localField(CONNECTION_BASE_PROFILE)
                .foreignField(UNIQUE_ID)
                .as(PRIVACY_PROFILE_DATA);

        LookupOperation lookupUserData = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.PEOPLE_USER.getCollectionName())
                .localField(CONNECTION_TO_ID)
                .foreignField(UNIQUE_ID)
                .as(USER_DATA);

        SortOperation sortingOnFNameAndLName = sort(pageable.getSort());

        ProjectionOperation projectUserConnectionWithName = project(UNIQUE_ID, CONNECTION_FROM_ID, DEVICE_CONTACT_ID,
                CONNECTION_TO_ID, CONNECTION_STATUS, IS_FAVOURITE, SEQUENCE_NUMBER, SHARED_PRIVACY_PROFILE_ID,
                REAL_TIME_SHARED_DATA, STATIC_SHARED_DATA, CONTACT_STATIC_DATA, GROUP_ID_LIST, IS_BLOCKED, LAST_UPDATED_ON,
                PRIVACY_PROFILE_DATA, USER_DATA)
                .and(ConditionalOperators.ifNull(PEOPLE_USER_FNAME)
                        .thenValueOf(CONNECTION_FNAME))
                .as(FIRST_NAME)
                .and(ConditionalOperators.ifNull(PEOPLE_USER_LNAME)
                        .thenValueOf(CONNECTION_LNAME))
                .as(LAST_NAME);

        AggregationOptions collation = AggregationOptions.builder().collation(Collation.of("en")
                .strength(Collation.ComparisonLevel.primary())).build();

        /**
         * 1. All connections which are marked as favourite is fetched
         * 2. For each connection peopleUser details and privacy profiles are fetched
         * 3. Projection is used to perform sorting on firstName and lastName
         *      preference is given for realTime data, if not available then contactStaticData values will be considered
         */
        Aggregation aggregation = Aggregation.newAggregation(
                match(Criteria
                        .where(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(userId))
                        .and(CONNECTION_STATUS).ne(ConnectionStatus.DELETED.getValue())
                        .and(IS_FAVOURITE).is(Boolean.TRUE)),
                lookupPrivacyProfile,
                lookupUserData,
                unwind(USER_DATA, true),
                projectUserConnectionWithName,
                sortingOnFNameAndLName,
                skip((long) (pageable.getPageNumber()) * (pageable.getPageSize())),
                limit(pageable.getPageSize())
        ).withOptions(collation);

        Query queryToGetTotalFavouriteCount = new Query(Criteria
                .where(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(userId))
                .and(CONNECTION_STATUS).ne(ConnectionStatus.DELETED.getValue())
                .and(IS_FAVOURITE).is(Boolean.TRUE));

        AggregationResults<UserConnection> results = mongoOperations.aggregate(aggregation, COLLECTION_NAME,
                UserConnection.class);

        return new PageImpl<>(results.getMappedResults(), pageable, mongoOperations.count(queryToGetTotalFavouriteCount,
                UserConnection.class, COLLECTION_NAME));
    }

    @Override
    public List<UserConnection> findConnectionByUserIdAndPrivacyProfileId(String userId,
                                                                          List<String> privacyProfileIds) {
        Query query = new Query(Criteria
                .where(CONNECTION_TO_ID).is(PeopleUtils.convertStringToObjectId(userId))
                .and(CONNECTION_BASE_PROFILE).in(PeopleUtils.convertStringToObjectId(privacyProfileIds))
                .and(CONNECTION_STATUS).is(ConnectionStatus.CONNECTED.getValue()));

        return mongoOperations.find(query, UserConnection.class, COLLECTION_NAME);
    }

    @Override
    public void deleteAllConnectionForAUser(String userId) {
        Query findAllQuery = new Query(Criteria
                .where(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(userId)));

        mongoOperations.remove(findAllQuery, UserConnection.class, PeopleCollectionKeys.Collection.USER_CONNECTION.getCollectionName());
    }

    @Override
    public void deleteConnectionsByUserIdAndConnectionIds(String userId, List<String> connectionsToBeDeleted) {
        Query findAllQuery = new Query(Criteria
                .where(UNIQUE_ID).in(PeopleUtils.convertStringToObjectId(connectionsToBeDeleted))
                .and(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(userId))
        );

        mongoOperations.remove(findAllQuery, UserConnection.class, PeopleCollectionKeys.Collection.USER_CONNECTION.getCollectionName());

    }

    @Override
    public List<UserConnection> findByFromIdAndPhoneNumberAndStatus(String fromId, String phoneNumber,
                                                                    List<String> connectionStatus) {
        Query query = new Query();
        query.addCriteria(Criteria
                .where(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(fromId))
                .and(CONNECTION_STATUS).in(connectionStatus)
                .and("contactStaticData.userMetadataList").elemMatch(
                        Criteria.where("category").is("PHONENUMBER")
                                .and("keyValueDataList").elemMatch(
                                Criteria.where("key").is("phoneNumber")
                                        .and("val").is(phoneNumber))));

        return mongoOperations.find(query, UserConnection.class, COLLECTION_NAME);

    }

    @Async
    @Override
    public void updateConnectionDataForDeletedAccount(List<String> connectionIds, DateTime lastUpdatedTime) {
        Query query = new Query(
                Criteria
                        .where(UNIQUE_ID).in(PeopleUtils.convertStringToObjectId(connectionIds)));

        Update update = new Update();
        update.set(CONNECTION_STATUS, ConnectionStatus.NOT_CONNECTED);
        update.set(LAST_UPDATED_ON, PeopleUtils.getCurrentTimeInUTC());
        update.unset(CONNECTION_TO_ID);
        update.unset(REAL_TIME_SHARED_DATA);
        update.unset(PRIVACY_PROFILE_DATA);
        update.unset(USER_DATA);

        mongoOperations.updateMulti(query, update, UserConnection.class, COLLECTION_NAME);
    }

	@Override
	public List<UserConnection> findCompanyOrTag(String groupOwnerId, String toColumn) {
		 Query query = new Query(Criteria
		          .where(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(groupOwnerId)));
		 query.fields().include("contactStaticData."+toColumn);
		 query.with(Sort.by(Sort.Direction.ASC, "contactStaticData."+toColumn));
		 query.collation(Collation.of("en").strength(Collation.ComparisonLevel.secondary()));
		 return mongoOperations.find(query, UserConnection.class, COLLECTION_NAME);
	}

	@Override
	public List<UserConnection> findAllContactByCompanyNameOrTagName(String groupOwnerId, String toColumn, String toValue) {
		 Query query = new Query(Criteria
		          .where(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(groupOwnerId)));
		 query.addCriteria(Criteria.where("contactStaticData."+toColumn).is(toValue));
		 query.fields().include("_id");
		 query.with(Sort.by(Sort.Direction.ASC, "contactStaticData.firstName"));
		 query.collation(Collation.of("en").strength(Collation.ComparisonLevel.secondary()));
		 return mongoOperations.find(query, UserConnection.class, COLLECTION_NAME);
	}

	@Override
	public List<UserConnection> findAllContactByConnected(String groupOwnerId) {
		Query query = new Query(Criteria
		         .where(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(groupOwnerId)));
		query.addCriteria(Criteria.where("connectionStatus").is("CONNECTED"));
		query.fields().include("_id");
		query.with(Sort.by(Sort.Direction.ASC, "contactStaticData.firstName"));
		return mongoOperations.find(query, UserConnection.class, COLLECTION_NAME);
	}

	@Override
	public List<UserConnection> findSocialMediaOrCityandState(String groupOwnerId) {
		Query query = new Query(Criteria
		         .where(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(groupOwnerId)));
		query.fields().include("contactStaticData.userMetadataList");
		query.with(Sort.by(Sort.Direction.ASC, "contactStaticData.userMetadataList.keyValueDataList.key"));
		query.collation(Collation.of("en").strength(Collation.ComparisonLevel.secondary()));
		return mongoOperations.find(query, UserConnection.class, COLLECTION_NAME);
	}

	@Override
	public List<UserConnection> findAllContactBySocialMediaName(String groupOwnerId, String toValue) {
		Query query = new Query(Criteria
		         .where(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(groupOwnerId)));
		query.addCriteria(Criteria.where("contactStaticData.userMetadataList.label").is(toValue));
		query.fields().include("_id");
		query.with(Sort.by(Sort.Direction.ASC, "contactStaticData.firstName"));
		return mongoOperations.find(query, UserConnection.class, COLLECTION_NAME);
	}

	@Override
	public List<UserConnection> findAllContactByCityAndState(String groupOwnerId) {
		Query query = new Query(Criteria
		     .where(CONNECTION_FROM_ID).is(PeopleUtils.convertStringToObjectId(groupOwnerId)));
		query.fields().include("_id");
		query.fields().include("contactStaticData.userMetadataList");
		query.with(Sort.by(Sort.Direction.ASC, "contactStaticData.firstName"));
		return mongoOperations.find(query, UserConnection.class, COLLECTION_NAME);
	}
	
	@Override
	public List<UserConnection> findAllConnectedContactByPeopleUserToId(String PeopleUserToId) {
		Query query = new Query(Criteria
                .where(CONNECTION_TO_ID).is(PeopleUtils.convertStringToObjectId(PeopleUserToId))
                .and(CONNECTION_STATUS).ne(ConnectionStatus.CONNECTED.getValue()));

        return mongoOperations.find(query, UserConnection.class, COLLECTION_NAME);
	}
	
	

}
