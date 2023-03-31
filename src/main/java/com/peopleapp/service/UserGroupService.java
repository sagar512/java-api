package com.peopleapp.service;

import java.util.List;

import com.peopleapp.dto.requestresponsedto.GroupIconsRequest;
import com.peopleapp.dto.requestresponsedto.*;
import com.peopleapp.dto.requestresponsedto.ContactSmartGroupResponse;
import com.peopleapp.dto.requestresponsedto.GroupIconsResponse;
import com.peopleapp.dto.requestresponsedto.SmartGroupsResponse;

public interface UserGroupService {

    AddUserGroupResponseDTO addGroups(UserGroupRequestDTO userGroupRequestDTO);

    FetchUserGroupResponseDTO fetchUserGroups(Integer pageNumber, Integer pageSize);

    String updateGroupFavouriteValue(UpdateGroupFavouriteRequestDTO updateGroupFavouriteRequestDTO);

    FetchUserGroupResponseDTO fetchFavouriteGroups();

    EditGroupResponse editGroup(EditUserGroupRequestDTO editUserGroupRequestDTO);

    DeletedUserGroupResponseDTO deleteUserGroups(DeleteUserGroupRequestDTO deleteUserGroupRequestDTO);

    UpdateGroupImageResponseDTO updateGroupImage(UpdateGroupImageRequestDTO updateImageRequest);
    
    GroupIconsResponse saveIcon(GroupIconsRequest groupIconsRequest);
    
    List<GroupIconsResponse> getIcons();
    
    SmartGroupsResponse getSmartGroupListByName(String smartGroupByName);
    
    ContactSmartGroupResponse getAllContactBysmartGroup(String smartGroupByName,String contactBysmartGroup);
}
