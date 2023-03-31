package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.ContactImage;
import lombok.Data;

import javax.validation.Valid;
import java.util.List;

@Data
public class UpdateContactImageRequest {

    @Valid
    private List<ContactImage> contactImageList;

}
