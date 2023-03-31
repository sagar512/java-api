package com.peopleapp.repository;

import com.peopleapp.model.SystemTagUser;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TagRepository extends MongoRepository<SystemTagUser, String>, CustomTagRepository {

}
