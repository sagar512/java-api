package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.SharedContactWithOtherDetail;
import lombok.Data;

import java.util.List;

@Data
public class SharedContactWithOthersResponse {

    private List<SharedContactWithOtherDetail> sharedContactDetailList;

    private int totalNumberOfPages;

    private long totalElements;

    private String nextURL;

}
