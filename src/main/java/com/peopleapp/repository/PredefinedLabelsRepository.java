package com.peopleapp.repository;

import com.peopleapp.model.PredefinedLabels;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PredefinedLabelsRepository extends MongoRepository<PredefinedLabels, String> {
	
	List<PredefinedLabels> findByCategory(String category);
}
