package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

@Data
public class ShareProfileResponse {

	private String label;

	private String key;

	private Boolean value;
	
	private String view_detail = "";

}
