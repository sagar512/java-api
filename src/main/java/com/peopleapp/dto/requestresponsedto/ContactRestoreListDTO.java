package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.model.UserRestoreConnection;
import lombok.Data;

import java.util.List;

@Data
public class ContactRestoreListDTO {

    private long recordCountInCurPage;
    private long totalRecordCount;
    private int totalPageCount;
    private List<UserRestoreConnection> userRestoreConnectionList;
}
