package com.peopleapp.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.peopleapp.model.SocialMediaCategory;

@Repository
public interface SocialMediaCategoryRepository extends MongoRepository<SocialMediaCategory, ObjectId> {

}