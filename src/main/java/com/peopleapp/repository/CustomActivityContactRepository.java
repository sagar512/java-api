package com.peopleapp.repository;

import com.mongodb.client.result.UpdateResult;
import com.peopleapp.model.ActivityContact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomActivityContactRepository {

    Page<ActivityContact> getActivityContactsByActivityId(String activityId, String searchString, Pageable pageable);

    Page<ActivityContact> getShareActivityContactsByInitiatorIdAndReceiverId(String initiatorId, String receiverId,
                                                                             String searchString, Pageable pageable);

    List<ActivityContact> getActivityContactsByIdsAndUserId(List<String> activitySubIdList, String userId);

    List<ActivityContact> getActivityContactsByActivityIdsAndUserId(List<String> activityIdList, String userId);

    List<ActivityContact> getActivityContactsByIdsAndReceiverId(List<String> activitySubIdList, String userId);

    List<ActivityContact> getActivityContactsByInitiatorIdAndConnectionId(String initiatorId, String connectionId);

    UpdateResult markActivityContactsInActiveByActivityId(List<String> activityId);

    void expireActivityContactsByInitiatorIdAndReceiverId(String sessionUserId, List<String> connectedContactsUserId,
                                                          boolean receivedActivityContacts);

    void updateConnectionIdForActivityContacts(String initiatorId, String masterConnectionId,
                                               List<String> connectionIdsToBeReplaced);

}
