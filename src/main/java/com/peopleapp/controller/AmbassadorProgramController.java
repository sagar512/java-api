package com.peopleapp.controller;

import com.peopleapp.configuration.LocaleMessageReader;
import com.peopleapp.constant.APIParamKeys;
import com.peopleapp.constant.MessageConstant;
import com.peopleapp.dto.requestresponsedto.BaseResponseDTO;
import com.peopleapp.dto.requestresponsedto.GetAmbassadorDetailsResponseDTO;
import com.peopleapp.dto.requestresponsedto.SendReferralRequestDTO;
import com.peopleapp.service.AmbassadorService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;

@RestController
@RequestMapping(value = "/v1.0/ambassador/api")
@Api(value = "ambassador", tags = "Ambassador Program related operations")
@ApiIgnore
public class AmbassadorProgramController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    private AmbassadorService ambassadorService;

    @Inject
    private LocaleMessageReader messages;

    @ApiOperation(
            value = "Refer contacts to join WATU and earn reward points.",
            notes = "<div>\n" + "\n" + "WATU user can send referral link to their contacts and will earn reward\n" +
                    "points when referred contact joins the app through the app link sent to\n" + "them.\n" + "\n" +
                    "</div>\n" + "\n" +
                    "| Error Case                                   || Error Code |\n" +
                    "| -------------------------------------------- || ---------- |\n" +
                    "| If referred number already part of Watu app. || 803        |\n" +
                    "| If referral already sent to the number.      || 1003       |"
    )
    @PostMapping(
            value = "/refer",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO sendReferralLink(
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken,
            @Validated @RequestBody SendReferralRequestDTO sendReferralRequestDTO) {

        logger.info("Inside AmbassadorProgramController -> sendReferralLink");

        ambassadorService.sendReferralLink(sendReferralRequestDTO);

        BaseResponseDTO baseResponseDTO = new BaseResponseDTO();
        baseResponseDTO.setMessage(messages.get(MessageConstant.REFERRAL_LINK_SENT_SUCCESSFULLY));

        return baseResponseDTO;
    }


    @ApiOperation(
            value = "Ambassador details of user.",
            notes = "This API will give the count of total number of referral's sent, total number of contacts registered " +
                    "with sent referral and total points collected by the user"
    )
    @GetMapping(
            value = "/details",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<GetAmbassadorDetailsResponseDTO> fetchUserAmbassadorDetail(
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside AmbassadorProgramController -> fetchUserAmbassadorDetail");

        BaseResponseDTO<GetAmbassadorDetailsResponseDTO> baseResponseDTO = new BaseResponseDTO<>();
        baseResponseDTO.setData(ambassadorService.getAmbassadorDetails());

        return baseResponseDTO;
    }
}
