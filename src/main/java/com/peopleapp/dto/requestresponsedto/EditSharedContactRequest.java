package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

import java.util.List;

@Data
public class EditSharedContactRequest {

    private List<String> activitySubIdList;

    private List<String> activityIdList;
}
