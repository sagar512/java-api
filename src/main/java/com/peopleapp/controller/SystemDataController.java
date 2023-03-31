package com.peopleapp.controller;

import com.peopleapp.constant.APIParamKeys;
import com.peopleapp.dto.requestresponsedto.BaseResponseDTO;
import com.peopleapp.dto.requestresponsedto.SystemDataResponseDTO;
import com.peopleapp.service.SystemDataService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.inject.Inject;

@RestController
@RequestMapping(value = "/v1.0/predefined-data/api")
@Api(value = "system configuration data", tags = "System configured data related operations")
public class SystemDataController {

    @Inject
    private SystemDataService systemDataService;

    @ApiOperation(
            value = "Pre-defined system data",
            notes = "<div>\n" + "\n" + "This API will list out all the system defined data such as\n" + "\n" + "</div>\n" + "\n" +
                    "  - **Labels**\n" + "  - **Tags**\n" + "  - **Network categories** "
    )
    @GetMapping(
            value = "/",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<SystemDataResponseDTO> getSystemData(
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        BaseResponseDTO<SystemDataResponseDTO> response = new BaseResponseDTO<>();
        response.setData(systemDataService.getSystemData());
        return response;
    }
}
