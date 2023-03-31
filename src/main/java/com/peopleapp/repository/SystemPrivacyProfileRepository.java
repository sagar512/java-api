package com.peopleapp.repository;

import com.peopleapp.model.SystemPrivacyProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemPrivacyProfileRepository extends MongoRepository<SystemPrivacyProfile, String> {
}
