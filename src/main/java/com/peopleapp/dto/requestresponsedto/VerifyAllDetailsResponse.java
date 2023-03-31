package com.peopleapp.dto.requestresponsedto;

import java.util.List;

import com.peopleapp.dto.requestresponsedto.SystemDataResponseDTO;
import com.peopleapp.dto.requestresponsedto.UpdateUserInfoResponseDTO;
import com.peopleapp.dto.requestresponsedto.UserSettingsResponseDTO;

import lombok.Data;

@Data
public class VerifyAllDetailsResponse {

	private List<SocialMediaCategoryResponse> socialMediaList;
	
	private QrResponse qrResponse;

	private UpdateUserInfoResponseDTO userInfoDetails;

	private SystemDataResponseDTO predefinedData;
}
