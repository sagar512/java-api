package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.UserContactData;
import lombok.Data;

import java.util.List;

@Data
public class ContactSyncResponseDTO {

    private List<UserContactData> userContactList;
}
