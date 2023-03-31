package com.peopleapp.service;

import java.util.List;

import com.peopleapp.dto.requestresponsedto.SocialMediaCategoryRequest;
import com.peopleapp.dto.requestresponsedto.SocialMediaCategoryResponse;

public interface SocialMediaCategoryService {

	List<SocialMediaCategoryResponse> getSocialMediaCategory();

	SocialMediaCategoryResponse createSocialMediaCateogry(SocialMediaCategoryRequest socialMediaCategoryRequestDTO);

	SocialMediaCategoryResponse getScoialMediaCategoryByObjectId(String socialId);

}
