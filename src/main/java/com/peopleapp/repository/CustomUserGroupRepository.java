package com.peopleapp.repository;

import com.peopleapp.model.UserGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomUserGroupRepository {

    /* Fetch groups created by session user for given groupId*/
    List<UserGroup> findByUserGroupIdAndOwnerId(List<String> groupIds, String groupOwnerId);

    List<UserGroup> fetchFavouriteGroups(String groupOwnerId);

    List<UserGroup> fetchAllUserGroups(String groupOwnerId);

    Page<UserGroup> fetchUserGroups(String groupOwnerId, Pageable pageable);

    List<UserGroup> deleteUserGroups(String groupOwnerId, List<String> groupIdList);

    void removeContactIdFromUserGroups(String groupOwnerId, String connectionId);

    /* Delete all group created by user*/
    void deleteAllGroupCreatedByUser(String groupOwnerId);

}
