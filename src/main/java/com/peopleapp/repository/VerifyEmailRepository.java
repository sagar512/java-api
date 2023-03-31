package com.peopleapp.repository;

import com.peopleapp.model.VerifyEmail;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface VerifyEmailRepository extends MongoRepository<VerifyEmail, ObjectId> {

    @Query("{verificationLink:'?0', isEmailVerified: false}")
    VerifyEmail findByVerificationLink(String verificationToken);

    @Query("{email:'?0'}")
    List<VerifyEmail> findByEmailId(String email);

}
