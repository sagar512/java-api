package com.peopleapp.repository;

import com.peopleapp.model.NetworkCategory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NetworkCategoryRepository extends MongoRepository<NetworkCategory, String> {

    NetworkCategory findByName(String name);
}
