package com.peopleapp.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.peopleapp.model.groupIcons;

@Repository
public interface groupIconsRepository extends MongoRepository<groupIcons, ObjectId> {

}
