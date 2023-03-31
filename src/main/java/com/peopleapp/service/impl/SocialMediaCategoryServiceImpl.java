package com.peopleapp.service.impl;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import com.peopleapp.dto.ApplicationConfigurationDTO;
import com.peopleapp.dto.requestresponsedto.SocialMediaCategoryRequest;
import com.peopleapp.dto.requestresponsedto.SocialMediaCategoryResponse;
import com.peopleapp.model.SocialMediaCategory;
import com.peopleapp.repository.SocialMediaCategoryRepository;
import com.peopleapp.service.SocialMediaCategoryService;

@Service
public class SocialMediaCategoryServiceImpl implements SocialMediaCategoryService {
	
	 @Inject
	 private SocialMediaCategoryRepository socialMediaCategoryRepository;
	 
	 @Inject
	 private ApplicationConfigurationDTO properties;
	 
	 @Override
		public List<SocialMediaCategoryResponse> getSocialMediaCategory() {
			List<SocialMediaCategoryResponse> socialMediaCategories = new ArrayList<>();
	        for(SocialMediaCategory socialMediaCategory : socialMediaCategoryRepository.findAll()){
	            // Creating complete path for imageUrl
	        	SocialMediaCategoryResponse category=new SocialMediaCategoryResponse();
	        	category.setImageURL(properties.getS3BaseUrlNetworkCategory()+"social_media/"+ socialMediaCategory.getImageURL());
	        	category.setTitle(socialMediaCategory.getTitle());
	        	category.setTitleLabel(socialMediaCategory.getTitleLabel());
	        	category.setSocialId(socialMediaCategory.getSocialId().toString());
	        	socialMediaCategories.add(category);
	        }
			return socialMediaCategories;
		}

		@Override
		public SocialMediaCategoryResponse createSocialMediaCateogry(
				SocialMediaCategoryRequest socialMediaCategoryRequestDTO) {
			SocialMediaCategory socialMediaCategory=new SocialMediaCategory();
			socialMediaCategory.setTitle(socialMediaCategoryRequestDTO.getTitle());
			socialMediaCategory.setTitleLabel(socialMediaCategoryRequestDTO.getTitleLabel());
			socialMediaCategory.setImageURL(socialMediaCategoryRequestDTO.getImageURL());
			SocialMediaCategory socialMediaCategoryRespone=socialMediaCategoryRepository.save(socialMediaCategory);
			SocialMediaCategoryResponse socialMediaCategoryResponseDTO=new SocialMediaCategoryResponse();
			socialMediaCategoryResponseDTO.setSocialId(socialMediaCategoryRespone.getSocialId().toString());
			socialMediaCategoryResponseDTO.setTitle(socialMediaCategoryRespone.getTitle());
			socialMediaCategoryResponseDTO.setTitleLabel(socialMediaCategoryRespone.getTitleLabel());
			socialMediaCategoryResponseDTO.setImageURL(properties.getS3BaseUrlNetworkCategory()+"social_media/"+ socialMediaCategoryRespone.getImageURL());
			return socialMediaCategoryResponseDTO;
		}

		@Override
		public SocialMediaCategoryResponse getScoialMediaCategoryByObjectId(String socialId) {
			ObjectId objId = new ObjectId(socialId);
			SocialMediaCategory socialMediaCategory=socialMediaCategoryRepository.findById(objId).get();
			SocialMediaCategoryResponse responseDTO=new SocialMediaCategoryResponse();
			responseDTO.setSocialId(socialMediaCategory.getSocialId().toString());
			responseDTO.setTitle(socialMediaCategory.getTitle());
			responseDTO.setImageURL(properties.getS3BaseUrlNetworkCategory()+"social_media/"+ socialMediaCategory.getImageURL());
			return responseDTO;
		}	

}
