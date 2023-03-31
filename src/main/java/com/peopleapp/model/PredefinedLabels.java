package com.peopleapp.model;

import com.peopleapp.enums.UserInfoCategory;
import com.peopleapp.validator.EnumValidator;
import lombok.Getter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Getter
@Document(collection = "predefinedLabels")
public class PredefinedLabels {

    @EnumValidator(enumClazz = UserInfoCategory.class)
    private String category;

    private List<Map<String, Object>> labelList;

    private List<String> keyNameList;

}
