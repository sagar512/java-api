package com.peopleapp.controller;


import com.peopleapp.configuration.LocaleMessageReader;
import com.peopleapp.constant.APIParamKeys;
import com.peopleapp.constant.MessageConstant;
import com.peopleapp.dto.requestresponsedto.BaseResponseDTO;
import com.peopleapp.dto.requestresponsedto.DeleteAccountRequest;
import com.peopleapp.service.UserAccountMgmtService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;

@RestController
@RequestMapping(value = "/v1.0/account/api")
@Api(value = "Account management", tags = "Account management related operations")
public class UserAccountMgmtController {

    @Inject
    private UserAccountMgmtService userAccountMgmtService;

    @Inject
    private LocaleMessageReader message;

    @ApiOperation(
            value = "Delete account",
            notes = "<div>\n" + "\n" + "This API will delete user account.\n" + "\n" + "</div>\n" + "\n" + "<div>\n" + "\n" +
                    "**Note:**\n" + "\n" + "</div>\n" +
                    "\n" +
                    "  - User will be removed from all the networks.\n" +
                    "  - All connection for user will be deleted.\n" +
                    "  - Number will be removed from the list of numbers registered with app.\n" +
                    "\n" +
                    "| Error Case                                                                                         | Error Code |\n" +
                    "| -------------------------------------------------------------------------------------------------- | ---------- |\n" +
                    "| If user is owner of any network, then account cannot be deleted till the ownership is transferred. | 929        |"
    )
    @DeleteMapping(
            value = "/delete",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO deleteUserAccount(@Validated @RequestBody DeleteAccountRequest deleteAccountRequest, @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        BaseResponseDTO<String> baseResponseDTO = new BaseResponseDTO<>();
        userAccountMgmtService.deleteUserAccount(deleteAccountRequest);
        baseResponseDTO.setMessage(message.get(MessageConstant.USER_ACCOUNT_DELETED));
        return baseResponseDTO;
    }

    @ApiIgnore
    @ApiOperation(
            value = "Suspend Account",
            notes = "This API will deactivate the user account."
    )
    @PostMapping(
            value = "/suspend",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public BaseResponseDTO suspendAccount(@RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        BaseResponseDTO<String> baseResponseDTO = new BaseResponseDTO<>();
        userAccountMgmtService.suspendAccount();
        baseResponseDTO.setMessage(message.get(MessageConstant.USER_ACCOUNT_SUSPENDED));
        return baseResponseDTO;
    }

    @ApiOperation(
            value = "Logout",
            notes = "This API will end user session."
    )
    @PostMapping(
            value = "/logout",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO logoutUser(@RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        BaseResponseDTO response = new BaseResponseDTO();
        userAccountMgmtService.logout();
        response.setMessage(message.get(MessageConstant.LOG_OUT));
        return response;
    }

}
