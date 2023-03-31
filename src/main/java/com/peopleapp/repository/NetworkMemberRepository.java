package com.peopleapp.repository;

import com.peopleapp.model.NetworkMember;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NetworkMemberRepository extends MongoRepository<NetworkMember, String>, CustomNetworkRepository {

}
