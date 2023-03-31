package com.peopleapp.controller;

import com.peopleapp.configuration.LocaleMessageReader;
import com.peopleapp.constant.APIParamKeys;
import com.peopleapp.constant.MessageConstant;
import com.peopleapp.dto.requestresponsedto.*;
import com.peopleapp.service.PrivacyProfileService;
import com.peopleapp.service.TagService;
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

@RestController
@RequestMapping(value = "/v1.0/privacy-profile/api")
@Api(value = "Privacy profiles", tags = "Privacy profile related operations")
public class UserPrivacyProfileController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    private PrivacyProfileService privacyProfileService;

    @Inject
    private TagService tagService;

    @Inject
    private LocaleMessageReader messages;

    @ApiOperation(
            value = "List of user privacy profiles",
            notes = "This API will list down all the privacy profiles of a user, includes both system generated privacy" +
                    " profiles and also any custom profiles created by user."
    )
    @GetMapping(
            value = "/details",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<GetListOfPrivacyProfilesResponseDTO> getListOfUserProfiles(@RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside UserPrivacyProfileController->GetListOfUserProfiles");

        GetListOfPrivacyProfilesResponseDTO getListOfPrivacyProfilesResponse = privacyProfileService.getListOfUserPrivacyProfiles();
        BaseResponseDTO<GetListOfPrivacyProfilesResponseDTO> getListOfPrivacyProfileResponse = new BaseResponseDTO<>();
        getListOfPrivacyProfileResponse.setData(getListOfPrivacyProfilesResponse);
        return getListOfPrivacyProfileResponse;
    }


    @ApiOperation(
            value = "Delete privacy profile",
            notes = "User can delete the privacy profile(s), If the deleted privacy profile is shared with " +
                    "any contacts then deleted privacy profile will be replaced with default profile."
    )
    @DeleteMapping(
            value = "/delete",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<DeletePrivacyProfileResponseDTO> deletePrivacyProfile(@Validated @RequestBody DeletePrivacyProfileRequestDTO deletePrivacyProfileRequest,
                                                                                 @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside UserPrivacyProfileController->deletePrivacyProfile");

        DeletePrivacyProfileResponseDTO deletePrivacyProfileResponse = privacyProfileService.deleteUserPrivacyProfile(deletePrivacyProfileRequest);
        BaseResponseDTO<DeletePrivacyProfileResponseDTO> baseResponseDTO = new BaseResponseDTO<>();
        baseResponseDTO.setData(deletePrivacyProfileResponse);
        return baseResponseDTO;
    }

    @ApiOperation(
            value = "Set default profile",
            notes = "User can select a default profile from the list of profiles."
    )
    @PostMapping(
            value = "/default/update",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO setDefaultProfile(@Validated @RequestBody SetDefaultProfileRequestDTO setDefaultProfileRequestDTO,
                                             @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside UserPrivacyProfileController->setDefaultProfile");

        String successMessage = privacyProfileService.setDefaultProfile(setDefaultProfileRequestDTO);
        BaseResponseDTO baseResponseDTO = new BaseResponseDTO();
        baseResponseDTO.setMessage(successMessage);
        return baseResponseDTO;
    }

    @ApiOperation(
            value = "Create custom privacy profile",
            notes = "User will be able to create privacy profile apart from those profiles which are available."
    )
    @PostMapping(
            value = "/create",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<CreateOrEditPrivacyProfileResponse> createCustomProfile(@Validated @RequestBody CreateCustomProfileRequestDTO createCustomProfileRequestDTO,
                                                                                   @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside UserPrivacyProfileController->createCustomProfile");

        CreateOrEditPrivacyProfileResponse createOrEditPrivacyProfileResponse = privacyProfileService.createCustomProfileNew(createCustomProfileRequestDTO);
        BaseResponseDTO<CreateOrEditPrivacyProfileResponse> baseResponseDTO = new BaseResponseDTO<>();
        baseResponseDTO.setData(createOrEditPrivacyProfileResponse);
        baseResponseDTO.setMessage(messages.get(MessageConstant.PROFILE_CREATED_SUCCESSFULLY));
        return baseResponseDTO;
    }


    @ApiOperation(
            value = "Edit privacy profile",
            notes = "<div>This API allows user to edit/modify privacy profile.</div>\n" +
                    "\n" +
                    "| Error Case                                                                    | Error Code |\n" +
                    "| ----------------------------------------------------------------------------- | ---------- |\n" +
                    "| If profile being edited is either invalid or does not belong to current user. | 810        |"
    )
    @PostMapping(
            value = "/edit",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<CreateOrEditPrivacyProfileResponse> editPrivacyProfile(@Validated @RequestBody EditPrivacyProfileRequestDTO editPrivacyProfileRequestDTO,
                                                                                  @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside UserPrivacyProfileController->editPrivacyProfile");

        CreateOrEditPrivacyProfileResponse response = privacyProfileService.editPrivacyProfileNew(editPrivacyProfileRequestDTO);
        BaseResponseDTO<CreateOrEditPrivacyProfileResponse> baseResponseDTO = new BaseResponseDTO<>();
        baseResponseDTO.setData(response);
        baseResponseDTO.setMessage(messages.get(MessageConstant.PROFILE_EDITED_SUCCESSFULLY));
        return baseResponseDTO;
    }

    @ApiIgnore
    @ApiOperation(
            value = "Share tags in privacy profile",
            notes = "<div>\n" + "This API allows user to share/un-share tags to privacy profile.\n" +
                    "</div>\n" + "\n" +
                    "| Error Case                                                                                               | Error Code |\n" +
                    "| -------------------------------------------------------------------------------------------------------- | ---------- |\n" +
                    "| If profile to which tags are being shared/un-shared is either invalid or does not belong to current user | 810        |"
    )
    @PostMapping(
            value = "/share-tag/setting",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO shareTagSetting(
            @Validated @RequestBody ShareTagRequest shareTagRequest,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String apiToken) {

        logger.info("Inside UserPrivacyProfileController->shareTagSetting");

        privacyProfileService.shareTag(shareTagRequest);
        BaseResponseDTO response = new BaseResponseDTO();
        response.setMessage("tags shared successfully");

        return response;
    }
}
