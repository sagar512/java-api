package com.peopleapp.repository;

import com.peopleapp.constant.PeopleCollectionKeys;
import com.peopleapp.model.UserPrivacyProfile;
import com.peopleapp.util.PeopleUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CustomPrivacyProfileRepositoryImpl implements CustomPrivacyProfileRepository {

    @Autowired
    private MongoOperations mongoOperations;

    private static final String UNIQUE_ID = "_id";
    private static final String IMAGE_URL = "imageURL";
    private static final String USE_DEFAULT_IMAGE = "useDefaultImage";
    private static final String LAST_UPDATED_ON= "lastUpdatedOn";
    private static final String VALUE_ID_LIST= "valueIdList";

    @Override
    public List<UserPrivacyProfile> deleteValidPrivacyProfiles(String userId, List<String> profileIdList) {

        Query query = new Query(Criteria
                .where(PeopleCollectionKeys.USER_REFERENCE_ID).is(PeopleUtils.convertStringToObjectId(userId))
                .and(UNIQUE_ID).in(profileIdList)
                .and("isDefault").ne(Boolean.TRUE)
                .and("isPublic").ne(Boolean.TRUE));
        query.fields().include(UNIQUE_ID).include(VALUE_ID_LIST);

        return mongoOperations.findAllAndRemove(query, UserPrivacyProfile.class, PeopleCollectionKeys.Collection.USER_PRIVACY_PROFILE.getCollectionName());

    }

    @Override
    public List<UserPrivacyProfile> findAllByUserId(String userId) {
        Query query = new Query(Criteria
                .where(PeopleCollectionKeys.USER_REFERENCE_ID).is(PeopleUtils.convertStringToObjectId(userId)));
        return mongoOperations.find(query, UserPrivacyProfile.class, PeopleCollectionKeys.Collection.USER_PRIVACY_PROFILE.getCollectionName());
    }

    @Override
    public List<UserPrivacyProfile> findAllByUserIdAndValueIds(String userId, List<String> valueIds) {
        Query query = new Query(Criteria
                .where(PeopleCollectionKeys.USER_REFERENCE_ID).is(PeopleUtils.convertStringToObjectId(userId))
                .and(VALUE_ID_LIST).in(valueIds));
        return mongoOperations.find(query, UserPrivacyProfile.class, PeopleCollectionKeys.Collection.USER_PRIVACY_PROFILE.getCollectionName());
    }

    @Override
    public UserPrivacyProfile findByProfileIdAndUserId(String profileId, String userId) {

        Query query = new Query(Criteria
                .where(PeopleCollectionKeys.USER_REFERENCE_ID).is(PeopleUtils.convertStringToObjectId(userId))
                .and(UNIQUE_ID).is(profileId));
        return mongoOperations.findOne(query, UserPrivacyProfile.class, PeopleCollectionKeys.Collection.USER_PRIVACY_PROFILE.getCollectionName());
    }

    @Override
    public UserPrivacyProfile findDefaultUserProfile(String userId, Boolean isDefault) {

        Query query = new Query(Criteria
                .where(PeopleCollectionKeys.USER_REFERENCE_ID).is(PeopleUtils.convertStringToObjectId(userId))
                .and("isDefault").is(Boolean.TRUE));
        return mongoOperations.findOne(query, UserPrivacyProfile.class, PeopleCollectionKeys.Collection.USER_PRIVACY_PROFILE.getCollectionName());
    }

    @Override
    public UserPrivacyProfile findPublicProfile(String userId) {
        Query query = new Query(Criteria
                .where(PeopleCollectionKeys.USER_REFERENCE_ID).is(PeopleUtils.convertStringToObjectId(userId))
                .and("isPublic").is(Boolean.TRUE));
        return mongoOperations.findOne(query, UserPrivacyProfile.class, PeopleCollectionKeys.Collection.USER_PRIVACY_PROFILE.getCollectionName());
    }

    @Override
    public List<UserPrivacyProfile> findSystemProfilesForUser(String userId) {
        Query query = new Query(Criteria
                .where(PeopleCollectionKeys.USER_REFERENCE_ID).is(PeopleUtils.convertStringToObjectId(userId))
                .and("isSystem").is(Boolean.TRUE));
        return mongoOperations.find(query, UserPrivacyProfile.class, PeopleCollectionKeys.Collection.USER_PRIVACY_PROFILE.getCollectionName());
    }

    @Override
    public void updatePrivacyProfileDefaultImage(String userId, String imageURL) {
        Query query = new Query(Criteria
                .where(PeopleCollectionKeys.USER_REFERENCE_ID).is(PeopleUtils.convertStringToObjectId(userId))
                .and(USE_DEFAULT_IMAGE).is(Boolean.TRUE));
        Update update = new Update();
        update.set(IMAGE_URL, imageURL);
        update.set(LAST_UPDATED_ON, PeopleUtils.getCurrentTimeInUTC());
        mongoOperations.updateMulti(query, update, UserPrivacyProfile.class, PeopleCollectionKeys.Collection.USER_PRIVACY_PROFILE.getCollectionName());
    }
}
