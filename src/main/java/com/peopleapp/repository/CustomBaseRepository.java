package com.peopleapp.repository;

import com.peopleapp.dto.Count;

import java.util.List;


public interface CustomBaseRepository {

    List<Count> getTotalCount(String userId, String collectionName);

    List<Count> getCurrYearCount(String userId, String groupById, String collectionName);

    List<Count> getCurrYearCount(String userId, String collectionName);

    List<Count> getCurrMonthCount(String userId, String groupById, String collectionName);

    List<Count> getCurrMonthCount(String userId, String collectionName);

    List<Count> getCurrWeekCount(String userId, String groupById, String collectionName);

    List<Count> getCurrWeekCount(String userId, String collectionName);

    List<Count> getTodayCount(String userId, String groupById, String collectionName);

    List<Count> getTodayCount(String userId, String collectionName);

}
