package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.SharedContactWithMeDetail;
import lombok.Data;

import java.util.List;

@Data
public class SharedContactWithMeResponse {

    private List<SharedContactWithMeDetail> sharedContactDetailList;

    private int totalNumberOfPages;

    private long totalElements;

    private String nextURL;
}
