package com.peopleapp.repository;

import com.peopleapp.enums.TokenStatus;
import com.peopleapp.model.TemporarySession;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface TemporarySessionRepository extends MongoRepository<TemporarySession, ObjectId> {

    @Query("{temporaryToken:'?0', tokenStatus: ?1}")
    TemporarySession findByTempTokenAndStatus(String tempToken, TokenStatus status);

}
