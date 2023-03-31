package com.peopleapp.repository;

import com.peopleapp.model.Network;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface NetworkRepository extends MongoRepository<Network, String>, CustomNetworkRepository {

    @Query("{_id: '?0', networkStatus : '?1'}")
    Network findNetworkByIdAndStatus(String networkId, String status);
}
