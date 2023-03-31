package com.peopleapp.repository;

import com.peopleapp.model.UserPrivacyProfile;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserPrivacyProfileRepository extends MongoRepository<UserPrivacyProfile, String>, CustomPrivacyProfileRepository {

}
