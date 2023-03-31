package com.peopleapp.service;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.google.zxing.WriterException;
import com.peopleapp.dto.ContactNumberDTO;
import com.peopleapp.dto.Coordinates;
import com.peopleapp.dto.requestresponsedto.BlockUserRequest;
import com.peopleapp.dto.requestresponsedto.ChangeMobileNumberRequest;
import com.peopleapp.dto.requestresponsedto.ChangeMobileNumberResponse;
import com.peopleapp.dto.requestresponsedto.ConnectRequestDTO;
import com.peopleapp.dto.requestresponsedto.ConnectResponseDTO;
import com.peopleapp.dto.requestresponsedto.FetchConnectionListResponseDTO;
import com.peopleapp.dto.requestresponsedto.InviteByNumberRequest;
import com.peopleapp.dto.requestresponsedto.InviteByNumberResponseDTO;
import com.peopleapp.dto.requestresponsedto.ReportUserRequest;
import com.peopleapp.dto.requestresponsedto.SearchByNumberResponseDTO;
import com.peopleapp.dto.requestresponsedto.UpdatePushNotificationSettingRequestDTO;
import com.peopleapp.dto.requestresponsedto.UpdatePushNotificationSettingResponseDTO;
import com.peopleapp.dto.requestresponsedto.UpdateUserInfoRequestDTO;
import com.peopleapp.dto.requestresponsedto.UpdateUserInfoResponseDTO;
import com.peopleapp.dto.requestresponsedto.UserSettingsResponseDTO;
import com.peopleapp.dto.requestresponsedto.VerificationStatusUpdateRequest;
import com.peopleapp.dto.requestresponsedto.VerifyOTPRequestDTO;
import com.peopleapp.dto.requestresponsedto.VerifyOTPResponseDTO;
import com.peopleapp.model.PeopleUser;

@Component
public interface PeopleUserService {

	PeopleUser findUserByContactNumber(ContactNumberDTO contactNumber);

	String getUserIdByContactNumber(ContactNumberDTO contactNumber);

	SearchByNumberResponseDTO searchGivenContactNumber(ContactNumberDTO numberToBeSearched);

	PeopleUser findUserByUserId(String userId);

	ConnectResponseDTO connect(ConnectRequestDTO connectRequestDTO);

	UpdateUserInfoResponseDTO updateUser(UpdateUserInfoRequestDTO updateUserInfoRequestDTO);

	ChangeMobileNumberResponse updatePrimaryNumber(ChangeMobileNumberRequest changeMobileNumberRequest);

	UpdatePushNotificationSettingResponseDTO updatePushNotificationSetting(
			UpdatePushNotificationSettingRequestDTO notificationSettingRequestDTO);

	void authenticateEmail(String email);

	void linkPrimaryEmail(String email);

	void verifyEmail(String verificationToken);

	UpdateUserInfoResponseDTO getUserDetails();

	VerifyOTPResponseDTO verifyOTP(VerifyOTPRequestDTO verifyOTPRequestDTO, String deviceTypeID);

	void updateUserDeviceLocation(Coordinates locationCoordinates);

	void updateSocialHandleVerificationStatus(VerificationStatusUpdateRequest verificationStatusUpdateRequest);

	void blockUser(BlockUserRequest blockUserRequest);

	String reportUser(ReportUserRequest reportUserRequest);

	InviteByNumberResponseDTO inviteByNumber(InviteByNumberRequest inviteByNumberRequest);

	FetchConnectionListResponseDTO getBlockedUserList(String searchString, Integer fNameOrder, Integer lNameOrder,
			Boolean lNamePreferred, Integer pageNumber, Integer pageSize);

	UserSettingsResponseDTO getUserSettings();

	String checkAndSetConnectionStatus(PeopleUser initiator, PeopleUser searchedUser);

	String generateQRCodeByUserId() throws IOException, WriterException;
}
