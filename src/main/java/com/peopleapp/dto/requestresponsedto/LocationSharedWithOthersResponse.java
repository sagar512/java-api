package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.SharedLocationWithOtherDetails;
import lombok.Data;

import java.util.List;

@Data
public class LocationSharedWithOthersResponse {

    List<SharedLocationWithOtherDetails> locationSharedWithOtherList;

}
