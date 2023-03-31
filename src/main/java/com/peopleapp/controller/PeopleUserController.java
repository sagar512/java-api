package com.peopleapp.controller;

import com.google.zxing.WriterException;
import com.peopleapp.configuration.LocaleMessageReader;
import com.peopleapp.constant.APIParamKeys;
import com.peopleapp.constant.MessageConstant;
import com.peopleapp.dto.CanadaAndUSAContactNumberDTO;
import com.peopleapp.dto.Coordinates;
import com.peopleapp.dto.requestresponsedto.*;
import com.peopleapp.service.OTPService;
import com.peopleapp.service.PeopleUserService;
import com.peopleapp.service.UserActivityService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;


import java.io.IOException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/v1.0/user/api")
@Api(value = "user", tags = "Watu user related operations")
public class PeopleUserController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final int ASCENDING_ORDER = 1;
    private static final int DESCENDING_ORDER = -1;
    private static final String DEFAULT_PAGE_NUMBER = "0";

    @Inject
    private PeopleUserService peopleUserService;

    @Inject
    private UserActivityService userActivityService;

    @Inject
    private OTPService otpService;

    @Inject
    private LocaleMessageReader messages;

    @ApiOperation(
            value = "Connect",
            notes = "<div>With this API user can sign-up/login to app.</div>\n" +
                    "<div>**Note:**</div>\n" +
                    "\n" +
                    "  - User can sign-up with USA or Canada number.**(No other country\n" +
                    "    numbers are supported)**\n" +
                    "  - User can login with either registered number or email.\n" +
                    "  - On successful validation of number/email OTP will be sent.\n" +
                    "\n" +
                    "| Error Case                                                     | Error Code |\n" +
                    "| -------------------------------------------------------------- | ---------- |\n" +
                    "| If user is trying to login with invalid or un-registered email   | 905        |\n" +
                    "| If user is trying to sign-up/login with invalid contact number | 601        |"
    )
    @PostMapping(
            value = "/connect",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<ConnectResponseDTO> connect(@Validated @RequestBody ConnectRequestDTO connectRequest) {

        ConnectResponseDTO connectResponseDTO = peopleUserService.connect(connectRequest);
        BaseResponseDTO<ConnectResponseDTO> baseResponseDTO= new BaseResponseDTO<>();
		baseResponseDTO.setData(connectResponseDTO);
	    baseResponseDTO.setMessage(messages.get(MessageConstant.OTP_SENT_SUCCESSFULLY));
        return baseResponseDTO;
    }

    @ApiOperation(
            value = "Verify OTP",
            notes = "<div>\n" + "\n" + "This API will be used to verify the OTP and create new session.\n" + "\n" +
                    "</div>\n" + "\n" + "<div>\n" + "\n" + "**API use cases:**\n" + "\n" + "</div>\n" + "\n" +
                    "  - Sign-up: New user account will be created and number is registered\n" +
                    "    to app.\n" +
                    "  - Login: Existing user will be logged in with new session.\n" +
                    "  - Change of primary number and re login.\n" + "\n" +
                    "| Error Case                                                | Error Code |\n" +
                    "| --------------------------------------------------------- | ---------- |\n" +
                    "| If OTP is invalid.                                        | 813        |\n" +
                    "| For any other use cases apart from those mentioned above. | 603        |"
    )
    @PostMapping(
            value = "/otp/verify",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<VerifyOTPResponseDTO> verifyOTP(
            @RequestBody VerifyOTPRequestDTO verifyOTPRequestDTO,
            @RequestHeader(value = APIParamKeys.TEMP_TOKEN) String tempToken,
            @RequestHeader(value = APIParamKeys.DEVICE_TYPE_ID, required = false) String deviceTypeID
    ) {

        VerifyOTPResponseDTO verifyOTPResponseDTO = peopleUserService.verifyOTP(verifyOTPRequestDTO, deviceTypeID);
        BaseResponseDTO<VerifyOTPResponseDTO> baseResponseDTO = new BaseResponseDTO<>();
        if (verifyOTPResponseDTO == null) {
            baseResponseDTO.setMessage(messages.get(MessageConstant.BLUETOOTH_TOKEN_ALREADY_EXIST));
        } else {
            baseResponseDTO.setMessage(messages.get(MessageConstant.USER_SESSION_CREATED_SUCCESSFULLY));
            baseResponseDTO.setData(verifyOTPResponseDTO);
        }

        return baseResponseDTO;
    }

    @ApiOperation(
            value = "Resend OTP",
            notes = "This API will resend the OTP to registered contact number."
    )
    @PostMapping(
            value = "/otp/resend",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<ResendOTPResponseDTO> resendOTP(@RequestHeader(value = APIParamKeys.TEMP_TOKEN) String tempToken) {

        ResendOTPResponseDTO resendOTPResponseDTO = otpService.resendOTP();
        BaseResponseDTO<ResendOTPResponseDTO> baseResponseDTO = new BaseResponseDTO<>();
        baseResponseDTO.setData(resendOTPResponseDTO);
        baseResponseDTO.setMessage(messages.get(MessageConstant.OTP_SENT_SUCCESSFULLY));
        return baseResponseDTO;

    }

    @ApiOperation(
            value = "Authenticate Email",
            notes = "<div>This API will send a verification mail to the email provided by user.</div>\n" + "\n" +
                    "| Error Case                               | Error Code |\n" +
                    "| ---------------------------------------- | ---------- |\n" +
                    "| If invalid email is being authenticated. | 602        |"
    )
    @PostMapping(
            value = "/email/authenticate"
    )
    public BaseResponseDTO authenticateEmail(
            @ApiParam(required = true, name = "valueId", value = "User email value Id") @RequestParam("valueId") String valueId,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside PeopleUserController->linkEmail");
        peopleUserService.authenticateEmail(valueId);
        BaseResponseDTO baseResponseDTO = new BaseResponseDTO();
        baseResponseDTO.setMessage(messages.get(MessageConstant.EMAIL_VERIFICATION_LINK_SENT_SUCCESSFULLY));
        return baseResponseDTO;

    }


    @ApiOperation(
            value = "Link primary email",
            notes = "<div>This API is used to link email as primary email while creating account.\n" +
                    "Verification email will be sent to the given mail id.</div>\n" +
                    "\n" +
                    "| Error Case                                                | Error Code |\n" +
                    "| --------------------------------------------------------- | ---------- |\n" +
                    "| If email being linked is registered to different account. | 805        |"
    )
    @PostMapping(
            value = "/primary-email/link"
    )
    public BaseResponseDTO linkEmail(
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken,
            @Validated @RequestBody LinkPrimaryEmailRequest linkPrimaryEmailRequest) {

        peopleUserService.linkPrimaryEmail(linkPrimaryEmailRequest.getEmailId());
        BaseResponseDTO baseResponseDTO = new BaseResponseDTO();
        baseResponseDTO.setMessage(messages.get(MessageConstant.EMAIL_VERIFICATION_LINK_SENT_SUCCESSFULLY));
        return baseResponseDTO;

    }

    @ApiOperation(
            value = "Update Push notification setting",
            notes = "This API is used to enable/disable device push notification, arn endpoint will be created " +
                    "for device if push notification is enabled."
    )
    @PostMapping(
            value = "/push-notification-setting/update",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<UpdatePushNotificationSettingResponseDTO> updatePushNotificationSettings(
            @Validated @RequestBody UpdatePushNotificationSettingRequestDTO notificationSettingRequestDTO,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        UpdatePushNotificationSettingResponseDTO updatePushNotificationSettingResponseDTO = peopleUserService.updatePushNotificationSetting(notificationSettingRequestDTO);
        BaseResponseDTO<UpdatePushNotificationSettingResponseDTO> baseResponseDTO = new BaseResponseDTO<>();
        baseResponseDTO.setMessage(messages.get(MessageConstant.PUSH_NOTIFICATION_SETTING));
        baseResponseDTO.setData(updatePushNotificationSettingResponseDTO);
        return baseResponseDTO;
    }

    @ApiOperation(
            value = "Profile update",
            notes = "<div>\n" + "\n" + "This API allows user to modify their profile. Notification will be sent\n" +
                    "to connected contacts if the fields to which changes made were shared\n" +
                    "with those contacts.\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                                                                        | Error Code |\n" +
                    "| --------------------------------------------------------------------------------- | ---------- |\n" +
                    "| If primary number or primary email are being modified as part of profile changes. | 603        |"
    )
    @PostMapping(
            value = "/profile/update",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<UpdateUserInfoResponseDTO> updateUser(@Validated @RequestBody UpdateUserInfoRequestDTO updateUserInfoRequest,
                                                                 @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        UpdateUserInfoResponseDTO userInformation = peopleUserService.updateUser(updateUserInfoRequest);

        BaseResponseDTO<UpdateUserInfoResponseDTO> response = new BaseResponseDTO<>();
        response.setData(userInformation);
        response.setMessage(messages.get(MessageConstant.USER_UPDATED_SUCCESSFULLY));
        return response;
    }
    
    @ApiOperation(
            value = "Profile Details",
            notes = "This API will fetch profile information of user."
    )
    @GetMapping(
            value = "/profile-details",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<UpdateUserInfoResponseDTO> getUserDetails(@RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside PeopleUserController->getUserDetails");

        UpdateUserInfoResponseDTO userDetails = peopleUserService.getUserDetails();
        BaseResponseDTO<UpdateUserInfoResponseDTO> response = new BaseResponseDTO<>();
        response.setData(userDetails);
        return response;
    }

    @ApiOperation(
            value = "Change primary number",
            notes = "<div>\n" + "\n" + "This API allows user to change their primary contact number.\n" + "\n" +
                    "</div>\n" + "\n" +
                    "| Error Case                                                  | Error Code |\n" +
                    "| ----------------------------------------------------------- | ---------- |\n" +
                    "| If new number is same as old number.                        | 802        |\n" +
                    "| If new number is already registered with different account. | 803        |"
    )
    @PostMapping(
            value = "/primary-number/update",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<ChangeMobileNumberResponse> updatePrimaryNumber(@Validated @RequestBody ChangeMobileNumberRequest changeMobileNumberRequest,
                                                                           @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside PeopleUserController->updatePrimaryNumber");

        ChangeMobileNumberResponse changeMobileNumberResponse = peopleUserService.updatePrimaryNumber(changeMobileNumberRequest);
        BaseResponseDTO<ChangeMobileNumberResponse> response = new BaseResponseDTO<>();

        response.setData(changeMobileNumberResponse);
        response.setMessage(messages.get(MessageConstant.OTP_SENT_SUCCESSFULLY));
        return response;
    }

    @ApiOperation(
            value = "Contact search",
            notes = "<div>\n" + "\n" + "This API allows user to search for any contact.\n" + "\n" + "</div>\n" +
                    "\n" + "<div>\n" + "\n" + "**API cases :**\n" + "\n" + "</div>\n" + "\n" +
                    "  - Case 1:  \n" +
                    "    Number being searched is present in user contact list, connection\n" +
                    "    object will be returned.\n" +
                    "  - Case 2:  \n" +
                    "    Number being searched is not part of the contact list but it is a\n" +
                    "    registered number, public information of the user to which the\n" +
                    "    number belongs to is returned.\n" +
                    "  - Case 3:  \n" +
                    "    Number is neither present in user contact list nor registered with Watu\n" +
                    "    app, no details will be fetched and searchedContactExist field is\n" +
                    "    marked as false.\n" + "\n" +
                    "| Error Case                                       | Error Code |\n" +
                    "| ------------------------------------------------ | ---------- |\n" +
                    "| If number being searched is of the session user. | 1005       |"
    )
    @PostMapping(
            value = "/searchByNumber",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<SearchByNumberResponseDTO> findByNumber(@Validated @RequestBody CanadaAndUSAContactNumberDTO numberToBeSearched,
                                                                   @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside PeopleUserController->findByNumber");

        SearchByNumberResponseDTO searchResult = peopleUserService.searchGivenContactNumber(numberToBeSearched.getContactNumberDTO());
        BaseResponseDTO<SearchByNumberResponseDTO> baseResponseDTO = new BaseResponseDTO<>();
        baseResponseDTO.setData(searchResult);
        return baseResponseDTO;
    }

    @ApiIgnore
    @ApiOperation(
            value = "Update device location",
            notes = "This API is used to update device location."
    )
    @PostMapping(
            value = "/location/update",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO updateDeviceLocation(@Validated @RequestBody Coordinates location,
                                                @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        peopleUserService.updateUserDeviceLocation(location);
        BaseResponseDTO response = new BaseResponseDTO();
        response.setMessage("User Device Location updated successfully.");

        return response;
    }

    @ApiIgnore
    @ApiOperation(
            value = "Update social profile status",
            notes = "<div>\n" + "\n" + "This API updates verification status of social profile given by user\n" +
                    "while updating profile.\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                                                                           | Error Code |\n" +
                    "| ------------------------------------------------------------------------------------ | ---------- |\n" +
                    "| If value Id provided in request body is invalid or it is not of Social profile type. | 602        |"
    )
    @PostMapping(
            value = "/social-handle/verification-status/update",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO updateStatus(@Validated @RequestBody VerificationStatusUpdateRequest request,
                                        @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        peopleUserService.updateSocialHandleVerificationStatus(request);
        BaseResponseDTO response = new BaseResponseDTO();
        response.setMessage("Social handle verification status updated successfully.");

        return response;
    }

    @ApiOperation(value = "Block contact",
            notes = "<div>\n" + "\n" + "This API allows user to block/un-block a contact.\n" + "\n" +
                    "</div>\n" + "\n" +
                    "| Error Case                                      | Error Code |\n" +
                    "| ----------------------------------------------- | ---------- |\n" +
                    "| If number being blocked is of the session user. | 603        |")
    @PostMapping(
            value = "/block-status/update",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO blockUser(
            @Valid @RequestBody BlockUserRequest blockUserRequest,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        Boolean isBlocked = blockUserRequest.getIsBlocked();
        String message;
        peopleUserService.blockUser(blockUserRequest);
        BaseResponseDTO baseResponseDTO = new BaseResponseDTO();
        if (isBlocked) {
            message = "User blocked successfully";
        } else {
            message = "User unblocked successfully";
        }
        baseResponseDTO.setMessage(message);

        return baseResponseDTO;
    }

    @ApiIgnore
    @ApiOperation(value = "Report contact", notes = "<div>\n" + "\n" + "This API allows user to report an issue with contact.\n" + "\n" +
            "</div>\n" + "\n" +
            "| Error Case                                       | Error Code |\n" +
            "| ------------------------------------------------ | ---------- |\n" +
            "| If number being reported is of the session user. | 603        |")
    @PostMapping(
            value = "/report",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO reportUser(
            @Valid @RequestBody ReportUserRequest reportUserRequest,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        BaseResponseDTO baseResponseDTO = new BaseResponseDTO();
        baseResponseDTO.setMessage(peopleUserService.reportUser(reportUserRequest));
        return baseResponseDTO;
    }

    @ApiOperation(value = "Invite a number",
            notes = "<div>\n" + "\n" + "This API enables user to send invitation to join Watu app to any USA or\n" +
                    "Canada number.\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                                                   | Error Code |\n" +
                    "| ------------------------------------------------------------ | ---------- |\n" +
                    "| If number being invited is already registered with Watu App. | 603        |\n" +
                    "| If sending invite to same contact more than once.            | 831        |\n" +
                    "| If invitee number does not belong to USA or Canada.          | 1007       |")
    @PostMapping(
            value = "/invite",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<InviteByNumberResponseDTO> invite(
            @Valid @RequestBody InviteByNumberRequest inviteByNumberRequest,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        BaseResponseDTO<InviteByNumberResponseDTO> baseResponseDTO = new BaseResponseDTO<>();
        baseResponseDTO.setData(peopleUserService.inviteByNumber(inviteByNumberRequest));
        baseResponseDTO.setMessage(messages.get(MessageConstant.INVITATION_TO_JOIN_WATU_APP));

        return baseResponseDTO;
    }

    /**
     * @param searchString   -> optional value
     * @param fNameOrder     -> 1 = Ascending, -1 = Descending, default = 1
     * @param lNameOrder     -> 1 = Ascending, -1 = Descending, default = 1
     * @param lNamePreferred -> default = false
     * @param pageNumber     -> default page = 0
     * @param pageSize       -> max size = 100
     * @param sessionToken
     * @return
     */
    @ApiIgnore
    @ApiOperation(value = "List of Blocked contacts",
            notes = "This API will list down all the contacts blocked by user and also used to search for any particular" +
                    " blocked contact.<b>Default page size is 100 and page number is 0</b>")
    @GetMapping(
            value = "/blocked-user/fetch",
            produces = APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<FetchConnectionListResponseDTO> getBlockedUserList(
            @RequestParam(value = APIParamKeys.SEARCH_STRING, required = false, defaultValue = "") String searchString,
            @RequestParam(value = APIParamKeys.FNAME_SORT_ORDER, required = false, defaultValue = "1")
            @Min(DESCENDING_ORDER) @Max(ASCENDING_ORDER) Integer fNameOrder,
            @RequestParam(value = APIParamKeys.LNAME_SORT_ORDER, required = false, defaultValue = "1")
            @Min(DESCENDING_ORDER) @Max(ASCENDING_ORDER) Integer lNameOrder,
            @RequestParam(value = APIParamKeys.LAST_NAME_PREFERRED, required = false, defaultValue = "false")
                    Boolean lNamePreferred,
            @RequestParam(value = APIParamKeys.PAGE_NUMBER, defaultValue = DEFAULT_PAGE_NUMBER) Integer pageNumber,
            @RequestParam(value = APIParamKeys.PAGE_SIZE, defaultValue = "100") @Max(100) Integer pageSize,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        BaseResponseDTO<FetchConnectionListResponseDTO> response = new BaseResponseDTO<>();
        response.setData(peopleUserService.getBlockedUserList(searchString, fNameOrder, lNameOrder, lNamePreferred,
                pageNumber, pageSize));
        return response;
    }

    @ApiOperation(value = "Account settings", notes = "User can view the account settings.")
    @GetMapping(
            value = "/settings",
            produces = APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<UserSettingsResponseDTO> getUserSettings(
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        BaseResponseDTO<UserSettingsResponseDTO> response = new BaseResponseDTO<>();
        response.setData(peopleUserService.getUserSettings());
        return response;
    }

    @ApiOperation(
            value = "Generate QR code by branch.io",
            notes = "<div>This API is used for generate QR code using branch.io link.\n" +
                    "\n" +
                    "| Error Case                                                | Error Code |\n" +
                    "| --------------------------------------------------------- | ---------- |\n" +
                    "| If something went wrong                                   | 602        |"
    )
    @GetMapping(
            value = "/generate-qrcode"
    )
    public String generateQRCodeByUserId(
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) throws IOException, WriterException {

        return peopleUserService.generateQRCodeByUserId();
    }

}

