package com.peopleapp.service;

import com.peopleapp.dto.ActivityContactsAPIParamData;
import com.peopleapp.dto.ActivityDetails;
import com.peopleapp.dto.ContactNumberDTO;
import com.peopleapp.dto.UserActivityData;
import com.peopleapp.dto.requestresponsedto.*;
import com.peopleapp.model.ActivityContact;
import com.peopleapp.model.UserActivity;
import org.joda.time.DateTime;

import java.util.List;

public interface UserActivityService {

    /* fetch APIs */
    /* Get all activities which are created for user - includes actionable and non actionable */
    ActivityListResponse getActivitiesCreatedForUser(int page, int size);

    /* Get all activities which are created for user - includes only actionable */
    ActivityListResponse getActionableActivitiesCreatedForUser(int page, int size);

    /* Get All activities created by the user */
    ActivityListResponse getActivitiesCreatedByUser(Integer page, Integer size);

    SharedLocationWithOthersResponse getActiveLocationSharedWithOthers(int page, int size);

    SharedLocationWithMeResponse getActiveLocationSharedWithMe(int page, int size);

    SharedContactWithOthersResponse getSharedContactWithOthers(int page, int size);

    SharedContactWithMeResponse getSharedContactWithMe(int page, int size);

    /* action on activities */
    void clearActivity(ClearActivityRequest clearActivityRequest);

    CancelRequestResponseDTO cancelActivity(CancelRequestDTO cancelRequestDTO);

    void ignoreActivity(IgnoreRequestDTO ignoreRequestDTO);

    void deleteActivity(DeleteActivityRequest deleteActivityRequest);

    List<UserActivity> createMultipleRequest(List<UserActivity> userActivityList);


    int getCountOfConnectionRequestsForTimeRange(String fromUserId, int timeRange, DateTime lastRequestCreatedTime);

    List<UserActivity> findByInitiateContactNumber(ContactNumberDTO contactNumber);

    ActivityDetails prepareActivityDetails(UserActivity userActivity, String activityDescription, boolean isInitiateInfoRequired);

    List<ActivityDetails> prepareActivityDetails(List<UserActivity> userActivityList);

    void editSharedContactActivity(EditSharedContactRequest editSharedContactRequest);

    List<UserActivityData> getActivityDetailsByActivityId(String activityId);

    ActivityContactsResponseDTO getActivityContactsByActivityId(ActivityContactsAPIParamData activityContactsAPIParamData);

    void updateTargetUserActivityByActivityContact(UserActivity targetActivity, List<ActivityContact> activityContacts);
}
