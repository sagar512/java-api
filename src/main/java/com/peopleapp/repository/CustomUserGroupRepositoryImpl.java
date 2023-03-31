package com.peopleapp.repository;

import com.peopleapp.constant.PeopleCollectionKeys;
import com.peopleapp.model.UserGroup;
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
public class CustomUserGroupRepositoryImpl implements CustomUserGroupRepository {

    private static final String UNIQUE_ID = "_id";
    private static final String CONTACTID_LIST = "contactIdList";
    private static final String COLLECTION_NAME = PeopleCollectionKeys.Collection.USER_GROUP.getCollectionName();

    @Autowired
    private MongoOperations mongoOperations;

    @Override
    public List<UserGroup> findByUserGroupIdAndOwnerId(List<String> groupIds, String groupOwnerId) {
        Query query = new Query(Criteria
                .where(UNIQUE_ID).in(groupIds)
                .and(PeopleCollectionKeys.USER_REFERENCE_ID).is(PeopleUtils.convertStringToObjectId(groupOwnerId)));
        return mongoOperations.find(query, UserGroup.class, COLLECTION_NAME);
    }

    @Override
    public List<UserGroup> fetchFavouriteGroups(String groupOwnerId) {
        Query query = new Query(Criteria
                .where(PeopleCollectionKeys.USER_REFERENCE_ID).is(PeopleUtils.convertStringToObjectId(groupOwnerId))
                .and("isFavourite").is(Boolean.TRUE));
        return mongoOperations.find(query, UserGroup.class, COLLECTION_NAME);
    }

    @Override
    public List<UserGroup> fetchAllUserGroups(String groupOwnerId) {
        Query query = new Query(Criteria
                .where(PeopleCollectionKeys.USER_REFERENCE_ID).is(PeopleUtils.convertStringToObjectId(groupOwnerId)));
        return mongoOperations.find(query, UserGroup.class, COLLECTION_NAME);

    }

    @Override
    public Page<UserGroup> fetchUserGroups(String groupOwnerId, Pageable pageable) {
        Query query = new Query(Criteria
                .where(PeopleCollectionKeys.USER_REFERENCE_ID).is(PeopleUtils.convertStringToObjectId(groupOwnerId)));

        Query paginatedQuery = query.skip((long) (pageable.getPageNumber()) * (pageable.getPageSize()))
                .limit(pageable.getPageSize());

        List<UserGroup> userGroupList = mongoOperations.find(paginatedQuery, UserGroup.class, COLLECTION_NAME);

        return new PageImpl<> (userGroupList, pageable, mongoOperations.count(query, COLLECTION_NAME));
    }

    @Override
    public List<UserGroup> deleteUserGroups(String groupOwnerId, List<String> groupIdList) {

        Query query = new Query(Criteria
                .where(PeopleCollectionKeys.USER_REFERENCE_ID).is(PeopleUtils.convertStringToObjectId(groupOwnerId))
                .and(UNIQUE_ID).in(groupIdList));
        query.fields().include(UNIQUE_ID);
        query.fields().include(CONTACTID_LIST);
        return mongoOperations.findAllAndRemove(query, UserGroup.class, COLLECTION_NAME);
    }

    @Override
    public void removeContactIdFromUserGroups(String groupOwnerId, String connectionId) {
        Query query = new Query(Criteria
                .where(PeopleCollectionKeys.USER_REFERENCE_ID).is(PeopleUtils.convertStringToObjectId(groupOwnerId)));

        Update update = new Update();
        update.pull(CONTACTID_LIST, connectionId);

        mongoOperations.updateMulti(query, update, COLLECTION_NAME );
    }

    @Override
    public void deleteAllGroupCreatedByUser(String groupOwnerId) {
        Query findAllGroup = new Query(Criteria
                .where(PeopleCollectionKeys.USER_REFERENCE_ID).is(PeopleUtils.convertStringToObjectId(groupOwnerId))
        );

        mongoOperations.remove(findAllGroup, UserGroup.class, COLLECTION_NAME);
    }

}
