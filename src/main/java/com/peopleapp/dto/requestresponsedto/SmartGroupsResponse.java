package com.peopleapp.dto.requestresponsedto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class SmartGroupsResponse {
	
	private String smartGroupName;
	
	private List<String> smartGroupSubList = new ArrayList<>();
}
