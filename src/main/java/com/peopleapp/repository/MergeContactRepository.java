package com.peopleapp.repository;

import com.peopleapp.model.MergedConnection;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MergeContactRepository extends MongoRepository<MergedConnection, String> {

}
