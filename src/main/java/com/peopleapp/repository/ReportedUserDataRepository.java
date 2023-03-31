package com.peopleapp.repository;

import com.peopleapp.model.ReportedData;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface ReportedUserDataRepository extends MongoRepository<ReportedData, String> {

    ReportedData findByReportedByUserIdAndReportedUserId(ObjectId reportedByUserId, ObjectId reportedUserId);

    ReportedData findByReportedByUserIdAndReportedNetworkId(ObjectId reportedByUserId, ObjectId reportedNetworkId);


}
