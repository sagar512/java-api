package com.peopleapp.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Data;

@Data
@Document(collection = "groupIcons")
public class groupIcons {
	
	@Id
	@Field(value = "_id")
	private ObjectId iconId;
	
	private String imageURL;

}
