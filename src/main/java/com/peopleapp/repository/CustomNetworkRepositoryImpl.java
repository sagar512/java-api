package com.peopleapp.repository;

import com.mongodb.client.result.UpdateResult;
import com.peopleapp.constant.PeopleCollectionKeys;
import com.peopleapp.dto.UserNetworkDetails;
import com.peopleapp.enums.*;
import com.peopleapp.model.Network;
import com.peopleapp.model.NetworkMember;
import com.peopleapp.util.PeopleUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
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
public class CustomNetworkRepositoryImpl implements CustomNetworkRepository {

    private static final String NETWORK_ID = "networkId";
    private static final String NETWORK_DETAILS = "networkDetails";
    private static final String NETWORK_MEMBER_DETAILS = "networkMemberDetails";
    private static final String ACTIVITY_DETAILS = "activityDetails";
    private static final String NETWORK_MEMBER_FIRST_NAME = "networkMemberDetails.firstName.value";
    private static final String NETWORK_MEMBER_LAST_NAME = "networkMemberDetails.lastName.value";
    private static final String NETWORK_MEMBER_FULL_NAME = "networkMemberDetails.fullName";
    private static final String NETWORK_MEMBER_COMPANY_NAME = "networkMemberDetails.company.value";

    private static final String NETWORK_NAME = "name";
    private static final String NETWORK_STATUS = "networkStatus";
    private static final String NETWORK_PRIVACY_TYPE = "privacyType";
    private static final String PEOPLE_USER_BY_ID = "peopleUserById";


    @Inject
    private MongoTemplate mongoTemplate;

    @Inject
    private MongoOperations mongoOperations;

    private String collectionName = PeopleCollectionKeys.Collection.NETWORK_MEMBER.getCollectionName();
    private String networkCollectionName = PeopleCollectionKeys.Collection.NETWORK.getCollectionName();
    private static final String UNIQUE_ID = "_id";
    private static final String MEMBER_ID = "memberId";
    private static final String MEMBER_STATUS = "memberStatus";
    private static final String MEMBER_ROLE = "memberRole";
    private static final String NETWORK_DETAILS_NAME = "networkDetails.name";
    private static final String NETWORK_CATEGORY = "networkCategory";
    private static final String MEMBER_COUNT = "memberCount";
    private static final String NETWORK_LOCATION = "networkLocation";
    private static final String ACTIVITY_REQUEST_TYPE = "activityType.requestType";
    private static final String ACTIVITY_ACTION_TYPE = "activityType.actionTaken";
    private static final String NETWORK_MEMBER_DETAILS_MEMBER_ID = "networkMemberDetails.peopleUserId";


    @Override
    public Page<UserNetworkDetails> getUserNetworks(String userId, Pageable pageable) {

        Query query = new Query(Criteria
                .where(PeopleCollectionKeys.USER_REFERENCE_ID).is(PeopleUtils.convertStringToObjectId(userId))
                .and(MEMBER_STATUS).is(NetworkMemberStatus.ACTIVE.getValue()));

        LookupOperation lookupOperation = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.NETWORK.getCollectionName())
                .localField(NETWORK_ID)
                .foreignField(UNIQUE_ID)
                .as(NETWORK_DETAILS);

        Aggregation aggregation = Aggregation.newAggregation(match(Criteria
                        .where(PeopleCollectionKeys.USER_REFERENCE_ID).is(PeopleUtils.convertStringToObjectId(userId))
                        .and(MEMBER_STATUS).is(NetworkMemberStatus.ACTIVE.getValue())),
                lookupOperation,
                unwind(NETWORK_DETAILS),
                skip((long) (pageable.getPageNumber()) * (pageable.getPageSize())),
                limit(pageable.getPageSize())
        );

        AggregationResults<UserNetworkDetails> results = mongoTemplate.aggregate(aggregation, collectionName, UserNetworkDetails.class);

