package com.peopleapp.controller;

import com.peopleapp.configuration.LocaleMessageReader;
import com.peopleapp.constant.APIParamKeys;
import com.peopleapp.constant.MessageConstant;
import com.peopleapp.dto.requestresponsedto.GroupIconsRequest;
import com.peopleapp.dto.requestresponsedto.*;
import com.peopleapp.dto.requestresponsedto.ContactSmartGroupResponse;
import com.peopleapp.dto.requestresponsedto.GroupIconsResponse;
import com.peopleapp.dto.requestresponsedto.SmartGroupsResponse;
import com.peopleapp.service.UserGroupService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;


import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;

@RestController
@RequestMapping(value = "/v1.0/group/api")
@Api(value = "group", tags = "Group related operations")
public class UserGroupController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    private LocaleMessageReader messages;

    @Inject
    private UserGroupService userGroupService;
    
    @ApiOperation(
            value = "Add New Icons",
            notes = "<div> This API get All icons in  group(s).")
    @PostMapping(
            value = "/addIcon",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
	public BaseResponseDTO<GroupIconsResponse> createIons(
			@RequestBody GroupIconsRequest groupIconsRequest,
			@RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {
		
		GroupIconsResponse groupIconsResponse = userGroupService
				.saveIcon(groupIconsRequest);
		BaseResponseDTO<GroupIconsResponse> response = new BaseResponseDTO<>();
		response.setData(groupIconsResponse);
		response.setMessage("group Icon added successfully");
		
		return response;
	}

	@GetMapping(
			value = "/getIcons", 
			produces = MediaType.APPLICATION_JSON_VALUE
	)
	public BaseResponseDTO<List<GroupIconsResponse>> getIcons(
			@RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

		BaseResponseDTO<List<GroupIconsResponse>> response = new BaseResponseDTO<>();
		response.setData(userGroupService.getIcons());
		response.setMessage("group Icons Data");

		return response;
	}
	
    @ApiOperation(
            value = "Create group(s)",
            notes = "User will be able to create group(s). <b>(OFFLINE supported)</b>"
    )
    @PostMapping(
            value = "/add",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<AddUserGroupResponseDTO> createGroups(
            @Validated @RequestBody UserGroupRequestDTO addUserGroupRequest,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside UserGroupController -> createGroups ");

        BaseResponseDTO<AddUserGroupResponseDTO> baseResponse = new BaseResponseDTO<>();
        baseResponse.setData(userGroupService.addGroups(addUserGroupRequest));
        baseResponse.setMessage(messages.get(MessageConstant.GROUPS_ADDED_SUCCESSFULLY));

        return baseResponse;
    }

    @ApiIgnore
    @ApiOperation(
            value = "Favourite/un-favourite a group(s)",
            notes = "<div> This API enables user to favourite or un-favourite group(s). <b>(OFFLINE supported)</b>" +
                    "</div>\n" + "\n" + "| Error Case                                   || Error Code |\n" +
                    "| -------------------------------------------- || ---------- |\n" +
                    "| If group(s) to be marked as favourite is invalid.\t   || 1000       |"
    )
    @PostMapping(
            value = "/update-favourite",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO updateGroupFavouriteValue(
            @Validated @RequestBody UpdateGroupFavouriteRequestDTO updateGroupFavouriteRequestDTO,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside UserGroupController -> updateGroupFavouriteValue ");

        BaseResponseDTO<String> baseResponseDTO = new BaseResponseDTO<>();
        String responseMessage = userGroupService.updateGroupFavouriteValue(updateGroupFavouriteRequestDTO);

        baseResponseDTO.setMessage(responseMessage);

        return baseResponseDTO;
    }

    @ApiIgnore
    @ApiOperation(
            value = "List of favourite groups.",
            notes = "This API will list down all groups that are marked as favourite.")
    @GetMapping(
            value = "/favourites",
            produces = APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<FetchUserGroupResponseDTO> fetchFavouriteGroups(
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside UserGroupController -> fetchFavouriteGroups ");

        BaseResponseDTO<FetchUserGroupResponseDTO> baseResponseDTO = new BaseResponseDTO<>();
        FetchUserGroupResponseDTO responseDTO = userGroupService.fetchFavouriteGroups();
        baseResponseDTO.setData(responseDTO);

        return baseResponseDTO;
    }

    @ApiOperation(
            value = "List of groups",
            notes = "This API will list down all groups created by user. This API is paginated and default page size is 10."
    )
    @GetMapping(
            value = "/details",
            produces = APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<FetchUserGroupResponseDTO> fetchUserGroups(
            @RequestParam(value = APIParamKeys.PAGE_NUMBER, defaultValue = "0") Integer pageNumber,
            @RequestParam(value = APIParamKeys.PAGE_SIZE, defaultValue = "50") Integer pageSize,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside UserGroupController -> fetchUserGroups");

        BaseResponseDTO<FetchUserGroupResponseDTO> baseResponseDTO = new BaseResponseDTO<>();
        baseResponseDTO.setData(userGroupService.fetchUserGroups(pageNumber, pageSize));

        return baseResponseDTO;
    }

    @ApiOperation(
            value = "Edit group(s)",
            notes = "<div>User will be able to edit any group. <b>(OFFLINE supported)</b></div>\n" +
                    "\n" +
                    "| Error Case                                   || Error Code |\n" +
                    "| -------------------------------------------- || ---------- |\n" +
                    "| If group(s) to be edited is invalid\t   || 1000       |"
    )
    @PostMapping(
            value = "/edit",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<EditGroupResponse> editGroup(
            @Validated @RequestBody EditUserGroupRequestDTO editUserGroupRequestDTO,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside UserGroupController -> editUserGroupRequestDTO");

        BaseResponseDTO<EditGroupResponse> response = new BaseResponseDTO<>();
        response.setData(userGroupService.editGroup(editUserGroupRequestDTO));
        response.setMessage(messages.get(MessageConstant.GROUPS_EDITED_SUCCESSFULLY));

        return response;
    }

    @ApiOperation(
            value = "Delete group(s)",
            notes = "User can delete group(s). <b>(OFFLINE supported)</b>"
    )
    @DeleteMapping(
            value = "/delete",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<DeletedUserGroupResponseDTO> deleteUserGroups(
            @Validated @RequestBody DeleteUserGroupRequestDTO deleteUserGroupRequestDTO,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside UserGroupController -> editUserGroupRequestDTO");

        BaseResponseDTO<DeletedUserGroupResponseDTO> baseResponseDTO = new BaseResponseDTO<>();
        baseResponseDTO.setData(userGroupService.deleteUserGroups(deleteUserGroupRequestDTO));
        return baseResponseDTO;
    }

    @ApiIgnore
    @ApiOperation(
            value = "Update image for group(s)",
            notes = "<div>This API is used to update group(s) image url. <b>(OFFLINE supported)</b></div>\n" +
                    "\n" +
                    "| Error Case                                   || Error Code |\n" +
                    "| -------------------------------------------- || ---------- |\n" +
                    "| If group(s) for which the image is to be updated is invalid\t   || 1000       |\n")
    @PostMapping(
            value = "/group-image/update",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<UpdateGroupImageResponseDTO> updateGroupImage(
            @Validated @RequestBody UpdateGroupImageRequestDTO updateImageRequest,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        BaseResponseDTO<UpdateGroupImageResponseDTO> response = new BaseResponseDTO<>();
        response.setData(userGroupService.updateGroupImage(updateImageRequest));
        response.setMessage("Image updated successfully");

        return response;
    }
    
    @ApiOperation(
            value = "List of smart groups",
            notes = "This API will list down all smart groups that list down all data.")
    @GetMapping(
            value = "/getSmartGroup",
            produces = APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<SmartGroupsResponse> fetchsmartGroupslist(
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken,
            @RequestParam(value = APIParamKeys.SMART_GROUP, required = true) String smartGroupByName) {

      BaseResponseDTO<SmartGroupsResponse> baseResponseDTO = new BaseResponseDTO<>();
      SmartGroupsResponse responseDTO = userGroupService.getSmartGroupListByName(smartGroupByName);
      baseResponseDTO.setData(responseDTO); 
      return baseResponseDTO;
    }
    
    @ApiOperation(
            value = "Sub List of Contact of smart groups",
            notes = "This API will list down all smartgroups Contact that list down all data.")
    @GetMapping(
            value = "/getContactsmartGroup",
            produces = APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<ContactSmartGroupResponse> fetchingContactBysmartGroup(
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken,
            @RequestParam(value = APIParamKeys.SMART_GROUP, required = true) String smartGroupByName,
            @RequestParam(value = APIParamKeys.CONTACT_SMART_GROUP, required = true) String contactBysmartGroup ) {

      BaseResponseDTO<ContactSmartGroupResponse> baseResponseDTO = new BaseResponseDTO<>();
      ContactSmartGroupResponse responseDTO = userGroupService.getAllContactBysmartGroup(smartGroupByName,contactBysmartGroup);
      baseResponseDTO.setData(responseDTO); 
      return baseResponseDTO;
    }


}
