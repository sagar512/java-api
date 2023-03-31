package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.model.UserGroup;
import lombok.Data;

import java.util.List;

@Data
public class FetchUserGroupResponseDTO {

    private List<UserGroup> userGroupList;
    
    private List<String> smartGroupList;

    private int totalNumberOfPages;

    private long totalElements;

    private String nextURL;

}
