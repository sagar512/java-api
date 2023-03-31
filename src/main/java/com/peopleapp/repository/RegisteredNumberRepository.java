package com.peopleapp.repository;

import com.peopleapp.model.RegisteredNumber;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RegisteredNumberRepository extends MongoRepository<RegisteredNumber, String> {
}
