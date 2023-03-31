package com.peopleapp.repository;

import com.peopleapp.constant.PeopleCollectionKeys;
import com.peopleapp.dto.Count;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;
import static org.springframework.data.mongodb.core.aggregation.AggregationSpELExpression.expressionOf;
import static org.springframework.data.mongodb.core.aggregation.ConditionalOperators.Cond.newBuilder;

@Repository
public class CustomBaseRepositoryImpl implements CustomBaseRepository {

    private static final String TOTAL_COUNT = "totalCount";
    private static final String CURRENT_DATA = "currentData";
    private static final String CONNECTION_FROM_ID = "peopleUserFromId";
    private static final String INITIATED_BY_ID = "peopleUserById";


    @Inject
    private MongoTemplate mongoTemplate;

    private AggregationSpELExpression currentYearExpression = expressionOf("year(createdOn)");
    private AggregationSpELExpression currentMonthExpression = expressionOf("month(createdOn)");
    private AggregationSpELExpression currentWeekExpression = expressionOf("week(createdOn)");
    private Calendar calendar = Calendar.getInstance();

    @Override
    public List<Count> getTotalCount(String userId, String collectionName) {

        Aggregation aggregation = newAggregation(
                filterUserOperation(userId, collectionName),
                group().count().as(TOTAL_COUNT)
        );
        AggregationResults<Count> results = mongoTemplate.aggregate(aggregation, collectionName, Count.class);
        return results.getMappedResults();
    }

    @Override
    public List<Count> getCurrYearCount(String userId, String groupById, String collectionName) {

        Aggregation aggregation = newAggregation(
                filterUserOperation(userId, collectionName),
                projectCurrentYear(groupById),
                group(groupById).sum(CURRENT_DATA).as(TOTAL_COUNT));
        AggregationResults<Count> results = mongoTemplate.aggregate(aggregation, collectionName, Count.class);
        return results.getMappedResults();
    }

    @Override
    public List<Count> getCurrYearCount(String userId, String collectionName) {

        Aggregation aggregation = newAggregation(
                filterUserOperation(userId, collectionName),
                projectCurrentYear(),
                group().sum(CURRENT_DATA).as(TOTAL_COUNT));
        AggregationResults<Count> results = mongoTemplate.aggregate(aggregation, collectionName, Count.class);
        return results.getMappedResults();
    }

    @Override
    public List<Count> getCurrMonthCount(String userId, String groupById, String collectionName) {

        Aggregation aggregation = newAggregation(
                filterUserOperation(userId, collectionName),
                projectCurrentMonth(groupById),
                group(groupById).sum(CURRENT_DATA).as(TOTAL_COUNT));
        AggregationResults<Count> results = mongoTemplate.aggregate(aggregation, collectionName, Count.class);
        return results.getMappedResults();
    }

    @Override
    public List<Count> getCurrMonthCount(String userId, String collectionName) {

        Aggregation aggregation = newAggregation(
                filterUserOperation(userId, collectionName),
                projectCurrentMonth(),
                group().sum(CURRENT_DATA).as(TOTAL_COUNT));
        AggregationResults<Count> results = mongoTemplate.aggregate(aggregation, collectionName, Count.class);
        return results.getMappedResults();
    }

    @Override
    public List<Count> getCurrWeekCount(String userId, String groupById, String collectionName) {

        Aggregation aggregation = newAggregation(
                filterUserOperation(userId, collectionName),
                projectCurrentWeek(groupById),
                group(groupById).sum(CURRENT_DATA).as(TOTAL_COUNT));
        AggregationResults<Count> results = mongoTemplate.aggregate(aggregation, collectionName, Count.class);
        return results.getMappedResults();
    }

    @Override
    public List<Count> getCurrWeekCount(String userId, String collectionName) {

        Aggregation aggregation = newAggregation(
                filterUserOperation(userId, collectionName),
                projectCurrentWeek(),
                group().sum(CURRENT_DATA).as(TOTAL_COUNT));
        AggregationResults<Count> results = mongoTemplate.aggregate(aggregation, collectionName, Count.class);
        return results.getMappedResults();
    }

    @Override
    public List<Count> getTodayCount(String userId, String groupById, String collectionName) {

        Aggregation aggregation = newAggregation(
                filterUserOperation(userId, collectionName),
                filterCurrentDateOperation(),
                group(groupById).count().as(TOTAL_COUNT));
        AggregationResults<Count> results = mongoTemplate.aggregate(aggregation, collectionName, Count.class);
        return results.getMappedResults();
    }

