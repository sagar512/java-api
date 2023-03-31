package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

@Data
public class SocialMediaCategoryResponse {

	private String socialId;

	private String title;
	
	private String titleLabel;

	private String imageURL;
}