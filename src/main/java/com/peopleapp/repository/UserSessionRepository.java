package com.peopleapp.repository;

import com.peopleapp.model.UserSession;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSessionRepository extends MongoRepository<UserSession, String> {

    @Query("{sessionToken:'?0'}")
    UserSession findBySessionToken(String sessionToken);

    @Query("{sessionToken:'?0', status : '?1'}")
    UserSession findBySessionTokenAndStatus(String sessionToken, String status);

    @Query("{peopleUserId : {$in : ?0}, status : 'ACTIVE'}")
    List<UserSession> findActiveSession(List<ObjectId> userIdList);

    @Query("{peopleUserId : ?0, status : 'ACTIVE'}")
    UserSession findActiveSession(ObjectId userId);

    @Query("{deviceToken : '?0', status : 'ACTIVE'}")
    List<UserSession> findActiveSessionByDeviceToken(String deviceUUID);

}