        return new PageImpl<>(results.getMappedResults(), pageable, mongoOperations.count(query, collectionName));

    }

    @Override
    public List<UserNetworkDetails> getUserNetworks(String userId) {

        LookupOperation lookupOperation = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.NETWORK.getCollectionName())
                .localField(NETWORK_ID)
                .foreignField(UNIQUE_ID)
                .as(NETWORK_DETAILS);

        Aggregation aggregation = Aggregation.newAggregation(match(Criteria
                        .where(PeopleCollectionKeys.USER_REFERENCE_ID).is(PeopleUtils.convertStringToObjectId(userId))
                        .and(MEMBER_STATUS).is(NetworkMemberStatus.ACTIVE.getValue())),
                lookupOperation,
                unwind(NETWORK_DETAILS)
        );

        AggregationResults<UserNetworkDetails> results = mongoTemplate.aggregate(aggregation, collectionName, UserNetworkDetails.class);

        return results.getMappedResults();
    }

    @Override
    public List<NetworkMember> getNetworkMemberDetailsForUser(String userId) {
        Query query = new Query(Criteria
                .where(MEMBER_ID).is(PeopleUtils.convertStringToObjectId(userId))
                .and(MEMBER_STATUS).is(NetworkMemberStatus.ACTIVE.getValue())
        );

        return mongoOperations.find(query, NetworkMember.class, collectionName);
    }

    @Override
    public void removeUserFromNetwork(String userId) {
        Query query = new Query(Criteria
                .where(MEMBER_ID).is(PeopleUtils.convertStringToObjectId(userId))
        );

        mongoOperations.remove(query, NetworkMember.class, collectionName);
    }

    @Override
    public List<Network> getAllNetworksById(List<String> networkIdList) {
        Query queryToFetchNetworks = new Query(Criteria
                .where(UNIQUE_ID).in(PeopleUtils.convertStringToObjectId(networkIdList))
                .and(NETWORK_STATUS).is(NetworkStatus.ACTIVE.getValue())
        );

        return mongoOperations.find(queryToFetchNetworks, Network.class, networkCollectionName);
    }

    /**
     * Find Network members for a network based on specified member roles and also searches for particular member based
     * on input searchString if present else it will fetch all members
     *
     * @param networkId
     * @param roles
     * @param pageable
     * @return
     */
    @Override
    public Page<NetworkMember> getNetworkMemberDetailsByIdAndRole(String networkId, String searchString,
                                                                  List<String> roles, Pageable pageable) {
        long totalElementCount;

        LookupOperation lookupOperation = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.PEOPLE_USER.getCollectionName())
                .localField(PeopleCollectionKeys.USER_REFERENCE_ID)
                .foreignField(UNIQUE_ID)
                .as(NETWORK_MEMBER_DETAILS);

        MatchOperation matchOperation = Aggregation.match(Criteria
                .where(NETWORK_ID).is(PeopleUtils.convertStringToObjectId(networkId))
                .and(MEMBER_ROLE).in(roles)
                .and(MEMBER_STATUS).is(NetworkMemberStatus.ACTIVE.getValue()));

        Aggregation aggregation;

        if (searchString.isEmpty()) {

            Query query = new Query(Criteria
                    .where(NETWORK_ID).is(PeopleUtils.convertStringToObjectId(networkId))
                    .and(MEMBER_ROLE).in(roles)
                    .and(MEMBER_STATUS).is(NetworkMemberStatus.ACTIVE.getValue()));

            aggregation = Aggregation.newAggregation(matchOperation, lookupOperation,
                    sort(pageable.getSort()),
                    skip((long) (pageable.getPageNumber()) * (pageable.getPageSize())),
                    limit(pageable.getPageSize())
            );
            totalElementCount = mongoOperations.count(query, collectionName);
        } else {
            Criteria criteria = new Criteria().orOperator(
                    Criteria
                            .where(NETWORK_MEMBER_FIRST_NAME).regex(("^").concat(searchString), "i"),
                    Criteria
                            .where(NETWORK_MEMBER_LAST_NAME).regex(("^").concat(searchString), "i"),
                    Criteria
                            .where(NETWORK_MEMBER_FULL_NAME).regex(("^").concat(searchString), "i"),
                    Criteria
                            .where(NETWORK_MEMBER_COMPANY_NAME).regex(("^").concat(searchString), "i")
            );

            aggregation = Aggregation.newAggregation(matchOperation, lookupOperation,
                    match(criteria),
                    sort(pageable.getSort()),
                    skip((long) (pageable.getPageNumber()) * (pageable.getPageSize())),
                    limit(pageable.getPageSize())
            );
            Aggregation aggregationCount = Aggregation.newAggregation(matchOperation, lookupOperation, match(criteria));
            totalElementCount = mongoTemplate.aggregate(aggregationCount, collectionName, NetworkMember.class)
                    .getMappedResults().size();
        }

        aggregation = aggregation.withOptions(AggregationOptions.builder().collation(Collation.of("en")
                .strength(Collation.ComparisonLevel.primary())).build());

        AggregationResults<NetworkMember> results = mongoTemplate.aggregate(aggregation, collectionName, NetworkMember.class);

        return new PageImpl<>(results.getMappedResults(), pageable, totalElementCount);
    }

    /*
    find member based on network id, member id and status
     */
    @Override
    public NetworkMember findByIdAndUserIdAndStatus(String networkId, String userId, String status) {

        Query query = new Query(
                Criteria.where(NETWORK_ID).is(PeopleUtils.convertStringToObjectId(networkId))
                        .and(MEMBER_ID).is(PeopleUtils.convertStringToObjectId(userId))
                        .and(MEMBER_STATUS).is(status)
        );

        return mongoTemplate.findOne(query, NetworkMember.class, collectionName);
    }

    /**
     * find NetworkMember based on MemberRole for the network
     */
    @Override
    public List<NetworkMember> findByIdAndRole(String networkId, List<String> memberRoles) {

        Query query = new Query(
                Criteria.where(NETWORK_ID).is(PeopleUtils.convertStringToObjectId(networkId))
                        .and(MEMBER_ROLE).in(memberRoles)
                        .and(MEMBER_STATUS).is(NetworkMemberStatus.ACTIVE.getValue())
        );

        return mongoTemplate.find(query, NetworkMember.class, collectionName);
    }


    @Override
    public UpdateResult updateMemberRolesForANetwork(String networkId, List<String> memberIdList, String currentRole,
                                                     String toBeUpdatedRole) {

        Query query = new Query(Criteria
                .where(NETWORK_ID).is(PeopleUtils.convertStringToObjectId(networkId))
                .and(PeopleCollectionKeys.USER_REFERENCE_ID).in(PeopleUtils.convertStringToObjectId(memberIdList))
                .and(MEMBER_ROLE).is(currentRole)
                .and(MEMBER_STATUS).is(NetworkMemberStatus.ACTIVE.getValue())
        );
        Update update = Update.update(MEMBER_ROLE, toBeUpdatedRole);

        return mongoOperations.updateMulti(query, update, NetworkMember.class, collectionName);
    }

    @Override
    public List<NetworkMember> findAllActiveNonAdminMembersByNetworkIdAndMemberId(String networkId, List<String> memberIdList) {
        Query query = new Query(Criteria
                .where(NETWORK_ID).is(PeopleUtils.convertStringToObjectId(networkId))
                .and(PeopleCollectionKeys.USER_REFERENCE_ID).in(PeopleUtils.convertStringToObjectId(memberIdList))
                .and(MEMBER_ROLE).is(NetworkMemberRole.MEMBER.getValue())
                .and(MEMBER_STATUS).is(NetworkMemberStatus.ACTIVE.getValue())
        );
        return mongoTemplate.find(query, NetworkMember.class, collectionName);
    }

    @Override
    public UserNetworkDetails getUserOwnedNetworkByName(String userId, String networkName) {
        LookupOperation lookupOperation = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.NETWORK.getCollectionName())
                .localField(NETWORK_ID)
                .foreignField(UNIQUE_ID)
                .as(NETWORK_DETAILS);

        Aggregation aggregation = Aggregation.newAggregation(match(Criteria
                        .where(PeopleCollectionKeys.USER_REFERENCE_ID).is(PeopleUtils.convertStringToObjectId(userId))
                        .and(MEMBER_ROLE).is(NetworkMemberRole.OWNER.getValue())
                        .and(MEMBER_STATUS).is(NetworkMemberStatus.ACTIVE.getValue())),
                lookupOperation, unwind(NETWORK_DETAILS), match(Criteria.where(NETWORK_DETAILS_NAME).is(networkName))
        ).withOptions(AggregationOptions.builder().collation(Collation.of("en")
                .strength(Collation.ComparisonLevel.primary())).build());

        AggregationResults<UserNetworkDetails> results = mongoTemplate.aggregate(aggregation, collectionName, UserNetworkDetails.class);
        return results.getUniqueMappedResult();

    }

    @Override
    public List<Network> getMostPopularNetworksForUserByCategory(String userId, String networkCategory, int limit) {

        MatchOperation networkCategoryMatchOperation = Aggregation.match(Criteria
                .where(NETWORK_CATEGORY).is(networkCategory)
                .and(NETWORK_PRIVACY_TYPE).ne(NetworkPrivacyType.PRIVATE.getValue())
                .and(NETWORK_STATUS).is(NetworkStatus.ACTIVE.getValue()));

        LookupOperation lookupForNetworkAndNetworkMember = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.NETWORK_MEMBER.getCollectionName())
                .localField(UNIQUE_ID)
                .foreignField(NETWORK_ID)
                .as(NETWORK_MEMBER_DETAILS);

        LookupOperation lookupForNetworkActivity = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.USER_ACTIVITY.getCollectionName())
                .localField(UNIQUE_ID)
                .foreignField(NETWORK_ID)
                .as(ACTIVITY_DETAILS);

        MatchOperation networkMemberMatchOperation = Aggregation.match(
                new Criteria().orOperator(Criteria.where(NETWORK_MEMBER_DETAILS).exists(false),
                        Criteria.where(NETWORK_MEMBER_DETAILS).not()
                                .elemMatch(Criteria.where(PeopleCollectionKeys.USER_REFERENCE_ID)
                                        .is(PeopleUtils.convertStringToObjectId(userId))))
        );

        MatchOperation activitiesMatchOperation = Aggregation.match(
                new Criteria().orOperator(Criteria.where(ACTIVITY_DETAILS).exists(false),
                        Criteria.where(ACTIVITY_DETAILS).not().elemMatch(Criteria.where(PEOPLE_USER_BY_ID)
                                .is(PeopleUtils.convertStringToObjectId(userId))
                                .and(ACTIVITY_REQUEST_TYPE).is(RequestType.NETWORK_JOIN_REQUEST.getValue())
                                .and(ACTIVITY_ACTION_TYPE).is(Action.INITIATED.getValue())))
        );

        SortOperation sortOnMemberCount = sort(new Sort(Sort.Direction.DESC, MEMBER_COUNT));

        Aggregation aggregation = Aggregation.newAggregation(networkCategoryMatchOperation,
                lookupForNetworkAndNetworkMember, lookupForNetworkActivity, networkMemberMatchOperation,
                activitiesMatchOperation, sortOnMemberCount, limit(limit)

        );

        AggregationResults<Network> results = mongoTemplate.aggregate(aggregation, networkCollectionName, Network.class);
        return results.getMappedResults();
    }

    @Override
    public List<Network> getTopMostPopularNetworksForUser(String userId, int limit) {
        MatchOperation networkCategoryMatchOperation = Aggregation.match(Criteria
                .where(NETWORK_PRIVACY_TYPE).ne(NetworkPrivacyType.PRIVATE.getValue())
                .and(NETWORK_STATUS).is(NetworkStatus.ACTIVE.getValue()));

        LookupOperation lookupForNetworkAndNetworkMember = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.NETWORK_MEMBER.getCollectionName())
                .localField(UNIQUE_ID)
                .foreignField(NETWORK_ID)
                .as(NETWORK_MEMBER_DETAILS);

        LookupOperation lookupForNetworkActivity = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.USER_ACTIVITY.getCollectionName())
                .localField(UNIQUE_ID)
                .foreignField(NETWORK_ID)
                .as(ACTIVITY_DETAILS);

        MatchOperation networkMemberMatchOperation = Aggregation.match(
                new Criteria().orOperator(Criteria.where(NETWORK_MEMBER_DETAILS).exists(false),
                        Criteria.where(NETWORK_MEMBER_DETAILS).not()
                                .elemMatch(Criteria.where(PeopleCollectionKeys.USER_REFERENCE_ID)
                                        .is(PeopleUtils.convertStringToObjectId(userId))))
        );

        MatchOperation activitiesMatchOperation = Aggregation.match(
                new Criteria().orOperator(Criteria.where(ACTIVITY_DETAILS).exists(false),
                        Criteria.where(ACTIVITY_DETAILS).not().elemMatch(Criteria.where(PEOPLE_USER_BY_ID)
                                .is(PeopleUtils.convertStringToObjectId(userId))
                                .and(ACTIVITY_REQUEST_TYPE).is(RequestType.NETWORK_JOIN_REQUEST.getValue())
                                .and(ACTIVITY_ACTION_TYPE).is(Action.INITIATED.getValue())))
        );

        SortOperation sortOnMemberCount = sort(new Sort(Sort.Direction.DESC, MEMBER_COUNT));

        Aggregation aggregation = Aggregation.newAggregation(networkCategoryMatchOperation,
                lookupForNetworkAndNetworkMember, lookupForNetworkActivity, networkMemberMatchOperation,
                activitiesMatchOperation, sortOnMemberCount, limit(limit)

        );

        AggregationResults<Network> results = mongoTemplate.aggregate(aggregation, networkCollectionName, Network.class);
        return results.getMappedResults();
    }

    @Override
    public List<Network> getLocalNetworksForUserByCategory(String userId, String networkCategory, double latitude,
                                                           double longitude,
                                                           double distanceInMiles, int limit) {
        Point geoPoint = new Point(longitude, latitude);
        Distance geoDistance = new Distance(distanceInMiles, Metrics.MILES);
        Circle geoCircle = new Circle(geoPoint, geoDistance);

        MatchOperation networkCategoryMatchOperation = Aggregation.match(Criteria
                .where(NETWORK_CATEGORY).is(networkCategory)
                .and(NETWORK_LOCATION).withinSphere(geoCircle)
                .and(NETWORK_PRIVACY_TYPE).ne(NetworkPrivacyType.PRIVATE.getValue())
                .and(NETWORK_STATUS).is(NetworkStatus.ACTIVE.getValue()));

        LookupOperation lookupForNetworkAndNetworkMember = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.NETWORK_MEMBER.getCollectionName())
                .localField(UNIQUE_ID)
                .foreignField(NETWORK_ID)
                .as(NETWORK_MEMBER_DETAILS);

        LookupOperation lookupForNetworkActivity = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.USER_ACTIVITY.getCollectionName())
                .localField(UNIQUE_ID)
                .foreignField(NETWORK_ID)
                .as(ACTIVITY_DETAILS);

        MatchOperation networkMemberMatchOperation = Aggregation.match(
                new Criteria().orOperator(Criteria.where(NETWORK_MEMBER_DETAILS).exists(false),
                        Criteria.where(NETWORK_MEMBER_DETAILS).not()
                                .elemMatch(Criteria.where(PeopleCollectionKeys.USER_REFERENCE_ID)
                                        .is(PeopleUtils.convertStringToObjectId(userId))))
        );

        MatchOperation activitiesMatchOperation = Aggregation.match(
                new Criteria().orOperator(Criteria.where(ACTIVITY_DETAILS).exists(false),
                        Criteria.where(ACTIVITY_DETAILS).not().elemMatch(Criteria.where(PEOPLE_USER_BY_ID)
                                .is(PeopleUtils.convertStringToObjectId(userId))
                                .and(ACTIVITY_REQUEST_TYPE).is(RequestType.NETWORK_JOIN_REQUEST.getValue())
                                .and(ACTIVITY_ACTION_TYPE).is(Action.INITIATED.getValue())))
        );

        SortOperation sortOnMemberCount = sort(new Sort(Sort.Direction.DESC, MEMBER_COUNT));

        TypedAggregation aggregation = newAggregation(Network.class, networkCategoryMatchOperation,
                lookupForNetworkAndNetworkMember, lookupForNetworkActivity, networkMemberMatchOperation,
                activitiesMatchOperation, sortOnMemberCount, limit(limit));

        AggregationResults<Network> results = mongoTemplate.aggregate(aggregation, networkCollectionName, Network.class);
        return results.getMappedResults();


    }

    @Override
    public List<Network> getLocalNetworksForUser(String userId, double latitude, double longitude,
                                                 double distanceInMiles, int limit) {
        Point geoPoint = new Point(longitude, latitude);
        Distance geoDistance = new Distance(distanceInMiles, Metrics.MILES);
        Circle geoCircle = new Circle(geoPoint, geoDistance);

        MatchOperation networkCategoryMatchOperation = Aggregation.match(Criteria
                .where(NETWORK_LOCATION).withinSphere(geoCircle)
                .and(NETWORK_PRIVACY_TYPE).ne(NetworkPrivacyType.PRIVATE.getValue())
                .and(NETWORK_STATUS).is(NetworkStatus.ACTIVE.getValue()));

        LookupOperation lookupForNetworkAndNetworkMember = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.NETWORK_MEMBER.getCollectionName())
                .localField(UNIQUE_ID)
                .foreignField(NETWORK_ID)
                .as(NETWORK_MEMBER_DETAILS);

        LookupOperation lookupForNetworkActivity = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.USER_ACTIVITY.getCollectionName())
                .localField(UNIQUE_ID)
                .foreignField(NETWORK_ID)
                .as(ACTIVITY_DETAILS);

        MatchOperation networkMemberMatchOperation = Aggregation.match(
                new Criteria().orOperator(Criteria.where(NETWORK_MEMBER_DETAILS).exists(false),
                        Criteria.where(NETWORK_MEMBER_DETAILS).not()
                                .elemMatch(Criteria.where(PeopleCollectionKeys.USER_REFERENCE_ID)
                                        .is(PeopleUtils.convertStringToObjectId(userId))))
        );

        MatchOperation activitiesMatchOperation = Aggregation.match(
                new Criteria().orOperator(Criteria.where(ACTIVITY_DETAILS).exists(false),
                        Criteria.where(ACTIVITY_DETAILS).not().elemMatch(Criteria.where(PEOPLE_USER_BY_ID)
                                .is(PeopleUtils.convertStringToObjectId(userId))
                                .and(ACTIVITY_REQUEST_TYPE).is(RequestType.NETWORK_JOIN_REQUEST.getValue())
                                .and(ACTIVITY_ACTION_TYPE).is(Action.INITIATED.getValue())))
        );

        SortOperation sortOnMemberCount = sort(new Sort(Sort.Direction.DESC, MEMBER_COUNT));

        TypedAggregation aggregation = newAggregation(Network.class, networkCategoryMatchOperation,
                lookupForNetworkAndNetworkMember, lookupForNetworkActivity, networkMemberMatchOperation,
                activitiesMatchOperation, sortOnMemberCount, limit(limit));

        AggregationResults<Network> results = mongoTemplate.aggregate(aggregation, networkCollectionName, Network.class);
        return results.getMappedResults();
    }

    @Override
    public Page<Network> searchNetwork(String userId, String searchString, int sortOrder, Pageable pageable) {

        searchString = searchString.replace("$", "\\$");
        searchString = searchString.replace("^", "\\^");
        MatchOperation networkNameMatchOperation = Aggregation.match(Criteria
                .where(NETWORK_NAME).regex((".*").concat(searchString).concat(".*"), "i")
                .and(NETWORK_STATUS).is(NetworkStatus.ACTIVE.getValue())
                .and(NETWORK_PRIVACY_TYPE).ne(NetworkPrivacyType.PRIVATE.getValue()));

        LookupOperation lookupForNetworkAndNetworkMember = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.NETWORK_MEMBER.getCollectionName())
                .localField(UNIQUE_ID)
                .foreignField(NETWORK_ID)
                .as(NETWORK_MEMBER_DETAILS);

        MatchOperation filterUsersNetwork = Aggregation.match(Criteria
                .where(NETWORK_MEMBER_DETAILS_MEMBER_ID).ne(PeopleUtils.convertStringToObjectId(userId)));

        AggregationOptions collation = AggregationOptions.builder().collation(Collation.of("en")
                .strength(Collation.ComparisonLevel.primary())).build();

        SortOperation sortOnNetworkName;
        if (sortOrder == SortingOrder.DESCENDING_ORDER.getValue()) {
            sortOnNetworkName = new SortOperation(new Sort(Sort.Direction.DESC, NETWORK_NAME));
        } else {
            sortOnNetworkName = new SortOperation(new Sort(Sort.Direction.ASC, NETWORK_NAME));
        }

        Aggregation aggregation = Aggregation.newAggregation(
                networkNameMatchOperation,
                lookupForNetworkAndNetworkMember,
                filterUsersNetwork,
                sortOnNetworkName,
                skip((long) (pageable.getPageNumber()) * (pageable.getPageSize())),
                limit(pageable.getPageSize())
        ).withOptions(collation);

        Aggregation aggregationCount = Aggregation.newAggregation(
                networkNameMatchOperation,
                lookupForNetworkAndNetworkMember,
                filterUsersNetwork,
                sortOnNetworkName
        ).withOptions(collation);

        AggregationResults<Network> results = mongoTemplate.aggregate(aggregation, networkCollectionName,
                Network.class);

        return new PageImpl<>(results.getMappedResults(), pageable, mongoTemplate.aggregate(aggregationCount,
                networkCollectionName, Network.class).getMappedResults().size());
    }

    @Override
    public long findCountOfUserEnrolledNetwork(String userId, String memberRole) {
        Query query = new Query(Criteria
                .where(MEMBER_ID).is(PeopleUtils.convertStringToObjectId(userId))
                .and(MEMBER_ROLE).is(memberRole)
                .and(MEMBER_STATUS).is(NetworkMemberStatus.ACTIVE.getValue())
        );

        return mongoOperations.count(query, NetworkMember.class, collectionName);
    }

    @Override
    public void deleteNetworkMemberByMemberIdsAndNetworkId(List<String> memberIds, String networkId) {
        Query deleteMember = new Query(Criteria
                .where(NETWORK_ID).is(PeopleUtils.convertStringToObjectId(networkId))
                .and(MEMBER_ID).in(PeopleUtils.convertStringToObjectId(memberIds))
        );

        mongoOperations.remove(deleteMember, NetworkMember.class, collectionName);
    }

    @Override
    public void updateNetworkMembersStatus(String networkId, String status) {
        Query query = new Query(Criteria
                .where(NETWORK_ID).is(PeopleUtils.convertStringToObjectId(networkId))
                .and(MEMBER_STATUS).is(NetworkMemberStatus.ACTIVE.getValue()));

        Update update = Update.update(MEMBER_STATUS, status);

        mongoTemplate.updateMulti(query, update, collectionName);
    }

    @Override
    public List<Network> findActiveNetworksByIdsNewToUser(String userId, List<String> networkIds, int limit) {
        MatchOperation networkCategoryMatchOperation = Aggregation.match(Criteria
                .where(UNIQUE_ID).in(PeopleUtils.convertStringToObjectId(networkIds))
                .and(NETWORK_PRIVACY_TYPE).ne(NetworkPrivacyType.PRIVATE.getValue())
                .and(NETWORK_STATUS).is(NetworkStatus.ACTIVE.getValue()));

        LookupOperation lookupForNetworkAndNetworkMember = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.NETWORK_MEMBER.getCollectionName())
                .localField(UNIQUE_ID)
                .foreignField(NETWORK_ID)
                .as(NETWORK_MEMBER_DETAILS);

        LookupOperation lookupForNetworkActivity = LookupOperation.newLookup()
                .from(PeopleCollectionKeys.Collection.USER_ACTIVITY.getCollectionName())
                .localField(UNIQUE_ID)
                .foreignField(NETWORK_ID)
                .as(ACTIVITY_DETAILS);

        MatchOperation networkMemberMatchOperation = Aggregation.match(
                new Criteria().orOperator(Criteria.where(NETWORK_MEMBER_DETAILS).exists(false),
                        Criteria.where(NETWORK_MEMBER_DETAILS).not()
                                .elemMatch(Criteria.where(PeopleCollectionKeys.USER_REFERENCE_ID)
                                        .is(PeopleUtils.convertStringToObjectId(userId))))
        );

        MatchOperation activitiesMatchOperation = Aggregation.match(
                new Criteria().orOperator(Criteria.where(ACTIVITY_DETAILS).exists(false),
                        Criteria.where(ACTIVITY_DETAILS).not().elemMatch(Criteria.where(PEOPLE_USER_BY_ID)
                                .is(PeopleUtils.convertStringToObjectId(userId))
                                .and(ACTIVITY_REQUEST_TYPE).is(RequestType.NETWORK_JOIN_REQUEST.getValue())
                                .and(ACTIVITY_ACTION_TYPE).is(Action.INITIATED.getValue())))
        );

        Aggregation aggregation = Aggregation.newAggregation(networkCategoryMatchOperation,
                lookupForNetworkAndNetworkMember, lookupForNetworkActivity, networkMemberMatchOperation,
                activitiesMatchOperation, limit(limit)

        );

        AggregationResults<Network> results = mongoTemplate.aggregate(aggregation, networkCollectionName, Network.class);
        return results.getMappedResults();
    }
}
