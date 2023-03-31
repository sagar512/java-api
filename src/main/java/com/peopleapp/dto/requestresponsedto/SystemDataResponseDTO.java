package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.model.NetworkCategory;
import com.peopleapp.model.PredefinedLabels;
import lombok.Data;

import java.util.List;

@Data
public class SystemDataResponseDTO {

    private List<PredefinedLabels> labels;

    private List<NetworkCategory> networkCategories;

    private List<String> tagList;
}
