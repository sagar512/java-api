package com.peopleapp.model;

import lombok.Data;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "socialMediaCategories")
public class SocialMediaCategory {

	@Id
	@Field(value = "_id")
	private ObjectId socialId;

	private String title;
	
    private String titleLabel;

	private String imageURL;

}
