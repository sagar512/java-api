package com.peopleapp.repository;

import com.peopleapp.model.PeopleUser;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface PeopleUserRepository extends MongoRepository<PeopleUser, String>, CustomUserRepository {

    @Query("{userId: '?0', status: '?1'}")
    PeopleUser findByuserId(String userId, String status);

    PeopleUser findByReferralCode(String referralCode);

    PeopleUser findByBluetoothToken(String bluetoothToken);
}

