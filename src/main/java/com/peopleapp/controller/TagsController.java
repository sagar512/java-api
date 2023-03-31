package com.peopleapp.controller;

import com.peopleapp.constant.APIParamKeys;
import com.peopleapp.dto.GetTagsResponseDTO;
import com.peopleapp.dto.requestresponsedto.*;
import com.peopleapp.service.TagService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/v1.0/tags/api")
@Api(value = "tags", tags = "Tags related operations")
@ApiIgnore
public class TagsController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    private TagService tagService;

    @ApiOperation(
            value = "List of tags",
            notes = "This API will return suggested tags while in manage networks, editing contact and other places." +
                    "Suggestion is based on user created tags and system tags."
    )
    @GetMapping(
            value = "/suggested-list",
            produces = APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<GetTagsResponseDTO> getSuggestedTags(
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside TagsController->getSuggestedTags");

        GetTagsResponseDTO getTagsResponseDTO = tagService.getSuggestedTagList();
        BaseResponseDTO<GetTagsResponseDTO> baseResponseDTO = new BaseResponseDTO<>();
        baseResponseDTO.setData(getTagsResponseDTO);
        return baseResponseDTO;
    }
}
