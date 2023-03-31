package com.peopleapp.repository;

import com.peopleapp.model.SystemTagUser;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;

@Repository
public class CustomTagRepositoryImpl implements CustomTagRepository {

    @Inject
    private MongoOperations mongoOperations;

    private String collectionName = "systemTagsUser";

    @Override
    public SystemTagUser findTags() {
        Query query = new Query();
        return mongoOperations.findOne(query, SystemTagUser.class, collectionName);
    }

}
