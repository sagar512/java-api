package com.peopleapp.repository;

import com.peopleapp.constant.PeopleCollectionKeys;
import com.peopleapp.model.RecentActiveNetwork;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;
import static org.springframework.data.mongodb.core.aggregation.ConditionalOperators.when;

@Repository
public class CustomRecentActiveNetworkRepositoryImpl implements CustomRecentActiveNetworkRepository {

    @Inject
    private MongoTemplate mongoTemplate;

    private String collectionName = PeopleCollectionKeys.Collection.RECENT_ACTIVE_NETWORK.getCollectionName();
    private static final String NETWORK_ID = "networkId";
    private static final String NETWORK_CATEGORY = "networkCategory";
    private static final String IS_NEW_NETWORK = "newNetwork";
    private static final String IS_NEW_MEMBER = "newMember";

    private static final String TOTAL_NEW_MEMBER_WEIGHTAGE = "totalNewMemberWeightage";
    private static final String TOTAL_NEW_NETWORK_WEIGHTAGE = "totalNewNetworkWeightage";
    private static final String TOTAL_NETWORK_WEIGHTAGE = "totalNetworkWeightage";

    @Override
    public List<RecentActiveNetwork> getRecentNetworksByCategory(String userId, String networkCategory,
                                                                 int newMemberWeightage, int newNetworkWeightage) {

        ProjectionOperation projectionOperation = project(NETWORK_ID)
                .and(when(Criteria.where(IS_NEW_MEMBER).is(Boolean.TRUE)).then(newMemberWeightage).otherwise(0)).as(IS_NEW_MEMBER)
                .and(when(Criteria.where(IS_NEW_NETWORK).is(Boolean.TRUE)).then(newNetworkWeightage).otherwise(0)).as(IS_NEW_NETWORK);
        GroupOperation groupOperation = group(NETWORK_ID)
                .sum(IS_NEW_MEMBER).as(TOTAL_NEW_MEMBER_WEIGHTAGE)
                .sum(IS_NEW_NETWORK).as(TOTAL_NEW_NETWORK_WEIGHTAGE);

        ProjectionOperation projectionOperationForTotalWeightage = project(NETWORK_ID)
                .and(TOTAL_NEW_MEMBER_WEIGHTAGE).plus(TOTAL_NEW_NETWORK_WEIGHTAGE).as(TOTAL_NETWORK_WEIGHTAGE);
        MatchOperation filterStates  = match(Criteria.where(NETWORK_CATEGORY).is(networkCategory));

        SortOperation sortOperation = sort(Sort.Direction.DESC, TOTAL_NETWORK_WEIGHTAGE);
        Aggregation aggregation = Aggregation.newAggregation(filterStates, projectionOperation, groupOperation,
                projectionOperationForTotalWeightage, sortOperation);

        AggregationResults<RecentActiveNetwork> results = mongoTemplate.aggregate(aggregation, collectionName,
                RecentActiveNetwork.class);
        return results.getMappedResults();
    }

    @Override
    public List<RecentActiveNetwork> getTopRecentNetworks(String userId, int newMemberWeightage,
                                                          int newNetworkWeightage) {

        ProjectionOperation projectionOperation = project(NETWORK_ID)
                .and(when(Criteria.where(IS_NEW_MEMBER).is(Boolean.TRUE)).then(newMemberWeightage).otherwise(0)).as(IS_NEW_MEMBER)
                .and(when(Criteria.where(IS_NEW_NETWORK).is(Boolean.TRUE)).then(newNetworkWeightage).otherwise(0)).as(IS_NEW_NETWORK);
        GroupOperation groupOperation = group(NETWORK_ID)
                .sum(IS_NEW_MEMBER).as(TOTAL_NEW_MEMBER_WEIGHTAGE)
                .sum(IS_NEW_NETWORK).as(TOTAL_NEW_NETWORK_WEIGHTAGE);

        ProjectionOperation projectionOperationForTotalWeightage = project(NETWORK_ID)
                .and(TOTAL_NEW_MEMBER_WEIGHTAGE).plus(TOTAL_NEW_NETWORK_WEIGHTAGE).as(TOTAL_NETWORK_WEIGHTAGE);

        SortOperation sortOperation = sort(Sort.Direction.DESC, TOTAL_NETWORK_WEIGHTAGE);
        Aggregation aggregation = Aggregation.newAggregation(projectionOperation, groupOperation,
                projectionOperationForTotalWeightage, sortOperation);

        AggregationResults<RecentActiveNetwork> results = mongoTemplate.aggregate(aggregation, collectionName,
                RecentActiveNetwork.class);
        return results.getMappedResults();
    }
}


