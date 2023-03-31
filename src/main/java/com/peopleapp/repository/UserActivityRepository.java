package com.peopleapp.repository;

import com.peopleapp.enums.TokenStatus;
import com.peopleapp.model.TemporarySession;
import com.peopleapp.model.UserActivity;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface UserActivityRepository extends MongoRepository<UserActivity, String>, CustomUserActivityRepository {

    UserActivity findByActivityByIdAndActivityForId(ObjectId activityById, ObjectId activityForId);
}
