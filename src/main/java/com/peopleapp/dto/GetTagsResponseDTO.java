package com.peopleapp.dto;

import lombok.Data;

import java.util.Set;

@Data
public class GetTagsResponseDTO {

    private Set<String> tagList;
}
