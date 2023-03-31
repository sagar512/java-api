package com.peopleapp.repository;

import com.peopleapp.model.NetworkCategory;
import com.peopleapp.model.UserRestoreConnection;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRestoreConnectionRepository extends MongoRepository<UserRestoreConnection, String>, CustomConnectionRepository {

    List<UserRestoreConnection> findByConnectionFromId(ObjectId userId);

    Page<UserRestoreConnection> findByConnectionFromId(ObjectId userId, Pageable pageable)
            ;
    /* Delete all restore contact created by user*/
    String deleteAllConnectionByConnectionFromId(ObjectId connectionFromId);

    Long countByConnectionFromId(ObjectId userId);
}