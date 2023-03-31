package com.peopleapp.repository;

import com.peopleapp.model.NetworkBroadcastMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NetworkMessagesRepository extends MongoRepository<NetworkBroadcastMessage, String> {

}
