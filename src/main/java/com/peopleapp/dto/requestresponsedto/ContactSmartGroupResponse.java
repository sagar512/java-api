package com.peopleapp.dto.requestresponsedto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ContactSmartGroupResponse {
	
	private String smartGroupSubName;
	
	private List<String> contactIdList = new ArrayList<>();
}
