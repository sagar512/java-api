package com.peopleapp.model;

import com.peopleapp.dto.ProfileKey;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "systemPrivacyProfiles")
public class SystemPrivacyProfile {

    private String profileName;

    private List<ProfileKey> profileKeyList;

    private Boolean isDefault;

    private Boolean isPublic;
}
