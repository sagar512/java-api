package com.peopleapp.repository;

import com.peopleapp.model.UserGroup;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserGroupRepository extends MongoRepository<UserGroup, String>, CustomUserGroupRepository {
}
