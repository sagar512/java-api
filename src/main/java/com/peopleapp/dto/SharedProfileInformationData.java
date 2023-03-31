package com.peopleapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.bson.types.ObjectId;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SharedProfileInformationData {

    @NotNull
    private ObjectId privacyProfileId;

    private Boolean isCompanyShared = Boolean.FALSE;

    private Boolean isPositionShared = Boolean.FALSE;

    private Boolean isNickNameShared = Boolean.FALSE;

    private Boolean isMaidenNameShared = Boolean.FALSE;

    private List<String> valueIdList;

    public String getPrivacyProfileId() {
        if (this.privacyProfileId != null) {
            return this.privacyProfileId.toString();
        } else {
            return null;
        }
    }

    public void setPrivacyProfileId(String privacyProfileId) {
        this.privacyProfileId = new ObjectId(privacyProfileId);
    }
}
