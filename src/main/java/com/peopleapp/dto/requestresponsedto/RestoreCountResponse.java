package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

@Data
public class RestoreCountResponse {
	
	private String backUpCount;
	
	private String currentCount;
	
	private String backUpDate;

}
