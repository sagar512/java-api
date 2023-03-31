package com.peopleapp.repository;

import com.peopleapp.model.RecentActiveNetwork;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecentActiveNetworkRepository extends MongoRepository<RecentActiveNetwork, String>, CustomRecentActiveNetworkRepository {

}
