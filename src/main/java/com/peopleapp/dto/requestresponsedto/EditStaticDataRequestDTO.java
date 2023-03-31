package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.ContactStaticData;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class EditStaticDataRequestDTO {

    @NotEmpty
    List<ContactStaticData> contactStaticDataList;

}