    @Override
    public List<Count> getTodayCount(String userId, String collectionName) {

        Aggregation aggregation = newAggregation(
                filterUserOperation(userId, collectionName),
                filterCurrentDateOperation(),
                group().count().as(TOTAL_COUNT));
        AggregationResults<Count> results = mongoTemplate.aggregate(aggregation, collectionName, Count.class);
        return results.getMappedResults();
    }

    private MatchOperation filterUserOperation(String userId, String collectionName) {
        MatchOperation matchOperation = null;
        if (PeopleCollectionKeys.Collection.USER_CONNECTION.getCollectionName().equals(collectionName)) {
            matchOperation = match(Criteria.where(CONNECTION_FROM_ID).is(new ObjectId(userId)));
        }
        if (PeopleCollectionKeys.Collection.USER_ACTIVITY.getCollectionName().equals(collectionName)) {
            matchOperation = match(Criteria.where(INITIATED_BY_ID).is(new ObjectId(userId)));
        }
        return matchOperation;
    }

    private MatchOperation filterCurrentDateOperation() {
        ZonedDateTime today = ZonedDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT, ZoneId.systemDefault());
        ZonedDateTime tomorrow = today.plusDays(1);
        return match(Criteria.where(PeopleCollectionKeys.CREATED_ON).gte(Date.from(today.toInstant())).lt(Date.from(tomorrow.toInstant())));
    }

    private ProjectionOperation projectCurrentYear(String groupById) {
        return project(groupById)
                .and(ConditionalOperators
                        .when(ComparisonOperators.valueOf(currentYearExpression).equalToValue(calendar.get(Calendar.YEAR)))
                        .then(1)
                        .otherwise(0)
                ).as(CURRENT_DATA);
    }

    private ProjectionOperation projectCurrentYear() {
        return project()
                .and(ConditionalOperators
                        .when(ComparisonOperators.valueOf(currentYearExpression).equalToValue(calendar.get(Calendar.YEAR)))
                        .then(1)
                        .otherwise(0)
                ).as(CURRENT_DATA);
    }

    private ProjectionOperation projectCurrentMonth(String groupById) {

        ConditionalOperators.Cond cond = newBuilder()
                .when(ComparisonOperators.valueOf(currentYearExpression).equalToValue(calendar.get(Calendar.YEAR)))
                .thenValueOf(newBuilder()
                        .when(ComparisonOperators.valueOf(currentMonthExpression).equalToValue(calendar.get(Calendar.MONTH) + 1))
                        .then(1)
                        .otherwise(0))
                .otherwise(0);

        return project(groupById).and(cond).as(CURRENT_DATA);

    }

    private ProjectionOperation projectCurrentMonth() {

        ConditionalOperators.Cond cond = newBuilder()
                .when(ComparisonOperators.valueOf(currentYearExpression).equalToValue(calendar.get(Calendar.YEAR)))
                .thenValueOf(newBuilder()
                        .when(ComparisonOperators.valueOf(currentMonthExpression).equalToValue(calendar.get(Calendar.MONTH) + 1))
                        .then(1)
                        .otherwise(0))
                .otherwise(0);

        return project().and(cond).as(CURRENT_DATA);

    }

    private ProjectionOperation projectCurrentWeek() {

        ConditionalOperators.Cond cond = newBuilder()
                .when(ComparisonOperators.valueOf(currentYearExpression).equalToValue(calendar.get(Calendar.YEAR)))
                .then(newBuilder()
                        .when(ComparisonOperators.valueOf(currentMonthExpression).equalToValue(calendar.get(Calendar.MONTH) + 1))
                        .thenValueOf(newBuilder()
                                .when(ComparisonOperators.valueOf(currentWeekExpression).equalToValue(calendar.get(Calendar.WEEK_OF_YEAR) - 1))
                                .then(1)
                                .otherwise(0))
                        .otherwise(0))
                .otherwise(0);

        return project().and(cond).as(CURRENT_DATA);

    }

    private ProjectionOperation projectCurrentWeek(String groupById) {

        ConditionalOperators.Cond cond = newBuilder()
                .when(ComparisonOperators.valueOf(currentYearExpression).equalToValue(calendar.get(Calendar.YEAR)))
                .then(newBuilder()
                        .when(ComparisonOperators.valueOf(currentMonthExpression).equalToValue(calendar.get(Calendar.MONTH) + 1))
                        .thenValueOf(newBuilder()
                                .when(ComparisonOperators.valueOf(currentWeekExpression).equalToValue(calendar.get(Calendar.WEEK_OF_YEAR) - 1))
                                .then(1)
                                .otherwise(0))
                        .otherwise(0))
                .otherwise(0);

        return project(groupById).and(cond).as(CURRENT_DATA);

    }


}
