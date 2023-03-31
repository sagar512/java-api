package com.peopleapp.controller;

import com.peopleapp.configuration.LocaleMessageReader;
import com.peopleapp.constant.APIParamKeys;
import com.peopleapp.constant.MessageConstant;
import com.peopleapp.dto.ActivityContactsAPIParamData;
import com.peopleapp.dto.UserActivityData;
import com.peopleapp.dto.requestresponsedto.*;
import com.peopleapp.service.UserActivityService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;

@RestController
@RequestMapping(value = "/v1.0/activity/api")
@Api(value = "Activity", tags = "Activity related operations")
public class UserActivityController {

    @Inject
    private UserActivityService userActivityService;

    @Inject
    private LocaleMessageReader message;

    private static final int ASCENDING_ORDER = 1;
    private static final int DESCENDING_ORDER = -1;
    private static final String DEFAULT_PAGE_NUMBER = "0";


    @ApiOperation(
            value = "List of user received activities",
            notes = "This API will list all activities initiated for user."
    )
    @GetMapping(
            value = "/activities",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<ActivityListResponse> userReceivedActivities(@RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken,
                                                                        @RequestParam(value = "page", defaultValue = "0", required = false) int page,
                                                                        @RequestParam(value = "size", defaultValue = "20", required = false) int size) {

        ActivityListResponse receivedRequestList = userActivityService.getActivitiesCreatedForUser(page, size);
        BaseResponseDTO<ActivityListResponse> baseResponseDTO = new BaseResponseDTO<>();
        baseResponseDTO.setData(receivedRequestList);
        return baseResponseDTO;
    }

    @ApiIgnore
    @ApiOperation(
            value = "List of user initiated activities",
            notes = "This API will list all activities initiated by the session user."
    )
    @GetMapping(
            value = "/activities/sent",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<ActivityListResponse> userSentRequests(@RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken,
                                                                  @RequestParam(value = "page", defaultValue = "0", required = false) Integer page,
                                                                  @RequestParam(value = "size", defaultValue = "10", required = false) Integer size) {

        ActivityListResponse sentRequestList = userActivityService.getActivitiesCreatedByUser(page, size);
        BaseResponseDTO<ActivityListResponse> baseResponseDTO = new BaseResponseDTO<>();
        baseResponseDTO.setData(sentRequestList);
        return baseResponseDTO;
    }

    @ApiIgnore
    @ApiOperation(
            value = "List of user received activities but only actionable",
            notes = "This API will list all actionable activities initiated for user."
    )
    @GetMapping(
            value = "/activities/received",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<ActivityListResponse> userReceivedActionableActivities(@RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken,
                                                                  @RequestParam(value = "page", defaultValue = "0", required = false) Integer page,
                                                                  @RequestParam(value = "size", defaultValue = "50", required = false) Integer size) {

        ActivityListResponse ReceivedRequestList = userActivityService.getActionableActivitiesCreatedForUser(page, size);
        BaseResponseDTO<ActivityListResponse> baseResponseDTO = new BaseResponseDTO<>();
        baseResponseDTO.setData(ReceivedRequestList);
        return baseResponseDTO;
    }

    @ApiIgnore
    @ApiOperation(
            value = "Clear activities",
            notes = "This API clears all the activities or only those specified."
    )
    @PostMapping(
            value = "/activities/clear-all",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO clearActivities(@RequestBody ClearActivityRequest clearActivityRequest,
                                           @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        userActivityService.clearActivity(clearActivityRequest);
        BaseResponseDTO baseResponseDTO = new BaseResponseDTO();
        baseResponseDTO.setMessage(message.get(MessageConstant.CLEAR_USER_ACTIVITY));
        return baseResponseDTO;
    }

    @ApiOperation(
            value = "Cancel activity",
            notes = "<div>\n" + "\n" + "This API is used by users to cancel any activity initiated by them.\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                             | Error Code |\n" +
                    "| -------------------------------------- | ---------- |\n" +
                    "| If trying to cancel in-valid activity. | 826        |"
    )
    @PostMapping(
            value = "/request/cancel",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<CancelRequestResponseDTO> cancelRequests(@RequestBody CancelRequestDTO requestDTO,
                                          @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {


        BaseResponseDTO<CancelRequestResponseDTO> baseResponseDTO = new BaseResponseDTO<>();
        baseResponseDTO.setData(userActivityService.cancelActivity(requestDTO));
        baseResponseDTO.setMessage(message.get(MessageConstant.REQUEST_CANCELLED));
        return baseResponseDTO;
    }

    @ApiIgnore
    @ApiOperation(
            value = "Ignore activity",
            notes = "<div>\n" + "\n" + "This API is used by users to ignore any activity they received.\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                             | Error Code |\n" +
                    "| -------------------------------------- | ---------- |\n" +
                    "| If trying to ignore in-valid activity. | 826        |"
    )
    @PostMapping(
            value = "/request/ignore",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public BaseResponseDTO ignoreRequests(@Validated @RequestBody IgnoreRequestDTO ignoreRequest,
                                          @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        userActivityService.ignoreActivity(ignoreRequest);

        BaseResponseDTO baseResponse = new BaseResponseDTO();
        baseResponse.setMessage(message.get(MessageConstant.REQUESTS_IGNORED_SUCCESSFULLY));

        return baseResponse;
    }

    @ApiIgnore
    @ApiOperation(
            value = "Delete activity",
            notes = "<div>\n" + "\n" + "This API is used by users to delete activity.\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                             | Error Code |\n" +
                    "| -------------------------------------- | ---------- |\n" +
                    "| If trying to delete in-valid activity. | 826        |"
    )
    @PostMapping(
            value = "/activity/delete",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public BaseResponseDTO deleteActivity(@Validated @RequestBody DeleteActivityRequest deleteActivityRequest,
                                          @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        userActivityService.deleteActivity(deleteActivityRequest);

        BaseResponseDTO baseResponse = new BaseResponseDTO();
        baseResponse.setMessage("Deleted successfully.");

        return baseResponse;
    }

    @ApiIgnore
    @ApiOperation(
            value = "List of location share activity performed by user",
            notes = "This API will list out the details of location shared by user."
    )
    @GetMapping(
            value = "/shared-location-with-others",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<SharedLocationWithOthersResponse> getSharedLocationWithOthers(@RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken,
                                                                                         @RequestParam(value = APIParamKeys.PAGE_NUMBER, defaultValue = "0") Integer pageNumber,
                                                                                         @RequestParam(value = APIParamKeys.PAGE_SIZE, defaultValue = "10") Integer pageSize) {

        SharedLocationWithOthersResponse sharedLocation = userActivityService.getActiveLocationSharedWithOthers(pageNumber, pageSize);
        BaseResponseDTO<SharedLocationWithOthersResponse> response = new BaseResponseDTO<>();
        response.setData(sharedLocation);
        return response;
    }

    @ApiIgnore
    @ApiOperation(
            value = "List of location shared with user",
            notes = "This API will list out the details of location shared with user."
    )
    @GetMapping(
            value = "/shared-location-with-me",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<SharedLocationWithMeResponse> getSharedLocationWithMe(@RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken,
                                                                                 @RequestParam(value = APIParamKeys.PAGE_NUMBER, defaultValue = "0") Integer pageNumber,
                                                                                 @RequestParam(value = APIParamKeys.PAGE_SIZE, defaultValue = "10") Integer pageSize) {

        SharedLocationWithMeResponse sharedLocation = userActivityService.getActiveLocationSharedWithMe(pageNumber, pageSize);
        BaseResponseDTO<SharedLocationWithMeResponse> response = new BaseResponseDTO<>();
        response.setData(sharedLocation);
        return response;
    }

    @ApiIgnore
    @ApiOperation(
            value = "List of contact share activity by user",
            notes = "This API gets details of contacts shared by user with others"
    )
    @GetMapping(
            value = "/shared-contact-with-others",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<SharedContactWithOthersResponse> getSharedContactWithOthers(@RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken,
                                                                                       @RequestParam(value = APIParamKeys.PAGE_NUMBER, defaultValue = "0") Integer pageNumber,
                                                                                       @RequestParam(value = APIParamKeys.PAGE_SIZE, defaultValue = "10") Integer pageSize) {

        SharedContactWithOthersResponse sharedContact = userActivityService.getSharedContactWithOthers(pageNumber, pageSize);
        BaseResponseDTO<SharedContactWithOthersResponse> response = new BaseResponseDTO<>();
        response.setData(sharedContact);
        return response;
    }

    @ApiIgnore
    @ApiOperation(
            value = "List of contacts shared with user",
            notes = "This API gets all the contacts shared with user"
    )
    @GetMapping(
            value = "/shared-contact-with-me",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<SharedContactWithMeResponse> getSharedContactWithMe(@RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken,
                                                                               @RequestParam(value = APIParamKeys.PAGE_NUMBER, defaultValue = "0") Integer pageNumber,
                                                                               @RequestParam(value = APIParamKeys.PAGE_SIZE, defaultValue = "10") Integer pageSize) {

        SharedContactWithMeResponse sharedContact = userActivityService.getSharedContactWithMe(pageNumber, pageSize);
        BaseResponseDTO<SharedContactWithMeResponse> response = new BaseResponseDTO<>();
        response.setData(sharedContact);
        return response;
    }

    @ApiIgnore
    @ApiOperation(
            value = "Get Activity details by activity id",
            notes = "<div>\n" + "\n" + "This API is used to get details of a specific activity.\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                                       | Error Code |\n" +
                    "| ------------------------------------------------ | ---------- |\n" +
                    "| If trying to fetch details of in-valid activity. | 1002       |"
    )
    @GetMapping(
            value = "/details/{activityId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<ActivityListResponse> getActivityDetailsById(@RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken,
                                                                        @PathVariable("activityId") String activityId) {

        List<UserActivityData> activityDataList = userActivityService.getActivityDetailsByActivityId(activityId);
        ActivityListResponse activityListResponse = new ActivityListResponse();
        activityListResponse.setUserActivityList(activityDataList);
        BaseResponseDTO<ActivityListResponse> response = new BaseResponseDTO<>();
        response.setData(activityListResponse);

        return response;
    }

    /**
     *
     * @param sessionToken
     * @param searchString
     * @param activityId
     * @param initiatorId
     * @param receiverId
     * @param fNameOrder     -> 1 = Ascending, -1 = Descending, default = 1
     * @param lNameOrder     -> 1 = Ascending, -1 = Descending, default = 1
     * @param lNamePreferred -> default = false
     * @param pageNumber     -> default = 0
     * @param pageSize       -> max size per page 500
     * @return
     */
    @ApiIgnore
    @ApiOperation(
            value = "List of activity contacts",
            notes = "<div>\n" + "\n" +
                    "This API is used to fetch all or search for specific activity contacts\n" +
                    "based on either activity id or by receiver and initiator id. This API is\n" +
                    "paginated with **default page size 500(MAX is 500) and page number 0.**\n" + "\n" + "</div>\n" + "\n" +
                    "<div>\n" + "\n" + "**Note: Activity contacts are the contacts involved by activity(share\n" +
                    "contact and introduction).**\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                                       | Error Code |\n" +
                    "| ------------------------------------------------ | ---------- |\n" +
                    "| If trying to fetch details of in-valid activity. | 1002       | "
    )
    @GetMapping(
            value = "/activity-contacts",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<ActivityContactsResponseDTO> getActivityContactsById(
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken,
            @RequestParam(value = APIParamKeys.SEARCH_STRING, required = false, defaultValue = "") String searchString,
            @RequestParam(value = APIParamKeys.ACTIVITY_ID, required = false, defaultValue = "-1") String activityId,
            @RequestParam(value = APIParamKeys.INITIATOR_ID, required = false, defaultValue = "-1") String initiatorId,
            @RequestParam(value = APIParamKeys.RECEIVER_ID, required = false, defaultValue = "-1") String receiverId,
            @RequestParam(value = APIParamKeys.FNAME_SORT_ORDER, required = false, defaultValue = "1")
            @Min(DESCENDING_ORDER) @Max(ASCENDING_ORDER) Integer fNameOrder,
            @RequestParam(value = APIParamKeys.LNAME_SORT_ORDER, required = false, defaultValue = "1")
            @Min(DESCENDING_ORDER) @Max(ASCENDING_ORDER) Integer lNameOrder,
            @RequestParam(value = APIParamKeys.LAST_NAME_PREFERRED, required = false, defaultValue = "false")
            @Min(DESCENDING_ORDER) @Max(ASCENDING_ORDER) Boolean lNamePreferred,
            @RequestParam(value = APIParamKeys.PAGE_NUMBER, defaultValue = DEFAULT_PAGE_NUMBER) Integer pageNumber,
            @RequestParam(value = APIParamKeys.PAGE_SIZE, defaultValue = "500") @Max(500)Integer pageSize) {

        ActivityContactsAPIParamData activityContactsAPIParamData = new ActivityContactsAPIParamData();
        activityContactsAPIParamData.setSearchString(searchString);
        activityContactsAPIParamData.setActivityId(activityId);
        activityContactsAPIParamData.setInitiatorId(initiatorId);
        activityContactsAPIParamData.setReceiverId(receiverId);
        activityContactsAPIParamData.setFNameOrder(fNameOrder);
        activityContactsAPIParamData.setLNameOrder(lNameOrder);
        activityContactsAPIParamData.setLNamePreferred(lNamePreferred);
        activityContactsAPIParamData.setPageNumber(pageNumber);
        activityContactsAPIParamData.setPageSize(pageSize);

        ActivityContactsResponseDTO activityContactsResponseDTO =
                userActivityService.getActivityContactsByActivityId(activityContactsAPIParamData);

        BaseResponseDTO<ActivityContactsResponseDTO> baseResponseDTO = new BaseResponseDTO<>();
        baseResponseDTO.setData(activityContactsResponseDTO);

        return baseResponseDTO;
    }

    @ApiIgnore
    @ApiOperation(
            value = "Edit shared contact activity",
            notes = "<div>\n" + "\n" + "This API can be called in 3 cases.\n" + "\n" + "</div>\n" + "\n" +
                    "1.  Edit single or multiple contact to remove it by initiator or receiver.\n" +
                    "2.  Remove all shared contacts by receiver.\n" + "3.  Stop sharing all shared contacts by initiator.\n" +
                    "\n" + "<div>\n" + "\n" + "**Note:**\n" + "\n" + "</div>\n" + "\n" + "<div>\n" + "\n" +
                    "In case 1, API request should have 'activitySubIdList' and must not be empty.\n" + "\n" + "</div>\n" +
                    "\n" + "<div>\n" + "\n" + "In case 2&3, API request should have 'activityIdList' and must not be\n" +
                    "empty.\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                                       | Error Code |\n" +
                    "| ------------------------------------------------ | ---------- |\n" +
                    "| If trying to fetch details of in-valid activity. | 1002       |"
    )
    @PostMapping(
            value = "/shared-contact/edit",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO editSharedContactActivity(@RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken,
                                                     @RequestBody EditSharedContactRequest editSharedContactRequest) {

        userActivityService.editSharedContactActivity(editSharedContactRequest);
        BaseResponseDTO response = new BaseResponseDTO();
        response.setMessage("Edited successfully.");

        return response;
    }

}
