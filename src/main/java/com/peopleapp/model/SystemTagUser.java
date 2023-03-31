package com.peopleapp.model;

import com.peopleapp.dto.SystemTagData;
import lombok.Getter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Document(collection = "systemTagsUser")
public class SystemTagUser {

    private List<SystemTagData> tagList;
}
