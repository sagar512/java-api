package com.peopleapp.controller;

import com.peopleapp.constant.APIParamKeys;
import com.peopleapp.dto.requestresponsedto.AppInformationResponseDTO;
import com.peopleapp.dto.requestresponsedto.BaseResponseDTO;
import com.peopleapp.service.AppInformationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

import org.apache.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

@RestController
@Api(value = "application", tags = "Application related operations")
@ApiIgnore
public class ApplicationController {

    @Inject
    private AppInformationService appInformationService;

    @ApiOperation(
            value = "Application health",
            notes = "This API keeps a check on app health."
    )
    @GetMapping(value = "/health")
    public int healthCheck() {
        return HttpStatus.SC_OK;
    }

    @ApiOperation(
            value = "Application build details",
            notes = "This API gets application's build related information based on deviceTypeId."
    )
    @GetMapping(
            value = "/version-info",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<AppInformationResponseDTO> getAppInformation(
            @RequestHeader(value = APIParamKeys.DEVICE_TYPE_ID, required = true) String deviceTypeID) {

        AppInformationResponseDTO appInformationResponseDTO = appInformationService.fetchAppInformationByDeviceTypeId(deviceTypeID);
        BaseResponseDTO baseResponseDTO = new BaseResponseDTO();
        baseResponseDTO.setData(appInformationResponseDTO);
        return baseResponseDTO;
    }

}