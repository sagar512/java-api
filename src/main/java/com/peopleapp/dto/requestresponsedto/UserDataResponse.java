package com.peopleapp.dto.requestresponsedto;

import java.util.List;

import com.peopleapp.dto.UserInformationDTO;

import lombok.Data;

@Data
public class UserDataResponse {

	private UserInformationDTO userDetails;
	
	private List<String> networkSharedValues;

}
