package com.peopleapp.service.impl;

import com.peopleapp.configuration.LocaleMessageReader;
import com.peopleapp.constant.MessageConstant;
import com.peopleapp.controller.UserGroupController;
import com.peopleapp.dto.ApplicationConfigurationDTO;
import com.peopleapp.dto.EditGroup;
import com.peopleapp.dto.GroupImage;
import com.peopleapp.dto.KeyValueData;
import com.peopleapp.dto.UpdateFavouriteGroup;
import com.peopleapp.dto.UserGroupData;
import com.peopleapp.dto.UserProfileData;
import com.peopleapp.dto.requestresponsedto.GroupIconsRequest;
import com.peopleapp.dto.requestresponsedto.*;
import com.peopleapp.dto.requestresponsedto.ContactSmartGroupResponse;
import com.peopleapp.dto.requestresponsedto.GroupIconsResponse;
import com.peopleapp.dto.requestresponsedto.SmartGroupsResponse;
import com.peopleapp.enums.MessageCodes;
import com.peopleapp.exception.BadRequestException;
import com.peopleapp.model.PeopleUser;
import com.peopleapp.model.PredefinedLabels;
import com.peopleapp.model.UserConnection;
import com.peopleapp.model.UserGroup;
import com.peopleapp.model.groupIcons;
import com.peopleapp.repository.PredefinedLabelsRepository;
import com.peopleapp.repository.UserConnectionRepository;
import com.peopleapp.repository.UserGroupRepository;
import com.peopleapp.repository.groupIconsRepository;
import com.peopleapp.security.TokenAuthService;
import com.peopleapp.service.UserGroupService;
import com.peopleapp.util.PeopleUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserGroupServiceImpl implements UserGroupService {

    @Inject
    private UserGroupRepository userGroupRepository;

    @Inject
    private UserConnectionRepository userConnectionRepository;

    @Inject
    private TokenAuthService tokenAuthService;

    @Inject
    private LocaleMessageReader messages;
    
    @Inject
    private groupIconsRepository groupIconsRepository;

    @Inject
	private ApplicationConfigurationDTO properties;
    
    @Inject
    private PredefinedLabelsRepository predefinedLabelsRepository;
    
    @Override
    public AddUserGroupResponseDTO addGroups(UserGroupRequestDTO userGroupRequestDTO) {
        UserGroup userGroup;

        String groupOwnerId = tokenAuthService.getSessionUser().getUserId();
        List<UserGroupData> listOfUserGroups = userGroupRequestDTO.getUserGroupList();

        List<UserGroup> listOfFetchedUserGroups = userGroupRepository.fetchAllUserGroups(groupOwnerId);

        List<UserGroup> userGroupList = new ArrayList<>();

        for (UserGroupData userGroupData : PeopleUtils.emptyIfNull(listOfUserGroups)) {
            List<String> listOfValidContactIdList = getListOfValidContactId(groupOwnerId, userGroupData.getContactIdList());

            userGroup = new UserGroup();

            userGroup.setTitle(userGroupData.getTitle());
            userGroup.setDesc(userGroupData.getDesc());
            userGroup.setImageURL(userGroupData.getImageURL());
            userGroup.setOrderNumber(listOfFetchedUserGroups.size() + 1);
            userGroup.setContactIdList(listOfValidContactIdList);
            userGroup.setOwnerId(groupOwnerId);
            userGroup.setLocalId(userGroupData.getLocalId());
            userGroup.setIsFavourite(userGroupData.getIsFavourite());
            userGroup.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
            
            for(String connectedID : userGroup.getContactIdList()) {
            	UserConnection userConnection = userConnectionRepository.findById(connectedID).get();
                userConnection.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
                userConnectionRepository.save(userConnection);
            }
            userGroupList.add(userGroup);

        }

        List<UserGroup> createdGroupList = userGroupRepository.insert(userGroupList);

        AddUserGroupResponseDTO addUserGroupResponseDTO = new AddUserGroupResponseDTO();
        addUserGroupResponseDTO.setUserGroupList(createdGroupList);
        return addUserGroupResponseDTO;
    }

    @Override
    public String updateGroupFavouriteValue(UpdateGroupFavouriteRequestDTO updateGroupFavouriteRequestDTO) {
        String groupOwnerId = tokenAuthService.getSessionUser().getUserId();
        Map<String, UpdateFavouriteGroup> favouriteGroupMap = new HashMap<>();
        /* Creating groupId to request mapping*/
        for (UpdateFavouriteGroup eachGroup : PeopleUtils.emptyIfNull(updateGroupFavouriteRequestDTO.getFavouriteGroups())) {
            favouriteGroupMap.put(eachGroup.getGroupId(), eachGroup);
        }
        /* Fetch list of valid groups from request*/
        List<UserGroup> listOfValidGroups = userGroupRepository.findByUserGroupIdAndOwnerId(
                new ArrayList<>(favouriteGroupMap.keySet()), groupOwnerId);

        if (PeopleUtils.isNullOrEmpty(listOfValidGroups)) {
            throw new BadRequestException(MessageCodes.INVALID_USER_GROUP.getValue());
        }

        /* Update the favourite status for respective valid groups*/
        for (UserGroup userGroup : PeopleUtils.emptyIfNull(listOfValidGroups)) {
            UpdateFavouriteGroup favouriteGroup = favouriteGroupMap.get(userGroup.getGroupId());
            if (favouriteGroup != null) {
                userGroup.setIsFavourite(favouriteGroup.getIsFavourite());
            }
        }

        // persist updated status of groups
        userGroupRepository.saveAll(listOfValidGroups);

        return messages.get(MessageConstant.GROUP_FAV_VALUE_UPDATED_SUCCESSFULLY);
    }

    @Override
    public FetchUserGroupResponseDTO fetchFavouriteGroups() {
        String groupOwnerId = tokenAuthService.getSessionUser().getUserId();

        List<UserGroup> favouriteGroupList = userGroupRepository.fetchFavouriteGroups(groupOwnerId);

        FetchUserGroupResponseDTO fetchUserGroupResponseDTO = new FetchUserGroupResponseDTO();
        fetchUserGroupResponseDTO.setUserGroupList(favouriteGroupList);
        return fetchUserGroupResponseDTO;

    }

    @Override
    public FetchUserGroupResponseDTO fetchUserGroups(Integer pageNumber, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        String groupOwnerId = tokenAuthService.getSessionUser().getUserId();

        Page<UserGroup> userGroupList = userGroupRepository.fetchUserGroups(groupOwnerId, pageable);

        //set smart group static data
        List<String> smartGroupList = new ArrayList<>();
        smartGroupList.add("Company");
        smartGroupList.add("City, state");
        smartGroupList.add("Social Media");
        smartGroupList.add("Connected contacts");
        smartGroupList.add("Tags");
        
        Link link = ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(UserGroupController.class)
                .fetchUserGroups(pageNumber + 1, pageSize, "")).withSelfRel();

        //Preparing the response structure
        FetchUserGroupResponseDTO fetchUserGroupResponseDTO = new FetchUserGroupResponseDTO();
        fetchUserGroupResponseDTO.setUserGroupList(userGroupList.getContent());
        fetchUserGroupResponseDTO.setSmartGroupList(smartGroupList);
        fetchUserGroupResponseDTO.setTotalNumberOfPages(userGroupList.getTotalPages());
        fetchUserGroupResponseDTO.setTotalElements(userGroupList.getTotalElements());
        if (!userGroupList.isLast()) {
            fetchUserGroupResponseDTO.setNextURL(link.getHref());
        }

        return fetchUserGroupResponseDTO;
    }

    @Override
    public EditGroupResponse editGroup(EditUserGroupRequestDTO editUserGroupRequestDTO) {

        String groupOwnerId = tokenAuthService.getSessionUser().getUserId();
        Map<String, EditGroup> editGroupMap = new HashMap<>();
        List<String> connectedIdList = new ArrayList<>();
        /*  Creating groupId to values to be edited */
        for (EditGroup editGroup : PeopleUtils.emptyIfNull(editUserGroupRequestDTO.getUserGroupsToBeEdited())) {
        	UserGroup userGroup = userGroupRepository.findById(editGroup.getGroupId()).get();
        	if(userGroup.getContactIdList()!=null ) {
        		for(String tempID : userGroup.getContactIdList()) {
        			connectedIdList.add(tempID);
        		}
        	}
        	for(String reqTempId : editGroup.getContactIdList()) {
        		connectedIdList.add(reqTempId);
        	}
            editGroupMap.put(editGroup.getGroupId(), editGroup);
        }

        /* fetch all valid group from list of groups to be edited */
        List<UserGroup> validUserGroups = userGroupRepository.findByUserGroupIdAndOwnerId(
                new ArrayList<>(editGroupMap.keySet()), groupOwnerId);
        
        if (PeopleUtils.isNullOrEmpty(validUserGroups)) {
            throw new BadRequestException(MessageCodes.INVALID_USER_GROUP.getValue());
        }

        /*  Updating latest values to existing Group object */
        for (UserGroup userGroup : validUserGroups) {
            EditGroup editGroup = editGroupMap.get(userGroup.getGroupId());
            if (editGroup != null) {
                userGroup.setTitle(editGroup.getTitle());
                userGroup.setDesc(editGroup.getDesc());
                userGroup.setImageURL(editGroup.getImageURL());
                userGroup.setLocalId(editGroup.getLocalId());
                userGroup.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
                userGroup.setContactIdList(getListOfValidContactId(groupOwnerId, editGroup.getContactIdList()));
                userGroup.setIsFavourite(editGroup.getIsFavourite());
            }
        }
        
        //update sync time in userConnection Table
        for(String updateConnectedId : removeDuplicates(connectedIdList)) {
        	UserConnection userConnection = userConnectionRepository.findById(updateConnectedId).get();
            userConnection.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
            userConnectionRepository.save(userConnection);
        }

        List<UserGroup> editedGroup = userGroupRepository.saveAll(validUserGroups);

        EditGroupResponse response = new EditGroupResponse();
        response.setGroupDetails(editedGroup);

        return response;
    }

    @Override
    public DeletedUserGroupResponseDTO deleteUserGroups(DeleteUserGroupRequestDTO deleteUserGroupRequestDTO) {

        String groupOwnerId = tokenAuthService.getSessionUser().getUserId();
        List<String> groupIdList = deleteUserGroupRequestDTO.getGroupIdList();

        List<UserGroup> deletedGroups = userGroupRepository.deleteUserGroups(groupOwnerId, groupIdList);
        List<String> deletedGroupIdList = new ArrayList<>();
        for (UserGroup deletedGroup : deletedGroups) {
            deletedGroupIdList.add(deletedGroup.getGroupId());
            for(String connectedID : deletedGroup.getContactIdList()) {
            	UserConnection userConnection = userConnectionRepository.findById(connectedID).get();
                userConnection.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
                userConnectionRepository.save(userConnection);
            }
        }
        DeletedUserGroupResponseDTO deletedUserGroupResponseDTO = new DeletedUserGroupResponseDTO();
        deletedUserGroupResponseDTO.setDeletedGroupIdList(deletedGroupIdList);

        return deletedUserGroupResponseDTO;
    }

    @Override
    public UpdateGroupImageResponseDTO updateGroupImage(UpdateGroupImageRequestDTO updateImageRequest) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        Map<String, String> groupIdToGroupImageMapping = new HashMap<>();
        Set<String> updatedGroups = new HashSet<>();
        /* creating the map for groupId and groupImage*/
        for (GroupImage groupImage : updateImageRequest.getListOfGroupImages()) {
            groupIdToGroupImageMapping.put(groupImage.getGroupId(), groupImage.getImageURL());
        }

        /*  Fetch all valid groups from the request list */
        List<UserGroup> validListOfGroups = userGroupRepository.findByUserGroupIdAndOwnerId(
                new ArrayList<>(groupIdToGroupImageMapping.keySet()), sessionUser.getUserId());

        if (PeopleUtils.isNullOrEmpty(validListOfGroups)) {
            throw new BadRequestException(MessageCodes.INVALID_USER_GROUP.getValue());
        }

        /*  Update all images url's to the respective groups*/
        for (UserGroup userGroup : PeopleUtils.emptyIfNull(validListOfGroups)) {
            userGroup.setImageURL(groupIdToGroupImageMapping.get(userGroup.getGroupId()));
            updatedGroups.add(userGroup.getGroupId());
        }

        // persist update values of group
        userGroupRepository.saveAll(validListOfGroups);

        // response
        UpdateGroupImageResponseDTO groupImageResponse = new UpdateGroupImageResponseDTO();
        groupImageResponse.setListOfGroupsWithUpdatedImages(updatedGroups);
        return groupImageResponse;
    }

    /**
     * This method will check the provided
     * contactIds against "user_connection" collection
     * and will return the valid list of contact ids.
     *
     * @param groupOwnerId
     * @param groupContactIds
     * @return List<String>
     */
    private List<String> getListOfValidContactId(String groupOwnerId, List<String> groupContactIds) {
        Set<String> uniqueContactIds = new HashSet<>();

        if (PeopleUtils.isNullOrEmpty(groupContactIds)) {
            return new ArrayList<>();
        }
        List<UserConnection> validContactList =
                userConnectionRepository.findContactByConnectionId(groupOwnerId, groupContactIds);

        for (UserConnection contact : PeopleUtils.emptyIfNull(validContactList)) {
            uniqueContactIds.add(contact.getConnectionId());
        }
        return new ArrayList<>(uniqueContactIds);
    }

	@Override
	public GroupIconsResponse saveIcon(GroupIconsRequest groupIconsRequest) {
		
		//set Model Request
		groupIcons groupIcons = new groupIcons();
		groupIcons.setImageURL(groupIconsRequest.getImageURL());
		
		//save database
		groupIcons icons = groupIconsRepository.save(groupIcons);
		
		//prepaid response
		GroupIconsResponse response = new GroupIconsResponse();
		response.setIconId(icons.getIconId().toString());
		response.setImageURL(properties.getS3BaseUrlNetworkCategory()+"group_icons/"+ icons.getImageURL());
		
		return response;
	}
	
	@Override
	public List<GroupIconsResponse> getIcons() {
		List<GroupIconsResponse> groupIconsResponses = new ArrayList<>();
        for(groupIcons icons : groupIconsRepository.findAll()){
        	GroupIconsResponse icon =new GroupIconsResponse();
        	icon.setIconId(icons.getIconId().toString());
        	icon.setImageURL(properties.getS3BaseUrlNetworkCategory()+"group_icons/"+ icons.getImageURL());
        	groupIconsResponses.add(icon);
        }
		return groupIconsResponses;
	}

	@Override
	public SmartGroupsResponse getSmartGroupListByName(String smartGroupByName) {
		
		SmartGroupsResponse groupsResponse = new SmartGroupsResponse();
		String groupOwnerId = tokenAuthService.getSessionUser().getUserId();
		
		switch (smartGroupByName) {
		case "Company":
			List<UserConnection> companyList = userConnectionRepository.findCompanyOrTag(groupOwnerId, "company");
			List<String> companyTempList = new ArrayList<>();
		    for(UserConnection u : PeopleUtils.emptyIfNull(companyList)) {
		       if(u.getContactStaticData()!=null){
		    	   if(!u.getContactStaticData().getCompany().trim().isEmpty()) {
			           companyTempList.add(u.getContactStaticData().getCompany().trim());
			       }
		       }
		    }
		    List<String> finalCompany = removeDuplicates(companyTempList);
		    Collections.sort(finalCompany, String.CASE_INSENSITIVE_ORDER);
		    groupsResponse.setSmartGroupName("Company");
		    groupsResponse.setSmartGroupSubList(finalCompany);
			break;
			
		case "City, state":
			List<UserConnection> cityAndStateList = userConnectionRepository.findSocialMediaOrCityandState(groupOwnerId);
			List<String> cityAndStateTempList = new ArrayList<>();
			for(UserConnection u: PeopleUtils.emptyIfNull(cityAndStateList)) {
				if(u.getContactStaticData()!=null) {
					for(UserProfileData up: PeopleUtils.emptyIfNull(u.getContactStaticData().getUserMetadataList())) {
						if(up.getCategory().equalsIgnoreCase("ADDRESS")) {
							String tempCity = null;
							String tempRegion = null;
							for(KeyValueData keyData: PeopleUtils.emptyIfNull(up.getKeyValueDataList())) {
								if(keyData.getKey().equalsIgnoreCase("city")) {
										tempCity = keyData.getVal().trim();
								}
							}
							for(KeyValueData keyData: PeopleUtils.emptyIfNull(up.getKeyValueDataList())) {
								if(keyData.getKey().equalsIgnoreCase("state")) {
										tempRegion = keyData.getVal().trim();
									if(!tempCity.isEmpty() && !tempRegion.isEmpty()) {
										cityAndStateTempList.add(tempCity+","+tempRegion);
									}
								}
							}
						}
					}
				}
			}
			List<String> finalCityAndState = removeDuplicates(cityAndStateTempList);
			Collections.sort(finalCityAndState, String.CASE_INSENSITIVE_ORDER);
			groupsResponse.setSmartGroupName("City, state");
			groupsResponse.setSmartGroupSubList(finalCityAndState);
			break;
			
		case "Social Media":
			List<UserConnection> socialMediaList = userConnectionRepository.findSocialMediaOrCityandState(groupOwnerId);
			List<PredefinedLabels> predefinedLabels = new ArrayList<>();
			
			List<PredefinedLabels> predefinedSocialLabelsList = predefinedLabelsRepository.findByCategory("SOCIALPROFILE");
			List<PredefinedLabels> predefinedInstantLabelsList = predefinedLabelsRepository.findByCategory("INSTANTMESSAGING");
			
			predefinedLabels.addAll(predefinedSocialLabelsList);
			predefinedLabels.addAll(predefinedInstantLabelsList);
			
			List<String> socialMediaTempList = new ArrayList<>();
			for(UserConnection u: PeopleUtils.emptyIfNull(socialMediaList)) {
				if(u.getContactStaticData()!=null && u.getContactStaticData().getUserMetadataList()!=null) {
					for(UserProfileData up: PeopleUtils.emptyIfNull(u.getContactStaticData().getUserMetadataList())) {
						if(up.getCategory().equalsIgnoreCase("SOCIALPROFILE") || up.getCategory().equalsIgnoreCase("INSTANTMESSAGING")) {
							for(PredefinedLabels label: PeopleUtils.emptyIfNull(predefinedLabels)) {
								for(Map<String, Object> value : label.getLabelList()) {
									if(value.get("key").equals(up.getLabel())) {
										socialMediaTempList.add((String) value.get("val"));
									}
								}
							}
						}
					}
				}
			}
			List<String> finalSocialMedia = removeDuplicates(socialMediaTempList);
			Collections.sort(finalSocialMedia, String.CASE_INSENSITIVE_ORDER);
			groupsResponse.setSmartGroupName("Social Media");
			groupsResponse.setSmartGroupSubList(finalSocialMedia);
			break;
			
		case "Connected contacts":
			break;
			
		case "Tags":
			List<UserConnection> tagList = userConnectionRepository.findCompanyOrTag(groupOwnerId, "tagList");
			List<String> tagTempList = new ArrayList<>();
			for(UserConnection u: PeopleUtils.emptyIfNull(tagList)) {
				if(u.getContactStaticData()!=null) {
					for(String strTag : PeopleUtils.emptyIfNull(u.getContactStaticData().getTagList())) {
						tagTempList.add(strTag.trim());
					}	
				}
			}
			List<String> finalTag = removeDuplicates(tagTempList);
			Collections.sort(finalTag, String.CASE_INSENSITIVE_ORDER);
			groupsResponse.setSmartGroupName(smartGroupByName);
			groupsResponse.setSmartGroupSubList(finalTag);
			break;

		default:
			break;
		}
		
		return groupsResponse;
	}
	
	@Override
	public ContactSmartGroupResponse getAllContactBysmartGroup(String smartGroupByName, String contactBysmartGroup) {
		
		ContactSmartGroupResponse contactSmartGroupResponse = new ContactSmartGroupResponse();
		String groupOwnerId = tokenAuthService.getSessionUser().getUserId();
		
		switch (smartGroupByName) {
		case "Company":
			List<UserConnection> contactCompanyList = userConnectionRepository.findAllContactByCompanyNameOrTagName(groupOwnerId, "company", contactBysmartGroup);
			List<String> contactCompanyTempList = new ArrayList<>();
			for(UserConnection u: PeopleUtils.emptyIfNull(contactCompanyList)) {
				contactCompanyTempList.add(u.getConnectionId());
			}
			contactSmartGroupResponse.setSmartGroupSubName(contactBysmartGroup);
			contactSmartGroupResponse.setContactIdList(contactCompanyTempList);
			break;
			
		case "City, state":
			String[] tempStrig = contactBysmartGroup.split(",");
			String tocity = tempStrig[0];
			String toState = tempStrig[1];
			List<UserConnection> contacCityAndStateList = userConnectionRepository.findAllContactByCityAndState(groupOwnerId);
			List<String> contacCityAndStateTempList = new ArrayList<>();
			for(UserConnection u: PeopleUtils.emptyIfNull(contacCityAndStateList)) {
				if(u.getContactStaticData()!=null) {
					for(UserProfileData up: PeopleUtils.emptyIfNull(u.getContactStaticData().getUserMetadataList())) {
						if(up.getCategory().equalsIgnoreCase("ADDRESS")) {
							String tempCity = null;
							String tempRegion = null;
							for(KeyValueData keyData: PeopleUtils.emptyIfNull(up.getKeyValueDataList())) {
								if(keyData.getKey().equalsIgnoreCase("city")) {
									if(keyData.getVal().equalsIgnoreCase(tocity)) {
										tempCity = keyData.getVal();
									}
								}
							}
							for(KeyValueData keyData: PeopleUtils.emptyIfNull(up.getKeyValueDataList())) {
								if(keyData.getKey().equalsIgnoreCase("state")) {
									if(keyData.getVal().equalsIgnoreCase(toState)) {
										tempRegion = keyData.getVal();	
										if(tempCity!=null && tempRegion!=null) {
											contacCityAndStateTempList.add(u.getConnectionId());
										}
									}
								}
							}
						}
					}
				}
			}
			contactSmartGroupResponse.setSmartGroupSubName(contactBysmartGroup);
			contactSmartGroupResponse.setContactIdList(contacCityAndStateTempList);
			break;
			
		case "Social Media":
			String socialMediaCode = null;
			List<PredefinedLabels> predefinedLabelsList = predefinedLabelsRepository.findByCategory("SOCIALPROFILE");
			for(PredefinedLabels label: PeopleUtils.emptyIfNull(predefinedLabelsList)) {
				for(Map<String, Object> value : PeopleUtils.emptyIfNull(label.getLabelList())) {
					if(value.get("val").equals(contactBysmartGroup)) {
						socialMediaCode = (String) value.get("key");
					}
				}
			}
			List<UserConnection> contactSocialMediaList = userConnectionRepository.findAllContactBySocialMediaName(groupOwnerId, socialMediaCode);
			List<String> contactSocialMediaTempList = new ArrayList<>();
			for(UserConnection u: PeopleUtils.emptyIfNull(contactSocialMediaList)) {
				contactSocialMediaTempList.add(u.getConnectionId());
			}
			contactSmartGroupResponse.setSmartGroupSubName(contactBysmartGroup);
			contactSmartGroupResponse.setContactIdList(contactSocialMediaTempList);
			break;
			
		case "Connected contacts":
			List<UserConnection> contactConnectedList = userConnectionRepository.findAllContactByConnected(groupOwnerId);
			List<String> contactConnectedTempList = new ArrayList<>();
			int cnt=0;
			for(UserConnection u: PeopleUtils.emptyIfNull(contactConnectedList)) {
				cnt++;
			    contactConnectedTempList.add(u.getConnectionId());
			}
			contactSmartGroupResponse.setSmartGroupSubName(contactBysmartGroup);
			if(cnt>=2) {
				contactSmartGroupResponse.setContactIdList(contactConnectedTempList);
			}
			break;
			
		case "Tags":
			List<UserConnection> contactTagList = userConnectionRepository.findAllContactByCompanyNameOrTagName(groupOwnerId, "tagList", contactBysmartGroup);
			List<String> contactTagTempList = new ArrayList<>();
			for(UserConnection u: PeopleUtils.emptyIfNull(contactTagList)) {
				contactTagTempList.add(u.getConnectionId());
			}
			contactSmartGroupResponse.setSmartGroupSubName(contactBysmartGroup);
			contactSmartGroupResponse.setContactIdList(contactTagTempList);
			break;

		default:
			break;
		}

		return contactSmartGroupResponse;
	}
	
	// using java 8
	private List<String> removeDuplicates(List<String> toList){
	   return new ArrayList<>(toList.stream().collect(Collectors.toMap(String::toLowerCase, str -> str,(prev, next) -> next, LinkedHashMap::new)).values());
	}
	
	//find Duplicate 
	/*private List<String> findDuplicate(List<String> toList){
		List<String> tempList = new ArrayList<>();
		for(int i=0;i<toList.size();i++) {
	    	for(int j=i+1;j<toList.size();j++) {
	    		if(toList.get(i).equalsIgnoreCase(toList.get(j))) {
	    			tempList.add(toList.get(i));
	    		}
	    	}
	    }
		return tempList;
	}*/
}
