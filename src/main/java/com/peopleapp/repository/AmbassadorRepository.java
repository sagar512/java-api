package com.peopleapp.repository;

import com.peopleapp.dto.ContactNumberDTO;
import com.peopleapp.model.PeopleAmbassador;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AmbassadorRepository extends MongoRepository<PeopleAmbassador,String> {
    /*Fetches all sent referrals records for the user */
    List<PeopleAmbassador> findByAmbassadorId(ObjectId ambassadorId);

    PeopleAmbassador findByAmbassadorIdAndReferredContactNumber(ObjectId ambassadorId, ContactNumberDTO referredContactNumber);

    PeopleAmbassador findByReferralCodeAndReferredContactNumber(String referralCode, ContactNumberDTO phoneNumber);

}
