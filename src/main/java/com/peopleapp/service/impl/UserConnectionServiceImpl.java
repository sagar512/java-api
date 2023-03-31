package com.peopleapp.service.impl;

import com.peopleapp.configuration.LocaleMessageReader;
import com.peopleapp.constant.PeopleConstants;
import com.peopleapp.constant.SMSTemplateKeys;
import com.peopleapp.controller.UserConnectionController;
import com.peopleapp.dto.*;
import com.peopleapp.dto.requestresponsedto.*;
import com.peopleapp.dto.requestresponsedto.RestoreCountResponse;
import com.peopleapp.dto.requestresponsedto.UserPrivacyProfileDTO;
import com.peopleapp.dto.requestresponsedto.ContactContactIDRequest;
import com.peopleapp.dto.requestresponsedto.ContactDTO;
import com.peopleapp.dto.requestresponsedto.GroupNameResponse;
import com.peopleapp.enums.*;
import com.peopleapp.exception.BadRequestException;
import com.peopleapp.model.*;
import com.peopleapp.repository.*;
import com.peopleapp.security.TokenAuthService;
import com.peopleapp.service.*;
import com.peopleapp.util.PeopleUtils;

import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class UserConnectionServiceImpl implements UserConnectionService {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private static final String FIRST_NAME = "firstName";
	private static final String LAST_NAME = "lastName";
	private static final String CONTACTSTATICDATA = "contactStaticData.";

	@Inject
	private UserConnectionRepository userConnectionRepository;

	@Inject
	private UserRestoreConnectionRepository userRestoreConnectionRepository;

	@Inject
	private LocaleMessageReader messages;

	@Inject
	private PeopleUserRepository peopleUserRepository;

	@Inject
	private UserActivityRepository userActivityRepository;

	@Inject
	private UserActivityService userActivityService;

	@Inject
	private QueueService smsService;

	@Inject
	private UserPrivacyProfileRepository userPrivacyProfileRepository;

	@Inject
	private UserGroupRepository userGroupRepository;

	@Inject
	private TokenAuthService tokenAuthService;

	@Inject
	private PeopleUserService peopleUserService;

	@Inject
	private QueueService queueService;

	@Inject
	private PrivacyProfileService privacyProfileService;

	@Inject
	private UserSessionRepository userSessionRepository;

	@Inject
	private NotificationService notificationService;

	@Inject
	private RegisteredNumberRepository registeredNumberRepository;

	@Inject
	private MasterService masterService;

	@Inject
	private TagService tagService;

	@Inject
	private ActivityContactRepository activityContactRepository;

	@Inject
	private MergeContactRepository mergeContactRepository;

	@Value("${connection.threshold-count}")
	private String connThresholdCount;

	@Value("${connection.time-range}")
	private String connTimeRange;

	@Value("${app.link}")
	private String appLink;

	@Value("${contact.batch-limit}")
	private String syncContactBatchLimit;

	@Override
	public ContactSyncResponseDTO syncContacts(ContactSyncRequestDTO request) {

		PeopleUser sessionUser = tokenAuthService.getSessionUser();

		List<UserInformationDTO> toBeSyncedContactList = request.getUserContactList();
		List<UserConnection> toBeSavedContactList = new ArrayList<>();

		// prepare list of contact number for each contact synced
		Set<String> combinedTagListForContacts = new HashSet<>();
		for (UserInformationDTO toBeSyncedContactData : PeopleUtils.emptyIfNull(toBeSyncedContactList)) {
			// validateAndRemoveIncorrectKeyValue(toBeSyncedContactData);

			// create a new user contact
			toBeSavedContactList.add(createNewContact(sessionUser.getUserId(), toBeSyncedContactData));
			List<String> contactTagList = toBeSyncedContactData.getTagList();
			if (!PeopleUtils.isNullOrEmpty(contactTagList)) {
				combinedTagListForContacts.addAll(contactTagList);
			}
		}
		// create new tag for user if doesn't exist
		if (!PeopleUtils.isNullOrEmpty(combinedTagListForContacts)) {
			sessionUser.setTagMap(tagService.createNewTagByUser(sessionUser.getTagMap(),
					new ArrayList<>(combinedTagListForContacts)));
			peopleUserRepository.save(sessionUser);
		}

		List<UserConnection> savedContactList = new ArrayList<>();

		final AtomicInteger counter = new AtomicInteger(0);

		final Collection<List<UserConnection>> partitioned = toBeSavedContactList.stream()
				.collect(Collectors
						.groupingBy(contact -> counter.getAndIncrement() / Integer.parseInt(syncContactBatchLimit)))
				.values();

		for (List<UserConnection> connectionList : partitioned) {
			savedContactList.addAll(userConnectionRepository.saveAll(connectionList));
		}

		// Save user connection restore data
		if (request.getIsOnBoard()) {
			final Collection<List<UserConnection>> partitionedRestore = toBeSavedContactList.stream()
					.collect(Collectors.groupingBy(contact -> counter.getAndIncrement())).values();
			for (List<UserConnection> connectionRestoreList : partitionedRestore) {
				userRestoreConnectionRepository.saveAll(createNewRestoreContact(connectionRestoreList));
			}
		}

		// get registered numbers list
		Set<String> numberList = masterService.getRegisteredNumberList();

		// prepare response
		ContactSyncResponseDTO response = new ContactSyncResponseDTO();
		List<UserContactData> syncedContactList = new ArrayList<>();

		for (UserConnection userContact : PeopleUtils.emptyIfNull(savedContactList)) {
			if (userContact.getContactStaticData() != null) {
				// updating verification status of phone number
				populateStaticDataWithIsVerifiedInfo(userContact.getContactStaticData(), numberList);
			}
			UserContactData userContactData = new UserContactData();
			userContactData.setConnectionId(userContact.getConnectionId());
			userContactData.setConnectionStatus(userContact.getConnectionStatus().getValue());
			userContactData.setStaticProfileData(userContact.getContactStaticData());
			userContactData.setDeviceContactId(userContact.getDeviceContactId());

			if (userContact.getIsFavourite()) {
				userContactData.setIsFavourite(true);
				userContactData.setSequenceNumber(userContact.getSequenceNumber());
			}
			syncedContactList.add(userContactData);
		}

		response.setUserContactList(syncedContactList);

		return response;
	}

//    @Override
//    public ContactRestoreListDTO restoreContacts(Integer pageNumber, Integer pageSize) {
//        PeopleUser sessionUser = tokenAuthService.getSessionUser();
//        ContactRestoreListDTO response = new ContactRestoreListDTO();
//
//        String userId = sessionUser.getUserId();
//        List<UserRestoreConnection> userRestoreConnectionList =
//                userRestoreConnectionRepository.findByConnectionFromId(new ObjectId(sessionUser.getUserId()));
//
//        response.setUserRestoreConnectionList(userRestoreConnectionList);
//        return response;
//    }

	@Override
	public ContactRestoreListDTO restoreContacts(Integer pageNumber, Integer pageSize, boolean isPageable) {
		PeopleUser sessionUser = tokenAuthService.getSessionUser();
		ObjectId objectId = new ObjectId(sessionUser.getUserId());

		ContactRestoreListDTO response = new ContactRestoreListDTO();
		if (isPageable) {
			Pageable pageable = PageRequest.of(pageNumber, pageSize); // pageNumber=0 based
			Page<UserRestoreConnection> page = userRestoreConnectionRepository.findByConnectionFromId(objectId,
					pageable);

			response.setRecordCountInCurPage(page.getNumberOfElements());
			response.setTotalRecordCount(page.getTotalElements());
			response.setTotalPageCount(page.getTotalPages());
			response.setUserRestoreConnectionList(page.getContent());
		} else {
			List<UserRestoreConnection> userRestoreConnectionList = userRestoreConnectionRepository
					.findByConnectionFromId(new ObjectId(sessionUser.getUserId()));
			response.setUserRestoreConnectionList(userRestoreConnectionList);
		}

		return response;
	}

	@Override
	public RestoreCountResponse getContactBackupDetail() {
		PeopleUser sessionUser = tokenAuthService.getSessionUser();
		RestoreCountResponse countResponse = new RestoreCountResponse();
		countResponse.setBackUpCount(userRestoreConnectionRepository
				.countByConnectionFromId(new ObjectId(sessionUser.getUserId())).toString());
		countResponse.setCurrentCount(
				userConnectionRepository.countByConnectionFromId(new ObjectId(sessionUser.getUserId())).toString());
		String date = "";
		List<UserRestoreConnection> userRestoreConnections = userRestoreConnectionRepository
				.findByConnectionFromId(new ObjectId(sessionUser.getUserId()));
		if (userRestoreConnections.size() != 0) {
			String datetemp = userRestoreConnections.get(0).getLastUpdatedOn().toString();
			date = PeopleUtils.convertDate(datetemp);
		}
		countResponse.setBackUpDate(date);
		return countResponse;
	}

	@Override
	public BluetoothConnectionDetailsResponseDTO checkBluetoothConnectionDetails(String bluetoothToken) {
		BluetoothConnectionDetailsResponseDTO response = new BluetoothConnectionDetailsResponseDTO();
		PeopleUser userDetails = peopleUserRepository.findByBluetoothToken(bluetoothToken);
		// PeopleUser sessionUser = tokenAuthService.getSessionUser();

		// UserActivity userActivity =
		// userActivityRepository.findByActivityByIdAndActivityForId(new
		// ObjectId(userDetails.getUserId()), new ObjectId(sessionUser.getUserId()));

//        if (userActivity.getOverallStatus().getValue() == "INFORMATIVE" || userActivity.getOverallStatus().getValue() == "PENDING") {
//            return response;
//        }
		if (userDetails != null) {
			SearchByNumberResponseDTO searchByNumberResponseDTO = peopleUserService
					.searchGivenContactNumber(userDetails.getVerifiedContactNumber());
			if (searchByNumberResponseDTO.getSearchedContactDetails() != null) {
				response.setConnectionId(PeopleUtils
						.getDefaultOrEmpty(searchByNumberResponseDTO.getSearchedContactDetails().getConnectionId()));
				response.setConnectionStatus(
						searchByNumberResponseDTO.getSearchedContactDetails().getConnectionStatus());
			}
			if (searchByNumberResponseDTO.getSearchedWatuContactDetails() != null) {
				response.setConnectionId(PeopleUtils.getDefaultOrEmpty(
						searchByNumberResponseDTO.getSearchedWatuContactDetails().getConnectionId()));
				response.setConnectionStatus(
						searchByNumberResponseDTO.getSearchedWatuContactDetails().getConnectionStatus());
			}
			response.setBluetoothTokenUserId(PeopleUtils.getDefaultOrEmpty(userDetails.getUserId()));
			response.setContactNumberDTO(userDetails.getVerifiedContactNumber());
			response.setName(PeopleUtils.getDefaultOrEmpty(userDetails.getFullName()));
			response.setDefaultImageUrl(PeopleUtils.getDefaultOrEmpty(userDetails.getDefaultImageUrl()));
			response.setPosition(PeopleUtils.getDefaultOrEmpty(userDetails.getPositionValue()));
		}
		return response;
	}

	@Override
	public SendConnectionRequestResponse sendConnectionRequest(SendConnectionRequest request) {

		PeopleUser initiator = tokenAuthService.getSessionUser();
		String initiatorId = initiator.getUserId();
		List<UserActivity> activityList = new ArrayList<>();

		// on initiator
		if (checkRequestThreshold(initiatorId)) {
			initiator.setIsFlagged(Boolean.TRUE);
			initiator.setFlaggedReason(PeopleConstants.FlaggedReason.EXCEEDED_CONNECTION_REQUEST_THRESHOLD);
		}

		// check if shared privacy profile is valid
		if (request.getSharedPrivacyProfileKey() != null
				&& request.getSharedPrivacyProfileKey().getPrivacyProfileId() != null) {
			UserPrivacyProfile sharedProfile = userPrivacyProfileRepository
					.findByProfileIdAndUserId(request.getSharedPrivacyProfileKey().getPrivacyProfileId(), initiatorId);
			if (sharedProfile == null) {
				throw new BadRequestException(MessageCodes.INVALID_PROFILEID.getValue());
			}
		}

		request.setRequestFlowAndOperationType();
		SendConnectionRequestFlow requestFlow = request.getRequestFlow();
		OperationType operationType = request.getOperationType();

		UserActivityAndConnectionData userActivityAndConnectionData = null;

		switch (requestFlow) {
		case CONNECTION_ID_FLOW:
			switch (operationType) {
			case SINGLE:
				activityList.add(sendConnectionRequestToContactSingle(initiator, request));
				break;
			case BULK:
				activityList.addAll(sendConnectionRequestToContactBulk(initiator, request));
				break;
			default:
				throw new BadRequestException(MessageCodes.INVALID_OPERATION.getValue());
			}
			break;
		case USER_ID_FLOW:
			switch (operationType) {
			case SINGLE:
				userActivityAndConnectionData = sendConnectionRequestToPeopleUserSingle(initiator, request);
				if (userActivityAndConnectionData != null) {
					activityList.add(userActivityAndConnectionData.getUserActivity());
				}
				break;

			case BULK:
				throw new BadRequestException(MessageCodes.INVALID_OPERATION.getValue());
			default:
				throw new BadRequestException(MessageCodes.INVALID_OPERATION.getValue());
			}
			break;
		case ACTIVITY_ID_FLOW:
			activityList.addAll(sendConnectionRequestByActivityIdList(initiator, request));
			break;

		case ACTIVITY_SUB_ID_FLOW:
			activityList.addAll(sendConnectionRequestByActivitySubIdList(initiator, request));
			break;

		default:
			throw new BadRequestException(MessageCodes.INVALID_OPERATION.getValue());
		}

		peopleUserRepository.save(initiator);

		// prepare response
		SendConnectionRequestResponse response = new SendConnectionRequestResponse();
		response.setActivityDetailsList(userActivityService.prepareActivityDetails(activityList));
		if (request.isStaticContactToBeCreated() && (userActivityAndConnectionData != null)) {
			List<UserContactData> createdContactDetails = getUserContactDataList(initiator,
					Collections.singletonList(userActivityAndConnectionData.getUserConnection()), null,
					masterService.getRegisteredNumberList());

			response.setContactDetails(createdContactDetails.get(0));
		}

		return response;
	}

	private List<UserActivity> sendConnectionRequestByActivityIdList(PeopleUser initiator,
			SendConnectionRequest request) {
		List<String> activityIdList = request.getActivityIdList();

		List<ActivityContact> validActivityContacts = activityContactRepository
				.getActivityContactsByActivityIdsAndUserId(activityIdList, initiator.getUserId());

		return sendConnectionRequestAndPrepareResponse(initiator, request, validActivityContacts);
	}

	private List<UserActivity> sendConnectionRequestByActivitySubIdList(PeopleUser initiator,
			SendConnectionRequest request) {

		List<String> activitySubIdList = request.getActivitySubIdList();

		List<ActivityContact> validActivityContacts = activityContactRepository
				.getActivityContactsByIdsAndReceiverId(activitySubIdList, initiator.getUserId());

		return sendConnectionRequestAndPrepareResponse(initiator, request, validActivityContacts);
	}

	private List<UserActivity> sendConnectionRequestAndPrepareResponse(PeopleUser initiator,
			SendConnectionRequest request, List<ActivityContact> validActivityContacts) {

		List<UserActivity> userActivityList = new ArrayList<>();
		short blockedContactsCount = 0;
		short invalidContacts = 0;
		for (ActivityContact activityContact : validActivityContacts) {

			if (!activityContact.getIsActive()) {
				continue;
			}

			// create activity and send notification for each activity contact
			UserActivity connectionRequestActivity = null;

			try {
				connectionRequestActivity = prepareConnectionRequest(initiator, activityContact, request,
						validActivityContacts.size());

				// add created connection activity to userActivityList
				if (connectionRequestActivity != null) {
					userActivityList.add(connectionRequestActivity);
				}
			} catch (Exception exception) {
				if (exception.getMessage()
						.equalsIgnoreCase(MessageCodes.CANNOT_PERFORM_ANY_ACTION_WITH_BLOCKED_USERS.getValue())) {
					blockedContactsCount++;
				} else if (exception.getMessage()
						.equalsIgnoreCase(MessageCodes.INVALID_COUNTRY_CODE_OR_PHONE_NUMBER.getValue())) {
					invalidContacts++;
				} else {
					throw new BadRequestException(exception.getMessage());
				}
			}

		}

		checkIfAllContactsAreInvalid(invalidContacts, validActivityContacts.size());
		checkIfRequestReceiversAreBlocked(blockedContactsCount, validActivityContacts.size());

		return userActivityList;
	}

	private UserActivity prepareConnectionRequest(PeopleUser initiator, ActivityContact activityContact,
			SendConnectionRequest request, int activityContactsListSize) {
		ContactNumberDTO receiverContactNumberDTO;
		boolean isAlreadyConnected = false;
		boolean isInitiatorBlocked;
		boolean isRequestAlreadySent;
		boolean isReceiverContactSameAsInitiatorContact = false;

		UserConnection sharedConnection = userConnectionRepository.findConnectionByConnectionIdAndInitiatorId(
				activityContact.getInitiatorId(), activityContact.getConnectionId());

		// fetch receiver details if they are watu users
		PeopleUser receiverUser = getReceiverUser(sharedConnection);

		if (receiverUser != null) {

			// check if connection request is being sent to blocked contact
			requestReceiverBlockedStatusVerification(tokenAuthService.getSessionUser(), receiverUser.getUserId());

			if (receiverUser.getUserId().equalsIgnoreCase(initiator.getUserId())) {
				isReceiverContactSameAsInitiatorContact = true;
			}

			receiverContactNumberDTO = receiverUser.getVerifiedContactNumber();

			// check if the 'people user' is already a real-time connection
			UserConnection existingUserConnection = userConnectionRepository
					.findConnectionByFromIdAndToId(initiator.getUserId(), receiverUser.getUserId());

			if (existingUserConnection != null) {
				isAlreadyConnected = isContactInConnectedState(existingUserConnection);
			}

			request.setReceiverUserId(receiverUser.getUserId());
			request.setReceiverNumber(receiverUser.getVerifiedContactNumber());

		} else {

			// Need to find the contact number for target user
			receiverContactNumberDTO = getContactNumberForTargetUser(sharedConnection);
			if (receiverContactNumberDTO == null) {
				throw new BadRequestException(MessageCodes.INVALID_COUNTRY_CODE_OR_PHONE_NUMBER.getValue());
			}
			UserContact userContact = new UserContact();
			userContact.setContactNumber(receiverContactNumberDTO);
			receiverContactNumberDTO = userContact.getContactNumber()
					.getContactNumberWithDefaultCountryCode(getDefaultCountryCode());

			request.setReceiverNumber(receiverContactNumberDTO);

		}

		// handle cases of request sent from both ends
		// check to verify if connection request already sent to contact
		isRequestAlreadySent = isConnectionRequestAlreadySentToContact(initiator, receiverContactNumberDTO);

		// check to verify if sender of connection request is blocked by receiver
		isInitiatorBlocked = masterService.isUserBlockedByContact(request.getReceiverUserId(), initiator.getUserId());

		if (isRequestAlreadySent || isAlreadyConnected || isReceiverContactSameAsInitiatorContact) {

			// throw exception when list of activityContacts is of size 1
			throwExceptionIfRequestIsSentOrIsConnectedContact(isRequestAlreadySent, isAlreadyConnected,
					isReceiverContactSameAsInitiatorContact, request, activityContactsListSize);

		} else {

			request.setInitiatorUserId(initiator.getUserId());
			request.setInitiatorName(initiator.getNameValue());
			request.setInitiatorNumber(initiator.getVerifiedContactNumber());

			updateConnectionStatus(initiator, receiverUser, request);

			return createConnectionRequestActivityAndSendPushNotification(initiator, isInitiatorBlocked, request);
		}

		return null;
	}

	private UserActivity createConnectionRequestActivityAndSendPushNotification(PeopleUser initiator,
			boolean isInitiatorBlocked, SendConnectionRequest request) {
		UserActivity otherActivity = userActivityRepository
				.getPendingConnectionRequestActivity(request.getReceiverUserId(), request.getInitiatorUserId());

		if (otherActivity != null) {
			// if user already has a connection request from the receiver, then instead of
			// new connection
			// request, accept the previous request and follow accept connection request
			// flow.

			// In this flow initiator becomes acceptor
			UserConnection acceptorToInitiatorConnection = acceptConnectionRequestAlreadyReceived(otherActivity,
					request.getSharedPrivacyProfileKey());

			// triggering silent notification to acceptor(using initiatorId, since this used
			// becomes
			// acceptor for existing connection request)
			queueService.sendPayloadToSQS(notificationService.prepareSQSPayloadForSilentNotification(
					initiator.getUserId(), RequestType.CONNECTION_REQUEST_ACCEPTED.getValue(), null,
					acceptorToInitiatorConnection.getConnectionId(), null));
			return null;
		} else {
			UserActivity createdActivity = userActivityRepository
					.save(createActivityForConnectionRequest(request, isInitiatorBlocked));

			if (!isInitiatorBlocked) {
				queueService.sendPayloadToSQS(
						prepareSQSPayloadForSendConnectionRequest(request, createdActivity, initiator));
			}
			return createdActivity;
		}
	}

	private void throwExceptionIfRequestIsSentOrIsConnectedContact(Boolean isRequestSent, Boolean isConnectedContact,
			Boolean isReceiverContactSameAsInitiatorContact, SendConnectionRequest request,
			int validActivityContactsSize) {
		if (validActivityContactsSize == 1) {
			if (isConnectedContact) {
				throw new BadRequestException(MessageCodes.ALREADY_CONNECTED.getValue());
			} else if (isRequestSent) {
				throw new BadRequestException(
						String.format(messages.get(MessageCodes.CONNECTION_REQUEST_ALREADY_SENT_TO_NUMBER.getValue()),
								request.getReceiverNumber().getPhoneNumber()));
			} else if (isReceiverContactSameAsInitiatorContact) {
				throw new BadRequestException(MessageCodes.CANNOT_SEND_CONNECTION_REQUEST_TO_SELF.getValue());
			}
		}
	}

	private boolean isConnectionRequestAlreadySentToContact(PeopleUser initiator, ContactNumberDTO receiverContact) {
		boolean requestSent = false;

		UserActivity pendingActivity = userActivityRepository.getPendingConnectionRequestActivity(initiator.getUserId(),
				receiverContact);

		if (pendingActivity != null) {
			requestSent = true;
		}

		return requestSent;
	}

	private void updateConnectionStatus(PeopleUser initiator, PeopleUser receiverUser, SendConnectionRequest request) {
		ContactNumberDTO receiverUserContactNumber;
		if (receiverUser != null) {
			receiverUserContactNumber = receiverUser.getVerifiedContactNumber();
		} else {
			receiverUserContactNumber = request.getReceiverNumber();
		}
		List<UserConnection> staticContactsWithSameContactNumber = userConnectionRepository
				.findByFromIdAndPhoneNumberAndStatus(initiator.getUserId(), receiverUserContactNumber.getPhoneNumber(),
						Collections.singletonList(ConnectionStatus.NOT_CONNECTED.getValue()));

		if (!PeopleUtils.isNullOrEmpty(staticContactsWithSameContactNumber)) {
			UserConnection userConnection = staticContactsWithSameContactNumber.get(0);
			userConnection.setConnectionStatus(ConnectionStatus.PENDING);
			userConnectionRepository.save(userConnection);
			// set "connectionId"
			request.setReceiverConnectionId(userConnection.getConnectionId());
		}
	}

	private PeopleUser getReceiverUser(UserConnection sharedConnection) {

		PeopleUser receiverUser = null;

		// check if the shared contact is a 'people user' from the shared connection
		if (sharedConnection != null && sharedConnection.getConnectionToId() != null) {
			receiverUser = peopleUserRepository.findByUserIdAndStatus(sharedConnection.getConnectionToId(),
					UserStatus.ACTIVE);
		}

		// We will check if the receiver is a people user even if "NOT_CONNECTED" in
		// shared connection
		// we will use contact number to get a matched people user
		if (receiverUser == null) {
			ContactNumberDTO contactNumberForTargetUser = getContactNumberForTargetUser(sharedConnection);
			if (contactNumberForTargetUser != null) {
				receiverUser = peopleUserRepository.findByCodeAndNumber(contactNumberForTargetUser.getCountryCode(),
						contactNumberForTargetUser.getPhoneNumber());
			}

		}

		return receiverUser;
	}

	private ContactNumberDTO getContactNumberForTargetUser(UserConnection sharedConnection) {
		ContactNumberDTO contactNumberDTO = null;

		if (sharedConnection == null) {
			return null;
		}

		UserInformationDTO contactStaticData = sharedConnection.getContactStaticData();
		List<UserProfileData> userMetadataList = contactStaticData.getUserMetadataList();

		for (UserProfileData userProfileData : userMetadataList) {
			if (UserInfoCategory.CONTACT_NUMBER.getValue().equalsIgnoreCase(userProfileData.getCategory())) {
				contactNumberDTO = checkIfCountryCodeAndPhoneNumberAreValid(userProfileData.getContactNumber());
			}

			if (contactNumberDTO != null) {
				break;
			}
		}
		return contactNumberDTO;
	}

	private ContactNumberDTO checkIfCountryCodeAndPhoneNumberAreValid(ContactNumberDTO contactNumber) {
		if (!PeopleUtils.isNullOrEmpty(contactNumber.getCountryCode())
				&& (!PeopleUtils.isNullOrEmpty(contactNumber.getPhoneNumber())
						&& contactNumber.getPhoneNumber().length() == 10)) {
			return contactNumber;
		}
		return null;
	}

	private List<String> prepareConnectionListFromUserContactList(List<UserContact> userContactList) {

		List<String> connectionIdList = new ArrayList<>();
		for (UserContact userContact : PeopleUtils.emptyIfNull(userContactList)) {
			connectionIdList.add(userContact.getConnectionId());
		}
		return connectionIdList;
	}

	private void applyRulesForSendConnectionRequestSingle(SendConnectionRequest sendConnectionRequest) {

		String initiatorUserId = sendConnectionRequest.getInitiatorUserId();
		String receiverUserId = sendConnectionRequest.getReceiverUserId();
		ContactNumberDTO receiverNumber = sendConnectionRequest.getReceiverNumber();
		ContactNumberDTO initiatorNumber = sendConnectionRequest.getInitiatorNumber();

		// Rule 1: should not send to himself
		if ((receiverUserId != null && receiverUserId.equals(initiatorUserId))
				|| receiverNumber != null && receiverNumber.equals(initiatorNumber)) {
			throw new BadRequestException(MessageCodes.CANNOT_SEND_CONNECTION_REQUEST_TO_SELF.getValue());
		}

		// Rule 2 : check if any connection between initiator and receiver
		if (receiverUserId != null && initiatorUserId != null) {

			UserConnection userConnection = userConnectionRepository.findConnectionByFromIdAndToId(initiatorUserId,
					receiverUserId);
			if (userConnection != null) {
				throw new BadRequestException(MessageCodes.ALREADY_CONNECTED.getValue());
			}
		}

		// Rule 3 : check if any pending connection request activity from this user to
		// this number
		UserActivity pendingActivity = userActivityRepository.getPendingConnectionRequestActivity(initiatorUserId,
				sendConnectionRequest.getReceiverNumber());

		if (pendingActivity != null) {
			throw new BadRequestException(
					String.format(messages.get(MessageCodes.CONNECTION_REQUEST_ALREADY_SENT_TO_NUMBER.getValue()),
							sendConnectionRequest.getReceiverNumber().getPhoneNumber()));
		}

		// Rule 4 : connection request can be sent only to USA or canada numbers
		if (!masterService.isValidCanadaOrUSANumber(receiverNumber)) {
			throw new BadRequestException(messages.get(MessageCodes.INVALID_COUNTRY_CODE_OR_PHONE_NUMBER.getValue()));
		}
	}

	private Boolean applyRulesForSendConnectionRequestBulk(PeopleUser initiator, UserConnection userConnection,
			SendConnectionRequest sendConnectionRequest) {

		String initiatorUserId = sendConnectionRequest.getInitiatorUserId();
		String receiverUserId = sendConnectionRequest.getReceiverUserId();
		ContactNumberDTO receiverNumber = sendConnectionRequest.getReceiverNumber();
		ContactNumberDTO initiatorNumber = sendConnectionRequest.getInitiatorNumber();

		// Rule 1: should not send to himself
		if ((receiverUserId != null && receiverUserId.equals(initiatorUserId))
				|| receiverNumber != null && receiverNumber.equals(initiatorNumber)) {
			return Boolean.TRUE;
		}

		// Rule 2 : check if invalid connectionId
		if (userConnection == null) {
			return Boolean.TRUE;
		}

		// Rule 3 : check if already connected
		if (isContactInConnectedState(userConnection)) {
			return Boolean.TRUE;
		}

		// Rule 4 : check if any pending connection request activity from this user to
		// this number
		UserActivity pendingActivity = userActivityRepository.getPendingConnectionRequestActivity(initiatorUserId,
				sendConnectionRequest.getReceiverNumber());
		if (pendingActivity != null) {
			return Boolean.TRUE;
		}

		// Rule 5 : check if the receiver is blocked by the initiator
		if (receiverUserId != null && initiator.getBlockedUserIdList().contains(receiverUserId)) {
			return Boolean.TRUE;
		}

		// Rule 6 : connection request can be sent only to USA or canada numbers
		return !masterService.isValidCanadaOrUSANumber(receiverNumber);
	}

	private SQSPayload prepareSQSPayloadForSendConnectionRequest(SendConnectionRequest sendConnectionRequest,
			UserActivity connectionRequestActivity, PeopleUser sessionUser) {

		String initiatorName = sendConnectionRequest.getInitiatorName();

		if (sendConnectionRequest.getReceiverUserId() == null) {
			// prepare SMS payload
			Object[] messageParam = new Object[] { initiatorName,
					sessionUser.getVerifiedContactNumber().getMobileNumber(), appLink };
			return notificationService.prepareSQSPayloadForSMS(sendConnectionRequest.getReceiverNumber(),
					SMSTemplateKeys.SEND_CONNECTION_REQUEST_TO_NON_WATU_CONTACT, messageParam);
		} else if (masterService.isPushNotificationEnabledForUser(connectionRequestActivity.getActivityForId())) {
			PushNotificationDTO pushNotificationDTO = new PushNotificationDTO();
			pushNotificationDTO.setActivityId(connectionRequestActivity.getActivityId());
			pushNotificationDTO.setActivityRequestType(connectionRequestActivity.getActivityType().getRequestType());
			pushNotificationDTO.setInitiatorName(PeopleUtils.getDefaultOrEmpty(sessionUser.getFullName()));
			pushNotificationDTO.setReceiverUserId(connectionRequestActivity.getActivityForId());
			pushNotificationDTO.setActivityMessage(connectionRequestActivity.getMessage());

			// prepare notification payload
			return notificationService.prepareSQSPayloadForNotification(sendConnectionRequest.getReceiverUserId(),
					pushNotificationDTO);
		}
		return null;
	}

	@Override
	public ActivityDetails moreInfoRequest(RequestMoreInfoDTO request) {

		PeopleUser sessionUser = tokenAuthService.getSessionUser();
		Boolean isContactBlockedByInitiate = Boolean.FALSE;

		// check if request is for a valid connection
		UserConnection userConnection = userConnectionRepository.findConnectionByConnectionId(sessionUser.getUserId(),
				request.getConnectionId());

		if (userConnection == null) {
			throw new BadRequestException(MessageCodes.INVALID_CONNECTION.getValue());
		}

		// initiate request - create activity
		UserActivity moreInfoRequestActivity = createActivityForMoreInfo(sessionUser.getUserId(), userConnection);
		moreInfoRequestActivity.setMessage(request.getMessage());

		// otherConnection should never be null, so not putting null check
		if (isContactInBlockedState(sessionUser, userConnection)) {
			isContactBlockedByInitiate = Boolean.TRUE;
		}

		UserActivity persistedActivity = userActivityRepository.save(moreInfoRequestActivity);

		if (!isContactBlockedByInitiate
				&& masterService.isPushNotificationEnabledForUser(persistedActivity.getActivityForId())) {
			UserConnection otherConnection = getOtherConnection(sessionUser.getUserId(), request.getConnectionId());

			PushNotificationDTO pushNotificationDTO = new PushNotificationDTO();
			pushNotificationDTO.setActivityId(persistedActivity.getActivityId());
			pushNotificationDTO.setActivityRequestType(persistedActivity.getActivityType().getRequestType());
			pushNotificationDTO.setInitiatorName(PeopleUtils.getDefaultOrEmpty(sessionUser.getFullName()));
			pushNotificationDTO.setReceiverUserId(persistedActivity.getActivityForId());
			pushNotificationDTO.setActivityMessage(persistedActivity.getMessage());
			pushNotificationDTO.setConnectionId(otherConnection.getConnectionId());

			SQSPayload sqsPayload = notificationService
					.prepareSQSPayloadForNotification(persistedActivity.getActivityForId(), pushNotificationDTO);

			queueService.sendPayloadToSQS(new ArrayList<>(Collections.singletonList(sqsPayload)));

		}
		// prepare ActivityDetails
		return userActivityService.prepareActivityDetails(persistedActivity, null, Boolean.TRUE);
	}

	private Boolean isContactInBlockedState(PeopleUser peopleUser, UserConnection userContact) {

		Set<String> blockedIdList = peopleUser.getBlockedUserIdList();
		String toUserId = userContact.getConnectionToId();

		if (toUserId != null) {
			return blockedIdList.contains(toUserId);
		} else {
			return Boolean.FALSE;
		}

	}

	@Override
	public void introduceContactRequest(SendSingleIntroRequestDTO sendSingleIntroRequest) {

		List<SQSPayload> sqsPayloadList = new ArrayList<>();
		PeopleUser sessionUser = tokenAuthService.getSessionUser();
		String initiatorName = sessionUser.getNameValue();

		/* Initiator details */
		String initiatorUserId = sessionUser.getUserId();

		UserContact introducedUserContact = sendSingleIntroRequest.getIntroducedContact();
		Set<UserContact> initiateUserContact = new HashSet<>(sendSingleIntroRequest.getInitiateContactDetailsList());

		// updating missing contact numbers for receiver and initiateUserContact
		updateMissingContactNumbers(initiatorUserId, new ArrayList<>(Arrays.asList(introducedUserContact)));

		updateMissingContactNumbers(initiatorUserId, new ArrayList<>(initiateUserContact));

		// introduction request is both ways:
		// one to many
		OneToManyIntroduction oneToManyIntroductionRequest = new OneToManyIntroduction();
		oneToManyIntroductionRequest.setInitiatorId(initiatorUserId);
		oneToManyIntroductionRequest.setIntroductionMessage(sendSingleIntroRequest.getMessage());
		oneToManyIntroductionRequest.setReceiver(introducedUserContact);
		oneToManyIntroductionRequest.setIntroducedContactList(new ArrayList<>(initiateUserContact));
		oneToManyIntroductionRequest.setInitiatorName(initiatorName);

		// apply rule for one to many introduction
		applyRulesForOneToManyIntroduction(oneToManyIntroductionRequest);

		UserActivity userActivity = createActivityForOneToManyIntroductionWithActivityContacts(
				oneToManyIntroductionRequest);

		sqsPayloadList.add(
				prepareSQSPayloadForOneToManyIntroduction(oneToManyIntroductionRequest, userActivity, sessionUser));

		// one to one
		for (UserContact receiver : PeopleUtils.emptyIfNull(initiateUserContact)) {

			OneToOneIntroduction oneToOneIntroductionRequest = new OneToOneIntroduction();
			oneToOneIntroductionRequest.setInitiatorId(initiatorUserId);
			oneToOneIntroductionRequest.setIntroducedContact(introducedUserContact);
			oneToOneIntroductionRequest.setIntroductionMessage(sendSingleIntroRequest.getMessage());
			oneToOneIntroductionRequest.setReceiver(receiver);
			oneToOneIntroductionRequest.setInitiatorName(initiatorName);

			applyRulesForOneToOneIntroduction(oneToOneIntroductionRequest);
			UserActivity userPersistedActivity = createActivityForOneToOneIntroduction(oneToOneIntroductionRequest);

			sqsPayloadList.add(prepareSQSPayloadForOneToOneIntroduction(oneToOneIntroductionRequest,
					userPersistedActivity, sessionUser));

		}

		// Send SMS and Push notification
		queueService.sendPayloadToSQS(sqsPayloadList);

	}

	private SQSPayload prepareSQSPayloadForOneToManyIntroduction(OneToManyIntroduction oneToManyIntroductionRequest,
			UserActivity persistedActivity, PeopleUser sessionUser) {

		if (oneToManyIntroductionRequest.getReceiverId() == null) {

			String initiatorName = oneToManyIntroductionRequest.getInitiatorName();
			List<UserContact> introducedUserList = oneToManyIntroductionRequest.getIntroducedContactList();
			// prepare SQS Payload for one to many notification
			Map<ContactNumberDTO, PeopleUser> contactNumberToUserMap = oneToManyIntroductionRequest
					.getContactNumberToUserMap();

			List<String> introducedUserDetail = new ArrayList<>();
			for (UserContact introducedUser : PeopleUtils.emptyIfNull(introducedUserList)) {

				ContactNumberDTO number = introducedUser.getContactNumber();
				if (contactNumberToUserMap.containsKey(number)) {
					introducedUserDetail.add(contactNumberToUserMap.get(number).getNameValue());
				} else {
					introducedUserDetail.add(number.getMobileNumber());
				}
			}

			// prepare SMS payload
			Object[] messageParam = new Object[] { initiatorName, introducedUserDetail.get(0), appLink };
			return notificationService.prepareSQSPayloadForSMS(
					oneToManyIntroductionRequest.getReceiver().getContactNumber(),
					SMSTemplateKeys.APP_JOIN_INTRODUCTION_REQUEST, messageParam);

		} else if (masterService.isPushNotificationEnabledForUser(persistedActivity.getActivityForId())) {

			PushNotificationDTO pushNotificationDTO = new PushNotificationDTO();
			pushNotificationDTO.setActivityId(persistedActivity.getActivityId());
			pushNotificationDTO.setActivityRequestType(persistedActivity.getActivityType().getRequestType());
			pushNotificationDTO.setInitiatorName(PeopleUtils.getDefaultOrEmpty(sessionUser.getFullName()));
			pushNotificationDTO.setReceiverUserId(persistedActivity.getActivityForId());
			pushNotificationDTO.setActivityMessage(persistedActivity.getMessage());
			pushNotificationDTO.setNumberOfContacts(persistedActivity.getIntroducedContactNumber().size());

			// prepare notification payload
			return notificationService.prepareSQSPayloadForNotification(oneToManyIntroductionRequest.getReceiverId(),
					pushNotificationDTO);
		}
		return null;
	}

	private SQSPayload prepareSQSPayloadForOneToOneIntroduction(OneToOneIntroduction oneToOneIntroductionRequest,
			UserActivity persistedActivity, PeopleUser sessionUser) {

		String initiatorName = oneToOneIntroductionRequest.getInitiatorName();
		UserContact introducedUser = oneToOneIntroductionRequest.getIntroducedContact();
		// prepare SQS Payload for one to one notification
		Map<ContactNumberDTO, PeopleUser> contactNumberToUserMap = oneToOneIntroductionRequest
				.getContactNumberToUserMap();

		String introducedUserDetail;
		if (contactNumberToUserMap.containsKey(introducedUser.getContactNumber())) {
			introducedUserDetail = contactNumberToUserMap.get(introducedUser.getContactNumber()).getNameValue();
		} else {
			introducedUserDetail = introducedUser.getContactNumber().getMobileNumber();
		}

		if (oneToOneIntroductionRequest.getReceiverId() == null) {

			// prepare SMS payload
			Object[] messageParam = new Object[] { initiatorName, introducedUserDetail, appLink };
			return notificationService.prepareSQSPayloadForSMS(
					oneToOneIntroductionRequest.getReceiver().getContactNumber(),
					SMSTemplateKeys.APP_JOIN_INTRODUCTION_REQUEST, messageParam);

		} else if (masterService.isPushNotificationEnabledForUser(persistedActivity.getActivityForId())) {
			PushNotificationDTO pushNotificationDTO = new PushNotificationDTO();
			pushNotificationDTO.setActivityId(persistedActivity.getActivityId());
			pushNotificationDTO.setActivityRequestType(persistedActivity.getActivityType().getRequestType());
			pushNotificationDTO.setInitiatorName(PeopleUtils.getDefaultOrEmpty(sessionUser.getFullName()));
			pushNotificationDTO.setReceiverUserId(persistedActivity.getActivityForId());
			pushNotificationDTO.setActivityMessage(persistedActivity.getMessage());
			pushNotificationDTO.setNumberOfContacts(persistedActivity.getIntroducedContactNumber().size());

			// prepare notification payload
			return notificationService.prepareSQSPayloadForNotification(oneToOneIntroductionRequest.getReceiverId(),
					pushNotificationDTO);
		}
		return null;
	}

	private Map<ContactNumberDTO, PeopleUser> prepareContactNumberToUserMap(List<ContactNumberDTO> contactNumberList) {

		List<PeopleUser> peopleUserList = peopleUserRepository.findByContactNumberWithLimitedFields(contactNumberList);
		Map<ContactNumberDTO, PeopleUser> contactNumberToUserMap = new HashMap<>();
		for (PeopleUser peopleUser : PeopleUtils.emptyIfNull(peopleUserList)) {
			contactNumberToUserMap.put(peopleUser.getVerifiedContactNumber(), peopleUser);
		}

		return contactNumberToUserMap;

	}

	private List<ContactNumberDTO> getContactNumberList(List<UserContact> userContactList) {

		List<ContactNumberDTO> contactNumberList = new ArrayList<>();
		for (UserContact userContact : PeopleUtils.emptyIfNull(userContactList)) {
			if (userContact.getContactNumber() != null) {
				ContactNumberDTO contactNumber = prepareContactNumberWithDefaultCode(userContact.getContactNumber());
				userContact.setContactNumber(contactNumber);
				contactNumberList.add(contactNumber);
			}
		}

		return contactNumberList;
	}

	private UserActivity createActivityForOneToManyIntroductionWithActivityContacts(
			OneToManyIntroduction oneToManyIntroduction) {

		ObjectId requestId = new ObjectId();

		UserActivity userActivity = new UserActivity();
		userActivity.setRequestId(requestId.toString());
		userActivity.setActivityById(oneToManyIntroduction.getInitiatorId());
		userActivity.setActivityForId(oneToManyIntroduction.getReceiverId());
		userActivity.setInitiateDetails(oneToManyIntroduction.getReceiver());

		List<String> introducedContactNumbers = new ArrayList<>();

		for (UserContact userContact : oneToManyIntroduction.getIntroducedContactList()) {
			ContactNumberDTO contactNumberDTO = userContact.getContactNumber();

			introducedContactNumbers.add(contactNumberDTO.getCountryCode() + "_" + contactNumberDTO.getPhoneNumber());
		}
		userActivity.setIntroducedContactNumber(introducedContactNumbers);

		ActivityType activityType = new ActivityType();
		activityType.setRequestType(RequestType.INTRODUCTION_REQUEST);
		activityType.setActionTaken(Action.INITIATED);
		userActivity.setActivityType(activityType);
		userActivity.setMessage(oneToManyIntroduction.getIntroductionMessage());
		userActivity.setOverallStatus(ActivityStatus.PENDING);
		DateTime currentDateTime = PeopleUtils.getCurrentTimeInUTC();
		userActivity.setCreatedOn(currentDateTime);
		userActivity.setLastUpdatedOn(currentDateTime);
		userActivity.setIsInitiatorBlocked(masterService.isUserBlockedByContact(oneToManyIntroduction.getReceiverId(),
				oneToManyIntroduction.getInitiatorId()));

		userActivity = userActivityRepository.save(userActivity);

		// Persist ActivityContact(s) for introduced user
		for (UserContact userContact : oneToManyIntroduction.getIntroducedContactList()) {
			ActivityContact activityContact = new ActivityContact();
			activityContact.setActivityId(userActivity.getActivityId());
			activityContact.setConnectionId(userContact.getConnectionId());
			activityContact.setInitiatorId(oneToManyIntroduction.getInitiatorId());
			activityContact.setReceiverId(oneToManyIntroduction.getReceiverId());
			activityContact.setIsActive(Boolean.TRUE);
			activityContact.setRequestType(RequestType.INTRODUCTION_REQUEST);
			activityContact.setIntroducedContactNumber(userContact.getContactNumber());

			activityContactRepository.save(activityContact);

		}

		return userActivity;
	}

	private UserActivity createActivityForOneToOneIntroduction(OneToOneIntroduction oneToOneIntroduction) {

		UserActivity userActivity = new UserActivity();
		userActivity.setActivityById(oneToOneIntroduction.getInitiatorId());
		userActivity.setActivityForId(oneToOneIntroduction.getReceiverId());
		userActivity.setInitiateDetails(oneToOneIntroduction.getReceiver());

		List<String> introducedContactNumbers = new ArrayList<>();

		ContactNumberDTO contactNumberDTO = oneToOneIntroduction.getIntroducedContact().getContactNumber();
		introducedContactNumbers.add(contactNumberDTO.getCountryCode() + "_" + contactNumberDTO.getPhoneNumber());
		userActivity.setIsInitiatorBlocked(masterService.isUserBlockedByContact(oneToOneIntroduction.getReceiverId(),
				oneToOneIntroduction.getInitiatorId()));
		userActivity.setIntroducedContactNumber(introducedContactNumbers);

		ActivityType activityType = new ActivityType();
		activityType.setRequestType(RequestType.INTRODUCTION_REQUEST);
		activityType.setActionTaken(Action.INITIATED);
		userActivity.setActivityType(activityType);
		userActivity.setMessage(oneToOneIntroduction.getIntroductionMessage());
		userActivity.setOverallStatus(ActivityStatus.PENDING);
		DateTime currentDateTime = PeopleUtils.getCurrentTimeInUTC();
		userActivity.setCreatedOn(currentDateTime);
		userActivity.setLastUpdatedOn(currentDateTime);

		userActivityRepository.save(userActivity);

		UserContact userContact = oneToOneIntroduction.getIntroducedContact();
		// Persist ActivityContact(s) for initiated user
		ActivityContact activityContact = new ActivityContact();
		activityContact.setActivityId(userActivity.getActivityId());
		activityContact.setConnectionId(userContact.getConnectionId());
		activityContact.setInitiatorId(oneToOneIntroduction.getInitiatorId());
		activityContact.setReceiverId(oneToOneIntroduction.getReceiverId());
		activityContact.setIsActive(Boolean.TRUE);
		activityContact.setRequestType(RequestType.INTRODUCTION_REQUEST);
		activityContact.setIntroducedContactNumber(userContact.getContactNumber());

		activityContactRepository.save(activityContact);

		return userActivity;

	}

	private void applyRulesForOneToManyIntroduction(OneToManyIntroduction oneToManyIntroduction) {

		UserContact receiver = oneToManyIntroduction.getReceiver();
		List<UserContact> introducedContactList = oneToManyIntroduction.getIntroducedContactList();

		List<UserContact> userContactList = new ArrayList<>(introducedContactList);
		userContactList.add(receiver);

		List<ContactNumberDTO> contactNumberList = getContactNumberList(userContactList);
		Map<ContactNumberDTO, PeopleUser> contactNumberToUserMap = prepareContactNumberToUserMap(contactNumberList);
		oneToManyIntroduction.setContactNumberToUserMap(contactNumberToUserMap);

		// apply rules on receiver
		String initiateUserId = null;
		ContactNumberDTO receiverNumber = prepareContactNumberWithDefaultCode(receiver.getContactNumber());
		if (contactNumberToUserMap.containsKey(receiverNumber)) {
			initiateUserId = contactNumberToUserMap.get(receiverNumber).getUserId();
			oneToManyIntroduction.setReceiverId(initiateUserId);
		}
		if (initiateUserId == null && introducedContactList.size() > 1) {
			throw new BadRequestException(MessageCodes.INVALID_OPERATION.getValue());
		}

		// avoid self introduction
		introducedContactList.remove(receiver);
	}

	private void applyRulesForOneToOneIntroduction(OneToOneIntroduction oneToOneIntroduction) {

		UserContact receiver = oneToOneIntroduction.getReceiver();
		UserContact introducedUser = oneToOneIntroduction.getIntroducedContact();

		// avoid self introduction
		if (PeopleUtils.compareValues(receiver, introducedUser)) {
			return;
		}

		List<UserContact> userContactList = new ArrayList<>();
		userContactList.add(receiver);
		userContactList.add(introducedUser);

		List<ContactNumberDTO> contactNumberList = getContactNumberList(userContactList);
		Map<ContactNumberDTO, PeopleUser> contactNumberToUserMap = prepareContactNumberToUserMap(contactNumberList);
		oneToOneIntroduction.setContactNumberToUserMap(contactNumberToUserMap);

		// apply rules on receiver
		String initiateUserId;
		ContactNumberDTO receiverNumber = prepareContactNumberWithDefaultCode(receiver.getContactNumber());
		if (contactNumberToUserMap.containsKey(receiverNumber)) {
			initiateUserId = contactNumberToUserMap.get(receiverNumber).getUserId();
			oneToOneIntroduction.setReceiverId(initiateUserId);
		}
	}

	@Override
	public void introduceContactToEachOtherRequest(SendMultiIntroRequestDTO sendMultiIntroRequest) {

		PeopleUser sessionUser = tokenAuthService.getSessionUser();
		String initiatorUserId = sessionUser.getUserId();
		String initiatorName = sessionUser.getNameValue();
		List<SQSPayload> sqsPayloadList = new ArrayList<>();

		List<UserContact> userContactList = sendMultiIntroRequest.getContactDetailsList();
		List<UserContact> introducedContactList;

		updateContactNumbers(initiatorUserId, userContactList);
		for (UserContact receiver : PeopleUtils.emptyIfNull(userContactList)) {

			introducedContactList = new ArrayList<>(userContactList);

			introducedContactList.remove(receiver);

			// one to many
			OneToManyIntroduction oneToManyIntroductionRequest = new OneToManyIntroduction();
			oneToManyIntroductionRequest.setInitiatorId(initiatorUserId);
			oneToManyIntroductionRequest.setIntroductionMessage(sendMultiIntroRequest.getMessage());
			oneToManyIntroductionRequest.setReceiver(receiver);
			oneToManyIntroductionRequest.setReceiverId(receiver.getUserId());
			oneToManyIntroductionRequest.setIntroducedContactList(new ArrayList<>(introducedContactList));
			oneToManyIntroductionRequest.setInitiatorName(initiatorName);

			UserActivity userActivity = createActivityForOneToManyIntroductionWithActivityContacts(
					oneToManyIntroductionRequest);

			sqsPayloadList.add(
					prepareSQSPayloadForOneToManyIntroduction(oneToManyIntroductionRequest, userActivity, sessionUser));
		}

		queueService.sendPayloadToSQS(sqsPayloadList);

	}

	@Override
	public String shareContact(ShareContactRequest shareContactRequest) {

		PeopleUser sessionUser = tokenAuthService.getSessionUser();

		List<String> sharedWithConnectionIdList = shareContactRequest.getSharedWithConnectionIdList();
		List<SQSPayload> sqsPayloadList = new ArrayList<>();

		// get all valid shared with connections
		List<UserConnection> validConnectionList = userConnectionRepository
				.findConnectionByConnectionId(sessionUser.getUserId(), sharedWithConnectionIdList);

		if (PeopleUtils.isNullOrEmpty(validConnectionList)) {
			throw new BadRequestException(MessageCodes.INVALID_CONNECTION.getValue());
		}

		Set<String> blockedUsers = sessionUser.getBlockedUserIdList();

		int blockedUsersCount = 0;

		for (UserConnection connection : validConnectionList) {
			/* contacts can not be shared with blocked users */
			if (blockedUsers.contains(connection.getConnectionToId())) {
				blockedUsersCount++;
				continue;
			}
			shareContactRequest.setReceiverUserId(connection.getConnectionToId());
			shareContactRequest.setReceiverConnectionId(connection.getConnectionId());
			shareContactRequest.setInitiatorName(sessionUser.getNameValue());
			shareContactRequest.setInitiatorUserId(sessionUser.getUserId());

			// Create activity for a particular
			boolean isInitiatorBlocked = masterService.isUserBlockedByContact(connection.getConnectionToId(),
					sessionUser.getUserId());

			UserActivity activity = createActivityForShareContact(shareContactRequest, isInitiatorBlocked);
			if (activity != null) {

				activity = userActivityRepository.save(activity);

				// Save activity contacts for belongs to this activity
				for (String id : PeopleUtils.emptyIfNull(activity.getSharedConnectionIdList())) {
					ActivityContact activityContact = new ActivityContact();
					activityContact.setActivityId(activity.getActivityId());
					activityContact.setConnectionId(id);
					activityContact.setInitiatorId(sessionUser.getUserId());
					activityContact.setReceiverId(connection.getConnectionToId());
					activityContact.setIsActive(Boolean.TRUE);
					activityContact.setRequestType(RequestType.SHARE_CONTACT_ACTIVITY);

					activityContactRepository.save(activityContact);
				}

				sqsPayloadList.add(prepareSQSPayloadForShareContact(shareContactRequest, activity, sessionUser));
			}
		}

		queueService.sendPayloadToSQS(sqsPayloadList);

		/* If all connectionIds in sharedWithConnectionIdList are blocked */
		if (validConnectionList.size() == blockedUsersCount) {
			throw new BadRequestException(MessageCodes.CANNOT_PERFORM_ANY_ACTION_WITH_BLOCKED_USERS.getValue());
		}
		/* If some of connectionIds in sharedWithConnectionIdList are blocked */
		if (validConnectionList.size() > blockedUsersCount && (blockedUsersCount > 0)) {
			return messages.get(MessageCodes.SOME_ACTIONS_CANNOT_BE_PERFORMED_ON_BLOCKED_USERS.getValue());
		}
		return "Contact shared successfully";
	}

	private UserActivity createActivityForShareContact(ShareContactRequest shareContactRequest,
			boolean isInitiatorBlocked) {

		List<String> sharedContactIdList = shareContactRequest.getSharedContactIdList();

		UserActivity userActivity = new UserActivity();
		userActivity.setActivityForId(shareContactRequest.getReceiverUserId());
		userActivity.setActivityById(shareContactRequest.getInitiatorUserId());
		userActivity.setInitiateDetails(prepareInitiateDetails(shareContactRequest.getReceiverConnectionId()));
		ActivityType activityType = new ActivityType();
		activityType.setRequestType(RequestType.SHARE_CONTACT_ACTIVITY);
		activityType.setActionTaken(Action.INITIATED);
		userActivity.setActivityType(activityType);
		userActivity.setOverallStatus(ActivityStatus.ACTIVE);
		userActivity.setIsInitiatorBlocked(isInitiatorBlocked);

		Set<String> sharedConnectionIdList = new HashSet<>();

		for (String id : PeopleUtils.emptyIfNull(sharedContactIdList)) {
			if (!shareContactRequest.getReceiverConnectionId().equals(id)) {
				sharedConnectionIdList.add(id);
			}
		}

		if (sharedConnectionIdList.isEmpty()) {
			return null;
		}

		userActivity.setSharedConnectionIdList(sharedConnectionIdList);
		DateTime currentDateTime = PeopleUtils.getCurrentTimeInUTC();
		userActivity.setCreatedOn(currentDateTime);
		userActivity.setLastUpdatedOn(currentDateTime);

		return userActivity;
	}

	private SQSPayload prepareSQSPayloadForShareContact(ShareContactRequest shareContactRequest,
			UserActivity persistedActivity, PeopleUser sessionUser) {
		if (!masterService.isPushNotificationEnabledForUser(persistedActivity.getActivityForId())) {
			return null;
		}
		PushNotificationDTO pushNotificationDTO = new PushNotificationDTO();
		pushNotificationDTO.setActivityId(persistedActivity.getActivityId());
		pushNotificationDTO.setActivityRequestType(persistedActivity.getActivityType().getRequestType());
		pushNotificationDTO.setInitiatorName(PeopleUtils.getDefaultOrEmpty(sessionUser.getFullName()));
		pushNotificationDTO.setReceiverUserId(persistedActivity.getActivityForId());
		pushNotificationDTO.setActivityMessage(persistedActivity.getMessage());
		pushNotificationDTO.setNumberOfContacts(persistedActivity.getSharedConnectionIdList().size());

		return notificationService.prepareSQSPayloadForNotification(shareContactRequest.getReceiverUserId(),
				pushNotificationDTO);
	}

	private UserActivity createActivityForShareLocation(ShareLocationRequest shareLocationRequest) {

		UserActivity userActivity = new UserActivity();
		userActivity.setActivityForId(shareLocationRequest.getReceiverId());
		userActivity.setActivityById(shareLocationRequest.getInitiatorId());
		userActivity.setInitiateDetails(prepareInitiateDetails(shareLocationRequest.getReceiverConnectionId()));
		userActivity.setLocationSharedForTime(shareLocationRequest.getTimeInMinutes());

		ActivityType activityType = new ActivityType();
		activityType.setRequestType(RequestType.SHARE_LOCATION_ACTIVITY);
		activityType.setActionTaken(Action.INITIATED);
		userActivity.setActivityType(activityType);
		userActivity.setIsInitiatorBlocked(masterService.isUserBlockedByContact(shareLocationRequest.getReceiverId(),
				shareLocationRequest.getInitiatorId()));

		userActivity.setOverallStatus(ActivityStatus.ACTIVE);
		DateTime currentDateTime = PeopleUtils.getCurrentTimeInUTC();

		userActivity.setCreatedOn(currentDateTime);
		userActivity.setLastUpdatedOn(currentDateTime);
		userActivity.setExpireAt(currentDateTime.plusMinutes(shareLocationRequest.getTimeInMinutes() - 1));

		return userActivity;
	}

	@Override
	public void shareLocation(ShareLocationRequest shareLocationRequest) {

		PeopleUser sessionUser = tokenAuthService.getSessionUser();
		List<SQSPayload> sqsPayloadList = new ArrayList<>();

		List<String> sharedWithConnectionIdList = shareLocationRequest.getSharedWithConnectionIdList();
		shareLocationRequest.setInitiatorId(sessionUser.getUserId());
		shareLocationRequest.setInitiatorName(sessionUser.getNameValue());

		// get all valid connections
		List<UserConnection> validConnectionList = userConnectionRepository
				.findConnectionByConnectionIdWithLimitedFields(sessionUser.getUserId(), sharedWithConnectionIdList);

		// get all location share activity from this user
		List<UserActivity> userActivityList = userActivityRepository
				.getActiveLocationShareActivityByUser(sessionUser.getUserId());

		// prepare existing location share activity map
		Map<String, UserActivity> existingSharedLocationActivityMap = new HashMap<>();
		for (UserActivity userActivity : PeopleUtils.emptyIfNull(userActivityList)) {
			existingSharedLocationActivityMap.put(userActivity.getActivityForId(), userActivity);
		}

		for (UserConnection userConnection : PeopleUtils.emptyIfNull(validConnectionList)) {
			UserActivity existingActivity = null;
			UserActivity activityForShareLocation = null;
			// remove existing 'active' location share activity where'forActivity'
			// also present for latest location share activity receipient list
			if (existingSharedLocationActivityMap.get(userConnection.getConnectionToId()) != null) {
				existingActivity = existingSharedLocationActivityMap.get(userConnection.getConnectionToId());
				existingActivity.setLocationSharedForTime(shareLocationRequest.getTimeInMinutes());

				DateTime currentDateTime = PeopleUtils.getCurrentTimeInUTC();
				existingActivity.setCreatedOn(currentDateTime);
				existingActivity.setLastUpdatedOn(currentDateTime);
				existingActivity.setExpireAt(currentDateTime.plusMinutes(shareLocationRequest.getTimeInMinutes() - 1));
				userActivityRepository.save(existingActivity);
			}

			// prepare request for one activity
			shareLocationRequest.setReceiverConnectionId(userConnection.getConnectionId());
			shareLocationRequest.setReceiverId(userConnection.getConnectionToId());

			// create activity
			if (existingActivity != null) {
				activityForShareLocation = existingActivity;
			} else {
				activityForShareLocation = userActivityRepository
						.save(createActivityForShareLocation(shareLocationRequest));
			}
			// create payload
			sqsPayloadList.add(
					prepareSQSPayloadForLocationShare(shareLocationRequest, activityForShareLocation, sessionUser));

		}

		queueService.sendPayloadToSQS(sqsPayloadList);

	}

	private SQSPayload prepareSQSPayloadForLocationShare(ShareLocationRequest shareLocationRequest,
			UserActivity activityForShareLocation, PeopleUser sessionUser) {
		if (!masterService.isPushNotificationEnabledForUser(activityForShareLocation.getActivityForId())) {
			return null;
		}
		PushNotificationDTO pushNotificationDTO = new PushNotificationDTO();
		pushNotificationDTO.setActivityId(activityForShareLocation.getActivityId());
		pushNotificationDTO.setActivityRequestType(activityForShareLocation.getActivityType().getRequestType());
		pushNotificationDTO.setInitiatorName(PeopleUtils.getDefaultOrEmpty(sessionUser.getFullName()));
		pushNotificationDTO.setReceiverUserId(activityForShareLocation.getActivityForId());
		pushNotificationDTO.setActivityMessage(activityForShareLocation.getMessage());

		// send notification to shared with connections
		return notificationService.prepareSQSPayloadForNotification(shareLocationRequest.getReceiverId(),
				pushNotificationDTO);
	}

	@Override
	public AcceptConnectionResponseDTO acceptConnectionRequest(AcceptConnectionRequestDTO acceptConnectionRequest) {

		PeopleUser sessionUser = tokenAuthService.getSessionUser();

		// get the user activity
		UserActivity pendingRequest = getPendingRequestForAcceptor(sessionUser.getUserId(),
				acceptConnectionRequest.getActivityId());
		String acceptorId = sessionUser.getUserId();
		String initiatorId = pendingRequest.getActivityById();

		// check if valid initiator
		PeopleUser initiator = peopleUserService.findUserByUserId(initiatorId);
		validateInitiator(sessionUser, initiator);

		// Check and update any pending "connection-request" from 'current use' to
		// 'request initiator'
		checkAndUpdatePendingConnectionRequest(acceptorId, initiatorId);

		// check if shared privacy profile is valid
		checkIfValidPrivacyProfile(acceptConnectionRequest.getSharedPrivacyProfileKey().getPrivacyProfileId(),
				sessionUser.getUserId());

		// check if connection already exists
		UserConnection userConnection = userConnectionRepository.findConnectionByFromIdAndToId(initiatorId, acceptorId);
		if (userConnection != null) {
			throw new BadRequestException(MessageCodes.ALREADY_CONNECTED.getValue());
		}

		SharedProfileInformationData sharedWithInitiator = acceptConnectionRequest.getSharedPrivacyProfileKey();

		// check if valid privacy profile id

		if (sharedWithInitiator == null) {
			sharedWithInitiator = getDefaultSharedProfileData(acceptorId);
		}

		UserConnection acceptorToInitiatorConnection = acceptConnectionRequestAlreadyReceived(pendingRequest,
				acceptConnectionRequest.getSharedPrivacyProfileKey());

		// prepare response
		// fetch connection from acceptor to initiator
		UserContactData contactData = new UserContactData();
		contactData.setConnectionId(acceptorToInitiatorConnection.getConnectionId());
		contactData.setConnectionStatus(acceptorToInitiatorConnection.getConnectionStatus().getValue());
		contactData.setToUserId(acceptorToInitiatorConnection.getConnectionToId());

		UserPrivacyProfile sharedProfileWithAcceptor = privacyProfileService
				.getPrivacyProfileById(acceptorToInitiatorConnection.getSharedProfile().getPrivacyProfileId());
		acceptorToInitiatorConnection.setUserData(initiator);
		acceptorToInitiatorConnection.setPrivacyProfileData(sharedProfileWithAcceptor);
		contactData.setSharedProfileData(masterService.prepareSharedData1(acceptorToInitiatorConnection));

		PrivacyProfileData sharedProfileDataWithAcceptor = new PrivacyProfileData();
		PrivacyProfileData sharedProfileDataByAcceptor = new PrivacyProfileData();

		sharedProfileDataWithAcceptor.setProfileName(sharedProfileWithAcceptor.getProfileName());
		sharedProfileDataByAcceptor.setPrivacyProfileId(sharedWithInitiator.getPrivacyProfileId());
		sharedProfileDataByAcceptor.setValueIdList(sharedWithInitiator.getValueIdList());
		sharedProfileDataByAcceptor.setIsCompanyShared(sharedWithInitiator.getIsCompanyShared());
		sharedProfileDataByAcceptor.setIsPositionShared(sharedWithInitiator.getIsPositionShared());
		sharedProfileDataByAcceptor.setIsNickNameShared(sharedWithInitiator.getIsNickNameShared());
		sharedProfileDataByAcceptor.setIsMaidenNameShared(sharedWithInitiator.getIsMaidenNameShared());
		contactData.setSharedPrivacyProfileByContact(sharedProfileDataWithAcceptor);
		contactData.setSharedPrivacyProfileWithContact(sharedProfileDataByAcceptor);

		AcceptConnectionResponseDTO response = new AcceptConnectionResponseDTO();
		response.setContactData(contactData);

		return response;
	}

	private Boolean checkIfNumberPresentInContactData(ContactNumberDTO contactNumber,
			UserInformationDTO contactMetadata) {

		if (contactMetadata == null) {
			return Boolean.FALSE;
		}

		List<String> mobileNumberList = contactMetadata.getAllMobileNumbers();
		for (String mobileNumber : PeopleUtils.emptyIfNull(mobileNumberList)) {
			if (mobileNumber.equals(contactNumber.getMobileNumber())) {
				return Boolean.TRUE;
			}
		}
		return Boolean.FALSE;
	}

	private void checkIfValidPrivacyProfile(String privacyProfileId, String userId) {

		UserPrivacyProfile userPrivacyProfile = userPrivacyProfileRepository.findByProfileIdAndUserId(privacyProfileId,
				userId);
		if (userPrivacyProfile == null) {
			throw new BadRequestException(MessageCodes.INVALID_PROFILEID.getValue());
		}
	}

	@Override
	public void changePrivacyProfileForConnection(ChangePrivacyProfileRequestDTO changePrivacyProfileRequest) {

		PeopleUser sessionUser = tokenAuthService.getSessionUser();
		String userId = sessionUser.getUserId();

		// update privacy profile keys
		UserConnection userConnection = userConnectionRepository.findConnectionByConnectionId(userId,
				changePrivacyProfileRequest.getConnectionId());
		if (userConnection == null) {
			throw new BadRequestException(MessageCodes.INVALID_CONNECTION.getValue());
		}

		UserConnection otherConnection = userConnectionRepository
				.findConnectionByFromIdAndToId(userConnection.getConnectionToId(), sessionUser.getUserId());

		SharedProfileInformationData sharedProfile = otherConnection.getSharedProfile();
		SharedProfileInformationData changedProfile = changePrivacyProfileRequest.getSharedProfileInformationData();

		UserPrivacyProfile privacyProfileOld = userPrivacyProfileRepository.findByProfileIdAndUserId(
				otherConnection.getRealTimeSharedData().getPrivacyProfileId(), sessionUser.getUserId());

		UserPrivacyProfile privacyProfileNew = userPrivacyProfileRepository
				.findByProfileIdAndUserId(changedProfile.getPrivacyProfileId(), sessionUser.getUserId());

		Set<String> blockedUsers = sessionUser.getBlockedUserIdList();// Set of blockedUserIds
		// get deleted Meta List
		List<UserProfileData> deletedUserMetaList = masterService.getDeletedMetaList(sessionUser,
				privacyProfileOld.getValueIdList(), privacyProfileNew.getValueIdList());

		if (!blockedUsers.contains(userConnection.getConnectionToId())
				|| !PeopleUtils.isNullOrEmpty(deletedUserMetaList)) {
			UserInformationDTO informationDTO = Optional.ofNullable(otherConnection.getContactStaticData())
					.orElse(new UserInformationDTO());
//            informationDTO.setUserMetadataList(masterService.mergeMetaList(deletedUserMetaList,
//                    informationDTO.getUserMetadataList()));

			List<UserProfileData> profileDatas = new ArrayList<>();
			for (UserProfileData data : PeopleUtils.emptyIfNull(informationDTO.getUserMetadataList())) {
				if (data.getValueId().equalsIgnoreCase("")) {
					profileDatas.add(data);
				}
			}

			informationDTO.setUserMetadataList(profileDatas);
			otherConnection.setContactStaticData(informationDTO);
		}

		sharedProfile.setPrivacyProfileId(changedProfile.getPrivacyProfileId());
		List<String> valueIdList = new ArrayList<>();

		if (!PeopleUtils.isNullOrEmpty(changedProfile.getValueIdList())) {
			valueIdList.addAll(changedProfile.getValueIdList());
		}

		sharedProfile.setValueIdList(valueIdList);
		PeopleUtils.setIfNotNullOrEmpty(sharedProfile::setIsCompanyShared, changedProfile.getIsCompanyShared());
		PeopleUtils.setIfNotNullOrEmpty(sharedProfile::setIsPositionShared, changedProfile.getIsPositionShared());
		PeopleUtils.setIfNotNullOrEmpty(sharedProfile::setIsNickNameShared, changedProfile.getIsNickNameShared());
		PeopleUtils.setIfNotNullOrEmpty(sharedProfile::setIsMaidenNameShared, changedProfile.getIsMaidenNameShared());
		otherConnection.setRealTimeSharedData(sharedProfile);
		otherConnection.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());

		userConnectionRepository.save(otherConnection);

		// send notification to other user
		ActivityType activityType = new ActivityType();
		activityType.setRequestType(RequestType.UPDATE_CONTACT_ACTIVITY);
		activityType.setActionTaken(Action.INITIATED);

		DateTime currentDateTime = PeopleUtils.getCurrentTimeInUTC();

		if (!blockedUsers.contains(userConnection.getConnectionToId())
				&& !masterService.isUserBlockedByContact(userConnection.getConnectionFromId(), userId)) {
			boolean isInitiatorBlocked = masterService.isUserBlockedByContact(otherConnection.getConnectionFromId(),
					userId);

			// Create activity for the contact
			UserActivity userActivity = new UserActivity();
			userActivity.setActivityForId(otherConnection.getConnectionFromId());
			userActivity.setActivityById(otherConnection.getConnectionToId());
			userActivity.setActivityType(activityType);
			userActivity.setOverallStatus(ActivityStatus.ACTIVE);
			userActivity.setCreatedOn(currentDateTime);
			userActivity.setLastUpdatedOn(currentDateTime);
			userActivity.setIsInitiatorBlocked(isInitiatorBlocked);

			List<UserActivity> userActivities = userActivityRepository
					.getPendingActivitiesByInitiatedByIdAndRequestType(otherConnection.getConnectionFromId(),
							RequestType.UPDATE_CONTACT_ACTIVITY);

			if (userActivities != null) {
				for (UserActivity activity : PeopleUtils.emptyIfNull(userActivities)) {
					userActivityRepository.deleteById(activity.getActivityId());
				}
			}

			userActivityRepository.save(userActivity);

			queueService.sendPayloadToSQS(privacyProfileService.prepareSQSPayloadForUpdateContactActivity(userActivity,
					sessionUser, otherConnection.getConnectionId()));
		}
	}

	@Override
	public FavouriteContactsResponseDTO setFavouriteForContact(UpdateFavouriteRequestDTO updateFavouriteRequest) {
		PeopleUser sessionUser = tokenAuthService.getSessionUser();
		Map<String, Boolean> contactMap = new LinkedHashMap<>();

		List<FavouriteContactsSequenceNumber> contactsSequenceNumbers = new ArrayList<>();
		FavouriteContactsSequenceNumber favouriteContactsSequenceNumber;

		for (UpdateFavouriteContact favouriteContact : updateFavouriteRequest.getFavouriteContactList()) {
			contactMap.put(favouriteContact.getConnectionId(), favouriteContact.getIsFavourite());
		}

		List<String> connectionIds = new ArrayList<>(contactMap.keySet());
		// get max sequence number assigned to a contact
		UserConnection userConnectionForMaxSequenceNumber = userConnectionRepository
				.getMaxSequenceNumberConnectionForGivenUser(sessionUser.getUserId(), connectionIds);

		int maxSequenceNumber = userConnectionForMaxSequenceNumber != null
				&& userConnectionForMaxSequenceNumber.getSequenceNumber() != null
						? userConnectionForMaxSequenceNumber.getSequenceNumber()
						: -1;

		List<UserConnection> validUserConnectionList = new ArrayList<>();

		for (String connectionId : connectionIds) {
			/* valid connection from the list of requests */
			UserConnection validUserConnection = userConnectionRepository
					.findContactByConnectionId(sessionUser.getUserId(), connectionId);
			if (validUserConnection != null) {
				// Update the status and set the sequence number
				if (contactMap.get(connectionId) != null && contactMap.get(connectionId)) {
					validUserConnection.setIsFavourite(Boolean.TRUE);
					// retain existing sequence number if already present else assign new value
					updateSequenceNumberForFavouriteContact(validUserConnection, maxSequenceNumber);
					// adding the sequence for response
					favouriteContactsSequenceNumber = new FavouriteContactsSequenceNumber();
					favouriteContactsSequenceNumber.setConnectionId(validUserConnection.getConnectionId());
					favouriteContactsSequenceNumber.setSequenceNumber(validUserConnection.getSequenceNumber());
					contactsSequenceNumbers.add(favouriteContactsSequenceNumber);

				} else {
					validUserConnection.setIsFavourite(false);
					validUserConnection.setSequenceNumber(null);
				}
				validUserConnectionList.add(validUserConnection);
			}
		}
		if (PeopleUtils.isNullOrEmpty(validUserConnectionList)) {
			throw new BadRequestException(MessageCodes.INVALID_CONNECTION.getValue());
		}
		userConnectionRepository.saveAll(validUserConnectionList);
		// prepare response structure
		FavouriteContactsResponseDTO favoriteContactsResponse = new FavouriteContactsResponseDTO();
		favoriteContactsResponse.setFavouriteContacts(contactsSequenceNumbers);

		return favoriteContactsResponse;
	}

	@Override
	public AddContactsToGroupResponseDTO addContactToGroup(AddContactsToGroupRequestDTO addContactsToGroupRequest) {

		String groupOwnerId = tokenAuthService.getSessionUser().getUserId();
		String groupId = addContactsToGroupRequest.getGroupId();
		List<String> listOfContactIds = addContactsToGroupRequest.getContactIdList();

		UserGroup existingUserGroup = validateGroup(groupId, groupOwnerId);
		List<UserConnection> listOfValidContacts = getValidContactList(groupOwnerId, listOfContactIds);

		List<String> listOfValidContactIds = new ArrayList<>();

		for (UserConnection userConnection : PeopleUtils.emptyIfNull(listOfValidContacts)) {
			listOfValidContactIds.add(userConnection.getConnectionId());
		}

		List<String> existingContactList = existingUserGroup.getContactIdList();

		if (PeopleUtils.isNullOrEmpty(existingContactList)) {
			existingContactList = new ArrayList<>();
		}
		existingContactList.addAll(listOfValidContactIds);
		existingUserGroup.setContactIdList(existingContactList);

		userGroupRepository.save(existingUserGroup);
		AddContactsToGroupResponseDTO addContactsToGroupResponse = new AddContactsToGroupResponseDTO();
		addContactsToGroupResponse.setContactIdList(listOfValidContactIds);

		return addContactsToGroupResponse;

	}

	@Override
	public ContactListDTO removeContactFromGroup(RemoveContactsFromGroupRequestDTO removeContactsFromGroupRequest) {

		String groupOwnerId = tokenAuthService.getSessionUser().getUserId();
		String groupId = removeContactsFromGroupRequest.getGroupId();
		List<String> listOfContactIds = removeContactsFromGroupRequest.getContactIdList();

		UserGroup existingUserGroup = validateGroup(groupId, groupOwnerId);
		List<UserConnection> listOfValidContacts = getValidContactList(groupOwnerId, listOfContactIds);

		List<String> listOfValidContactIds = new ArrayList<>();

		for (UserConnection userConnection : PeopleUtils.emptyIfNull(listOfValidContacts)) {
			listOfValidContactIds.add(userConnection.getConnectionId());
		}

		List<String> remainingContactIds = existingUserGroup.getContactIdList();
		remainingContactIds.removeAll(listOfValidContactIds);

		existingUserGroup.setContactIdList(remainingContactIds);

		userGroupRepository.save(existingUserGroup);

		ContactListDTO contactListDTO = new ContactListDTO();
		contactListDTO.setContactList(listOfValidContactIds);

		return contactListDTO;

	}

	/**
	 * Static details of contact either connected or notConnected can be updated
	 *
	 * @return - list of edited contacts includes static data for not connected and
	 *         real-time data for connected contact
	 * @requestparam - list of contacts to be edited includes only static data.
	 */
	@Override
	public EditStaticDataResponseDTO updateContactStaticData(EditStaticDataRequestDTO editStaticDataRequest) {

		PeopleUser sessionUser = tokenAuthService.getSessionUser();

		Map<String, UserInformationDTO> connectionIdToUserInfoMap = new HashMap<>();
		for (ContactStaticData contactStaticData : PeopleUtils
				.emptyIfNull(editStaticDataRequest.getContactStaticDataList())) {

			if (contactStaticData.getStaticProfileData().getCompany() != null) {
				
				contactStaticData.getStaticProfileData()
						.setCompany(contactStaticData.getStaticProfileData().getCompany().trim());
			} else {
				contactStaticData.getStaticProfileData()
						.setCompany(contactStaticData.getStaticProfileData().getCompany());
			}

			connectionIdToUserInfoMap.put(contactStaticData.getConnectionId(),
					contactStaticData.getStaticProfileData());
		}

		// get connection details
		List<UserConnection> contactsToBeEdited = userConnectionRepository
				.getConnectionDataWithProfileForSelectedContact(sessionUser.getUserId(),
						new ArrayList<>(connectionIdToUserInfoMap.keySet()));

		if (PeopleUtils.isNullOrEmpty(contactsToBeEdited)) {
			throw new BadRequestException(MessageCodes.INVALID_CONTACT.getValue());
		}

		// contacts group information
		Map<String, List<String>> contactToGroupMap = prepareConnectionIdToGroupIdMap(sessionUser.getUserId());

		// get the list of registered numbers.
		Set<String> numberList = masterService.getRegisteredNumberList();

		List<UserContactData> updatedContactData = new ArrayList<>();
		Set<String> combinedTagListForContacts = new HashSet<>();

		for (UserConnection userConnection : PeopleUtils.emptyIfNull(contactsToBeEdited)) {

			UserInformationDTO staticInformationToBeEdited = connectionIdToUserInfoMap
					.get(userConnection.getConnectionId());

			if (userConnection.getConnectionStatus().toString().equalsIgnoreCase("CONNECTED")) {
				UserInformationDTO contactData = masterService.prepareSharedData1(userConnection);
				if (userConnection.getContactStaticData() == null) {
					userConnection.setContactStaticData(staticInformationToBeEdited);
				}
				if (contactData != null && userConnection.getContactStaticData() != null) {
					if (staticInformationToBeEdited.getName().equalsIgnoreCase(contactData.getName())) {
						staticInformationToBeEdited.setName(userConnection.getContactStaticData().getName());
					} else {
						staticInformationToBeEdited.setName(staticInformationToBeEdited.getName());
					}
					if (staticInformationToBeEdited.getFirstName().equalsIgnoreCase(contactData.getFirstName())) {
						staticInformationToBeEdited.setFirstName(userConnection.getContactStaticData().getFirstName());
					} else {
						staticInformationToBeEdited.setFirstName(staticInformationToBeEdited.getFirstName());
					}
					if (staticInformationToBeEdited.getMiddleName().equalsIgnoreCase(contactData.getMiddleName())) {
						staticInformationToBeEdited
								.setMiddleName(userConnection.getContactStaticData().getMiddleName());
					} else {
						staticInformationToBeEdited.setMiddleName(staticInformationToBeEdited.getMiddleName());
					}
					if (staticInformationToBeEdited.getLastName().equalsIgnoreCase(contactData.getLastName())) {
						staticInformationToBeEdited.setLastName(userConnection.getContactStaticData().getLastName());
					} else {
						staticInformationToBeEdited.setLastName(staticInformationToBeEdited.getLastName());
					}
					if (staticInformationToBeEdited.getFullName() != null) {
						if (staticInformationToBeEdited.getFullName().equalsIgnoreCase(contactData.getFullName())) {
							staticInformationToBeEdited
									.setFullName(userConnection.getContactStaticData().getFullName());
						} else {
							staticInformationToBeEdited.setFullName(staticInformationToBeEdited.getFullName());
						}
					}
					if (staticInformationToBeEdited.getPhoneticFirstName()
							.equalsIgnoreCase(contactData.getPhoneticFirstName())) {
						staticInformationToBeEdited
								.setPhoneticFirstName(userConnection.getContactStaticData().getPhoneticFirstName());
					} else {
						staticInformationToBeEdited
								.setPhoneticFirstName(staticInformationToBeEdited.getPhoneticFirstName());
					}
					if (staticInformationToBeEdited.getPhoneticMiddleName()
							.equalsIgnoreCase(contactData.getPhoneticMiddleName())) {
						staticInformationToBeEdited
								.setPhoneticMiddleName(userConnection.getContactStaticData().getPhoneticMiddleName());
					} else {
						staticInformationToBeEdited
								.setPhoneticMiddleName(staticInformationToBeEdited.getPhoneticMiddleName());
					}
					if (staticInformationToBeEdited.getPhoneticLastName()
							.equalsIgnoreCase(contactData.getPhoneticLastName())) {
						staticInformationToBeEdited
								.setPhoneticLastName(userConnection.getContactStaticData().getPhoneticLastName());
					} else {
						staticInformationToBeEdited
								.setPhoneticLastName(staticInformationToBeEdited.getPhoneticLastName());
					}
					if (staticInformationToBeEdited.getNamePrefix().equalsIgnoreCase(contactData.getNamePrefix())) {
						staticInformationToBeEdited
								.setNamePrefix(userConnection.getContactStaticData().getNamePrefix());
					} else {
						staticInformationToBeEdited.setNamePrefix(staticInformationToBeEdited.getNamePrefix());
					}
					if (staticInformationToBeEdited.getNameSuffix().equalsIgnoreCase(contactData.getNameSuffix())) {
						staticInformationToBeEdited
								.setNameSuffix(userConnection.getContactStaticData().getNameSuffix());
					} else {
						staticInformationToBeEdited.setNameSuffix(staticInformationToBeEdited.getNameSuffix());
					}
					if (staticInformationToBeEdited.getMaidenName().equalsIgnoreCase(contactData.getMaidenName())) {
						staticInformationToBeEdited
								.setMaidenName(userConnection.getContactStaticData().getMaidenName());
					} else {
						staticInformationToBeEdited.setMaidenName(staticInformationToBeEdited.getMaidenName());
					}
					if (staticInformationToBeEdited.getNickName().equalsIgnoreCase(contactData.getNickName())) {
						staticInformationToBeEdited.setNickName(userConnection.getContactStaticData().getNickName());
					} else {
						staticInformationToBeEdited.setNickName(staticInformationToBeEdited.getNickName());
					}
					if (staticInformationToBeEdited.getGender().equalsIgnoreCase(contactData.getGender())) {
						staticInformationToBeEdited.setGender(userConnection.getContactStaticData().getGender());
					} else {
						staticInformationToBeEdited.setGender(staticInformationToBeEdited.getGender());
					}
					if (staticInformationToBeEdited.getCompany().equalsIgnoreCase(contactData.getCompany())) {
						staticInformationToBeEdited
								.setCompany(userConnection.getContactStaticData().getCompany().trim());
					} else {
						staticInformationToBeEdited.setCompany(staticInformationToBeEdited.getCompany().trim());
					}
					if (staticInformationToBeEdited.getDepartment().equalsIgnoreCase(contactData.getDepartment())) {
						staticInformationToBeEdited
								.setDepartment(userConnection.getContactStaticData().getDepartment());
					} else {
						staticInformationToBeEdited.setDepartment(staticInformationToBeEdited.getDepartment());
					}
					if (staticInformationToBeEdited.getPosition().equalsIgnoreCase(contactData.getPosition())) {
						staticInformationToBeEdited.setPosition(userConnection.getContactStaticData().getPosition());
					} else {
						staticInformationToBeEdited.setPosition(staticInformationToBeEdited.getPosition());
					}
					if (staticInformationToBeEdited.getImageURL().equalsIgnoreCase(contactData.getImageURL())) {
						staticInformationToBeEdited.setImageURL(userConnection.getContactStaticData().getImageURL());
					} else {
						staticInformationToBeEdited.setImageURL(staticInformationToBeEdited.getImageURL());
					}
					if (staticInformationToBeEdited.getNotes().equalsIgnoreCase(contactData.getNotes())) {
						staticInformationToBeEdited.setNotes(userConnection.getContactStaticData().getNotes());
					} else {
						staticInformationToBeEdited.setNotes(staticInformationToBeEdited.getNotes());
					}
					if (staticInformationToBeEdited.getSip().equalsIgnoreCase(contactData.getSip())) {
						staticInformationToBeEdited.setSip(userConnection.getContactStaticData().getSip());
					} else {
						staticInformationToBeEdited.setSip(staticInformationToBeEdited.getSip());
					}
					if (staticInformationToBeEdited.getTagList() == contactData.getTagList()) {
						staticInformationToBeEdited.setTagList(userConnection.getContactStaticData().getTagList());
					} else {
						staticInformationToBeEdited.setTagList(staticInformationToBeEdited.getTagList());
					}
					if (staticInformationToBeEdited.getBirthday() != null) {
						if (staticInformationToBeEdited.getBirthday().equalsIgnoreCase(contactData.getBirthday())) {
							staticInformationToBeEdited
									.setBirthday(userConnection.getContactStaticData().getBirthday());
						} else {
							staticInformationToBeEdited.setBirthday(staticInformationToBeEdited.getBirthday());
						}
					}
					if (staticInformationToBeEdited.getAccountName() != null) {
						if (staticInformationToBeEdited.getAccountName()
								.equalsIgnoreCase(contactData.getAccountName())) {
							staticInformationToBeEdited
									.setAccountName(userConnection.getContactStaticData().getAccountName());
						} else {
							staticInformationToBeEdited.setAccountName(staticInformationToBeEdited.getAccountName());
						}
					}
					if (staticInformationToBeEdited.getAccountType() != null) {
						if (staticInformationToBeEdited.getAccountType()
								.equalsIgnoreCase(contactData.getAccountType())) {
							staticInformationToBeEdited
									.setAccountType(userConnection.getContactStaticData().getAccountType());
						} else {
							staticInformationToBeEdited.setAccountType(staticInformationToBeEdited.getAccountType());
						}
					}
					List<UserProfileData> profileDatas = new ArrayList<>();
					for (UserProfileData data : PeopleUtils
							.emptyIfNull(staticInformationToBeEdited.getUserMetadataList())) {
						if (data.getValueId().equalsIgnoreCase("")) {
							profileDatas.add(data);
						}
					}
					staticInformationToBeEdited.setUserMetadataList(profileDatas);
				}
			}

			userConnection.setContactStaticData(staticInformationToBeEdited);
			userConnection.setDeviceContactId(staticInformationToBeEdited.getDeviceContactId());

			// update tag or create new tag for user if doesn't exist
			List<String> contactTagList = staticInformationToBeEdited != null ? staticInformationToBeEdited.getTagList()
					: null;
			if (!PeopleUtils.isNullOrEmpty(contactTagList)) {
				combinedTagListForContacts.addAll(contactTagList);
			}

			if (!PeopleUtils.isNullOrEmpty(combinedTagListForContacts)) {
				sessionUser.setTagMap(tagService.createNewTagByUser(sessionUser.getTagMap(),
						new ArrayList<>(combinedTagListForContacts)));
				peopleUserRepository.save(sessionUser);
			}

			// update group information for contact
			if (contactToGroupMap.containsKey(userConnection.getConnectionId())) {
				userConnection.setGroupIdList(contactToGroupMap.get(userConnection.getConnectionId()));
			}

			UserContactData contactData = prepareContactStaticData(sessionUser, userConnection);
			UserInformationDTO editedContactStaticData = contactData.getStaticProfileData();

			if (editedContactStaticData != null) {
				populateStaticDataWithIsVerifiedInfo(editedContactStaticData, numberList);
			}
			switch (userConnection.getConnectionStatus()) {

			case PENDING:
				updatedContactData.add(contactData);
				break;
			case CONNECTED:
				updatedContactData.add(prepareUpdateContactSharedData(sessionUser, userConnection));
				break;
			default:
				break;
			}

			userConnection.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
		}

		List<UserConnection> saveData = userConnectionRepository.saveAll(contactsToBeEdited);
		List<UserContactData> updatedFinalContactData = new ArrayList<>();
		for (UserConnection userConnection : PeopleUtils.emptyIfNull(saveData)) {
			UserContactData contactData = prepareContactStaticData(sessionUser, userConnection);
			switch (userConnection.getConnectionStatus()) {
			case NOT_CONNECTED:
				updatedFinalContactData.add(contactData);
				break;
			case PENDING:
				updatedFinalContactData.add(contactData);
				break;
			case CONNECTED:
				updatedFinalContactData.add(prepareContactSharedData(sessionUser, userConnection));
				break;
			default:
				break;
			}
		}

		// prepare response structure
		EditStaticDataResponseDTO editStaticDataResponse = new EditStaticDataResponseDTO();
		editStaticDataResponse.setEditedContactDataList(updatedFinalContactData);

		return editStaticDataResponse;
	}

	/**
	 * This API will delete the contact and removes the contacts from groups if it
	 * was added. If the contact being delete is 'CONNECTED' then all activities
	 * w.r.t connections will be expired and contact state for the other user will
	 * be downgraded.
	 */
	@Override
	public DeleteContactResponse deleteContact(DeleteContactRequest deleteContactRequest) {

		PeopleUser sessionUser = tokenAuthService.getSessionUser();
		List<String> toBeDeletedContactIdList = deleteContactRequest.getConnectionIdList();
		List<String> otherConnectionId = new ArrayList<>();
		List<String> connectedContactIdList = new ArrayList<>();

		String sessionUserId = sessionUser.getUserId();

		/*
		 * Includes both connected contacts and static contacts
		 */
		List<UserConnection> validContactList = userConnectionRepository.findContactByConnectionId(sessionUserId,
				toBeDeletedContactIdList);

		/* Includes only connected contacts */
		List<UserConnection> connectedContactList = userConnectionRepository.findConnectionByConnectionId(sessionUserId,
				toBeDeletedContactIdList);

		if (PeopleUtils.isNullOrEmpty(validContactList)) {
			throw new BadRequestException(MessageCodes.INVALID_CONTACT.getValue());
		}

		for (UserConnection connection : PeopleUtils.emptyIfNull(connectedContactList)) {
			connectedContactIdList.add(connection.getConnectionToId());
		}

		List<UserConnection> toBeUpdatedConnectionList = new ArrayList<>();
		List<String> deletedContactIdList = new ArrayList<>();

		for (UserConnection userContact : validContactList) {

			if (isContactInConnectedState(userContact)) {

				// downgrade other connection to static contact
				// get other connection - from ID and To ID

				UserConnection otherConnection = userConnectionRepository
						.findConnectionByFromIdAndToId(userContact.getConnectionToId(), sessionUserId);

				otherConnection.setConnectionStatus(ConnectionStatus.NOT_CONNECTED);
				otherConnection.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());

				toBeUpdatedConnectionList.add(otherConnection);

				otherConnectionId.add(otherConnection.getConnectionId());
			}

			// remove connection from user groups
			userGroupRepository.removeContactIdFromUserGroups(sessionUserId, userContact.getConnectionId());

			// delete connection permanently
			removeDeletedContactFromAllSharedContactActivity(userContact);

			deletedContactIdList.add(userContact.getConnectionId());

		}

		// expire all activity and activityContacts created by this user to the
		// connectionId
		userActivityRepository.expireActivityForInitiate(sessionUserId, toBeDeletedContactIdList);
		activityContactRepository.expireActivityContactsByInitiatorIdAndReceiverId(sessionUserId,
				connectedContactIdList, false);

		if (!PeopleUtils.isNullOrEmpty(connectedContactIdList)) {
			// Expire all pending activities and activity contacts created by the 'deleted
			// contact'
			userActivityRepository.expireActivityCreatedByDeletedContact(connectedContactIdList, sessionUserId);
			activityContactRepository.expireActivityContactsByInitiatorIdAndReceiverId(sessionUserId,
					connectedContactIdList, true);
		}

		userConnectionRepository.saveAll(toBeUpdatedConnectionList);
		userConnectionRepository.deleteConnectionsByUserIdAndConnectionIds(sessionUserId, deletedContactIdList);

		// prepare response
		DeleteContactResponse response = new DeleteContactResponse();
		response.setDeletedConnectionIdList(deletedContactIdList);

		updateStaticSharedInformation(otherConnectionId);

		return response;
	}

	/**
	 * @implNote This API is used to downgrade connected contacts to not-connected
	 *           state, and does not delete the contact As the the connections are
	 *           downgraded all activities w.r.t connection are expired.
	 */
	@Override
	public RemoveConnectionResponse removeConnection(RemoveConnectionRequest removeConnectionRequest) {

		PeopleUser sessionUser = tokenAuthService.getSessionUser();
		List<String> connectionIdList = removeConnectionRequest.getConnectionIdList();
		List<String> connectionToBeUpdated = new ArrayList<>();
		List<String> removedContactsUserId = new ArrayList<>();

		// check if connection is valid connection
		List<UserConnection> validContactList = userConnectionRepository
				.findContactByConnectionId(sessionUser.getUserId(), connectionIdList);

		if (PeopleUtils.isNullOrEmpty(validContactList)) {
			throw new BadRequestException(MessageCodes.INVALID_CONTACT.getValue());
		}

		List<UserConnection> toBeUpdatedConnectionList = new ArrayList<>();
		List<String> removedConnectionIdList = new ArrayList<>();
		for (UserConnection userConnection : validContactList) {

			if (isContactInConnectedState(userConnection)) {

				// downgrade this connection to static contact
				userConnection.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
				userConnection.setConnectionStatus(ConnectionStatus.NOT_CONNECTED);

				UserConnection otherConnection = userConnectionRepository
						.findConnectionByFromIdAndToId(userConnection.getConnectionToId(), sessionUser.getUserId());
				// downgrade other connection to static contact
				otherConnection.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
				otherConnection.setConnectionStatus(ConnectionStatus.NOT_CONNECTED);

				toBeUpdatedConnectionList.add(userConnection);
				toBeUpdatedConnectionList.add(otherConnection);
				removedConnectionIdList.add(userConnection.getConnectionId());

				connectionToBeUpdated.add(userConnection.getConnectionId());
				connectionToBeUpdated.add(otherConnection.getConnectionId());
				removedContactsUserId.add(otherConnection.getConnectionFromId());

				userActivityRepository.expireActivityForInitiate(otherConnection.getConnectionFromId(),
						Collections.singletonList(otherConnection.getConnectionId()));
			}
		}

		// expire all activity created by this user to the connectionId
		userActivityRepository.expireActivityForInitiate(sessionUser.getUserId(), connectionIdList);
		userActivityRepository.expireActivityInitiatedByRemovedContact(sessionUser.getUserId(), removedContactsUserId,
				Arrays.asList(RequestType.SHARE_CONTACT_ACTIVITY, RequestType.SHARE_LOCATION_ACTIVITY));

		// expire all activity contacts
		activityContactRepository.expireActivityContactsByInitiatorIdAndReceiverId(sessionUser.getUserId(),
				removedContactsUserId, false);
		activityContactRepository.expireActivityContactsByInitiatorIdAndReceiverId(sessionUser.getUserId(),
				removedContactsUserId, true);

		userConnectionRepository.saveAll(toBeUpdatedConnectionList);

		updateStaticSharedInformation(connectionToBeUpdated);

		// creating response object
		Map<String, List<String>> contactToGroupMap = prepareConnectionIdToGroupIdMap(sessionUser.getUserId());
		List<UserContactData> removedContactDataList = new ArrayList<>();
		Set<String> numberList = masterService.getRegisteredNumberList();
		List<UserConnection> removedConnectionList = userConnectionRepository
				.getConnectionDataWithProfileForSelectedContact(sessionUser.getUserId(), removedConnectionIdList);

		// iterate over deltaContact list to fetch contact data
		for (UserConnection userContact : PeopleUtils.emptyIfNull(removedConnectionList)) {
			if (contactToGroupMap.containsKey(userContact.getConnectionId())) {
				userContact.setGroupIdList(contactToGroupMap.get(userContact.getConnectionId()));
			}
			UserContactData contactData = prepareContactStaticData(sessionUser, userContact);
			UserInformationDTO contactStaticData = contactData.getStaticProfileData();
			if (contactStaticData != null) {
				populateStaticDataWithIsVerifiedInfo(contactStaticData, numberList);
			}
			removedContactDataList.add(contactData);
		}

		// prepare response
		RemoveConnectionResponse response = new RemoveConnectionResponse();
		response.setRemovedConnectionIdList(removedContactDataList);
		return response;

	}

	@Async
	private void updateStaticSharedInformation(List<String> connectionIdList) {

		List<UserConnection> userConnectionList = userConnectionRepository
				.getSharedProfileDataForSelectedContact(connectionIdList);

		for (UserConnection userConnection : PeopleUtils.emptyIfNull(userConnectionList)) {

			userConnection.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
			userConnection.setStaticSharedProfileData(masterService.prepareSharedData1(userConnection));
			UserInformationDTO sharedData = masterService.prepareSharedData1(userConnection);

			if (userConnection.getContactStaticData() != null) {
				masterService.mergeSharedInfoToStaticInfo(sharedData, userConnection.getContactStaticData());
			} else {
				userConnection.setContactStaticData(sharedData);
			}
			userConnection.setPrivacyProfileData(null);
			userConnection.setUserData(null);
		}

		userConnectionRepository.saveAll(userConnectionList);
	}

	private Boolean isContactInConnectedState(UserConnection contact) {
		return ConnectionStatus.CONNECTED.equals(contact.getConnectionStatus());
	}

	@Override
	public FetchConnectionListResponseDTO getConnectionList(DateTime lastSyncedTime, Integer pageNumber,
			Integer pageSize, boolean returnOnlyMeta, String sortBy) {
		long startTime = System.nanoTime();

		PeopleUser sessionUser = tokenAuthService.getSessionUser();
		List<UserConnection> userContactList;
		List<UserConnection> deltaContactList;
		int totalElements;
		if (lastSyncedTime != null) {
			Sort.Order order = new Sort.Order(Direction.ASC, CONTACTSTATICDATA + sortBy);
			PageRequest pageable = PageRequest.of(pageNumber, pageSize, Sort.by(order));
			userContactList = userConnectionRepository.getConnectionDataWithProfileModifiedAfterLastSyncTime(
					sessionUser.getUserId(), lastSyncedTime, pageable);
			deltaContactList = prepareDeltaContactList(userContactList, lastSyncedTime);
			totalElements = deltaContactList.size();
			int subLimitStart = (pageNumber * pageSize) > totalElements ? totalElements : (pageNumber * pageSize);
			int subLimitEnd = ((pageNumber + 1) * pageSize) > totalElements ? totalElements
					: ((pageNumber + 1) * pageSize);
			deltaContactList = deltaContactList.subList(subLimitStart, subLimitEnd);
		} else {
			Sort.Order order = new Sort.Order(Direction.ASC, CONTACTSTATICDATA + sortBy);
			PageRequest pageable = PageRequest.of(pageNumber, pageSize, Sort.by(order));
			Page<UserConnection> userConnectionsListPaginated = userConnectionRepository
					.getConnectionDataWithProfilePaginated(sessionUser.getUserId(), pageable);
			userContactList = userConnectionsListPaginated.getContent();
			deltaContactList = new ArrayList<>(userContactList);
			totalElements = (int) userConnectionsListPaginated.getTotalElements();
		}

		logger.info("Inside getConnectionList() for user {} with {} contacts", sessionUser.getUserId(),
				userContactList.size());
		Map<String, List<String>> contactToGroupMap = prepareConnectionIdToGroupIdMap(sessionUser.getUserId());

		List<UserContactData> contactDataList = new ArrayList<>();

		if (!returnOnlyMeta) {
			logger.info("UserId: {} - Execution time after preparing Delta Contact list is {} secs",
					sessionUser.getUserId(), (System.nanoTime() - startTime) / 1_000_000_000.0);

			// get is people user data for static contacts
			Set<String> numberList = masterService.getRegisteredNumberList();

			logger.info("UserId: {} - Execution time after fetching registered number list is {} secs",
					sessionUser.getUserId(), (System.nanoTime() - startTime) / 1_000_000_000.0);
			// iterate over deltaContact list to fetch contact data
			contactDataList = getUserContactDataList(sessionUser, deltaContactList, contactToGroupMap, numberList);
		}
		logger.info("UserId: {} - Execution time after fetching contact data is {} secs", sessionUser.getUserId(),
				(System.nanoTime() - startTime) / 1_000_000_000.0);

		// prepare response
		FetchConnectionListResponseDTO response = new FetchConnectionListResponseDTO();
		response.setContactList(contactDataList);
		response.setLastSyncedTime(PeopleUtils.getCurrentTimeInUTC());
		response.setTotalNumberOfPages((long) Math.ceil((float) totalElements / pageSize));
		response.setTotalElements(totalElements);

		// used Math.floor() since the page number is starting with zero
		if (Math.floor((float) totalElements / pageSize) > pageNumber) {
			DateTime lastSyncedTimeInUTC = lastSyncedTime != null ? lastSyncedTime.toDateTime(DateTimeZone.UTC)
					: new DateTime(0).toDateTime(DateTimeZone.UTC);
			Link link = ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(UserConnectionController.class)
					.connectionsList("", lastSyncedTimeInUTC, pageNumber + 1, pageSize, returnOnlyMeta, sortBy))
					.withSelfRel();
			response.setNextURL(link.getHref());
		} else {
			response.setNextURL("");
		}

		logger.info("UserId: {} - Complete Method Execution time is {} secs", sessionUser.getUserId(),
				(System.nanoTime() - startTime) / 1_000_000_000.0);
		return response;
	}

	@Override
	public List<UserContactData> getUserContactDataList(PeopleUser sessionUser, List<UserConnection> deltaContactList,
			Map<String, List<String>> contactToGroupMap, Set<String> numberList) {
		List<UserContactData> contactDataList = new ArrayList<>();

		// iterate over deltaContact list to fetch contact data
		for (UserConnection userContact : PeopleUtils.emptyIfNull(deltaContactList)) {
			if (contactToGroupMap != null && contactToGroupMap.containsKey(userContact.getConnectionId())) {
				userContact.setGroupIdList(contactToGroupMap.get(userContact.getConnectionId()));
			}
			UserContactData contactData = prepareContactStaticData(sessionUser, userContact);
			UserInformationDTO contactStaticData = contactData.getStaticProfileData();
			if (contactStaticData != null) {
				populateStaticDataWithIsVerifiedInfoSetUserId(contactStaticData, numberList, contactData, sessionUser);
			}

			switch (userContact.getConnectionStatus()) {

			case NOT_CONNECTED:
			case PENDING:
				contactDataList.add(contactData);
				break;
			case CONNECTED:
				contactDataList.add(prepareContactSharedData(sessionUser, userContact));
				break;
			default:
				break;
			}
		}
		return contactDataList;
	}

	@Override
	public List<String> updateContactImage(UpdateContactImageRequest updateImageRequest) {

		PeopleUser sessionUser = tokenAuthService.getSessionUser();

		List<ContactImage> imageList = updateImageRequest.getContactImageList();
		Map<String, String> contactImageMap = new HashMap<>();
		List<String> connectionIdList = new ArrayList<>();
		List<String> updatedConnectionIdList = new ArrayList<>();

		for (ContactImage contactImage : PeopleUtils.emptyIfNull(imageList)) {
			connectionIdList.add(contactImage.getConnectionId());
			contactImageMap.put(contactImage.getConnectionId(), contactImage.getImageURL());
		}

		List<UserConnection> validConnectionList = userConnectionRepository
				.findContactByConnectionId(sessionUser.getUserId(), connectionIdList);

		for (UserConnection userConnection : PeopleUtils.emptyIfNull(validConnectionList)) {

			UserInformationDTO staticData = userConnection.getContactStaticData();
			if (staticData == null) {
				staticData = new UserInformationDTO();
			}
			staticData.setImageURL(contactImageMap.get(userConnection.getConnectionId()));
			updatedConnectionIdList.add(userConnection.getConnectionId());
		}

		if (!PeopleUtils.isNullOrEmpty(validConnectionList)) {
			userConnectionRepository.saveAll(validConnectionList);
		}

		return updatedConnectionIdList;
	}

	private List<UserConnection> prepareDeltaContactList(List<UserConnection> userConnectionList,
			DateTime lastSyncedTime) {

		List<UserConnection> deltaContactList = new ArrayList<>();

		for (UserConnection userContact : PeopleUtils.emptyIfNull(userConnectionList)) {

			UserPrivacyProfile sharedProfile = userContact.getPrivacyProfileData();
			if (lastSyncedTime.compareTo(userContact.getLastUpdatedOn()) < 0
					|| (sharedProfile != null && lastSyncedTime.compareTo(sharedProfile.getLastUpdatedOn()) < 0)
					|| checkIfSharedDataUpdate(userContact, lastSyncedTime)) {
				deltaContactList.add(userContact);
			}
		}
		return deltaContactList;
	}

	private Boolean checkIfSharedDataUpdate(UserConnection userConnection, DateTime lastSyncedTime) {

		UserPrivacyProfile sharedProfile = userConnection.getPrivacyProfileData();
		PeopleUser sharedData = userConnection.getUserData();
		if (sharedProfile == null || sharedData == null) {
			return Boolean.FALSE;
		}

		if (sharedProfile.getIsCompanyShared() && sharedData.getCompany() != null
				&& lastSyncedTime.compareTo(sharedData.getCompany().getLastUpdatedOn()) < 0) {
			return Boolean.TRUE;
		}

		if (sharedProfile.getIsPositionShared() && sharedData.getPosition() != null
				&& lastSyncedTime.compareTo(sharedData.getPosition().getLastUpdatedOn()) < 0) {
			return Boolean.TRUE;
		}

		if (isNameValueShared(sharedProfile, sharedData, lastSyncedTime)) {
			return Boolean.TRUE;
		}

		return isSharedDataPresentAndUpdated(sharedData, sharedProfile, lastSyncedTime);
	}

	private Boolean isNameValueShared(UserPrivacyProfile sharedProfile, PeopleUser sharedData,
			DateTime lastSyncedTime) {
		if (sharedProfile.getIsNickNameShared() && sharedData.getNickName() != null
				&& lastSyncedTime.compareTo(sharedData.getNickName().getLastUpdatedOn()) < 0) {
			return Boolean.TRUE;
		}

		if (sharedProfile.getIsMaidenNameShared() && sharedData.getMaidenName() != null
				&& lastSyncedTime.compareTo(sharedData.getMaidenName().getLastUpdatedOn()) < 0) {
			return Boolean.TRUE;
		}

		if (sharedData.getName() != null && lastSyncedTime.compareTo(sharedData.getName().getLastUpdatedOn()) < 0) {
			return Boolean.TRUE;
		}

		if (sharedData.getFirstName() != null
				&& lastSyncedTime.compareTo(sharedData.getFirstName().getLastUpdatedOn()) < 0) {
			return Boolean.TRUE;
		}

		if (sharedData.getLastName() != null
				&& lastSyncedTime.compareTo(sharedData.getLastName().getLastUpdatedOn()) < 0) {
			return Boolean.TRUE;
		}

		return Boolean.FALSE;
	}

	private Boolean isSharedDataPresentAndUpdated(PeopleUser sharedData, UserPrivacyProfile sharedProfile,
			DateTime lastSyncedTime) {
		Map<String, UserProfileData> metadata = sharedData.getMetadataMap();
		for (String valueId : PeopleUtils.emptyIfNull(sharedProfile.getValueIdList())) {
			UserProfileData sharedProfileData = metadata.getOrDefault(valueId, null);
			if (sharedProfileData != null && lastSyncedTime.compareTo(sharedProfileData.getLastUpdatedOn()) < 0) {
				return Boolean.TRUE;
			}
		}
		return Boolean.FALSE;
	}

	@Override
	public UserContactData prepareContactStaticData(PeopleUser peopleUser, UserConnection userContact) {

		UserContactData userContactData = new UserContactData();
		List<GroupNameResponse> groupNameList = new ArrayList<>();
		if (userContact.getContactStaticData() != null && userContact.getContactStaticData().getDepartment() == null) {
			userContact.getContactStaticData().setDepartment(PeopleUtils.getDefaultOrEmpty(""));
		}
		userContactData.setStaticProfileData(userContact.getContactStaticData());
		userContactData.setConnectionId(userContact.getConnectionId());
		userContactData.setDeviceContactId(PeopleUtils.getDefaultOrEmpty(userContact.getDeviceContactId()));
		userContactData.setConnectionStatus(userContact.getConnectionStatus().getValue());
		userContactData.setToUserId(userContact.getConnectionToId());
		userContactData.setIsFavourite(userContact.getIsFavourite());
		userContactData.setSequenceNumber(userContact.getSequenceNumber());
		userContactData.setGroupIdList(userContact.getGroupIdList());
		for (String groupId : PeopleUtils.emptyIfNull(userContactData.getGroupIdList())) {
			UserGroup userGroup = userGroupRepository.findById(groupId).get();
			if (userGroup != null) {
				GroupNameResponse nameResponse = new GroupNameResponse();
				nameResponse.setId(userGroup.getGroupId());
				nameResponse.setName(userGroup.getTitle());
				groupNameList.add(nameResponse);
			}
		}
		if (userContactData.getStaticProfileData() != null) {
			userContactData.getStaticProfileData().setGroupNameList(groupNameList);
		}
		Set<String> blockedIdList = peopleUser.getBlockedUserIdList();
		String toUserId = userContact.getConnectionToId();

		if (toUserId != null) {
			userContactData.setIsBlocked(blockedIdList.contains(toUserId));
		}

		return userContactData;
	}

	@Override
	public void mergeContacts(MergeContactsRequestDTO mergeContactsRequest) {
		PeopleUser sessionUser = tokenAuthService.getSessionUser();
		DateTime currentTime = PeopleUtils.getCurrentTimeInUTC();
		List<MergedConnection> mergedConnections = new ArrayList<>();
		// list of connectionIds to be deleted
		Set<String> mergedContacts = new HashSet<>();
		// get all user group
		List<UserGroup> userGroups = userGroupRepository.fetchAllUserGroups(sessionUser.getUserId());

		for (MergeContact mergeContact : PeopleUtils.emptyIfNull(mergeContactsRequest.getListOfContactsToBeMerged())) {
			// Removing master connection id if present in merged connections list
			List<String> mergedConnectionList = mergeContact.getMergedConnectionIds();
			mergedConnectionList.remove(mergeContact.getMasterConnectionId());
			mergeContact.setMergedConnectionIds(mergedConnectionList);

			// merged contact details to be persisted
			MergedConnection mergedConnection = new MergedConnection();
			mergedConnection.setMasterConnectionId(mergeContact.getMasterConnectionId());
			mergedConnection.setMergedConnectionIds(mergeContact.getMergedConnectionIds());
			mergedConnection.setUserId(sessionUser.getUserId());
			mergedConnection.setMergedOn(currentTime);
			mergedConnections.add(mergedConnection);
			// handling groups
			replaceMergedContactsWithMasterContactInGroups(mergeContact, userGroups);
			// handling activities - shared contacts, introduction, shared location
			replaceMergedContactsWithMasterContactInShareContactAndIntroductionActivities(sessionUser, mergeContact);
			// adding list of contacts to be deleted
			mergedContacts.addAll(mergeContact.getMergedConnectionIds());
		}
		// delete merged connections
		userConnectionRepository.deleteConnectionsByUserIdAndConnectionIds(sessionUser.getUserId(),
				new ArrayList<>(mergedContacts));
		// persist merge details
		mergeContactRepository.saveAll(mergedConnections);
	}

	/*
	 * Update user activities and activity contacts involving merged connections
	 */
	private void replaceMergedContactsWithMasterContactInShareContactAndIntroductionActivities(PeopleUser sessionUser,
			MergeContact mergeContact) {
		List<UserActivity> userActivities = userActivityRepository
				.getSharedContactsActivitiesByUserIdAndSharedConnectionIds(sessionUser.getUserId(),
						mergeContact.getMergedConnectionIds());
		Iterator<UserActivity> userActivityIterator = userActivities.iterator();
		UserActivity userActivity;

		while (userActivityIterator.hasNext()) {
			userActivity = userActivityIterator.next();
			userActivity.getSharedConnectionIdList().removeAll(mergeContact.getMergedConnectionIds());
			userActivity.getSharedConnectionIdList().add(mergeContact.getMasterConnectionId());
		}

		activityContactRepository.updateConnectionIdForActivityContacts(sessionUser.getUserId(),
				mergeContact.getMasterConnectionId(), mergeContact.getMergedConnectionIds());

		userActivityRepository.saveAll(userActivities);
	}

	/*
	 * Update user groups involving merged connections
	 */
	private void replaceMergedContactsWithMasterContactInGroups(MergeContact mergeContact, List<UserGroup> userGroups) {
		Set<String> listOfContactsInGroup = new HashSet<>();
		// update user groups by replacing merged contacts with master contact
		for (UserGroup userGroup : PeopleUtils.emptyIfNull(userGroups)) {
			listOfContactsInGroup.clear();
			listOfContactsInGroup.addAll(userGroup.getContactIdList());
			if (listOfContactsInGroup.removeAll(mergeContact.getMergedConnectionIds())) {
				listOfContactsInGroup.add(mergeContact.getMasterConnectionId());
				userGroup.setContactIdList(new ArrayList<>(listOfContactsInGroup));
			}
		}
		userGroupRepository.saveAll(userGroups);
	}

	@Override
	public ConnectionDetailsResponseDTO fetchConnectionDetails(String connectionId) {
		PeopleUser sessionUser = tokenAuthService.getSessionUser();

		// will have only one object in connection list
		List<UserConnection> userConnectionList = userConnectionRepository
				.getConnectionDataWithProfileForSelectedContact(sessionUser.getUserId(),
						Collections.singletonList(connectionId));

		if (PeopleUtils.isNullOrEmpty(userConnectionList)) {
			throw new BadRequestException(MessageCodes.INVALID_CONNECTION.getValue());
		}

		Set<String> numberList = masterService.getRegisteredNumberList();
		Map<String, List<String>> contactToGroupMap = prepareConnectionIdToGroupIdMap(sessionUser.getUserId());

		List<UserContactData> contactDataList = getUserContactDataList(sessionUser, userConnectionList,
				contactToGroupMap, numberList);

		// prepare response
		ConnectionDetailsResponseDTO connectionDetail = new ConnectionDetailsResponseDTO();

		// as there will be only one data in contactDataList.
		connectionDetail.setUserConnectionDetail(contactDataList.get(0));
		return connectionDetail;
	}

	@Override
	public void populateStaticDataWithIsVerifiedInfo(UserInformationDTO staticContactData, Set<String> numberList) {

		List<UserProfileData> userProfileDataList = staticContactData.getUserMetadataList();
		for (UserProfileData userProfileData : PeopleUtils.emptyIfNull(userProfileDataList)) {
			userProfileData.setIsPrimary(null);
			if (UserInfoCategory.CONTACT_NUMBER.getValue().equalsIgnoreCase(userProfileData.getCategory())) {
				ContactNumberDTO staticContact = userProfileData.getContactNumber();
				if (staticContact != null && numberList.contains(staticContact.getMobileNumber())) {
					userProfileData.setVerification(UserInformationVerification.VERIFIED);
				} else {
					userProfileData.setVerification(UserInformationVerification.NOT_VERIFIED);
				}
			}
		}
	}

	@Override
	public void populateStaticDataWithIsVerifiedInfoSetUserId(UserInformationDTO staticContactData,
			Set<String> numberList, UserContactData contactData, PeopleUser sessionUser) {

		List<UserProfileData> userProfileDataList = staticContactData.getUserMetadataList();
		for (UserProfileData userProfileData : PeopleUtils.emptyIfNull(userProfileDataList)) {
			userProfileData.setIsPrimary(null);
			if (UserInfoCategory.CONTACT_NUMBER.getValue().equalsIgnoreCase(userProfileData.getCategory())) {
				ContactNumberDTO staticContact = userProfileData.getContactNumber();
				if (staticContact != null && numberList.contains(staticContact.getMobileNumber())) {
					userProfileData.setVerification(UserInformationVerification.VERIFIED);

					if (contactData.getToUserId() == null) {
						PeopleUser peopleUser = peopleUserRepository.findByCodeAndNumber(staticContact.getCountryCode(),
								staticContact.getPhoneNumber());
						contactData.setToUserId(peopleUser.getUserId());
						Set<String> blockedIdList = sessionUser.getBlockedUserIdList();
						if (contactData.getToUserId() != null) {
							contactData.setIsBlocked(blockedIdList.contains(contactData.getToUserId()));
						}
					}
				} else {
					userProfileData.setVerification(UserInformationVerification.NOT_VERIFIED);
				}
			}
		}
	}

	@Override
	public UserContactData prepareUpdateContactSharedData(PeopleUser peopleUser, UserConnection userContact) {

		UserPrivacyProfile privacyProfileData = userContact.getPrivacyProfileData();
		// prepare and set shared data
		UserContactData userContactData = prepareContactStaticData(peopleUser, userContact);

		UserConnection otherConnection = userConnectionRepository
				.findConnectionByFromIdAndToId(userContact.getConnectionToId(), peopleUser.getUserId());

		// set privacy profile information
		PrivacyProfileData sharedByContact = new PrivacyProfileData();
		if (privacyProfileData != null) {
			sharedByContact.setProfileName(privacyProfileData.getProfileName());
		}
		userContactData.setSharedPrivacyProfileByContact(sharedByContact);

		PrivacyProfileData sharedProfileWithContact = new PrivacyProfileData();
		sharedProfileWithContact.setPrivacyProfileId(otherConnection.getSharedProfile().getPrivacyProfileId());
		sharedProfileWithContact.setValueIdList(otherConnection.getSharedProfile().getValueIdList());
		sharedProfileWithContact.setIsCompanyShared(otherConnection.getSharedProfile().getIsCompanyShared());
		sharedProfileWithContact.setIsPositionShared(otherConnection.getSharedProfile().getIsPositionShared());
		sharedProfileWithContact.setIsNickNameShared(otherConnection.getSharedProfile().getIsNickNameShared());
		sharedProfileWithContact.setIsMaidenNameShared(otherConnection.getSharedProfile().getIsMaidenNameShared());
		userContactData.setSharedPrivacyProfileWithContact(sharedProfileWithContact);

		if (!masterService.isUserBlockedByContact(userContact.getConnectionToId(), peopleUser.getUserId())) {

			UserInformationDTO sharedData1 = masterService.prepareSharedData1(userContact);
			userContactData.setSharedProfileData(sharedData1);
			userContactData.setDeleteProfileData(userContact.getConnectionDeletedData());
		}

		return userContactData;
	}

	@Override
	public UserContactData prepareContactSharedData(PeopleUser peopleUser, UserConnection userContact) {

		UserPrivacyProfile privacyProfileData = userContact.getPrivacyProfileData();
		// prepare and set shared data
		UserContactData userContactData = prepareContactStaticData(peopleUser, userContact);

		UserConnection otherConnection = userConnectionRepository
				.findConnectionByFromIdAndToId(userContact.getConnectionToId(), peopleUser.getUserId());

		// set privacy profile information
		PrivacyProfileData sharedByContact = new PrivacyProfileData();
		if (privacyProfileData != null) {
			sharedByContact.setProfileName(privacyProfileData.getProfileName());
		}
		userContactData.setSharedPrivacyProfileByContact(sharedByContact);

		PrivacyProfileData sharedProfileWithContact = new PrivacyProfileData();
		sharedProfileWithContact.setPrivacyProfileId(otherConnection.getSharedProfile().getPrivacyProfileId());
		sharedProfileWithContact.setValueIdList(otherConnection.getSharedProfile().getValueIdList());
		sharedProfileWithContact.setIsCompanyShared(otherConnection.getSharedProfile().getIsCompanyShared());
		sharedProfileWithContact.setIsPositionShared(otherConnection.getSharedProfile().getIsPositionShared());
		sharedProfileWithContact.setIsNickNameShared(otherConnection.getSharedProfile().getIsNickNameShared());
		sharedProfileWithContact.setIsMaidenNameShared(otherConnection.getSharedProfile().getIsMaidenNameShared());
		userContactData.setSharedPrivacyProfileWithContact(sharedProfileWithContact);

		if (!masterService.isUserBlockedByContact(userContact.getConnectionToId(), peopleUser.getUserId())) {

			// UserInformationDTO sharedData = masterService.prepareSharedData(userContact);
			UserInformationDTO sharedData1 = masterService.prepareSharedData1(userContact);
			if (userContact.getContactStaticData() != null) {
				UserInformationDTO contactStaticData = margeshareTostaticData(userContact.getContactStaticData(),
						sharedData1);
				if (privacyProfileData != null) {
					UserPrivacyProfileDTO profile = new UserPrivacyProfileDTO();
					profile.setPrivacyProfileId(
							PeopleUtils.getDefaultOrEmpty(otherConnection.getSharedProfile().getPrivacyProfileId()));
					UserPrivacyProfile profileData = userPrivacyProfileRepository
							.findById(otherConnection.getSharedProfile().getPrivacyProfileId()).get();
					if (profileData != null) {
						profile.setProfileName(PeopleUtils
								.getDefaultOrEmpty(PeopleUtils.getDefaultOrEmpty(profileData.getProfileName())));
					}
					contactStaticData.setUserPrivacyProfile(profile);
				}
				userContactData.setStaticProfileData(contactStaticData);
			} else {
				userContactData.setStaticProfileData(sharedData1);
				userContactData.getStaticProfileData().setDeviceContactId(PeopleUtils.getDefaultOrEmpty("5555"));
				userContactData.setDeviceContactId(PeopleUtils.getDefaultOrEmpty("5555"));
			}
			userContactData.setSharedProfileData(sharedData1);
			userContactData.setDeleteProfileData(userContact.getConnectionDeletedData());
		}

		return userContactData;
	}

	private UserInformationDTO margeshareTostaticData(UserInformationDTO informationDTO, UserInformationDTO shareData) {

		if (shareData.getName() != null && !shareData.getName().isEmpty()) {
			informationDTO.setName(shareData.getName());
		}
		if (shareData.getFirstName() != null && !shareData.getFirstName().isEmpty()) {
			informationDTO.setFirstName(shareData.getFirstName());
		}
		if (shareData.getMiddleName() != null && !shareData.getMiddleName().isEmpty()) {
			informationDTO.setMiddleName(shareData.getMiddleName());
		}
		if (shareData.getLastName() != null && !shareData.getLastName().isEmpty()) {
			informationDTO.setLastName(shareData.getLastName());
		}
		if (shareData.getFullName() != null && !shareData.getFullName().isEmpty()) {
			informationDTO.setFullName(shareData.getFullName());
		}
		if (shareData.getPhoneticFirstName() != null && !shareData.getPhoneticFirstName().isEmpty()) {
			informationDTO.setPhoneticFirstName(shareData.getPhoneticFirstName());
		}
		if (shareData.getPhoneticMiddleName() != null && !shareData.getPhoneticMiddleName().isEmpty()) {
			informationDTO.setPhoneticMiddleName(shareData.getPhoneticMiddleName());
		}
		if (shareData.getPhoneticLastName() != null && !shareData.getPhoneticLastName().isEmpty()) {
			informationDTO.setPhoneticLastName(shareData.getPhoneticLastName());
		}
		if (shareData.getNamePrefix() != null && !shareData.getNamePrefix().isEmpty()) {
			informationDTO.setNamePrefix(shareData.getNamePrefix());
		}
		if (shareData.getNameSuffix() != null && !shareData.getNameSuffix().isEmpty()) {
			informationDTO.setNameSuffix(shareData.getNameSuffix());
		}
		if (shareData.getMaidenName() != null && !shareData.getMaidenName().isEmpty()) {
			informationDTO.setMaidenName(shareData.getMaidenName());
		}
		if (shareData.getNickName() != null && !shareData.getNickName().isEmpty()) {
			informationDTO.setNickName(shareData.getNickName());
		}
		if (shareData.getGender() != null && !shareData.getGender().isEmpty()) {
			informationDTO.setGender(shareData.getGender());
		}
		if (shareData.getCompany() != null && !shareData.getCompany().isEmpty()) {
			informationDTO.setCompany(shareData.getCompany());
		}
		if (shareData.getDepartment() != null && !shareData.getDepartment().isEmpty()) {
			informationDTO.setDepartment(shareData.getDepartment());
		}
		if (shareData.getPosition() != null && !shareData.getPosition().isEmpty()) {
			informationDTO.setPosition(shareData.getPosition());
		}
		if (shareData.getImageURL() != null && !shareData.getImageURL().isEmpty()) {
			informationDTO.setImageURL(shareData.getImageURL());
		}
		if (shareData.getNotes() != null && !shareData.getNotes().isEmpty()) {
			informationDTO.setNotes(shareData.getNotes());
		}
		if (shareData.getSip() != null && !shareData.getSip().isEmpty()) {
			informationDTO.setSip(shareData.getSip());
		}
		if (shareData.getTagList() != null && !shareData.getTagList().isEmpty()) {
			informationDTO.setTagList(shareData.getTagList());
		}
		if (shareData.getBirthday() != null && !shareData.getBirthday().isEmpty()) {
			informationDTO.setBirthday(shareData.getBirthday());
		}
		if (shareData.getAccountName() != null && !shareData.getAccountName().isEmpty()) {
			informationDTO.setAccountName(shareData.getAccountName());
		}
		if (shareData.getAccountType() != null && !shareData.getAccountType().isEmpty()) {
			informationDTO.setAccountType(shareData.getAccountType());
		}
		if (informationDTO.getUserMetadataList().isEmpty()) {
			informationDTO.setUserMetadataList(shareData.getUserMetadataList());
		} else {
			List<UserProfileData> userProfileDatas = new ArrayList<>();
			userProfileDatas.addAll(informationDTO.getUserMetadataList());
			userProfileDatas.addAll(shareData.getUserMetadataList());

			for (int i = 0; i < userProfileDatas.size(); i++) {
				for (int j = i + 1; j < userProfileDatas.size(); j++) {
					if (userProfileDatas.get(i).getKeyValueDataList()
							.containsAll(userProfileDatas.get(j).getKeyValueDataList())) {
						if (userProfileDatas.get(i).getLabel().equalsIgnoreCase(userProfileDatas.get(j).getLabel())) {
							if (!userProfileDatas.get(j).getValueId().equalsIgnoreCase("")) {
								userProfileDatas.remove(j);
							}
						}
					}
				}
			}
			informationDTO.setUserMetadataList(userProfileDatas);
		}

		return informationDTO;
	}

	/*
	 * Check if connection requests have reached configured threshold value
	 */
	private boolean checkRequestThreshold(String fromUserId) {
		int requestThreshold = Integer.parseInt(connThresholdCount);
		int timeRange = Integer.parseInt(connTimeRange);
		// Number of requests created
		int numberOfRequests = userActivityService.getCountOfConnectionRequestsForTimeRange(fromUserId, timeRange,
				PeopleUtils.getCurrentTimeInUTC());
		if (numberOfRequests >= requestThreshold) {
			// Flag the user account
			peopleUserRepository.flagUserAccount(fromUserId,
					PeopleConstants.FlaggedReason.EXCEEDED_CONNECTION_REQUEST_THRESHOLD);
			return true;
		}
		return false;
	}

	private UserInformationDTO saveBasicData(UserInformationDTO userInformation) {

		UserInformationDTO savedUserData = new UserInformationDTO();

		savedUserData.setNamePrefix(PeopleUtils.getDefaultOrEmpty(userInformation.getNamePrefix()));
		savedUserData.setName(PeopleUtils.getDefaultOrEmpty(userInformation.getName()));
		savedUserData.setFirstName(PeopleUtils.getDefaultOrEmpty(userInformation.getFirstName()));
		savedUserData.setMiddleName(PeopleUtils.getDefaultOrEmpty(userInformation.getMiddleName()));
		savedUserData.setLastName(PeopleUtils.getDefaultOrEmpty(userInformation.getLastName()));
		savedUserData.setFullName(PeopleUtils.getDefaultOrEmpty(userInformation.getFirstName())
				.concat(PeopleUtils.getDefaultOrEmpty(userInformation.getLastName())));
		savedUserData.setNickName(PeopleUtils.getDefaultOrEmpty(userInformation.getNickName()));
		savedUserData.setPhoneticFirstName(PeopleUtils.getDefaultOrEmpty(userInformation.getPhoneticFirstName()));
		savedUserData.setPhoneticMiddleName(PeopleUtils.getDefaultOrEmpty(userInformation.getPhoneticMiddleName()));
		savedUserData.setPhoneticLastName(PeopleUtils.getDefaultOrEmpty(userInformation.getPhoneticLastName()));
		savedUserData.setMiddleName(PeopleUtils.getDefaultOrEmpty(userInformation.getMiddleName()));
		savedUserData.setMaidenName(PeopleUtils.getDefaultOrEmpty(userInformation.getMaidenName()));
		savedUserData.setNameSuffix(PeopleUtils.getDefaultOrEmpty(userInformation.getNameSuffix()));
		savedUserData.setGender(PeopleUtils.getDefaultOrEmpty(userInformation.getGender()));
		if (userInformation.getCompany() != null) {
			savedUserData.setCompany(PeopleUtils.getDefaultOrEmpty(userInformation.getCompany().trim()));
		} else {
			savedUserData.setCompany(PeopleUtils.getDefaultOrEmpty(userInformation.getCompany()));
		}
		savedUserData.setDepartment(PeopleUtils.getDefaultOrEmpty(userInformation.getDepartment()));
		savedUserData.setPosition(PeopleUtils.getDefaultOrEmpty(userInformation.getPosition()));
		savedUserData.setImageURL(PeopleUtils.getDefaultOrEmpty(userInformation.getImageURL()));
		savedUserData.setNotes(PeopleUtils.getDefaultOrEmpty(userInformation.getNotes()));
		savedUserData.setTagList(userInformation.getTagList());
		savedUserData.setSip(PeopleUtils.getDefaultOrEmpty(userInformation.getSip()));
		savedUserData.setGroupNameList(userInformation.getGroupNameList());
		savedUserData.setBirthday(PeopleUtils.getDefaultOrEmpty(userInformation.getBirthday()));
		savedUserData.setAccountName(PeopleUtils.getDefaultOrEmpty(userInformation.getAccountName()));
		savedUserData.setAccountType(PeopleUtils.getDefaultOrEmpty(userInformation.getAccountType()));

		return savedUserData;

	}

	private void requestReceiverBlockedStatusVerification(PeopleUser sessionUser, String receiverUserId) {
		if (receiverUserId != null && sessionUser.getBlockedUserIdList().contains(receiverUserId)) {
			throw new BadRequestException(MessageCodes.CANNOT_PERFORM_ANY_ACTION_WITH_BLOCKED_USERS.getValue());
		}
	}

	private UserActivity sendConnectionRequestToContactSingle(PeopleUser initiator, SendConnectionRequest request) {

		List<UserContact> contactList = request.getInitiateContactDetailsList();
		String initiatorId = initiator.getUserId();
		String initiatorName = initiator.getNameValue();
		List<SQSPayload> sqsPayloadList = new ArrayList<>();
		UserActivity createdActivity = null;

		UserContact receiverContact = contactList.get(0);
		receiverContact.setContactNumber(
				receiverContact.getContactNumber().getContactNumberWithDefaultCountryCode(getDefaultCountryCode()));
		// get connection obj
		UserConnection userConnection = userConnectionRepository.findContactByConnectionId(initiatorId,
				receiverContact.getConnectionId());
		if (userConnection == null) {
			throw new BadRequestException(MessageCodes.INVALID_CONNECTION.getValue());
		}

		PeopleUser receiverUser = peopleUserRepository
				.findByContactNumberWithLimitedFields(receiverContact.getContactNumber());
		if (receiverUser != null) {
			request.setReceiverUserId(receiverUser.getUserId());
			// check if connection request is being sent to blocked contact
			requestReceiverBlockedStatusVerification(initiator, receiverUser.getUserId());
		}
		request.setReceiverNumber(receiverContact.getContactNumber());
		request.setReceiverConnectionId(receiverContact.getConnectionId());
		request.setInitiatorUserId(initiatorId);
		request.setInitiatorName(initiatorName);
		request.setInitiatorNumber(initiator.getVerifiedContactNumber());

		applyRulesForSendConnectionRequestSingle(request);

		userConnection.setConnectionStatus(ConnectionStatus.PENDING);

		boolean isInitiatorBlocked = masterService.isUserBlockedByContact(request.getReceiverUserId(), initiatorId);
		UserActivity otherActivity = userActivityRepository
				.getPendingConnectionRequestActivity(request.getReceiverUserId(), request.getInitiatorUserId());

		if (otherActivity != null) {
			// if user already has a connection request from the receiver, then instead of
			// new connection
			// request, accept the previous request and follow accept connection request
			// flow.

			// In this flow initiator becomes acceptor
			UserConnection acceptorToInitiatorConnection = acceptConnectionRequestAlreadyReceived(otherActivity,
					request.getSharedPrivacyProfileKey());

			// triggering silent notification to acceptor(using initiatorId, since this used
			// becomes
			// acceptor for existing connection request)
			queueService.sendPayloadToSQS(notificationService.prepareSQSPayloadForSilentNotification(initiatorId,
					RequestType.CONNECTION_REQUEST_ACCEPTED.getValue(), null,
					acceptorToInitiatorConnection.getConnectionId(), null));
		} else {
			createdActivity = userActivityRepository
					.save(createActivityForConnectionRequest(request, isInitiatorBlocked));
			userConnectionRepository.save(userConnection);

			// Adding "connectionId" to userActivity
			createdActivity.setConnectionId(receiverContact.getConnectionId());

			// prepare SQS payload
			if (!isInitiatorBlocked) {
				sqsPayloadList.add(prepareSQSPayloadForSendConnectionRequest(request, createdActivity, initiator));
			}

			queueService.sendPayloadToSQS(sqsPayloadList);
		}
		return createdActivity;
	}

	private List<UserActivity> sendConnectionRequestToContactBulk(PeopleUser initiator, SendConnectionRequest request) {

		List<UserContact> contactList = request.getInitiateContactDetailsList();
		String initiatorId = initiator.getUserId();
		String initiatorName = initiator.getNameValue();
		List<SQSPayload> sqsPayloadList = new ArrayList<>();
		List<UserConnection> connectionToBeUpdated = new ArrayList<>();
		List<UserActivity> createdActivityList = new ArrayList<>();

		Map<String, UserConnection> connectionMap = getInitiateContactMap(initiatorId, contactList);
		Map<ContactNumberDTO, PeopleUser> contactNumberToUserMap = prepareContactNumberToUserMap(
				getContactNumberList(contactList));
		short blockedContactsCount = 0;
		String receiverUserId = null;
		// for all checks, no exception, just ignore and proceed
		for (UserContact receiverContact : PeopleUtils.emptyIfNull(contactList)) {

			UserConnection userConnection = connectionMap.getOrDefault(receiverContact.getConnectionId(), null);

			if (contactNumberToUserMap.containsKey(receiverContact.getContactNumber())) {
				receiverUserId = contactNumberToUserMap.get(receiverContact.getContactNumber()).getUserId();
				request.setReceiverUserId(receiverUserId);
				if (initiator.getBlockedUserIdList().contains(receiverUserId)) {
					blockedContactsCount++;
				}
			}

			request.setReceiverNumber(receiverContact.getContactNumber());
			request.setInitiatorUserId(initiatorId);
			request.setInitiatorName(initiatorName);
			request.setInitiatorNumber(initiator.getVerifiedContactNumber());
			request.setReceiverConnectionId(receiverContact.getConnectionId());

			if (applyRulesForSendConnectionRequestBulk(initiator, userConnection, request)) {
				continue;
			}

			UserActivity otherActivity = userActivityRepository
					.getPendingConnectionRequestActivity(request.getReceiverUserId(), request.getInitiatorUserId());

			if (otherActivity != null) {
				// if user already has a connection request from the receiver, then instead of
				// new connection
				// request, accept the previous request and follow accept connection request
				// flow.

				// In this flow initiator becomes acceptor
				UserConnection acceptorToInitiatorConnection = acceptConnectionRequestAlreadyReceived(otherActivity,
						request.getSharedPrivacyProfileKey());

				// triggering silent notification to acceptor(using initiatorId, since this used
				// becomes
				// acceptor for existing connection request)
				queueService.sendPayloadToSQS(notificationService.prepareSQSPayloadForSilentNotification(initiatorId,
						RequestType.CONNECTION_REQUEST_ACCEPTED.getValue(), null,
						acceptorToInitiatorConnection.getConnectionId(), null));
			} else {
				// checking if initiator is blocked
				boolean isInitiatorBlocked = masterService.isUserBlockedByContact(request.getReceiverUserId(),
						initiatorId);

				// create activity
				UserActivity activityForConnectionRequest = userActivityRepository
						.save(createActivityForConnectionRequest(request, isInitiatorBlocked));

				// Adding "connectionId" to userActivity
				activityForConnectionRequest.setConnectionId(receiverContact.getConnectionId());

				createdActivityList.add(activityForConnectionRequest);

				// prepare SQS payload
				userConnection.setConnectionStatus(ConnectionStatus.PENDING);
				connectionToBeUpdated.add(userConnection);

				// prepare SQS payload
				if (!isInitiatorBlocked) {
					sqsPayloadList.add(prepareSQSPayloadForSendConnectionRequest(request, activityForConnectionRequest,
							initiator));
				}
			}
		}

		userConnectionRepository.saveAll(connectionToBeUpdated);

		queueService.sendPayloadToSQS(sqsPayloadList);

		checkIfRequestReceiversAreBlocked(blockedContactsCount, contactList.size());

		return createdActivityList;
	}

	private void checkIfRequestReceiversAreBlocked(short blockedContactsCount, int contactListSize) {
		if (blockedContactsCount > 0 && (contactListSize == blockedContactsCount)) {
			throw new BadRequestException(MessageCodes.CANNOT_PERFORM_ANY_ACTION_WITH_BLOCKED_USERS.getValue());
		} else if (blockedContactsCount > 0) {
			throw new BadRequestException(MessageCodes.SOME_ACTIONS_CANNOT_BE_PERFORMED_ON_BLOCKED_USERS.getValue());
		}
	}

	private void checkIfAllContactsAreInvalid(short invalidContacts, int contactListSize) {
		if (invalidContacts > 0 && (contactListSize == invalidContacts)) {
			throw new BadRequestException(MessageCodes.INVALID_COUNTRY_CODE_OR_PHONE_NUMBER.getValue());
		}
	}

	private UserActivity createActivityForConnectionRequest(SendConnectionRequest request, Boolean isInitiatorBlocked) {

		UserActivity userActivity = new UserActivity();
		userActivity.setActivityById(request.getInitiatorUserId());
		ActivityType activityType = new ActivityType();
		activityType.setRequestType(RequestType.CONNECTION_REQUEST);
		activityType.setActionTaken(Action.INITIATED);
		userActivity.setActivityType(activityType);
		userActivity.setActivityForId(request.getReceiverUserId());
		userActivity.setOverallStatus(ActivityStatus.PENDING);
		DateTime currentDateTime = PeopleUtils.getCurrentTimeInUTC();
		userActivity.setCreatedOn(currentDateTime);
		userActivity.setLastUpdatedOn(currentDateTime);
		userActivity.setMessage(request.getMessage());
		userActivity.setSharedProfileInformationData(request.getSharedPrivacyProfileKey());
		userActivity.setIsInitiatorBlocked(isInitiatorBlocked);

		UserContact initiateDetails = new UserContact();
		initiateDetails.setContactNumber(request.getReceiverNumber());
		initiateDetails.setConnectionId(request.getReceiverConnectionId());
		userActivity.setInitiateDetails(initiateDetails);

		return userActivity;
	}

	private Map<String, UserConnection> getInitiateContactMap(String userId, List<UserContact> initiateList) {

		Map<String, UserConnection> userConnectionMap = new HashMap<>();
		List<String> idList = prepareConnectionListFromUserContactList(initiateList);
		List<UserConnection> contactList = userConnectionRepository.findContactByConnectionId(userId, idList);
		if (PeopleUtils.isNullOrEmpty(contactList)) {
			throw new BadRequestException(MessageCodes.INVALID_CONTACT.getValue());
		}
		for (UserConnection userConnection : PeopleUtils.emptyIfNull(contactList)) {
			userConnectionMap.put(userConnection.getConnectionId(), userConnection);
		}
		return userConnectionMap;
	}

	private void checkIfNameIsProvidedForSearchedContact(UserInformationDTO searchedUserInformation) {
		if (searchedUserInformation == null || PeopleUtils.isNullOrEmpty(searchedUserInformation.getFirstName())) {
			throw new BadRequestException(MessageCodes.NAME_FIELD_CANNOT_BE_EMPTY.getValue());
		}
	}

	private UserActivityAndConnectionData sendConnectionRequestToPeopleUserSingle(PeopleUser initiator,
			SendConnectionRequest request) {
		String sessionUserId = initiator.getUserId();
		UserActivityAndConnectionData activityAndConnectionData = new UserActivityAndConnectionData();
		// check if initiate in valid
		String receiverUserId = request.getInitiateUserIdList().get(0);
		PeopleUser receiverUser = peopleUserService.findUserByUserId(receiverUserId);

		if (receiverUser == null) {
			throw new BadRequestException(MessageCodes.INVALID_INITIATE.getValue());
		}

		// check if connection request is being sent to blocked contact
		requestReceiverBlockedStatusVerification(initiator, receiverUser.getUserId());

		request.setReceiverUserId(receiverUser.getUserId());
		request.setReceiverNumber(receiverUser.getVerifiedContactNumber());
		request.setInitiatorUserId(initiator.getUserId());
		request.setInitiatorName(initiator.getNameValue());
		request.setInitiatorNumber(initiator.getVerifiedContactNumber());

		applyRulesForSendConnectionRequestSingle(request);

		UserActivity otherActivity = userActivityRepository
				.getPendingConnectionRequestActivity(request.getReceiverUserId(), request.getInitiatorUserId());

		if (otherActivity != null) {
			// if user already has a connection request from the receiver, then instead of
			// new connection
			// request, accept the previous request and follow accept connection request
			// flow.

			// In this flow initiator becomes acceptor
			UserConnection acceptorToInitiatorConnection = acceptConnectionRequestAlreadyReceived(otherActivity,
					request.getSharedPrivacyProfileKey());

			// triggering silent notification to acceptor(using initiatorId, since this used
			// becomes
			// acceptor for existing connection request)
			queueService.sendPayloadToSQS(notificationService.prepareSQSPayloadForSilentNotification(sessionUserId,
					RequestType.CONNECTION_REQUEST_ACCEPTED.getValue(), null,
					acceptorToInitiatorConnection.getConnectionId(), null));
			return null;
		} else {
			boolean isInitiatorBlocked = masterService.isUserBlockedByContact(request.getReceiverUserId(),
					sessionUserId);
			UserActivity createdActivity = userActivityRepository
					.save(createActivityForConnectionRequest(request, isInitiatorBlocked));

			if (request.isStaticContactToBeCreated()) {
				checkIfNameIsProvidedForSearchedContact(request.getInitiateUserInformation());
				UserConnection newConnection = createNewContact(sessionUserId, request.getInitiateUserInformation());
				newConnection.setConnectionStatus(ConnectionStatus.PENDING);
				newConnection = userConnectionRepository.save(newConnection);
				// set "connectionId"
				createdActivity.setConnectionId(newConnection.getConnectionId());
				createdActivity.getInitiateDetails().setConnectionId(newConnection.getConnectionId());
				activityAndConnectionData.setUserConnection(newConnection);
			} else {
				// check if the initiate contact number already belongs to
				// any static contact from current user
				ContactNumberDTO receiverUserVerifiedContactNumber = receiverUser.getVerifiedContactNumber();
				List<UserConnection> staticContactsWithSameContactNumber = userConnectionRepository
						.findByFromIdAndPhoneNumberAndStatus(sessionUserId,
								receiverUserVerifiedContactNumber.getPhoneNumber(),
								Collections.singletonList(ConnectionStatus.NOT_CONNECTED.getValue()));

				if (!PeopleUtils.isNullOrEmpty(staticContactsWithSameContactNumber)) {

					UserConnection existingConnection = staticContactsWithSameContactNumber.get(0);
					existingConnection.setConnectionStatus(ConnectionStatus.PENDING);
					existingConnection = userConnectionRepository.save(existingConnection);
					// set "connectionId"
					createdActivity.setConnectionId(existingConnection.getConnectionId());
					createdActivity.getInitiateDetails().setConnectionId(existingConnection.getConnectionId());
					activityAndConnectionData.setUserConnection(existingConnection);
				}
			}

			activityAndConnectionData.setUserActivity(userActivityRepository.save(createdActivity));

			if (!isInitiatorBlocked) {
				queueService.sendPayloadToSQS(
						prepareSQSPayloadForSendConnectionRequest(request, createdActivity, initiator));
			}

			return activityAndConnectionData;
		}
	}

	private UserActivity createActivityForMoreInfo(String initiatorId, UserConnection connection) {

		UserActivity userActivity = new UserActivity();
		userActivity.setActivityById(initiatorId);
		userActivity.setActivityForId(connection.getConnectionToId());
		userActivity.setInitiateDetails(prepareInitiateDetails(connection.getConnectionId()));
		ActivityType activityType = new ActivityType();
		activityType.setRequestType(RequestType.MORE_INFO_REQUEST);
		activityType.setActionTaken(Action.INITIATED);
		userActivity.setActivityType(activityType);
		userActivity.setOverallStatus(ActivityStatus.PENDING);
		DateTime currentDateTime = PeopleUtils.getCurrentTimeInUTC();
		userActivity.setCreatedOn(currentDateTime);
		userActivity.setLastUpdatedOn(currentDateTime);
		userActivity.setIsInitiatorBlocked(
				masterService.isUserBlockedByContact(connection.getConnectionToId(), initiatorId));

		return userActivity;
	}

	private UserActivity createActivityForAcceptConnectionRequest(String initiatorId, UserConnection connection,
			String receiverConnectionId) {
		UserActivity userActivity = new UserActivity();
		userActivity.setActivityById(initiatorId);
		userActivity.setActivityForId(connection.getConnectionToId());
		userActivity.setInitiateDetails(prepareInitiateDetails(connection.getConnectionId()));
		ActivityType activityType = new ActivityType();
		activityType.setRequestType(RequestType.CONNECTION_REQUEST_ACCEPTED);
		activityType.setActionTaken(Action.ACCEPTED);
		userActivity.setActivityType(activityType);
		userActivity.setOverallStatus(ActivityStatus.INFORMATIVE);
		DateTime currentDateTime = PeopleUtils.getCurrentTimeInUTC();
		userActivity.setCreatedOn(currentDateTime);
		userActivity.setLastUpdatedOn(currentDateTime);
		userActivity.setIsInitiatorBlocked(
				masterService.isUserBlockedByContact(connection.getConnectionToId(), initiatorId));
		userActivity.setConnectionId(receiverConnectionId);

		return userActivity;
	}

	private UserConnection getOtherConnection(String userId, String connectionId) {

		UserConnection connection = userConnectionRepository.findConnectionByConnectionId(userId, connectionId);

		if (connection == null) {
			throw new BadRequestException(MessageCodes.INVALID_CONNECTION.getValue());
		}
		UserConnection otherConnection = userConnectionRepository
				.findConnectionByFromIdAndToId(connection.getConnectionToId(), connection.getConnectionFromId());
		if (otherConnection == null) {
			throw new BadRequestException(MessageCodes.INVALID_CONNECTION.getValue());
		}
		return otherConnection;
	}

	private UserActivity getPendingRequestForAcceptor(String acceptorId, String activityId) {

		UserActivity pendingRequest = userActivityRepository.getPendingActivityById(activityId);

		if (pendingRequest == null) {
			throw new BadRequestException(MessageCodes.INVALID_REQUEST.getValue());
		}
		// check if user is valid acceptor of request
		if (!acceptorId.equals(pendingRequest.getActivityForId())) {
			throw new BadRequestException(MessageCodes.UNAUTHORIZED_TO_ACCEPT_IGNORE_REQUESTS.getValue());
		}
		return pendingRequest;
	}

	public SharedProfileInformationData getDefaultSharedProfileData(String userId) {

		UserPrivacyProfile userPrivacyProfile = userPrivacyProfileRepository.findDefaultUserProfile(userId,
				Boolean.TRUE);
		SharedProfileInformationData sharedProfileInformationData = new SharedProfileInformationData();
		sharedProfileInformationData.setPrivacyProfileId(userPrivacyProfile.getPrivacyProfileId());
		return sharedProfileInformationData;
	}

	private ContactNumberDTO prepareContactNumberWithDefaultCode(ContactNumberDTO contactNumber) {
		if (contactNumber != null && PeopleUtils.isNullOrEmpty(contactNumber.getCountryCode())) {
			contactNumber.setCountryCode(getDefaultCountryCode());
		}
		return contactNumber;
	}

	private UserContact prepareInitiateDetails(String connectionId) {

		UserContact contactDetails = new UserContact();
		contactDetails.setConnectionId(connectionId);
		return contactDetails;
	}

	private String getDefaultCountryCode() {
		return tokenAuthService.getSessionUser().getVerifiedContactNumber().getCountryCode();
	}

	public UserConnection createNewContact(String userId, UserInformationDTO contactData) {

		// prepare contact static data
		UserInformationDTO toBeSavedData = saveBasicData(contactData);
		toBeSavedData.setUserMetadataList(contactData.getUserMetadataList());

		UserConnection newContact = new UserConnection();
		newContact.setConnectionStatus(ConnectionStatus.NOT_CONNECTED);
		newContact.setConnectionFromId(userId);
		newContact.setDeviceContactId(contactData.getDeviceContactId());
		newContact.setContactStaticData(toBeSavedData);

		if (contactData.getIsFavourite() != null && contactData.getIsFavourite()) {
			newContact.setIsFavourite(true);
			newContact.setSequenceNumber(contactData.getSequenceNumber());
		}

		newContact.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());

		return newContact;
	}

	public List<UserRestoreConnection> createNewRestoreContact(List<UserConnection> userConnectionList) {

		List<UserRestoreConnection> userRestoreConnections = new ArrayList<>();

		for (UserConnection userConnection : userConnectionList) {
			UserRestoreConnection userRestoreConnection = new UserRestoreConnection();
			userRestoreConnection.setConnectionStatus(ConnectionStatus.NOT_CONNECTED);
			userRestoreConnection.setConnectionFromId(userConnection.getConnectionFromId());
			userRestoreConnection.setDeviceContactId(userConnection.getDeviceContactId());
			userRestoreConnection.setStaticProfileData(userConnection.getContactStaticData());

			userRestoreConnection.setIsFavourite(userConnection.getIsFavourite());
			userRestoreConnection.setSequenceNumber(userConnection.getSequenceNumber());

			userRestoreConnection.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
			userRestoreConnections.add(userRestoreConnection);
		}

		return userRestoreConnections;
	}

	private UserGroup validateGroup(String groupId, String groupOwnerId) {

		List<UserGroup> existingUserGroup = userGroupRepository
				.findByUserGroupIdAndOwnerId(Collections.singletonList(groupId), groupOwnerId);

		if (PeopleUtils.isNullOrEmpty(existingUserGroup)) {
			throw new BadRequestException(MessageCodes.INVALID_USER_GROUP.getValue());
		}
		return existingUserGroup.get(0);
	}

	private List<UserConnection> getValidContactList(String userId, List<String> contactIdList) {

		List<UserConnection> listOfValidContacts = userConnectionRepository.findContactByConnectionId(userId,
				contactIdList);
		if (PeopleUtils.isNullOrEmpty(listOfValidContacts)) {
			throw new BadRequestException(MessageCodes.INVALID_CONNECTION.getValue());
		}
		return listOfValidContacts;
	}

	/**
	 * Updates the missing contact number with primary phone number of the contact
	 * based on connectionId mapping
	 *
	 * @param userId
	 * @param userContactList
	 * @return updated userContactList
	 */
	private List<UserContact> updateMissingContactNumbers(String userId, List<UserContact> userContactList) {

		List<UserContact> updatedUserContactList = new ArrayList<>();
		Map<String, UserContact> contactMap = new HashMap<>();
		for (UserContact eachUserContact : userContactList) {
			contactMap.put(eachUserContact.getConnectionId(), eachUserContact);
		}

		List<UserConnection> connectionsList = userConnectionRepository.getPeopleUserDataForConnectionList(userId,
				new ArrayList<>(contactMap.keySet()));

		UserContact contact;
		for (UserConnection userConnection : connectionsList) {
			if (contactMap.containsKey(userConnection.getConnectionId())
					&& userConnection.getConnectionStatus().equals(ConnectionStatus.CONNECTED)) {
				/* if connected get verified contact number from userData -> verified number */

				if (userConnection.getUserData() != null) {
					contact = contactMap.get(userConnection.getConnectionId());
					contact.setContactNumber(userConnection.getUserData().getVerifiedContactNumber());
					updatedUserContactList.add(contact);
				}

			} else if (contactMap.containsKey(userConnection.getConnectionId())) {
				/* if Not connected get valid number from contactStaticData -> userMetaList */
				contact = contactMap.get(userConnection.getConnectionId());

				boolean contactNumberUpdated = masterService.updateContactForNonWatuRegisteredNumbers(
						userConnection.getContactStaticData().getUserMetadataList(), contact);

				if (contactNumberUpdated) {
					updatedUserContactList.add(contact);
				}
			}
		}

		// error message needs correction
		if (PeopleUtils.isNullOrEmpty(updatedUserContactList)) {
			throw new BadRequestException(MessageCodes.INVALID_OPERATION.getValue());
		}

		return updatedUserContactList;
	}

	private List<UserContact> updateContactNumbers(String userId, List<UserContact> userContactList) {
		Map<String, UserContact> connectionIdToContactMap = new HashMap<>();

		// getting connectionIdList
		for (UserContact userContact : userContactList) {
			connectionIdToContactMap.put(userContact.getConnectionId(), userContact);
		}

		List<UserConnection> connectionsList = userConnectionRepository.findContactByConnectionId(userId,
				new ArrayList<>(connectionIdToContactMap.keySet()));

		for (UserConnection userConnection : PeopleUtils.emptyIfNull(connectionsList)) {
			if (userConnection.getConnectionStatus().equals(ConnectionStatus.CONNECTED)) {
				/*
				 * for connected 1. Get connectionToId and update userContact 2. create a map of
				 * userId to connection id for further processing to get people user and their
				 * verified contact number
				 */
				UserContact contact = connectionIdToContactMap.get(userConnection.getConnectionId());
				PeopleUser verifiedUser = peopleUserRepository.findByuserId(userConnection.getConnectionToId(),
						UserStatus.ACTIVE.getValue());

				contact.setUserId(userConnection.getConnectionToId());
				contact.setContactNumber(verifiedUser.getVerifiedContactNumber());

			} else {

				UserContact contact = connectionIdToContactMap.get(userConnection.getConnectionId());
				/*
				 * for static contact 1. Based on priority fetch the contact 2. Check if the
				 * number is registered number 3. Fetch the people user details from the
				 * verified number and update userContact
				 */

				List<UserProfileData> userProfileData = userConnection.getContactStaticData().getUserMetadataList();
				boolean isUserContactUpdated = masterService
						.updateContactForStaticContactWithVerifiedNumber(userProfileData, contact);

				if (!isUserContactUpdated) {
					userContactList.remove(connectionIdToContactMap.get(userConnection.getConnectionId()));
				}
			}
		}
		return userContactList;
	}

	@Override
	public List<String> manageFavouritesForContact(ManageFavouritesRequestDTO manageFavouritesRequestDTO) {

		PeopleUser peopleUser = tokenAuthService.getSessionUser();
		// clear contact favourite list for user
		userConnectionRepository.removeFavouritesForGivenUser(peopleUser.getUserId());
		List<String> responseConnectionString = new ArrayList<>();
		if (!manageFavouritesRequestDTO.getFavouriteConnectionList().isEmpty()) {
			// update the favourite list as per request and sequence specified
			userConnectionRepository.updateFavouritesForGivenUser(peopleUser.getUserId(), manageFavouritesRequestDTO);

			List<String> requestConnectionList = new ArrayList<>();
			for (FavouriteConnectionSequenceDTO sequenceDTO : manageFavouritesRequestDTO.getFavouriteConnectionList()) {
				requestConnectionList.add(sequenceDTO.getConnectionId());
			}

			// prepare response with updated connections
			List<UserConnection> connectionsList = userConnectionRepository
					.findContactByConnectionId(peopleUser.getUserId(), requestConnectionList);
			for (UserConnection userConnection : connectionsList) {
				responseConnectionString.add(userConnection.getConnectionId());
			}
		}

		return responseConnectionString;
	}

	@Override
	public FetchFavouritesListResponseDTO getFavouritesList(Integer fNameOrder, Integer lNameOrder,
			Boolean lNamePreferred, Integer pageNumber, Integer pageSize) {
		PeopleUser sessionUser = tokenAuthService.getSessionUser();

		PageRequest pageable = PageRequest.of(pageNumber, pageSize,
				getSortOperationForFavouriteContacts(fNameOrder, lNameOrder, lNamePreferred));

		// fetching the paginated response for favourite contacts
		Page<UserConnection> listOfFavouriteContacts = userConnectionRepository
				.getFavouritesForGivenUser(sessionUser.getUserId(), pageable);

		List<UserConnection> favouritesList = listOfFavouriteContacts.getContent();

		Map<String, List<String>> contactToGroupMap = prepareConnectionIdToGroupIdMap(sessionUser.getUserId());

		Set<String> numberList = masterService.getRegisteredNumberList();

		// prepare userContactData for favorite connections
		List<UserContactData> contactDataList = getUserContactDataList(sessionUser, favouritesList, contactToGroupMap,
				numberList);

		// prepare response with page details and link to next page if present.
		FetchFavouritesListResponseDTO response = new FetchFavouritesListResponseDTO();
		response.setContactList(contactDataList);
		response.setTotalNumberOfPages(listOfFavouriteContacts.getTotalPages());
		response.setTotalElements(listOfFavouriteContacts.getTotalElements());
		if (!listOfFavouriteContacts.isLast()) {
			response.setNextURL(
					ControllerLinkBuilder
							.linkTo(ControllerLinkBuilder.methodOn(UserConnectionController.class).favouritesList(
									fNameOrder, lNameOrder, lNamePreferred, (pageNumber + 1), pageSize, ""))
							.withSelfRel().getHref());
		}

		return response;
	}

	@Override
	public DeletedInfoResponseDTO deleteInfo(DeleteInfoRequestDTO requestDTO) {
		DeletedInfoResponseDTO responseDTO = new DeletedInfoResponseDTO();
		PeopleUser sessionUser = tokenAuthService.getSessionUser();
		Map<String, List<String>> contactToGroupMap = prepareConnectionIdToGroupIdMap(sessionUser.getUserId());
		Set<String> numberList = masterService.getRegisteredNumberList();

		UserConnection userConnection = userConnectionRepository.findConnectionByConnectionId(sessionUser.getUserId(),
				requestDTO.getConnectionId());

		if (userConnection == null) {
			throw new BadRequestException(MessageCodes.INVALID_CONTACT.getValue());
		}

		if (requestDTO.isRetrieveDeletedInfo() && userConnection.getConnectionDeletedData() != null) {
			UserInformationDTO informationDTO = Optional.ofNullable(userConnection.getContactStaticData())
					.orElse(new UserInformationDTO());
			informationDTO.setUserMetadataList(
					masterService.mergeMetaList(userConnection.getConnectionDeletedData().getUserMetadataList(),
							informationDTO.getUserMetadataList()));
			userConnection.setContactStaticData(informationDTO);
		}
		// deleting connection deleted data object
		userConnection.setConnectionDeletedData(null);

		userConnectionRepository.save(userConnection);

		// create response object
		userConnection = userConnectionRepository.getConnectionDataWithProfileForSelectedContact(
				sessionUser.getUserId(), Collections.singletonList(requestDTO.getConnectionId())).get(0);

		if (contactToGroupMap.containsKey(userConnection.getConnectionId())) {
			userConnection.setGroupIdList(contactToGroupMap.get(userConnection.getConnectionId()));
		}
		UserContactData contactData = prepareContactStaticData(sessionUser, userConnection);
		UserInformationDTO contactStaticData = contactData.getStaticProfileData();
		if (contactStaticData != null) {
			populateStaticDataWithIsVerifiedInfo(contactStaticData, numberList);
		}

		responseDTO.setContactData(prepareContactSharedData(sessionUser, userConnection));
		return responseDTO;
	}

	// Remove shared contact activity(s)
	// for the deleted contact
	private void removeDeletedContactFromAllSharedContactActivity(UserConnection userContact) {
		// 1. Fetch list of 'ActivityContact' with 'initiatorId' and 'connectionId'
		// combination
		// 2. Iterate over activity contacts list and collect involved 'UserActivities'
		// 3. Mark the particular 'ActivityContact' as ACTIVE false
		// 4. Remove associated entry from "sharedConnectionIdMap" for involved
		// "UserActivities" record in in "SHARE_CONTACT_ACTIVITY" case
		// 5. Remove associated entry from "introducedContactNumber" for involved
		// "UserActivities" record in in "INTRODUCTION_REQUEST" case

		List<ActivityContact> toBeUpdatedActivityContacts = new ArrayList<>();
		List<UserActivity> toBeUpdatedUserActivities = new ArrayList<>();

		List<ActivityContact> validActivityContacts = activityContactRepository
				.getActivityContactsByInitiatorIdAndConnectionId(userContact.getConnectionFromId(),
						userContact.getConnectionId());

		for (ActivityContact activityContact : PeopleUtils.emptyIfNull(validActivityContacts)) {

			UserActivity involvedUserActivity = userActivityRepository.findById(activityContact.getActivityId())
					.orElse(null);

			if (involvedUserActivity == null) {
				continue;
			}
			if (activityContact.getRequestType().equals(RequestType.INTRODUCTION_REQUEST)) {
				ContactNumberDTO introducedContactNumberDTO = activityContact.getIntroducedContactNumber();

				String introducedContactNumberKey = introducedContactNumberDTO.getCountryCode() + "_"
						+ introducedContactNumberDTO.getPhoneNumber();

				// Remove the key from "introducedContactNumber"
				involvedUserActivity.getIntroducedContactNumber().remove(introducedContactNumberKey);

				if (PeopleUtils.isNullOrEmpty(involvedUserActivity.getIntroducedContactNumber())) {
					involvedUserActivity.setOverallStatus(ActivityStatus.INACTIVE);
				}

			} else if (activityContact.getRequestType().equals(RequestType.SHARE_CONTACT_ACTIVITY)) {

				String connectionId = activityContact.getConnectionId();

				// Remove the key from "sharedConnectionIdMap"
				involvedUserActivity.getSharedConnectionIdList().remove(connectionId);

				if (PeopleUtils.isNullOrEmpty(involvedUserActivity.getSharedConnectionIdList())) {
					involvedUserActivity.setOverallStatus(ActivityStatus.INACTIVE);
				}
			}

			// Add involved "UserActivity" to 'toBeUpdatedUserActivities' list
			toBeUpdatedUserActivities.add(involvedUserActivity);

			// Update the involved "ActivityContact" ACTIVE status
			activityContact.setIsActive(Boolean.FALSE);
			toBeUpdatedActivityContacts.add(activityContact);
		}

		if (!toBeUpdatedActivityContacts.isEmpty()) {
			activityContactRepository.saveAll(toBeUpdatedActivityContacts);
		}

		if (!toBeUpdatedUserActivities.isEmpty()) {
			userActivityRepository.saveAll(toBeUpdatedUserActivities);
		}
	}

	private Sort getSortOperationForFavouriteContacts(int fNameOrder, int lNameOrder, boolean lNamePreferred) {
		Set<SortElement> set = new TreeSet<>();
		if (lNamePreferred) {
			set.add(new SortElement(LAST_NAME, lNameOrder, 1));
			set.add(new SortElement(FIRST_NAME, fNameOrder, 2));
		} else {
			set.add(new SortElement(FIRST_NAME, fNameOrder, 1));
			set.add(new SortElement(LAST_NAME, lNameOrder, 2));
		}
		return PeopleUtils.getSort(set);
	}

	@Override
	public Map<String, List<String>> prepareConnectionIdToGroupIdMap(String groupOwnerId) {

		List<UserGroup> userGroupList = userGroupRepository.fetchAllUserGroups(groupOwnerId);
		Map<String, List<String>> connectionToGroupMap = new HashMap<>();

		for (UserGroup userGroup : PeopleUtils.emptyIfNull(userGroupList)) {

			String groupId = userGroup.getGroupId();
			List<String> contactIdList = userGroup.getContactIdList();
			List<String> groupIdList;

			for (String contactId : PeopleUtils.emptyIfNull(contactIdList)) {
				if (connectionToGroupMap.containsKey(contactId)) {
					groupIdList = connectionToGroupMap.get(contactId);
				} else {
					groupIdList = new ArrayList<>();
				}
				groupIdList.add(groupId);
				connectionToGroupMap.put(contactId, groupIdList);
			}
		}

		return connectionToGroupMap;
	}

	private void checkAndUpdatePendingConnectionRequest(String currentUserId, String receiverUserId) {
		// check if any pending connection request activity from this user to the other
		// user
		// Then mark the pending activity as "EXPIRED"
		UserActivity pendingActivity = userActivityRepository.getPendingConnectionRequestActivity(currentUserId,
				receiverUserId);
		if (pendingActivity != null) {
			pendingActivity.setOverallStatus(ActivityStatus.EXPIRED);
			pendingActivity.setActionTakenById(currentUserId);
			ActivityType activityType = pendingActivity.getActivityType();
			activityType.setActionTaken(Action.CANCELLED);
			pendingActivity.setActivityType(activityType);
			pendingActivity.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
			userActivityRepository.save(pendingActivity);
		}
	}

	private void validateInitiator(PeopleUser sessionUser, PeopleUser initiator) {
		if (initiator == null) {
			throw new BadRequestException(MessageCodes.INVALID_REQUEST.getValue());
		}

		// check if 'request initiator' belongs to the current user's blocked list
		if (sessionUser.getBlockedUserIdList().stream().anyMatch(id -> id.equals(initiator.getUserId()))) {
			throw new BadRequestException(MessageCodes.CANNOT_ACCEPT_BLOCKED_USER_CONNECTION_REQUEST.getValue());
		}
	}

	private List<UserConnection> checkIfStaticContactPresent(List<UserConnection> existingContactList,
			PeopleUser peopleUser) {
		List<UserConnection> staticContactPresentList = new ArrayList<>();

		// check if any unique static contact with the initiator number already present
		// in the acceptor contact list
		for (UserConnection acceptorExistingContact : PeopleUtils.emptyIfNull(existingContactList)) {

			UserInformationDTO contactStaticData = acceptorExistingContact.getContactStaticData();
			if (contactStaticData == null) {
				continue;
			}
			if (checkIfNumberPresentInContactData(peopleUser.getVerifiedContactNumber(), contactStaticData)) {
				staticContactPresentList.add(acceptorExistingContact);
			}
		}

		return staticContactPresentList;
	}

	private UserConnection acceptConnectionRequestAlreadyReceived(UserActivity activity,
			SharedProfileInformationData sharedProfileDataWithInitiator) {
		String acceptorId = activity.getActivityForId();
		String initiatorId = activity.getActivityById();

		PeopleUser initiator = peopleUserRepository.findByuserId(initiatorId, UserStatus.ACTIVE.toString());
		PeopleUser sessionUser = tokenAuthService.getSessionUser();

		// get all contacts of acceptor
		List<UserConnection> acceptorExistingContactList = userConnectionRepository.findAllContact(acceptorId);

		// check if any unique static contact with the initiator number already present
		// in the acceptor contact list
		List<UserConnection> staticContactPresentList = checkIfStaticContactPresent(acceptorExistingContactList,
				initiator);

		SharedProfileInformationData sharedWithAcceptor = activity.getSharedProfileInformationData();

		if (sharedWithAcceptor == null) {
			sharedWithAcceptor = getDefaultSharedProfileData(initiatorId);
		}
		SharedProfileInformationData sharedWithInitiator = sharedProfileDataWithInitiator;

		// check if valid privacy profile id

		if (sharedWithInitiator == null) {
			sharedWithInitiator = getDefaultSharedProfileData(acceptorId);
		}

		UserConnection acceptorToInitiatorConnection;
		if (!PeopleUtils.isNullOrEmpty(staticContactPresentList)) {

			// update existing connection status of acceptor
			UserConnection acceptorExistingContact = staticContactPresentList.get(0);

			acceptorExistingContact.setConnectionStatus(ConnectionStatus.CONNECTED);
			acceptorExistingContact.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
			acceptorExistingContact.setRealTimeSharedData(sharedWithAcceptor);
			acceptorExistingContact.setConnectionToId(initiatorId);
			acceptorToInitiatorConnection = acceptorExistingContact;

		} else {

			// create new connection from acceptor to initiator
			UserConnection connection = new UserConnection();
			connection.setConnectionFromId(acceptorId);
			connection.setConnectionToId(initiator.getUserId());
			connection.setConnectionStatus(ConnectionStatus.CONNECTED);
			connection.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
			connection.setRealTimeSharedData(sharedWithAcceptor);
			acceptorToInitiatorConnection = connection;
		}

		// get existing contact of initiator
		UserConnection existingContact = userConnectionRepository.findContactByConnectionId(initiatorId,
				activity.getInitiateDetails().getConnectionId());

		List<UserConnection> initiatorExistingContactList = userConnectionRepository.findAllContact(initiatorId);

		// check if any unique static contact with the acceptor number already present
		// in the initiator contact list
		List<UserConnection> initiatorStaticContactPresentList = checkIfStaticContactPresent(
				initiatorExistingContactList, sessionUser);

		UserConnection initiatorToAcceptorConnection;
		if (existingContact != null) {
			existingContact.setConnectionStatus(ConnectionStatus.CONNECTED);
			existingContact.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
			existingContact.setRealTimeSharedData(sharedWithInitiator);
			existingContact.setConnectionToId(acceptorId);
			initiatorToAcceptorConnection = existingContact;

		} else if (!PeopleUtils.isNullOrEmpty(initiatorStaticContactPresentList)) {
			// update existing connection status of acceptor
			UserConnection initiatorExistingContact = initiatorStaticContactPresentList.get(0);
			initiatorExistingContact.setConnectionStatus(ConnectionStatus.CONNECTED);
			initiatorExistingContact.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
			initiatorExistingContact.setRealTimeSharedData(sharedWithInitiator);
			initiatorExistingContact.setConnectionToId(acceptorId);
			initiatorToAcceptorConnection = initiatorExistingContact;
		} else {
			UserConnection newConnection = new UserConnection();
			newConnection.setConnectionFromId(initiatorId);
			newConnection.setConnectionToId(acceptorId);
			newConnection.setConnectionStatus(ConnectionStatus.CONNECTED);
			newConnection.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
			newConnection.setRealTimeSharedData(sharedWithInitiator);
			initiatorToAcceptorConnection = newConnection;
		}

		// update activity
		activity.setOverallStatus(ActivityStatus.INACTIVE);
		ActivityType activityType = activity.getActivityType();
		activityType.setActionTaken(Action.ACCEPTED);
		activity.setActivityType(activityType);
		activity.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());

		acceptorToInitiatorConnection = userConnectionRepository.save(acceptorToInitiatorConnection);
		UserConnection userConnectionInitiatorToAcceptor = userConnectionRepository.save(initiatorToAcceptorConnection);
		userActivityRepository.save(activity);

		UserActivity createdActivity = userActivityRepository.save(createActivityForAcceptConnectionRequest(acceptorId,
				acceptorToInitiatorConnection, userConnectionInitiatorToAcceptor.getConnectionId()));

		if (masterService.isPushNotificationEnabledForUser(createdActivity.getActivityForId())) {
			PushNotificationDTO pushNotificationDTO = new PushNotificationDTO();
			pushNotificationDTO.setActivityId(createdActivity.getActivityId());
			pushNotificationDTO.setActivityRequestType(createdActivity.getActivityType().getRequestType());
			pushNotificationDTO.setInitiatorName(PeopleUtils.getDefaultOrEmpty(sessionUser.getFullName()));
			pushNotificationDTO.setReceiverUserId(createdActivity.getActivityForId());
			pushNotificationDTO.setActivityMessage(createdActivity.getMessage());
			pushNotificationDTO.setConnectionId(userConnectionInitiatorToAcceptor.getConnectionId());

			queueService.sendPayloadToSQS(notificationService
					.prepareSQSPayloadForNotification(createdActivity.getActivityForId(), pushNotificationDTO));
		}

		return acceptorToInitiatorConnection;
	}

	private void validateAndRemoveIncorrectKeyValue(UserInformationDTO userInformationDTO) {
		for (UserProfileData profileData : PeopleUtils.emptyIfNull(userInformationDTO.getUserMetadataList())) {
			if (profileData != null) {
				List<KeyValueData> toBeRemovedKeyValueData = new ArrayList<>();
				for (KeyValueData keyValueData : profileData.getKeyValueDataList()) {
					if (PeopleUtils.isNullOrEmpty(keyValueData.getKey())
							|| PeopleUtils.isNullOrEmpty(keyValueData.getVal())) {
						toBeRemovedKeyValueData.add(keyValueData);
					}
				}
				List<KeyValueData> keyValueDataList = profileData.getKeyValueDataList();
				keyValueDataList.removeAll(toBeRemovedKeyValueData);
				profileData.setKeyValueDataList(keyValueDataList);
			}
		}
	}

	private void updateSequenceNumberForFavouriteContact(UserConnection validUserConnection, int maxSequenceNumber) {
		if (validUserConnection.getSequenceNumber() == null) {
			validUserConnection.setSequenceNumber(++maxSequenceNumber);
		}
	}

	@Override
	public void deleteAllContactByPeopleUId() {
		PeopleUser sessionUser = tokenAuthService.getSessionUser();
		DeleteContactRequest deleteContactRequest = new DeleteContactRequest();
		List<UserConnection> userConnections = userConnectionRepository.findAllContactID(sessionUser.getUserId());
		List<String> connectionIdList = new ArrayList<>();
		for (UserConnection connection : PeopleUtils.emptyIfNull(userConnections)) {
			connectionIdList.add(connection.getConnectionId());
		}
		deleteContactRequest.setConnectionIdList(connectionIdList);
		List<String> toBeDeletedContactIdList = deleteContactRequest.getConnectionIdList();
		List<String> otherConnectionId = new ArrayList<>();
		List<String> connectedContactIdList = new ArrayList<>();

		String sessionUserId = sessionUser.getUserId();

		/*
		 * Includes both connected contacts and static contacts
		 */
		List<UserConnection> validContactList = userConnectionRepository.findContactByConnectionId(sessionUserId,
				toBeDeletedContactIdList);

		/* Includes only connected contacts */
		List<UserConnection> connectedContactList = userConnectionRepository.findConnectionByConnectionId(sessionUserId,
				toBeDeletedContactIdList);

		if (PeopleUtils.isNullOrEmpty(validContactList)) {
			throw new BadRequestException(MessageCodes.INVALID_CONTACT.getValue());
		}

		for (UserConnection connection : PeopleUtils.emptyIfNull(connectedContactList)) {
			connectedContactIdList.add(connection.getConnectionToId());
		}

		List<UserConnection> toBeUpdatedConnectionList = new ArrayList<>();
		List<String> deletedContactIdList = new ArrayList<>();

		for (UserConnection userContact : validContactList) {

			if (isContactInConnectedState(userContact)) {

				// downgrade other connection to static contact
				// get other connection - from ID and To ID

				UserConnection otherConnection = userConnectionRepository
						.findConnectionByFromIdAndToId(userContact.getConnectionToId(), sessionUserId);

				otherConnection.setConnectionStatus(ConnectionStatus.NOT_CONNECTED);
				otherConnection.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());

				toBeUpdatedConnectionList.add(otherConnection);

				otherConnectionId.add(otherConnection.getConnectionId());
			}

			// remove connection from user groups
			userGroupRepository.removeContactIdFromUserGroups(sessionUserId, userContact.getConnectionId());

			// delete connection permanently
			removeDeletedContactFromAllSharedContactActivity(userContact);

			deletedContactIdList.add(userContact.getConnectionId());

		}

		// expire all activity and activityContacts created by this user to the
		// connectionId
		userActivityRepository.expireActivityForInitiate(sessionUserId, toBeDeletedContactIdList);
		activityContactRepository.expireActivityContactsByInitiatorIdAndReceiverId(sessionUserId,
				connectedContactIdList, false);

		if (!PeopleUtils.isNullOrEmpty(connectedContactIdList)) {
			// Expire all pending activities and activity contacts created by the 'deleted
			// contact'
			userActivityRepository.expireActivityCreatedByDeletedContact(connectedContactIdList, sessionUserId);
			activityContactRepository.expireActivityContactsByInitiatorIdAndReceiverId(sessionUserId,
					connectedContactIdList, true);
		}

		userConnectionRepository.saveAll(toBeUpdatedConnectionList);
		userConnectionRepository.deleteConnectionsByUserIdAndConnectionIds(sessionUserId, deletedContactIdList);

		// prepare response
		DeleteContactResponse response = new DeleteContactResponse();
		response.setDeletedConnectionIdList(deletedContactIdList);

		updateStaticSharedInformation(otherConnectionId);
	}

	@Override
	public ContactContactIDRequest updateContactId(ContactContactIDRequest request) {

		PeopleUser sessionUser = tokenAuthService.getSessionUser();

		for (ContactDTO dto : PeopleUtils.emptyIfNull(request.getContactStaticDataList())) {
			UserConnection userConnection = userConnectionRepository
					.findConnectionByConnectionIdnnNew(sessionUser.getUserId(), dto.getConnectionId());
			if (userConnection == null) {
				throw new BadRequestException(MessageCodes.INVALID_CONTACT.getValue());
			}
			userConnection.setDeviceContactId(dto.getContactId());
			if (userConnection.getContactStaticData() != null) {
				userConnection.getContactStaticData().setDeviceContactId(dto.getContactId());
			}
			userConnectionRepository.save(userConnection);
		}
		return null;
	}

	@Override
	public DeleteContactRequest IdenticalFlagUpdate(DeleteContactRequest contactRequest) {
		PeopleUser sessionUser = tokenAuthService.getSessionUser();
		List<String> list = new ArrayList<>();
		for (String connectionId : PeopleUtils.emptyIfNull(contactRequest.getConnectionIdList())) {
			UserConnection userConnection = userConnectionRepository
					.findConnectionByConnectionIdnnNew(sessionUser.getUserId(), connectionId);
			if (userConnection == null) {
				throw new BadRequestException(MessageCodes.INVALID_CONTACT.getValue());
			}
			if (userConnection.getContactStaticData() != null) {
				userConnection.getContactStaticData().setIsIdentical(true);
			}
			userConnectionRepository.save(userConnection);
			list.add(userConnection.getConnectionId());
		}

		// set Response
		DeleteContactRequest response = new DeleteContactRequest();
		response.setConnectionIdList(list);

		return response;
	}
}