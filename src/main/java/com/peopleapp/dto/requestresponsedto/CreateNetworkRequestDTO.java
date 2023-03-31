package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.Address;
import com.peopleapp.dto.NetworkPrimaryContactMethod;
import com.peopleapp.enums.NetworkCategoryType;
import com.peopleapp.enums.NetworkPrivacyType;
import com.peopleapp.validator.EnumValidator;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class CreateNetworkRequestDTO {

    private String imageURL;

    private String bannerImageURL;

    @NotEmpty
    private String name;

    @EnumValidator(enumClazz = NetworkPrivacyType.class)
    private String privacyType;

    private String description;

    private List<String> tagList;

    @Valid
    @NotNull
    private NetworkPrimaryContactMethod primaryContactMethod;

    private Address networkLocation;

    @EnumValidator(enumClazz = NetworkCategoryType.class)
    private String networkCategory;

}
