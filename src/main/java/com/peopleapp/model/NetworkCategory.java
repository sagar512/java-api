package com.peopleapp.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "networkCategories")
public class NetworkCategory {

    private String name;

    private String imageURL;

    private String description;
}
