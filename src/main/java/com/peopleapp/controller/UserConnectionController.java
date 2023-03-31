package com.peopleapp.controller;

import com.peopleapp.configuration.LocaleMessageReader;
import com.peopleapp.constant.APIParamKeys;
import com.peopleapp.constant.MessageConstant;
import com.peopleapp.dto.ActivityDetails;
import com.peopleapp.dto.requestresponsedto.*;
import com.peopleapp.dto.requestresponsedto.ContactContactIDRequest;
import com.peopleapp.dto.requestresponsedto.RestoreCountResponse;
import com.peopleapp.exception.BadRequestException;
import com.peopleapp.service.TagService;
import com.peopleapp.service.UserConnectionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/v1.0/connection/api")
@Api(value = "Connections", tags = "Connection related operations")
public class UserConnectionController {

    @Inject
    private LocaleMessageReader messages;

    @Inject
    @Lazy
    private UserConnectionService userConnectionService;

    @Lazy
    @Inject
    private TagService tagService;

    private static final int ASCENDING_ORDER = 1;
    private static final int DESCENDING_ORDER = -1;
    private static final String DEFAULT_PAGE_NUMBER = "0";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @ApiOperation(
            value = "Sync contacts",
            notes = "All native contacts from user device will be imported to Watu Server."
    )
    @PostMapping(
            value = "/contacts/sync",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<ContactSyncResponseDTO> contactInformationSync(@Validated @RequestBody ContactSyncRequestDTO contactSyncDTO,
                                                                          @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        BaseResponseDTO<ContactSyncResponseDTO> response = new BaseResponseDTO<>();
        ContactSyncResponseDTO syncContactResponse = userConnectionService.syncContacts(contactSyncDTO);
        response.setData(syncContactResponse);
        response.setMessage(messages.get(MessageConstant.CONTACTS_SAVED_SUCCESSFULLY));

        return response;
    }

    @ApiOperation(
            value = "Restore contacts",
            notes = "All native contacts from user device will be export to Watu application."
    )
    @GetMapping(
            value = "/contacts/restore"
    )
    public BaseResponseDTO<ContactRestoreListDTO> contactInformationRestore(
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken,
            @RequestParam(value = APIParamKeys.PAGE_NUMBER, defaultValue = "0") Integer pageNumber,
            @RequestParam(value = APIParamKeys.PAGE_SIZE, defaultValue = "1000") Integer pageSize,
            @ApiParam("false means ignore pageNumber & pageSize params") @RequestParam(value = APIParamKeys.IS_PAGEABLE, defaultValue = "true") boolean isPageable
    ) {
        BaseResponseDTO<ContactRestoreListDTO> response = new BaseResponseDTO<>();
        ContactRestoreListDTO restoreContacts = userConnectionService.restoreContacts(pageNumber, pageSize, isPageable);
        response.setData(restoreContacts);

        return response;
    }

    @ApiOperation(
            value = "Count of restore contacts",
            notes = "All native count of contacts from user device will be export to Watu application."
    )
    @GetMapping(
            value = "/contacts/restore-count"
    )
    public BaseResponseDTO<RestoreCountResponse> contactInformationRestoreCount(
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken
    ) {
        BaseResponseDTO<RestoreCountResponse> response = new BaseResponseDTO<>();
        response.setData(userConnectionService.getContactBackupDetail());
        return response;
    }

    @ApiOperation(
            value = "Send connection request",
            notes = "<div>\n" + "\n" + "This API sends connection request to contacts, registered and non-registered with Watu app.\n" +
                    "\n" + "</div>\n" + "\n" + "<div>\n" + "\n" + "**API cases for sending request**\n" + "\n" + "</div>\n" + "\n" +
                    "  - **Connection Id flow (SINGLE)**  \n" +
                    "    User is having contact in their contact list, connection id and number of that contact will be specified in request body. If in case\n" +
                    "    of contact not part of WATU app an SMS will be sent or else activity will be created for the contact.\n" +
                    "  - **Connection Id flow (BULK)**  \n" +
                    "    This is similar to 'SINGLE operation' except that it takes list of connection id and number details in request body and sends request\n" +
                    "    to multiple contacts in one single call.\n" +
                    "  - **User id flow**  \n" +
                    "    User will be able to send request to any WATU user by using their userId.\n" +
                    "  - **User id flow with name**  \n" +
                    "    This is similar to previous case but before sending request a contact will be created and added to user contact list.\n" +
                    "  - **Activity Id and Activity subId flow**  \n" +
                    "    This is used to send request to contacts that were shared by other users\n" + "\n" + "<div>\n" + "\n" +
                    "**Note: If connection request is also initiated by the contact then in such cases instead of " +
                    "new connection request, will accept the previous request.**\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                                                                                  | Error Code |\n" +
                    "| ------------------------------------------------------------------------------------------- | ---------- |\n" +
                    "| If connection is in-valid.(case1)                                                           | 807        |\n" +
                    "| If shared profile is in-valid.(case1)                                                       | 810        |\n" +
                    "| If userId does not belong to any Watu user.(case 3,4)                                       | 815        |\n" +
                    "| If request already sent.(case 1,3)                                                          | 820        |\n" +
                    "| If already connected.(case 1,3)                                                             | 821        |\n" +
                    "| If user has blocked the contact to which they are trying to send request.(case 2,5)         | 829        |\n" +
                    "| If user has blocked the few of the contact to which they are trying to send request.(case2) | 829        |\n" +
                    "| If trying to send request to self.(case 1,3)                                                | 830        |\n" +
                    "| If sending request to non U.S.A/Canada number.(case 1,3,5)                                  | 1007       |"
    )
    @PostMapping(
            value = "/connection-request/send",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<SendConnectionRequestResponse> sendConnectionRequest(@Validated @RequestBody SendConnectionRequest requestDTO,
                                                                                @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {
        BaseResponseDTO<SendConnectionRequestResponse> baseResponseDTO = new BaseResponseDTO<>();
        SendConnectionRequestResponse responseDTO = userConnectionService.sendConnectionRequest(requestDTO);
        baseResponseDTO.setData(responseDTO);
        baseResponseDTO.setMessage(messages.get(MessageConstant.CONNECTION_REQUEST_SENT));
        return baseResponseDTO;
    }

    @GetMapping(
            value = "/check-bluetooth-connection/{bluetoothToken}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<BluetoothConnectionDetailsResponseDTO> getBluetoothConnectionDetails(
            @PathVariable("bluetoothToken") String bluetoothToken,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside UserConnection Controller -> getBluetoothConnectionDetails");
        BaseResponseDTO<BluetoothConnectionDetailsResponseDTO> response = new BaseResponseDTO<>();
        BluetoothConnectionDetailsResponseDTO
                bluetoothConnectionDetailsResponseDTO = userConnectionService.checkBluetoothConnectionDetails(bluetoothToken);

        if (bluetoothConnectionDetailsResponseDTO.getConnectionId() == null && bluetoothConnectionDetailsResponseDTO.getBluetoothTokenUserId() == null) {
            //response.setMessage("Both user is already connected or pending.");
        	throw new BadRequestException("");
        } else {
            response.setData(bluetoothConnectionDetailsResponseDTO);
        }

        return response;
    }

    @ApiOperation(
            value = "Favourite/Un-favourite connections",
            notes = "<div>\n" + "\n" + "This API will be used to mark connections as favourite/un-favourite.\n" + "\n" +
                    "</div>\n" + "\n" + "<div>\n" + "\n" + "**Note:** Sequence numbers for favourite contact will be auto allotted.\n" +
                    "\n" + "</div>\n" + "\n" +
                    "| Error Case                       | Error Code |\n" +
                    "| -------------------------------- | ---------- |\n" +
                    "| If connections are all in-valid. | 807        |"
    )
    @PostMapping(
            value = "/favourite/update",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<FavouriteContactsResponseDTO> updateFavourite(@Validated @RequestBody UpdateFavouriteRequestDTO updateFavouriteRequest,
                                                                         @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {


        BaseResponseDTO<FavouriteContactsResponseDTO> baseResponse = new BaseResponseDTO<>();
        baseResponse.setData(userConnectionService.setFavouriteForContact(updateFavouriteRequest));
        baseResponse.setMessage(messages.get(MessageConstant.FAVOURITE_SET));
        return baseResponse;
    }

    @ApiOperation(
            value = "List of connections",
            notes = "<div>\n" + "\n" + "User contacts will be fetched. This API is paginated **default page size\n" +
                    "1000 and page number 0.**\n" + "\n" + "</div>\n" + "\n" + "<div>\n" + "\n" + "**Note:** By specifying" +
                    " lastSyncedTime response will contain contacts which are created after the mentioned time else it would return entire\n" +
                    "list of contacts\n" + "\n" + "</div>"
    )
    @GetMapping(
            value = "/user-connection-list",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<FetchConnectionListResponseDTO> connectionsList(
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken,
            @RequestParam(value = APIParamKeys.LAST_SYNCED_TIME, required = false) @DateTimeFormat(iso =
                    DateTimeFormat.ISO.DATE_TIME) DateTime lastSyncedTime,
            @RequestParam(value = APIParamKeys.PAGE_NUMBER, defaultValue = "0") Integer pageNumber,
            @RequestParam(value = APIParamKeys.PAGE_SIZE, defaultValue = "1000") Integer pageSize,
            @RequestParam(value = APIParamKeys.RETURN_ONLY_META, required = false) boolean returnOnlyMeta,
            @RequestParam(value = APIParamKeys.SORT_BY, defaultValue = "firstName") String sortBy ){
    	
        BaseResponseDTO<FetchConnectionListResponseDTO> baseResponseDTO = new BaseResponseDTO<>();
        FetchConnectionListResponseDTO responseDTO = userConnectionService.getConnectionList(lastSyncedTime,
                pageNumber, pageSize, returnOnlyMeta, sortBy);
        baseResponseDTO.setData(responseDTO);
        return baseResponseDTO;
    }
    
    //Delete all Contact By People User _id
    @DeleteMapping(
            value = "/delete-all",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<?> deleteAllContact(@RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken){
    	BaseResponseDTO<?> baseResponseDTO=new BaseResponseDTO<>();
    	userConnectionService.deleteAllContactByPeopleUId();
    	baseResponseDTO.setMessage("All contact Delete successfully");
    	return baseResponseDTO;
    }
    
    
    @ApiIgnore
    @ApiOperation(
            value = "Change shared privacy profile",
            notes = "<div>\n" + "\n" + "This API is used to change the privacy profile shared with the\n" + "connection.\n" +
                    "\n" + "</div>\n" + "\n" +
                    "| Error Case                       | Error Code |\n" +
                    "| -------------------------------- | ---------- |\n" +
                    "| If connections are all in-valid. | 807        |"
    )
    @PostMapping(
            value = "/privacy-profile/change",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO changePrivacyProfile(@Validated @RequestBody ChangePrivacyProfileRequestDTO changePrivacyProfileRequest,
                                                @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        userConnectionService.changePrivacyProfileForConnection(changePrivacyProfileRequest);

        BaseResponseDTO baseResponseDTO = new BaseResponseDTO();
        baseResponseDTO.setMessage(messages.get(MessageConstant.PRIVACY_PROFILE_CONNECTION));
        return baseResponseDTO;
    }

    @ApiIgnore
    @ApiOperation(
            value = "Multi introduction",
            notes = "<div>\n" + "\n" + "This API is used to introduce contacts to each other.\n" + "\n" +
                    "**Used for Many to Many introduction. Contacts on both side must be registered with Watu app.**\n" + "\n" + "</div>")
    @PostMapping(
            value = "/multi-introduction/send",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO sendMultiIntroRequest(@Validated @RequestBody SendMultiIntroRequestDTO sendMultiIntroRequestDTO,
                                                 @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        userConnectionService.introduceContactToEachOtherRequest(sendMultiIntroRequestDTO);
        BaseResponseDTO baseResponseDTO = new BaseResponseDTO();
        baseResponseDTO.setMessage(messages.get(MessageConstant.INTRODUCTION_REQUEST_SENT));
        return baseResponseDTO;
    }

    @ApiIgnore
    @ApiOperation(
            value = "Introduce contacts",
            notes = "<div>\n" + "\n" +"This API is used to introduce contacts to each other.\n" + "\n" +
                    "**Used for both One to One and One to Many introduction.**\n" + "\n" + "</div>"
    )
    @PostMapping(
            value = "/single-introduction/send",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO sendSingleIntroRequest(@Validated @RequestBody SendSingleIntroRequestDTO sendSingleIntroRequestDTO,
                                                  @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        userConnectionService.introduceContactRequest(sendSingleIntroRequestDTO);
        BaseResponseDTO baseResponseDTO = new BaseResponseDTO();
        baseResponseDTO.setMessage(messages.get(MessageConstant.INTRODUCTION_REQUEST_SENT));
        return baseResponseDTO;
    }

    @ApiIgnore
    @ApiOperation(
            value = "Request for more information",
            notes = "<div>\n" + "\n" + "User will be requesting their 'CONNECTED' contacts for more information \n" +
                    "to be shared.\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                 | Error Code |\n" +
                    "| -------------------------- | ---------- |\n" +
                    "| If connection is in-valid. | 807        "
    )
    @PostMapping(
            value = "/more-info/request",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<ActivityDetails> sendRequestForMoreInfo(@Validated @RequestBody RequestMoreInfoDTO requestMoreInfoDTO,
                                                                   @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        ActivityDetails activityDetails = userConnectionService.moreInfoRequest(requestMoreInfoDTO);
        BaseResponseDTO<ActivityDetails> baseResponseDTO = new BaseResponseDTO<>();
        baseResponseDTO.setMessage(messages.get(MessageConstant.MORE_INFO_REQUEST_CREATED_SUCCESSFULLY));
        baseResponseDTO.setData(activityDetails);
        return baseResponseDTO;
    }

    @ApiOperation(value = "Add contact(s) to group",
            notes = "<div>\n" + "\n" + "User will be able to add contact(s) to a group.\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                       | Error Code |\n" +
                    "| -------------------------------- | ---------- |\n" +
                    "| If connections are all in-valid. | 807        |\n" +
                    "| If a group is in-valid.          | 1000       |")
    @PostMapping(
            value = "/group/add",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<AddContactsToGroupResponseDTO> addContactsInGroup(
            @Valid @RequestBody AddContactsToGroupRequestDTO addContactsToGroupRequestDTO,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        BaseResponseDTO<AddContactsToGroupResponseDTO> baseResponseDTO = new BaseResponseDTO<>();
        AddContactsToGroupResponseDTO addContactsToGroupResponse = userConnectionService.addContactToGroup(addContactsToGroupRequestDTO);
        baseResponseDTO.setData(addContactsToGroupResponse);
        baseResponseDTO.setMessage(messages.get(MessageConstant.CONTACTS_ADDED_TO_GROUP_SUCCESSFULLY));

        return baseResponseDTO;
    }

    @ApiOperation(value = "Remove contact(s) from group",
            notes = "<div>\n" + "\n" + "User will be able to remove contact(s) from existing group.\n" + "\n" + "</div>\n" + "\n" +
            "| Error Case                       | Error Code |\n" +
            "| -------------------------------- | ---------- |\n" +
            "| If connections are all in-valid. | 807        |\n" +
            "| If a group is in-valid.          | 1000       |")
    @PostMapping(
            value = "/group/remove",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<ContactListDTO> removeContactsFromGroup(
            @Valid @RequestBody RemoveContactsFromGroupRequestDTO removeContactsFromGroupRequestDTO,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        ContactListDTO contactListDTO = userConnectionService.removeContactFromGroup(removeContactsFromGroupRequestDTO);
        BaseResponseDTO<ContactListDTO> baseResponseDTO = new BaseResponseDTO<>();
        baseResponseDTO.setData(contactListDTO);
        baseResponseDTO.setMessage(messages.get(MessageConstant.CONTACTS_REMOVED_FROM_GROUP_SUCCESSFULLY));

        return baseResponseDTO;
    }

    @ApiOperation(value = "Edit static data",
            notes = "<div>\n" + "\n" + "User will be able to edit static details of contact either in 'CONNECTED' or " +
                    "'NOT-CONNECTED' state.\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                                    | Error Code |\n" +
                    "| --------------------------------------------- | ---------- |\n" +
                    "| If connections being edited are all in-valid. | 817        |\n")
    @PostMapping(
            value = "/static-data/update",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<EditStaticDataResponseDTO> editStaticData(
            @Valid @RequestBody EditStaticDataRequestDTO editStaticDataRequest,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        BaseResponseDTO<EditStaticDataResponseDTO> baseResponseDTO = new BaseResponseDTO<>();
        baseResponseDTO.setData(userConnectionService.updateContactStaticData(editStaticDataRequest));
        baseResponseDTO.setMessage(messages.get(MessageConstant.CONNECTION_STATIC_DATA_EDITED_SUCCESSFULLY));

        return baseResponseDTO;
    }

    @ApiOperation(value = "Accept connection request",
            notes = "<div>\n" + "\n" + "With this API user can accept the connection request sent to them.\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                                          | Error Code |\n" +
                    "| --------------------------------------------------- | ---------- |\n" +
                    "| If request initiator is no longer part of Watu app. | 908        |\n" +
                    "| If request initiator is blocked by the user.        | 1004       |\n" +
                    "| If shared profile is in-valid.                      | 810        |\n" +
                    "| If already connected.                               | 821        |")
    @PostMapping(
            value = "/connection-request/accept",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<AcceptConnectionResponseDTO> acceptConnectionRequest(
            @Valid @RequestBody AcceptConnectionRequestDTO acceptConnectionRequest,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        AcceptConnectionResponseDTO acceptConnectionResponse = userConnectionService.acceptConnectionRequest(acceptConnectionRequest);
        BaseResponseDTO<AcceptConnectionResponseDTO> response = new BaseResponseDTO<>();
        response.setData(acceptConnectionResponse);
        response.setMessage(messages.get(MessageConstant.ACCEPT_CONNECTION_REQUEST));

        return response;
    }

    @ApiOperation(value = "Delete contact",
            notes = "<div>\n" + "\n" + "This API will delete the contact\n" + "\n" + "</div>\n" + "\n" + "<div>\n" + "\n" +
                    "**Note:**\n" + "\n" + "</div>\n" + "\n" + "  - Contacts will be removed from groups if it was added.\n" +
                    "  - If the contact being deleted is 'CONNECTED' then all activities w.r.t connections will be expired " +
                    "and contact state for the other user will be downgraded.\n" + "\n" +
                    "| Error Case                              | Error Code |\n" +
                    "| --------------------------------------- | ---------- |\n" +
                    "| If contacts being deleted are in-valid. | 817        |")
    @DeleteMapping(
            value = "/delete",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<DeleteContactResponse> deleteContact(
            @Valid @RequestBody DeleteContactRequest deleteContactRequest,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        DeleteContactResponse deleteContactResponse = userConnectionService.deleteContact(deleteContactRequest);
        BaseResponseDTO<DeleteContactResponse> response = new BaseResponseDTO<>();
        response.setData(deleteContactResponse);
        response.setMessage("Contact deleted successfully");

        return response;
    }

    @ApiIgnore
    @ApiOperation(value = "Share contact",
            notes = "<div>\n" + "\n" + "This API is used to share user's contacts with 'CONNECTED' contacts.\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                                                                     | Error Code |\n" +
                    "| ------------------------------------------------------------------------------ | ---------- |\n" +
                    "| If connections are in-valid.                                                   | 807        |\n" +
                    "| If some connections to whom the contacts are being shared are blocked by user. | 828        |\n" +
                    "| If all connections to whom the contacts are being shared are blocked by user.  | 829        |")
    @PostMapping(
            value = "/share",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO shareContact(
            @Valid @RequestBody ShareContactRequest shareContactRequest,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        BaseResponseDTO baseResponseDTO = new BaseResponseDTO();
        baseResponseDTO.setMessage(userConnectionService.shareContact(shareContactRequest));

        return baseResponseDTO;
    }

    @ApiIgnore
    @ApiOperation(value = "Share location",
            notes = "This API is used to share device location with 'CONNECTED' contacts. If the location was shared to " +
                    "contact already and if it is still active, sharing the location again will update the existing activity.")
    @PostMapping(
            value = "/location/share",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO shareLocation(
            @Valid @RequestBody ShareLocationRequest shareLocationRequest,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        userConnectionService.shareLocation(shareLocationRequest);
        BaseResponseDTO response = new BaseResponseDTO();
        response.setMessage("Location shared successfully");

        return response;
    }

    @ApiIgnore
    @ApiOperation(value = "Update contact image", notes = "API is used for updating image url for the contact.")
    @PostMapping(
            value = "/image/update",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<UpdateContactImageResponse> updateImage(
            @Valid @RequestBody UpdateContactImageRequest updateImageRequest,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        List<String> updatedList = userConnectionService.updateContactImage(updateImageRequest);

        BaseResponseDTO<UpdateContactImageResponse> response = new BaseResponseDTO<>();
        UpdateContactImageResponse updateContactImageResponse = new UpdateContactImageResponse();
        updateContactImageResponse.setUpdatedConnectionIdList(updatedList);
        response.setData(updateContactImageResponse);
        response.setMessage("Image updated successfully");

        return response;
    }

    @ApiIgnore
    @ApiOperation(value = "Remove connection",
            notes = "<div>\n" + "\n" + "This API is used to downgrade connected contacts to 'NOT-CONNECTED' state,\n" +
            "and will not delete the contact. As the the connections are downgraded all activities w.r.t connection are expired.\n" +
            "\n" + "</div>\n" + "\n" +
            "| Error Case                   | Error Code |\n" +
            "| ---------------------------- | ---------- |\n" +
            "| If connections are in-valid. | 817        |")
    @DeleteMapping(
            value = "/remove",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<RemoveConnectionResponse> removeConnection(
            @Valid @RequestBody RemoveConnectionRequest removeConnectionRequest,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        RemoveConnectionResponse removeConnectionResponse = userConnectionService.removeConnection(removeConnectionRequest);
        BaseResponseDTO<RemoveConnectionResponse> response = new BaseResponseDTO<>();
        response.setData(removeConnectionResponse);
        response.setMessage(messages.get(MessageConstant.CONNECTION_REMOVED_SUCCESSFULLY));

        return response;
    }

    @ApiIgnore
    @ApiOperation(
            value = "Manage favourite connections",
            notes = "This API is used to re-order the favourite connections sequence."
    )
    @PostMapping(
            value = "/favourites/manage",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO manageFavourites(
            @Validated @RequestBody ManageFavouritesRequestDTO manageFavouritesRequestDTOList,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        List<String> updatedConnectionList =
                userConnectionService.manageFavouritesForContact(manageFavouritesRequestDTOList);
        BaseResponseDTO baseResponse = new BaseResponseDTO();
        baseResponse.setData(updatedConnectionList);
        return baseResponse;
    }

    /**
     * @param fNameOrder     -> 1 = Ascending, -1 = Descending, default = 1
     * @param lNameOrder     -> 1 = Ascending, -1 = Descending, default = 1
     * @param lNamePreferred -> default = false
     * @param pageNumber     -> default = 0
     * @param pageSize       -> Max size is 100 per page
     * @param sessionToken
     * @return
     */
    @ApiIgnore
    @ApiOperation(
            value = "List of favourite contacts",
            notes = "<div>\n" + "\n" + "This API fetches connection which are marked favourite by user. This is\n" +
                    "a paginated API with default **page size 100(max size 100) and page number 0.** .\n" + "\n" + "</div>\n" +
                    "\n" + "<div>\n" + "\n" + "For fNameOrder and lNameOrder : **1 = Ascending, -1 = Descending,\n" +
                    "default = 1**\n" + "\n" + "</div>"
    )
    @GetMapping(
            value = "/user-favourite-list",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO favouritesList(
            @RequestParam(value = APIParamKeys.FNAME_SORT_ORDER, defaultValue = "1")
            @Min(DESCENDING_ORDER) @Max(ASCENDING_ORDER) Integer fNameOrder,
            @RequestParam(value = APIParamKeys.LNAME_SORT_ORDER, defaultValue = "1")
            @Min(DESCENDING_ORDER) @Max(ASCENDING_ORDER) Integer lNameOrder,
            @RequestParam(value = APIParamKeys.LAST_NAME_PREFERRED, defaultValue = "false") Boolean lNamePreferred,
            @RequestParam(value = APIParamKeys.PAGE_NUMBER, defaultValue = DEFAULT_PAGE_NUMBER) Integer pageNumber,
            @RequestParam(value = APIParamKeys.PAGE_SIZE, defaultValue = "100") @Max(100) Integer pageSize,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        BaseResponseDTO<FetchFavouritesListResponseDTO> baseResponseDTO = new BaseResponseDTO<>();
        FetchFavouritesListResponseDTO responseDTO = userConnectionService.getFavouritesList(fNameOrder, lNameOrder,
                lNamePreferred, pageNumber, pageSize);
        baseResponseDTO.setData(responseDTO);
        return baseResponseDTO;
    }

    @ApiOperation(
            value = "Merge connections",
            notes = "This API is used to merge multiple contacts into one contact(master contact). If the merged " +
                    "contacts were part of any group then it will be replaced by the master contact."
    )
    @PostMapping(
            value = "/contacts/merge",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO mergeContacts(
            @Validated @RequestBody MergeContactsRequestDTO mergeContactsRequest,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken){

        logger.info("Inside UserConnection Controller -> mergeContacts");
        userConnectionService.mergeContacts(mergeContactsRequest);
        BaseResponseDTO baseResponseDTO = new BaseResponseDTO<>();
        baseResponseDTO.setMessage(messages.get(MessageConstant.MERGE_CONTACTS_SUCCESS));
        return baseResponseDTO;

    }

    @ApiOperation(
            value = "Connection details",
            notes = "<div>\n" + "\n" + "This API fetches connection details for given connection id.\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                     | Error Code |\n" +
                    "| ------------------------------ | ---------- |\n" +
                    "| If user connection is invalid. | 807        |"
    )
    @GetMapping(
            value = "/contact/detail/{connectionId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<ConnectionDetailsResponseDTO> getConnectionDetails(
            @PathVariable("connectionId") String connectionId,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside UserConnection Controller -> getConnectionDetails");
        BaseResponseDTO<ConnectionDetailsResponseDTO> baseResponseDTO = new BaseResponseDTO<>();
        baseResponseDTO.setData(userConnectionService.fetchConnectionDetails(connectionId));
        return baseResponseDTO;

    }

    @ApiIgnore
    @ApiOperation(
            value = "Retain/Discard deleted information",
            notes = "<div>\n" + "\n" + "User can retain or discard the information which was deleted from their\n" +
                    "connected contacts profile. To get notified about the deleted information it must be shared with user.\n" +
                    "\n" + "</div>\n" + "\n" +
                    "| Error Case                     | Error Code |\n" +
                    "| ------------------------------ | ---------- |\n" +
                    "| If user connection is invalid. | 817        |"
    )
    @PostMapping(
            value = "/contact/deleted-info",
            consumes = APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<DeletedInfoResponseDTO> deleteInfo(
            @Validated @RequestBody DeleteInfoRequestDTO requestDTO,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside UserConnection Controller -> deleteInfo");
        BaseResponseDTO<DeletedInfoResponseDTO> baseResponseDTO = new BaseResponseDTO<>();
        baseResponseDTO.setData(userConnectionService.deleteInfo(requestDTO));
        return baseResponseDTO;

    }
    
    @PostMapping(
            value = "/contact-id/update",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<ContactContactIDRequest> updateContactId(@RequestBody ContactContactIDRequest request,
                                                                         @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        BaseResponseDTO<ContactContactIDRequest> baseResponse = new BaseResponseDTO<>();
        baseResponse.setData((userConnectionService.updateContactId(request)));
        baseResponse.setMessage("Connection Contact Id successfully Updated");
        return baseResponse;
    }
    
    @PostMapping(
            value = "/makeContactidentical",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<DeleteContactRequest> makeContactidenticalUpdate(@Valid @RequestBody DeleteContactRequest contactRequest,
                                                                         @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        BaseResponseDTO<DeleteContactRequest> baseResponse = new BaseResponseDTO<>();
        baseResponse.setData((userConnectionService.IdenticalFlagUpdate(contactRequest)));
        baseResponse.setMessage("Connection Contact Identical flag successfully Updated");
        return baseResponse;
    }
}
