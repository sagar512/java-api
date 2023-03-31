package com.peopleapp.controller;

import java.util.List;

import javax.inject.Inject;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.peopleapp.constant.APIParamKeys;
import com.peopleapp.dto.requestresponsedto.SocialMediaCategoryRequest;
import com.peopleapp.dto.requestresponsedto.BaseResponseDTO;
import com.peopleapp.dto.requestresponsedto.SocialMediaCategoryResponse;
import com.peopleapp.service.SocialMediaCategoryService;

import io.swagger.annotations.Api;

@RestController
@RequestMapping(value = "/v1.0/predefined-data/api")
@Api(value = "social media configuration data", tags = "social media configured data related operations")
public class SocialMediaController {

	@Inject
	private SocialMediaCategoryService socialMediaDataService;

	@GetMapping(value = "/socialMedia", produces = MediaType.APPLICATION_JSON_VALUE)
	public BaseResponseDTO<List<SocialMediaCategoryResponse>> getSociaMediaCategory(
			@RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

		BaseResponseDTO<List<SocialMediaCategoryResponse>> response = new BaseResponseDTO<>();
		response.setData(socialMediaDataService.getSocialMediaCategory());
		response.setMessage("Social Media Category Data");

		return response;
	}

	@PostMapping(value = "/createSocialMedia", produces = MediaType.APPLICATION_JSON_VALUE)
	public BaseResponseDTO<SocialMediaCategoryResponse> createSociaMediaCategory(
			@RequestBody SocialMediaCategoryRequest socialMediaCategoryRequestDTO,
			@RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {
		
		SocialMediaCategoryResponse socialMediaCategoryResponseDTO = socialMediaDataService
				.createSocialMediaCateogry(socialMediaCategoryRequestDTO);
		BaseResponseDTO<SocialMediaCategoryResponse> response = new BaseResponseDTO<>();
		response.setData(socialMediaCategoryResponseDTO);
		response.setMessage("Social Media added successfully");
		
		return response;
	}

}
