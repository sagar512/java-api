package com.peopleapp.repository;

import com.peopleapp.model.AppInformation;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AppInformationRepository extends MongoRepository<AppInformation, String> {

    AppInformation findByDeviceTypeId(Integer deviceTypeId);
}
