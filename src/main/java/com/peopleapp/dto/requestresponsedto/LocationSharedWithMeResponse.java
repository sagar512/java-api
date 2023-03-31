package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.SharedLocationWithMeDetails;
import lombok.Data;

import java.util.List;

@Data
public class LocationSharedWithMeResponse {

    List<SharedLocationWithMeDetails> locationSharedWithMeList;

}
