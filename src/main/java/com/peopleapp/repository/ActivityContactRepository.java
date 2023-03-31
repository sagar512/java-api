package com.peopleapp.repository;

import com.peopleapp.model.ActivityContact;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ActivityContactRepository extends MongoRepository<ActivityContact, String>, CustomActivityContactRepository {
}
