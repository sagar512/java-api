package com.peopleapp.repository;

import com.peopleapp.model.UserConnection;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserConnectionRepository extends MongoRepository<UserConnection, String>, CustomConnectionRepository {
	
	Long countByConnectionFromId(ObjectId userId);

}