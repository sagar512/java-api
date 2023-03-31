package com.peopleapp.service.impl;

import com.mongodb.client.result.UpdateResult;
import com.peopleapp.configuration.LocaleMessageReader;
import com.peopleapp.constant.MessageConstant;
import com.peopleapp.controller.NetworkController;
import com.peopleapp.dto.*;
import com.peopleapp.dto.requestresponsedto.*;
import com.peopleapp.enums.*;
import com.peopleapp.exception.BadRequestException;
import com.peopleapp.model.*;
import com.peopleapp.repository.*;
import com.peopleapp.security.TokenAuthService;
import com.peopleapp.service.*;
import com.peopleapp.util.PeopleUtils;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import javax.inject.Inject;
import java.util.*;

@Service
public class NetworkServiceImpl implements NetworkService {

    @Value("${app.link}")
    private String appLink;

    @Value("${recommendedNetwork.limit}")
    private int recommendedNetworkLimit;

    @Value("${recommendedNetwork.local.radiusInMiles}")
    private int localNetworkRadiusInMiles;

    @Value("${recommendedNetwork.top.popular.limit}")
    private int topPopularLimit;

    @Value("${recommendedNetwork.top.suggested.limit}")
    private int topSuggestedLimit;

    @Value("${recommendedNetwork.top.local.limit}")
    private int toplocalLimit;

    @Value("${recommendedNetwork.newMemberWeightage}")
    private int newMemberWeightage;

    @Value("${recommendedNetwork.newNetworkWeightage}")
    private int newNetworkWeightage;

    @Inject
    private TokenAuthService tokenAuthService;

    @Inject
    private NetworkMemberRepository networkMemberRepository;

    @Inject
    private NetworkRepository networkRepository;

    @Inject
    private PeopleUserService peopleUserService;

    @Inject
    private UserConnectionRepository userConnectionRepository;

    @Inject
    private UserActivityService userActivityService;

    @Inject
    private UserActivityRepository userActivityRepository;

    @Inject
    private NotificationService notificationService;

    @Inject
    private PeopleUserRepository peopleUserRepository;

    @Inject
    private UserConnectionService userConnectionService;

    @Inject
    private ReportedUserDataRepository reportedUserDataRepository;

    @Inject
    private RecentActiveNetworkRepository recentActiveNetworkRepository;

    @Inject
    private NetworkCategoryRepository networkCategoryRepository;

    @Inject
    private MasterService masterService;

    @Inject
    private Jedis redisClient;

    @Inject
    private LocaleMessageReader messages;

    @Inject
    private QueueService queueService;

    @Inject
    private NetworkMessagesRepository networkMessagesRepository;

    private static final String NETWORK_MEMBER_FIRST_NAME = "networkMemberDetails.firstName.value";
    private static final String NETWORK_MEMBER_LAST_NAME = "networkMemberDetails.lastName.value";
    private static final String MEMBER_ROLE = "memberRole";
    private static final String PHONE_NUMBER = "PHONENUMBER";
    private static final String EMAIL = "EMAIL";
    private static final String SOCIAL_PROFILE = "SOCIALPROFILE";
    private static final String TWITTER = "PL.02.00";
    private static final String LINKEDIN = "PL.02.01";
    private static final String INSTAGRAM = "PL.02.06";

    @Override
    public void updateNetworkSetting(UpdateNetworkSettingDTO updateNetworkSetting) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        List<NetworkMember> networkMember = networkMemberRepository.getNetworkMemberDetailsForUser(sessionUser.getUserId());
        List<UserActivity> networkJoinRequests = userActivityRepository.getPendingActivitiesByInitiatedByIdAndRequestType(
                sessionUser.getUserId(), RequestType.NETWORK_JOIN_REQUEST);

        List<String> networkIdList = new ArrayList<>();

        for (NetworkMember member : PeopleUtils.emptyIfNull(networkMember)) {
            networkIdList.add(member.getNetworkId());
        }

        if (PeopleUtils.isNullOrEmpty(updateNetworkSetting.getNetworkSharedValueList()) &&
                PeopleUtils.isNullOrEmpty(networkMember) && PeopleUtils.isNullOrEmpty(networkJoinRequests) ) {
            sessionUser.getNetworkSharedValueList().clear();
        } else if (PeopleUtils.isNullOrEmpty(updateNetworkSetting.getNetworkSharedValueList())) {
            throw new BadRequestException(MessageCodes.NETWORK_SHARED_CONTACTS_CANNOT_BE_REMOVED.getValue());
        } else {
            checkUpdatedSharedValueIdList(sessionUser, networkJoinRequests, updateNetworkSetting.getNetworkSharedValueList(), networkIdList);
        }

        peopleUserRepository.save(sessionUser);
    }

    @Override
    public List<String> getNetworkDefaultSetting() {
        PeopleUser sessionUser = tokenAuthService.getSessionUser();

        Map<String, UserProfileData> metadataMap = sessionUser.getMetadataMap();
        List<String> networkSharedValue = sessionUser.getNetworkSharedValueList();
        List<String> networkDefaultSetting = new ArrayList<>();
        for (String value : PeopleUtils.emptyIfNull(networkSharedValue)) {
            if (metadataMap.containsKey(value)) {
                networkDefaultSetting.add(value);
            }
        }
        sessionUser.setNetworkSharedValueList(networkDefaultSetting);
        peopleUserRepository.save(sessionUser);
        return networkDefaultSetting;
    }

    @Override
    public String updateNetworkFavouriteStatus(UpdateNetworkFavouriteRequestDTO updateNetworkFavouriteRequestDTO) {
        PeopleUser peopleUser = tokenAuthService.getSessionUser();
        NetworkMember networkMember = getNetworkMemberDetailsIfActive(updateNetworkFavouriteRequestDTO.getNetworkId(),
                peopleUser.getUserId());
        networkMember.setIsFavourite(updateNetworkFavouriteRequestDTO.isNetworkFavorite());
        networkMemberRepository.save(networkMember);
        return messages.get(MessageConstant.NETWORK_FAVOURITE_STATUS);
    }

    @Override
    public GetUserNetworksResponseDTO getUserNetworks(Integer pageNumber, Integer pageSize) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        String userId = sessionUser.getUserId();
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Page<UserNetworkDetails> userNetworkDetailsList = networkMemberRepository.getUserNetworks(userId, pageable);

        Link link = ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(NetworkController.class)
                .getUserNetworks(pageNumber + 1, pageSize, "")).withSelfRel();

        //Preparing the response structure
        GetUserNetworksResponseDTO getUserNetworksResponse = new GetUserNetworksResponseDTO();
        getUserNetworksResponse.setUserNetworkDetailsList(userNetworkDetailsList.getContent());
        getUserNetworksResponse.setTotalNumberOfPages(userNetworkDetailsList.getTotalPages());
        getUserNetworksResponse.setTotalElements(userNetworkDetailsList.getTotalElements());
        if (!userNetworkDetailsList.isLast()) {
            getUserNetworksResponse.setNextURL(link.getHref());
        }
        return getUserNetworksResponse;

    }


    @Override
    public CreateOrEditNetworkResponseDTO createNetwork(CreateNetworkRequestDTO createNetworkRequest) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        String userId = sessionUser.getUserId();

        //check to verify if user has created network with same name
        UserNetworkDetails userNetworkDetails = networkMemberRepository.getUserOwnedNetworkByName(sessionUser.getUserId(),
                createNetworkRequest.getName());

        //if value present for userNetworkDetails then network already exist
        if (userNetworkDetails != null) {
            throw new BadRequestException(MessageCodes.NETWORK_NAME_ALREADY_EXIST.getValue());
        }

        // create network object
        Network network = new Network();

        network.setPrivacyType(createNetworkRequest.getPrivacyType());
        network.setName(createNetworkRequest.getName().trim());
        network.setImageURL(createNetworkRequest.getImageURL());
        network.setBannerImageURL(createNetworkRequest.getBannerImageURL());
        network.setNetworkLocation(createNetworkRequest.getNetworkLocation());
        network.setPrimaryContactMethod(createNetworkRequest.getPrimaryContactMethod());
        network.setMemberCount(1);
        network.setAdminCount(1);
        network.setTagList(createNetworkRequest.getTagList());
        network.setDescription(createNetworkRequest.getDescription());
        network.setNetworkCategory(createNetworkRequest.getNetworkCategory());
        network.setLastModifiedTime(new DateTime());

        // check if communication type shared by initiator is valid
        Boolean isCommunicationTypeShared = checkIfCommunicationTypeShared(sessionUser, network);

        if (!isCommunicationTypeShared) {
            throw new BadRequestException(MessageCodes.INCOMPLETE_PROFILE_INFORMATION.getValue());
        }

        Network createdNetwork = networkRepository.save(network);

        // create network owner
        NetworkMember networkMember = new NetworkMember();
        networkMember.setNetworkId(createdNetwork.getNetworkId());
        networkMember.setMemberId(userId);
        networkMember.setMemberRole(NetworkMemberRole.OWNER.getValue());

        networkMemberRepository.save(networkMember);

        // Create Entry in Recent Active Network
        createRecentActiveNetwork(createdNetwork.getNetworkId(), createdNetwork.getNetworkCategory(), Boolean.TRUE,
                Boolean.FALSE);

        // populate response object
        CreateOrEditNetworkResponseDTO createNetworkResponseDTO = new CreateOrEditNetworkResponseDTO();
        NetworkDetails networkDetails = new NetworkDetails();
        networkDetails.setName(createdNetwork.getName());
        networkDetails.setImageURL(createdNetwork.getImageURL());
        networkDetails.setBannerImageURL(createdNetwork.getBannerImageURL());
        networkDetails.setPrivacyType(createdNetwork.getPrivacyType());
        networkDetails.setPrimaryContactMethod(createdNetwork.getPrimaryContactMethod());
        networkDetails.setNetworkLocation(createdNetwork.getNetworkLocation());
        networkDetails.setMemberCount(createdNetwork.getMemberCount());
        networkDetails.setAdminCount(createdNetwork.getAdminCount());
        networkDetails.setTagList(createdNetwork.getTagList());
        networkDetails.setNetworkCategory(createdNetwork.getNetworkCategory());
        networkDetails.setLastModifiedTime(createdNetwork.getLastModifiedTime());
        networkDetails.setDescription(createdNetwork.getDescription());
        createNetworkResponseDTO.setNetworkDetails(networkDetails);
        createNetworkResponseDTO.setNetworkId(createdNetwork.getNetworkId());

        return createNetworkResponseDTO;
    }

    @Override
    public JoinNetworkResponseDTO joinNetwork(JoinNetworkRequestDTO joinNetworkRequestDTO) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        String networkId = joinNetworkRequestDTO.getNetworkId();

        // check valid network
        Network networkToBeJoined = getNetworkDetailsIfActive(networkId);

        //check if user is already member of the network
        NetworkMember validMember = networkMemberRepository.findByIdAndUserIdAndStatus(networkId, sessionUser.getUserId(),
                NetworkMemberStatus.ACTIVE.getValue());

        if (validMember != null) {
            throw new BadRequestException(MessageCodes.NETWORK_MEMBER_ALREADY_EXIST.getValue());
        }

        //check if request already sent and is in pending state
        UserActivity userActivity = userActivityRepository.getPendingNetworkRequestActivityForUser(sessionUser.getUserId(),
                networkId, ActivityStatus.PENDING.getValue());

        if (userActivity != null) {
            throw new BadRequestException(MessageCodes.NETWORK_REQUEST_ALREADY_SENT.getValue());
        }

        // check if communication type shared by initiator is valid
        Boolean isCommunicationTypeShared = checkIfCommunicationTypeShared(sessionUser, networkToBeJoined);

        if (!isCommunicationTypeShared) {
            throw new BadRequestException(MessageCodes.INCOMPLETE_PROFILE_INFORMATION.getValue());
        }

        return joinPublicNetwork(networkToBeJoined, sessionUser);
    }

    @Override
    public UserNetworkDetails getNetworkDetails(String networkId) {
        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        Network verifiedNetwork = getNetworkDetailsIfActive(networkId);
        NetworkPrimaryContactMethod contactMethod = verifiedNetwork.getPrimaryContactMethod();
        NetworkMember validNetworkMember = networkMemberRepository.findByIdAndUserIdAndStatus(networkId,
                sessionUser.getUserId(), NetworkMemberStatus.ACTIVE.getValue());

        String memberRole = NetworkMemberRole.GUEST.getValue();
        Boolean isOwnerOrAdmin = Boolean.FALSE;
        Boolean isOwner = Boolean.FALSE;
        UserNetworkDetails userNetworkDetails = new UserNetworkDetails();
        //If session user is not a GUEST to network
        if (validNetworkMember != null) {
            memberRole = validNetworkMember.getMemberRole();
            isOwnerOrAdmin = checkIfMemberIsOwnerOrAdmin(validNetworkMember);
            isOwner = checkIfMemberIsOwner(validNetworkMember);
            userNetworkDetails.setFavourite(validNetworkMember.getIsFavourite());
        } else {
            long numberOfRequestSent = userActivityRepository.getCountOfSentNetworkRequestActivity(networkId,
                    sessionUser.getUserId());
            if (numberOfRequestSent > 0) {
                userNetworkDetails.setIsRequestSent(Boolean.TRUE);
            } else {
                userNetworkDetails.setIsRequestSent(Boolean.FALSE);
            }
        }

        //prepare general response consisting of networkID, details and role
        userNetworkDetails.setNetworkId(networkId);
        userNetworkDetails.setNetworkDetails(prepareNetworkDetails(verifiedNetwork));
        userNetworkDetails.setMemberRole(memberRole);


        //Set pending request for owner and admin's
        if (isOwnerOrAdmin) {
            userNetworkDetails.setPendingRequestCount(userActivityRepository.getPendingJoinRequestCountForNetwork(
                    sessionUser.getUserId(), networkId));
        }

        /* Set owner details -
         * if session user is owner of network public data will be given
         * else owner information will be given based on session user and owners connection status
         * */
        if (!isOwner) {
            List<NetworkMember> networkMembers = networkMemberRepository.findByIdAndRole(networkId,
                    Arrays.asList(NetworkMemberRole.OWNER.getValue()));
            // prepare connection object for session user to owner of the network
            UserContactData ownerContactData = getConnectionObject(sessionUser, networkMembers.get(0), contactMethod);

            List<PeopleUser> membersUserData = peopleUserRepository.findByUserIdsAndStatus(
                    Arrays.asList(ownerContactData.getToUserId()), UserStatus.ACTIVE);

            // check if connection request was initiated by session user to owner.
            UserActivity pendingConnectionRequestActivity = userActivityRepository.getPendingConnectionRequestActivity(
                    sessionUser.getUserId(), membersUserData.get(0).getVerifiedContactNumber());

            /* if the connection request was sent update connection status to PENDING */
            if (pendingConnectionRequestActivity != null) {
                ownerContactData.setConnectionStatus(ConnectionStatus.PENDING.getValue());
            }

            userNetworkDetails.setNetworkOwnerContact(ownerContactData);
        } else {
            UserContactData ownerContactData = new UserContactData();
            ownerContactData.setToUserId(sessionUser.getUserId());
            ownerContactData.setPublicProfileData(masterService.prepareUserPublicData(sessionUser));
            ownerContactData.setNetworkSharedInformationData(getNetworkSharedData(validNetworkMember, contactMethod));
            userNetworkDetails.setNetworkOwnerContact(ownerContactData);
        }

        return userNetworkDetails;
    }

    @Override
    public NetworkMembersResponseDTO getMembersOfNetwork(String networkId, APIRequestParamData apiRequestParamData) {
        //get session user
        PeopleUser sessionUser = tokenAuthService.getSessionUser();

        //parsing values
        int sortByRole = apiRequestParamData.getSortByRole();
        int fNameOrder = apiRequestParamData.getFNameOrder();
        int lNameOrder = apiRequestParamData.getLNameOrder();
        boolean lNamePreferred = apiRequestParamData.getLastNamePreferred();
        int pageNumber = apiRequestParamData.getPageNumber();
        int pageSize = apiRequestParamData.getPageSize();

        //verify network
        Network verifiedNetwork = getNetworkDetailsIfActive(networkId);
        NetworkPrimaryContactMethod contactMethod = verifiedNetwork.getPrimaryContactMethod();

        //verify user is part of network
        getNetworkMemberDetailsIfActive(networkId, sessionUser.getUserId());

        Pageable pageable = PageRequest.of(pageNumber, pageSize, getSortForNetworkMembers(sortByRole, fNameOrder,
                lNameOrder, lNamePreferred));

        //fetch all network members including admins with page object
        Page<NetworkMember> listOfNetworkMembers = networkMemberRepository.getNetworkMemberDetailsByIdAndRole(networkId,
                apiRequestParamData.getSearchString().trim(), Arrays.asList(NetworkMemberRole.OWNER.getValue(),
                        NetworkMemberRole.ADMIN.getValue(), NetworkMemberRole.MEMBER.getValue()), pageable);

        Link link = ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(NetworkController.class)
                .getMembersOfNetwork(networkId, apiRequestParamData.getSearchString(), apiRequestParamData.getSortByRole(),
                        apiRequestParamData.getFNameOrder(), apiRequestParamData.getLNameOrder(),
                        apiRequestParamData.getLastNamePreferred(), (pageNumber + 1),
                        apiRequestParamData.getPageSize(), "")).withSelfRel();

        return getNetworkMembersResponseDTO(sessionUser, link, listOfNetworkMembers, contactMethod);
    }

    @Override
    public NetworkMembersResponseDTO getAdminsOfNetwork(String networkId, APIRequestParamData apiRequestParamData) {

        //get session user
        PeopleUser sessionUser = tokenAuthService.getSessionUser();

        //verify network
        Network verifiedNetwork = getNetworkDetailsIfActive(networkId);
        NetworkPrimaryContactMethod contactMethod = verifiedNetwork.getPrimaryContactMethod();

        //verify user is part of network
        getNetworkMemberDetailsIfActive(networkId, sessionUser.getUserId());

        //parsing values
        int fNameOrder = apiRequestParamData.getFNameOrder();
        int lNameOrder = apiRequestParamData.getLNameOrder();
        boolean lNamePreferred = apiRequestParamData.getLastNamePreferred();
        int pageNumber = apiRequestParamData.getPageNumber();
        int pageSize = apiRequestParamData.getPageSize();

        //pageable object with pageNUmber, pageSize and sorting logic.
        Pageable pageable = PageRequest.of(pageNumber, pageSize, getSortForNetworkMembers(0, fNameOrder,
                lNameOrder, lNamePreferred));

        //fetch all network admins with page object
        Page<NetworkMember> listOfNetworkAdmins = networkMemberRepository.getNetworkMemberDetailsByIdAndRole(networkId,
                apiRequestParamData.getSearchString().trim(), Arrays.asList(NetworkMemberRole.OWNER.getValue(),
                        NetworkMemberRole.ADMIN.getValue()), pageable);

        Link link = ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(NetworkController.class)
                .getAdminsOfNetwork(networkId, apiRequestParamData.getSearchString(), apiRequestParamData.getFNameOrder(),
                        apiRequestParamData.getLNameOrder(), apiRequestParamData.getLastNamePreferred(),
                        (pageNumber + 1), apiRequestParamData.getPageSize(), "")).withSelfRel();

        return getNetworkMembersResponseDTO(sessionUser, link, listOfNetworkAdmins, contactMethod);
    }

    @Override
    public void messageAllMembers(MessageNetworkMembersDTO messageNetworkMembersDTO) {
        PeopleUser sessionUser = tokenAuthService.getSessionUser();

        //validating network
        String networkId = messageNetworkMembersDTO.getNetworkId();
        Network network = getNetworkDetailsIfActive(networkId);

        //validating if user is part of network or not
        NetworkMember networkMember = getNetworkMemberDetailsIfActive(networkId, sessionUser.getUserId());

        boolean isOwnerOrAdmin = checkIfMemberIsOwnerOrAdmin(networkMember);

        if (!isOwnerOrAdmin) {
            throw new BadRequestException(MessageCodes.ACTION_CANNOT_BE_TAKEN.getValue());
        }

        //check communication type
        Boolean isShared = checkIfCommunicationTypeShared(sessionUser, network);
        if (!isShared) {
            throw new BadRequestException(MessageCodes.INCOMPLETE_PROFILE_INFORMATION.getValue());
        }

        //get all members of the network
        List<NetworkMember> listOfMembersToBeMessaged = networkMemberRepository.findByIdAndRole(networkId, Arrays.asList(
                NetworkMemberRole.OWNER.getValue(), NetworkMemberRole.ADMIN.getValue(), NetworkMemberRole.MEMBER.getValue()));

        List<UserActivity> userActivityList = new ArrayList<>();

        ActivityType activityType = new ActivityType();
        activityType.setRequestType(RequestType.NETWORK_MESSAGE_BROADCAST);
        activityType.setActionTaken(Action.INITIATED);

        List<SQSPayload> sqsPayloadList = new ArrayList<>();

        for (NetworkMember member : listOfMembersToBeMessaged) {
            //ignoring the user sending this message
            if (member.getMemberId().equals(networkMember.getMemberId())) {
                continue;
            }

            //create user activity for each member
            UserActivity userActivity = new UserActivity();
            userActivity.setActivityById(sessionUser.getUserId());
            userActivity.setActivityForId(member.getMemberId());
            userActivity.setOverallStatus(ActivityStatus.ACTIVE);
            userActivity.setMessage(messageNetworkMembersDTO.getMessageToBroadcast());
            userActivity.setNetworkId(PeopleUtils.convertStringToObjectId(networkId));
            userActivity.setActivityType(activityType);
            userActivity.setCreatedOn(new DateTime());
            userActivity.setLastUpdatedOn(new DateTime());
            userActivityList.add(userActivity);
        }
        userActivityRepository.saveAll(userActivityList);

        // creating sqsPayload
        for (UserActivity activity : userActivityList) {
            sqsPayloadList.add(prepareSQSPayloadForNetwork(network, activity, sessionUser));
        }

        queueService.sendPayloadToSQS(sqsPayloadList);

        /* record the message broadCasted on network */
        NetworkBroadcastMessage persistMessage = new NetworkBroadcastMessage();
        persistMessage.setMessage(messageNetworkMembersDTO.getMessageToBroadcast());
        persistMessage.setNetworkId(new ObjectId(networkId));
        persistMessage.setBroadcasterId(new ObjectId(sessionUser.getUserId()));
        persistMessage.setBroadcasterRole(networkMember.getMemberRole());
        persistMessage.setSentCount((long) network.getMemberCount() - 1);
        persistMessage.setMessageBroadcastTime(new DateTime());

        networkMessagesRepository.save(persistMessage);
    }

    /**
     * @param searchString
     * @param pageNumber
     * @param pageSize
     * @return
     */
    @Override
    public SearchedNetworkResponseDTO searchNetwork(String searchString, Integer sortOrder, Integer pageNumber,
                                                    Integer pageSize) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();

        if (searchString.trim().isEmpty()) {
            throw new BadRequestException(MessageCodes.INVALID_OPERATION.getValue());
        }

        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Page<Network> searchResult = networkRepository.searchNetwork(sessionUser.getUserId(),
                searchString.trim(), sortOrder, pageable);

        List<SearchedNetworkDetails> matchedNetworks = new ArrayList<>();

        for (Network eachNetwork : searchResult.getContent()) {
            SearchedNetworkDetails searchedNetwork = new SearchedNetworkDetails();
            searchedNetwork.setNetworkId(eachNetwork.getNetworkId());
            searchedNetwork.setNetworkDetails(prepareNetworkDetails(eachNetwork));
            matchedNetworks.add(searchedNetwork);
        }

        SearchedNetworkResponseDTO searchedNetworkResponse = new SearchedNetworkResponseDTO();
        searchedNetworkResponse.setMatchedNetworkList(matchedNetworks);
        searchedNetworkResponse.setTotalElements(searchResult.getTotalElements());
        searchedNetworkResponse.setTotalNumberOfPages(searchResult.getTotalPages());

        if (!searchResult.isLast()) {
            searchedNetworkResponse.setNextURL(ControllerLinkBuilder.linkTo(ControllerLinkBuilder
                    .methodOn(NetworkController.class)
                    .searchNetwork(searchString, sortOrder, pageNumber + 1, pageSize, ""))
                    .withSelfRel().getHref());
        }

        return searchedNetworkResponse;
    }

    @Override
    public String inviteMembersToNetwork(NetworkInviteRequestDTO networkInviteRequest) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        String initiatorId = sessionUser.getUserId();
        String networkId = networkInviteRequest.getNetworkId();
        List<NetworkInviteeContact> networkInviteeList = networkInviteRequest.getNetworkInviteeList();

        // check if valid network
        Network inviteToNetwork = getNetworkDetailsIfActive(networkId);

        // check if valid member
        NetworkMember networkMember = getNetworkMemberDetailsIfActive(networkId, initiatorId);

        // check if initiator is owner or admin
        Boolean isOwnerOrAdmin = checkIfMemberIsOwnerOrAdmin(networkMember);

        if (!isOwnerOrAdmin) {
            throw new BadRequestException(MessageCodes.ACTION_CANNOT_BE_TAKEN.getValue());
        }

        //check communication type
        Boolean isShared = checkIfCommunicationTypeShared(sessionUser, inviteToNetwork);
        if (!isShared) {
            throw new BadRequestException(MessageCodes.INCOMPLETE_PROFILE_INFORMATION.getValue());
        }

        List<NetworkInviteeContact> validUserContactList = prepareValidContactList(initiatorId, networkInviteeList);
        updateInvitationStatusAndMemberStatus(sessionUser, validUserContactList, networkId, false);

        if (PeopleUtils.isNullOrEmpty(validUserContactList)) {
            throw new BadRequestException(MessageCodes.INVALID_OPERATION.getValue());
        }

        // create network share activity for all valid contact/s
        List<UserActivity> userActivityListToBePersisted = new ArrayList<>();
        List<SQSPayload> payloadForNotification = new ArrayList<>();

        ActivityType activityType = new ActivityType();
        activityType.setRequestType(RequestType.NETWORK_MEMBER_INVITE);
        activityType.setActionTaken(Action.INITIATED);

        int numberOfContactsForWhichNetworkInvitationIsSent = 0;

        for (NetworkInviteeContact userContact : PeopleUtils.emptyIfNull(validUserContactList)) {
            if (userContact.getIsInviteeAPeopleUser()) {
                if (userContact.getIsAlreadyANetworkMember() || userContact.getIsNetworkSharedOrInviteSent()) {
                    continue;
                }

                numberOfContactsForWhichNetworkInvitationIsSent++;

                UserActivity userActivity = new UserActivity();
                userActivity.setActivityById(initiatorId);
                userActivity.setOverallStatus(ActivityStatus.PENDING);
                userActivity.setActivityType(activityType);
                userActivity.setRequestId(new ObjectId().toString());
                userActivity.setActivityForId(userContact.getInviteeUserId());
                userActivity.setMessage(networkInviteRequest.getMessage());
                userActivity.setNetworkId(PeopleUtils.convertStringToObjectId(networkId));
                userActivity.setCreatedOn(new DateTime());
                userActivity.setLastUpdatedOn(new DateTime());

                userActivityListToBePersisted.add(userActivity);

            }

        }

        if (numberOfContactsForWhichNetworkInvitationIsSent == 0) {
            throw new BadRequestException(MessageCodes.NETWORK_INVITEE_SENT.getValue());
        }

        userActivityRepository.saveAll(userActivityListToBePersisted);

        //Send notifications
        for (UserActivity shareNetworkActivity : PeopleUtils.emptyIfNull(userActivityListToBePersisted)) {
            payloadForNotification.add(prepareSQSPayloadForNetwork(inviteToNetwork, shareNetworkActivity, sessionUser));
        }
        queueService.sendPayloadToSQS(payloadForNotification);

        return messages.get(MessageConstant.NETWORK_INVITATION);
    }

    /**
     * Remove member/s from network
     * Only members can be removed
     */
    @Override
    public String removeMemberFromNetwork(RemoveMemberFromNetworkDTO removeMemberFromNetwork) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        String userId = getUserId();
        // check if network is a valid
        Network network = getNetworkDetailsIfActive(removeMemberFromNetwork.getNetworkId());

        // check if initiate is a member of network
        NetworkMember networkMember = getNetworkMemberDetailsIfActive(network.getNetworkId(), userId);

        // check if initiate is owner or admin
        Boolean isOwnerOrAdmin = checkIfMemberIsOwnerOrAdmin(networkMember);
        if (!isOwnerOrAdmin) {
            throw new BadRequestException(MessageCodes.ACTION_CANNOT_BE_TAKEN.getValue());
        }

        //check communication type
        Boolean isShared = checkIfCommunicationTypeShared(sessionUser, network);
        if (!isShared) {
            throw new BadRequestException(MessageCodes.INCOMPLETE_PROFILE_INFORMATION.getValue());
        }

        // get the valid network member list
        List<NetworkMember> networkMemberList = networkMemberRepository.findAllActiveNonAdminMembersByNetworkIdAndMemberId(
                network.getNetworkId(), removeMemberFromNetwork.getMemberIdList());
        List<String> removedMemberIds = new ArrayList<>();
        List<SQSPayload> sqsPayloadList = new ArrayList<>();
        for (NetworkMember toBeRemovedMember : PeopleUtils.emptyIfNull(networkMemberList)) {
            toBeRemovedMember.setMemberStatus(NetworkMemberStatus.REMOVED.getValue());
            removedMemberIds.add(toBeRemovedMember.getMemberId());
            sqsPayloadList.add(notificationService.prepareSQSPayloadForSilentNotification(toBeRemovedMember.getMemberId(),
                    RequestType.REMOVE_NETWORK_MEMBER.getValue(), null, null, network.getNetworkId()));
        }

        queueService.sendPayloadToSQS(sqsPayloadList);

        // reduce the member count of network
        int initialCount = network.getMemberCount();
        if (!PeopleUtils.isNullOrEmpty(networkMemberList)) {
            network.setMemberCount(initialCount - networkMemberList.size());
            networkMemberRepository.saveAll(networkMemberList);
            networkMemberRepository.deleteNetworkMemberByMemberIdsAndNetworkId(removedMemberIds, network.getNetworkId());
            networkRepository.save(network);
        } else {
            throw new BadRequestException(MessageCodes.INVALID_NETWORK_MEMBER.getValue());
        }

        return messages.get(MessageConstant.NETWORK_MEMBER_REMOVE);
    }

    /**
     * Only owner can promote members to admin
     * Each promoted member will get an activity item and a notification
     * Initiator will get a success message if all selected members are promoted.
     *
     * @param promoteToNetworkAdmin
     * @return
     */
    @Override
    public String promoteAdminsToNetwork(NetworkAdminPromoteDTO promoteToNetworkAdmin) {
        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        String networkId = promoteToNetworkAdmin.getNetworkId();

        // check if network is valid
        Network network = getNetworkDetailsIfActive(networkId);

        // check if initiate is a valid member
        NetworkMember initiator = getNetworkMemberDetailsIfActive(networkId, sessionUser.getUserId());

        // check if initiate is owner - only owner can promote member to admin
        Boolean isOwner = checkIfMemberIsOwner(initiator);

        if (!isOwner) {
            throw new BadRequestException(MessageCodes.ACTION_CANNOT_BE_TAKEN.getValue());
        }

        //check communication type
        Boolean isShared = checkIfCommunicationTypeShared(sessionUser, network);
        if (!isShared) {
            throw new BadRequestException(MessageCodes.INCOMPLETE_PROFILE_INFORMATION.getValue());
        }

        // get valid members
        List<NetworkMember> memberList = networkMemberRepository.findAllActiveNonAdminMembersByNetworkIdAndMemberId(
                networkId, promoteToNetworkAdmin.getMemberIdList());
        List<String> promotedMemberList = new ArrayList<>();

        // set members admin status
        for (NetworkMember networkMember : PeopleUtils.emptyIfNull(memberList)) {
            networkMember.setMemberRole(NetworkMemberRole.ADMIN.getValue());
            promotedMemberList.add(networkMember.getMemberId());
        }
        // increasing the admin count
        network.setAdminCount(network.getAdminCount() + promotedMemberList.size());

        //persisting the modified data
        networkRepository.save(network);
        networkMemberRepository.saveAll(memberList);

        // creating activity and sending notification for promoted members
        promoteToNetworkAdmins(sessionUser, network, promotedMemberList);

        return messages.get(MessageConstant.NETWORK_MEMBERS_PROMOTED);
    }


    @Override
    public String acceptInvitation(AcceptInvitationRequestDTO invitationRequest) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        String requestId = invitationRequest.getActivityId();

        // get request details if valid
        UserActivity pendingRequest = userActivityRepository.getPendingActivityById(requestId);

        if (pendingRequest == null) {
            throw new BadRequestException(MessageCodes.REQUEST_NOT_PENDING.getValue());
        }

        String networkId = pendingRequest.getNetworkId();

        //check if user is already member of the network
        NetworkMember validMember = networkMemberRepository.findByIdAndUserIdAndStatus(networkId, sessionUser.getUserId(),
                NetworkMemberStatus.ACTIVE.getValue());

        if (validMember != null) {
            throw new BadRequestException(MessageCodes.NETWORK_MEMBER_ALREADY_EXIST.getValue());
        }

        //check if network is valid
        Network networkToBeJoined = getNetworkDetailsIfActive(networkId);

        // check communication type
        Boolean isShared = checkIfCommunicationTypeShared(sessionUser, networkToBeJoined);

        if (!isShared) {
            throw new BadRequestException(MessageCodes.INCOMPLETE_PROFILE_INFORMATION.getValue());
        }

        // create network member
        createNetworkMember(networkId, sessionUser.getUserId(), networkToBeJoined.getNetworkCategory());

        setMemberCount(networkToBeJoined, 1);

        //update all network invitation and share sent for user
        userActivityRepository.updateAllNetworkInvitationAndShareActivityForUser(sessionUser.getUserId(),
                networkToBeJoined.getNetworkId());

        // expire all join request activity  created by user for network.
        expireActivities(networkToBeJoined, Collections.singletonList(sessionUser.getUserId()), true);

        return messages.get(MessageConstant.NETWORK_JOIN_SUCCESS);
    }

    @Override
    public NetworkPendingRequestDetailsDTO fetchJoinRequestDetailsForNetwork(String networkId, Integer pageNumber,
                                                                             Integer pageSize) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();

        // check if network is valid
        getNetworkDetailsIfActive(networkId);

        //  check if initiator is a valid network member
        NetworkMember member = getNetworkMemberDetailsIfActive(networkId, sessionUser.getUserId());

        // check if initiator is admin or owner
        Boolean isOwnerOrAdmin = checkIfMemberIsOwnerOrAdmin(member);
        if (!isOwnerOrAdmin) {
            throw new BadRequestException(MessageCodes.ACTION_CANNOT_BE_TAKEN.getValue());
        }

        Pageable pageable = PageRequest.of(pageNumber, pageSize, new Sort(Sort.Direction.DESC, "lastUpdatedOn"));
        // get all the join request for that particular network
        Page<UserActivity> joinRequestList = userActivityRepository.findPendingJoinRequestForNetwork(sessionUser.getUserId(),
                networkId, pageable);

        List<NetworkPendingRequest> pendingRequestDetailsList = new ArrayList<>();
        for (UserActivity userActivity : joinRequestList.getContent()) {
            NetworkPendingRequest networkPendingRequest = new NetworkPendingRequest();
            networkPendingRequest.setPendingRequestDetails(
                    userActivityService.prepareActivityDetails(userActivity,
                            " would like to join your network.", Boolean.FALSE));
            networkPendingRequest.setPublicProfileData(masterService.prepareUserPublicData(
                    peopleUserRepository.findByUserIdAndStatus(userActivity.getActivityById(), UserStatus.ACTIVE)));

            pendingRequestDetailsList.add(networkPendingRequest);
        }

        Link link = ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(NetworkController.class)
                .fetchAllJoinRequest(networkId, pageNumber + 1, pageSize, "")).withSelfRel();

        // populate response
        NetworkPendingRequestDetailsDTO networkPendingRequestDetails = new NetworkPendingRequestDetailsDTO();
        networkPendingRequestDetails.setNetworkId(networkId);
        networkPendingRequestDetails.setRequestDetailsList(pendingRequestDetailsList);
        networkPendingRequestDetails.setTotalNumberOfPages(joinRequestList.getTotalPages());
        networkPendingRequestDetails.setTotalElements(joinRequestList.getTotalElements());

        if (!joinRequestList.isLast()) {
            networkPendingRequestDetails.setNextURL(link.getHref());
        }

        return networkPendingRequestDetails;
    }

    @Override
    public String handleNetworkJoinRequest(NetworkJoinRequestDTO acceptRequest) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        String requestId = acceptRequest.getActivityId();
        String actionMessage;

        // get request details if valid
        UserActivity pendingJoinRequest = userActivityRepository.findJoinNetworkPendingRequestByActivityIdAndStatus(
                requestId, ActivityStatus.PENDING.getValue());

        if (pendingJoinRequest == null) {
            throw new BadRequestException(MessageCodes.REQUEST_NOT_PENDING.getValue());
        }

        String networkId = pendingJoinRequest.getNetworkId();

        //check if network is valid
        Network networkToBeJoined = getNetworkDetailsIfActive(networkId);

        // check if acceptor is the owner or admin of the network for which join request is initiated
        NetworkMember acceptor = getNetworkMemberDetailsIfActive(networkId, sessionUser.getUserId());

        Boolean isOwnerOrAdmin = checkIfMemberIsOwnerOrAdmin(acceptor);
        if (!isOwnerOrAdmin) {
            throw new BadRequestException(MessageCodes.ACTION_CANNOT_BE_TAKEN.getValue());
        }

        //check communication type
        Boolean isShared = checkIfCommunicationTypeShared(sessionUser, networkToBeJoined);
        if (!isShared) {
            throw new BadRequestException(MessageCodes.INCOMPLETE_PROFILE_INFORMATION.getValue());
        }

        ActivityType activityType = pendingJoinRequest.getActivityType();

        if (acceptRequest.isRequestAccepted()) {

            // create network member
            createNetworkMember(networkId, pendingJoinRequest.getActivityById(), networkToBeJoined.getNetworkCategory());

            // set action and over all status on activity
            activityType.setActionTaken(Action.ACCEPTED);

            // set member count
            setMemberCount(networkToBeJoined, 1);

            // create activity for accept join request
            UserActivity requestAccepted = new UserActivity();
            requestAccepted.setActivityById(sessionUser.getUserId());
            requestAccepted.setActivityForId(pendingJoinRequest.getActivityById());
            requestAccepted.setNetworkId(PeopleUtils.convertStringToObjectId(pendingJoinRequest.getNetworkId()));
            requestAccepted.setOverallStatus(ActivityStatus.INFORMATIVE);
            requestAccepted.setActivityType(activityType);
            requestAccepted.setRequestId(pendingJoinRequest.getRequestId());
            requestAccepted.setCreatedOn(new DateTime());
            requestAccepted.setLastUpdatedOn(new DateTime());
            ActivityType accepted = new ActivityType();
            accepted.setRequestType(RequestType.NETWORK_JOIN_REQUEST_ACCEPTED);
            accepted.setActionTaken(Action.ACCEPTED);
            requestAccepted.setActivityType(accepted);

            actionMessage = messages.get(MessageConstant.NETWORK_JOIN_REQUEST_ACCEPT);
            UserActivity userActivity = userActivityRepository.save(requestAccepted);

            // expire all join request activity  created for other admins.
            expireActivities(networkToBeJoined, Arrays.asList(pendingJoinRequest.getActivityById()), true);

            // send notification to the initiator
            queueService.sendPayloadToSQS(prepareSQSPayloadForNetwork(networkToBeJoined, userActivity, sessionUser));

            //update and expire all network invitation and share sent for user
            userActivityRepository.updateAllNetworkInvitationAndShareActivityForUser(pendingJoinRequest.getActivityById(),
                    networkToBeJoined.getNetworkId());

        } else {

            actionMessage = messages.get(MessageConstant.NETWORK_JOIN_REQUEST_REJECT);
            // expire all join request activity  created for other admins.
            expireActivities(networkToBeJoined, Arrays.asList(pendingJoinRequest.getActivityById()), false);

        }

        return actionMessage;
    }

    @Override
    public CreateOrEditNetworkResponseDTO editNetworkDetails(EditNetworkRequestDTO editNetworkRequest) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        String userId = getUserId();
        String networkId = editNetworkRequest.getNetworkId();

        // get network details if valid
        Network networkToBeEdited = getNetworkDetailsIfActive(networkId);

        // get member details of initiate
        NetworkMember initiate = getNetworkMemberDetailsIfActive(networkId, userId);

        // check if he is owner or admin
        Boolean isOwnerOrAdmin = checkIfMemberIsOwnerOrAdmin(initiate);

        if (!isOwnerOrAdmin) {
            throw new BadRequestException(MessageCodes.ACTION_CANNOT_BE_TAKEN.getValue());
        }

        //check communication type
        Boolean isShared = checkIfCommunicationTypeShared(sessionUser, networkToBeEdited);
        if (!isShared) {
            throw new BadRequestException(MessageCodes.INCOMPLETE_PROFILE_INFORMATION.getValue());
        }

        // set all network parameters
        NetworkDetails editNetwork = editNetworkRequest.getNetworkDetails();


        if (!editNetwork.getName().trim().equalsIgnoreCase(networkToBeEdited.getName())) {
            //check to verify if user has created network with same name
            UserNetworkDetails userNetworkDetails = networkMemberRepository.getUserOwnedNetworkByName(
                    sessionUser.getUserId(), editNetwork.getName().trim());

            //if value present for userNetworkDetails then network already exist
            if (userNetworkDetails != null) {
                throw new BadRequestException(MessageCodes.NETWORK_EDIT_NAME_ALREADY_EXIST.getValue());
            }
        }

        networkToBeEdited.setName(editNetwork.getName().trim());
        networkToBeEdited.setImageURL(editNetwork.getImageURL());
        networkToBeEdited.setBannerImageURL(editNetwork.getBannerImageURL());
        networkToBeEdited.setNetworkCategory(editNetwork.getNetworkCategory());
        networkToBeEdited.setNetworkLocation(editNetwork.getNetworkLocation());
        networkToBeEdited.setDescription(editNetwork.getDescription());
        networkToBeEdited.setTagList(editNetwork.getTagList());
        networkToBeEdited.setLastModifiedTime(new DateTime());

        List<String> requesteeUserIdList = new ArrayList<>();

        if (!editNetwork.getPrivacyType().equals(networkToBeEdited.getPrivacyType())) {

            // if public to open -> all request to join should be automatically accepted
            if (editNetwork.getPrivacyType().equalsIgnoreCase(NetworkPrivacyType.DIRECT_JOIN.getValue())) {

                // fetch all request activity and accept it
                List<UserActivity> requestsToBeAccepted = userActivityRepository.findPendingJoinRequestForNetwork(
                        sessionUser.getUserId(), networkId);

                for (UserActivity request : PeopleUtils.emptyIfNull(requestsToBeAccepted)) {
                    // create network member
                    createNetworkMember(networkId, request.getActivityById(), editNetwork.getNetworkCategory());
                    requesteeUserIdList.add(request.getActivityById());

                }
                // expire all join request activity  created for other admins.
                expireActivities(networkToBeEdited, requesteeUserIdList, true);

                // set member count
                setMemberCount(networkToBeEdited, requestsToBeAccepted.size());
                userActivityRepository.updateAllJoinRequestToNetworkByNetworkId(networkId, true);

            } else if (editNetwork.getPrivacyType().equalsIgnoreCase(NetworkPrivacyType.PRIVATE.getValue())) {
                // if public to private -> all request should be declined
                userActivityRepository.updateAllJoinRequestToNetworkByNetworkId(networkId, false);
            }

        }

        networkToBeEdited.setPrivacyType(editNetwork.getPrivacyType());

        // populate response
        CreateOrEditNetworkResponseDTO response = new CreateOrEditNetworkResponseDTO();
        response.setNetworkDetails(prepareNetworkDetails(networkRepository.save(networkToBeEdited)));
        response.setNetworkId(networkId);

        return response;
    }

    @Override
    public void leaveNetwork(LeaveNetworkRequestDTO leaveNetworkRequest) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        String networkId = leaveNetworkRequest.getNetworkId();

        // check if network is valid
        Network network = getNetworkDetailsIfActive(networkId);

        // check if network member is valid
        NetworkMember networkMember = getNetworkMemberDetailsIfActive(networkId, sessionUser.getUserId());

        // check if owner - transfer ownership before leave network
        Boolean isOwner = checkIfMemberIsOwner(networkMember);

        if (isOwner) {
            throw new BadRequestException(MessageCodes.OWNER_CANNOT_LEAVE_NETWORK.getValue());
        }

        // update status
        networkMember.setMemberStatus(NetworkMemberStatus.UNSUBSCRIBED.getValue());
        networkMemberRepository.save(networkMember);

        // delete user from network members
        networkMemberRepository.deleteNetworkMemberByMemberIdsAndNetworkId(Collections.singletonList(sessionUser.getUserId()),
                networkId);

        if (networkMember.getMemberRole().equalsIgnoreCase(NetworkMemberRole.ADMIN.getValue())) {
            int initialCount = network.getAdminCount();
            network.setAdminCount(initialCount - 1);
        }
        // update member count of network
        setMemberCount(network, -1);

        //remove all network activity for user
        userActivityRepository.expireAllNetworkActivityByActivityForIdAndNetworkId(sessionUser.getUserId(), networkId);

    }

    @Override
    public String reportNetwork(ReportNetworkRequestDTO reportNetworkRequest) {

        String userId = getUserId();

        // check if network valid
        getNetworkDetailsIfActive(reportNetworkRequest.getNetworkId());

        ReportedData reportedUserData = reportedUserDataRepository.findByReportedByUserIdAndReportedNetworkId(PeopleUtils.convertStringToObjectId(userId),
                PeopleUtils.convertStringToObjectId(reportNetworkRequest.getNetworkId()));

        if (reportedUserData == null) {

            reportedUserData = new ReportedData();
            reportedUserData.setCreatedOn(PeopleUtils.getCurrentTimeInUTC());
            reportedUserData.setReportedByUserId(userId);
            reportedUserData.setReportedNetworkId(reportNetworkRequest.getNetworkId());
            reportedUserData.setReportMessage(reportNetworkRequest.getReportMessage());
            reportedUserData.setReportDataType(ReportDataType.NETWORK);

            reportedUserDataRepository.save(reportedUserData);
            return messages.get(MessageConstant.NETWORK_REPORT);
        }

        return messages.get(MessageConstant.ALREADY_REPORTED_NETWORK);
    }

    @Override
    public String shareNetwork(ShareNetworkRequestDTO shareNetworkRequest) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();

        String networkId = shareNetworkRequest.getNetworkId();
        // check if network is valid
        Network networkToBeShared = getNetworkDetailsIfActive(networkId);

        // check if member is valid
        getNetworkMemberDetailsIfActive(networkId, sessionUser.getUserId());

        List<NetworkInviteeContact> contactList = shareNetworkRequest.getSharedWithContactList();

        //private network can not be shared
        if (NetworkPrivacyType.PRIVATE.getValue().equalsIgnoreCase(networkToBeShared.getPrivacyType())) {
            throw new BadRequestException(MessageCodes.PRIVATE_NETWORK_SHARE.getValue());
        }

        List<NetworkInviteeContact> validUserContactList = prepareValidContactList(sessionUser.getUserId(), contactList);

        updateInvitationStatusAndMemberStatus(sessionUser, validUserContactList, networkId, true);

        if (PeopleUtils.isNullOrEmpty(validUserContactList)) {
            throw new BadRequestException(MessageCodes.INVALID_OPERATION.getValue());
        }

        // create network share activity for all valid contact/s
        List<UserActivity> userActivityListToBePersisted = new ArrayList<>();
        ActivityType activityType = new ActivityType();
        activityType.setRequestType(RequestType.NETWORK_SHARE);
        activityType.setActionTaken(Action.INITIATED);


        List<String> validContactIdList = new ArrayList<>();
        List<SQSPayload> payloadForNotification = new ArrayList<>();
        int numberOfContactsForWhichNetworkIsShared = 0;
        for (NetworkInviteeContact userContact : PeopleUtils.emptyIfNull(validUserContactList)) {
            validContactIdList.add(userContact.getConnectionId());

            if (userContact.getIsInviteeAPeopleUser()) {
                if (userContact.getIsAlreadyANetworkMember() || userContact.getIsNetworkSharedOrInviteSent()) {
                    continue;
                }
                numberOfContactsForWhichNetworkIsShared++;

                UserActivity shareNetworkActivity = new UserActivity();
                shareNetworkActivity.setActivityById(sessionUser.getUserId());
                shareNetworkActivity.setActivityType(activityType);
                shareNetworkActivity.setRequestId(new ObjectId().toString());
                shareNetworkActivity.setActivityForId(userContact.getInviteeUserId());
                shareNetworkActivity.setNetworkId(PeopleUtils.convertStringToObjectId(networkId));
                shareNetworkActivity.setOverallStatus(ActivityStatus.INFORMATIVE);
                shareNetworkActivity.setCreatedOn(new DateTime());
                shareNetworkActivity.setLastUpdatedOn(new DateTime());

                userActivityListToBePersisted.add(shareNetworkActivity);
            }

        }

        if (numberOfContactsForWhichNetworkIsShared == 0) {
            throw new BadRequestException(MessageCodes.NETWORK_SHARED.getValue());
        }

        // save all user activities created
        userActivityListToBePersisted = userActivityRepository.saveAll(userActivityListToBePersisted);

        //Send notifications
        for (UserActivity shareNetworkActivity : PeopleUtils.emptyIfNull(userActivityListToBePersisted)) {
            payloadForNotification.add(prepareSQSPayloadForNetwork(networkToBeShared, shareNetworkActivity, sessionUser));
        }
        queueService.sendPayloadToSQS(payloadForNotification);

        return messages.get(MessageConstant.NETWORK_SHARE);
    }

    @Override
    public void deleteNetwork(DeleteNetworkRequestDTO deleteNetworkRequest) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        String networkId = deleteNetworkRequest.getNetworkId();

        // check if network valid
        Network networkToBeDeleted = getNetworkDetailsIfActive(networkId);

        // check if member valid
        NetworkMember initiator = getNetworkMemberDetailsIfActive(networkId, sessionUser.getUserId());

        // check if owner
        Boolean isOwner = checkIfMemberIsOwner(initiator);

        if (!isOwner) {
            throw new BadRequestException(MessageCodes.ACTION_CANNOT_BE_TAKEN.getValue());
        }

        //check communication type
        Boolean isShared = checkIfCommunicationTypeShared(sessionUser, networkToBeDeleted);
        if (!isShared) {
            throw new BadRequestException(MessageCodes.INCOMPLETE_PROFILE_INFORMATION.getValue());
        }

        // update all network members status to Deleted state
        networkMemberRepository.updateNetworkMembersStatus(networkId, NetworkMemberStatus.DELETED.getValue());
        userActivityRepository.expireAllActivityOfANetwork(networkId);
        // change network status to deleted
        networkToBeDeleted.setNetworkStatus(NetworkStatus.DELETED.getValue());
        networkRepository.save(networkToBeDeleted);
    }

    @Override
    public String demoteAdmins(DemoteAdminRequestDTO removeAdminRequest) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        String networkId = removeAdminRequest.getNetworkId();
        List<String> adminList = removeAdminRequest.getMemberIdList();

        // check if network is valid
        Network network = getNetworkDetailsIfActive(networkId);

        // check if member is valid
        NetworkMember initiate = getNetworkMemberDetailsIfActive(networkId, sessionUser.getUserId());

        // check if owner
        Boolean isOwner = checkIfMemberIsOwner(initiate);
        if (!isOwner) {
            throw new BadRequestException(MessageCodes.ACTION_CANNOT_BE_TAKEN.getValue());
        }

        //check communication type
        Boolean isShared = checkIfCommunicationTypeShared(sessionUser, network);
        if (!isShared) {
            throw new BadRequestException(MessageCodes.INCOMPLETE_PROFILE_INFORMATION.getValue());
        }

        if (adminList.contains(sessionUser.getUserId())) {
            throw new BadRequestException((MessageCodes.OWNER_CANNOT_BE_DEMOTED.getValue()));
        }

        // get all valid members of network
        UpdateResult validMemberList = networkMemberRepository.updateMemberRolesForANetwork(networkId, adminList,
                NetworkMemberRole.ADMIN.getValue(), NetworkMemberRole.MEMBER.getValue());

        if (validMemberList.getModifiedCount() > 0) {
            // decrementing the admin count
            network.setAdminCount((int) (network.getAdminCount() - validMemberList.getModifiedCount()));
            networkRepository.save(network);

        }

        return messages.get((MessageConstant.NETWORK_ADMIN_DEMOTE));
    }

    @Override
    public void transferOwnership(TransferOwnerShipRequestDTO transferOwnerShipRequest) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        String userId = sessionUser.getUserId();
        String networkId = transferOwnerShipRequest.getNetworkId();
        String memberId = transferOwnerShipRequest.getMemberId();
        List<NetworkMember> updatedNetworkMemberList = new ArrayList<>();

        // check if network valid
        Network network = getNetworkDetailsIfActive(networkId);

        // check if initiate is valid member
        NetworkMember initiator = getNetworkMemberDetailsIfActive(networkId, userId);

        // check if initiate is owner
        Boolean isOwner = checkIfMemberIsOwner(initiator);
        if (!isOwner) {
            throw new BadRequestException(MessageCodes.ACTION_CANNOT_BE_TAKEN.getValue());
        }

        //check communication type
        Boolean isShared = checkIfCommunicationTypeShared(sessionUser, network);
        if (!isShared) {
            throw new BadRequestException(MessageCodes.INCOMPLETE_PROFILE_INFORMATION.getValue());
        }


        // check if initiator is not same as initiate
        if (userId.equals(memberId)) {
            throw new BadRequestException(MessageCodes.NEW_OWNER_SAME_AS_PREVIOUS.getValue());
        }

        // check if other member is a valid member and admin of the network
        NetworkMember toBecomeOwner = getNetworkMemberDetailsIfActive(networkId, memberId);
        if (toBecomeOwner == null || toBecomeOwner.getMemberRole().equals(NetworkMemberRole.MEMBER.getValue())) {
            throw new BadRequestException(MessageCodes.INVALID_NETWORK_MEMBER.getValue());
        }

        // After transferring ownership initiator becomes Admin
        initiator.setMemberRole(NetworkMemberRole.ADMIN.getValue());
        updatedNetworkMemberList.add(initiator);

        // change status of other member
        toBecomeOwner.setMemberRole(NetworkMemberRole.OWNER.getValue());
        updatedNetworkMemberList.add(toBecomeOwner);

        // creating activity
        UserActivity ownershipTransferActivity = new UserActivity();
        ownershipTransferActivity.setActivityById(sessionUser.getUserId());
        ownershipTransferActivity.setActivityForId(memberId);

        ActivityType activityType = new ActivityType();
        activityType.setRequestType(RequestType.NETWORK_OWNERSHIP_TRANSFER);
        activityType.setActionTaken(Action.TRANSFERRED);
        ownershipTransferActivity.setActivityType(activityType);
        ownershipTransferActivity.setOverallStatus(ActivityStatus.ACTIVE);
        ownershipTransferActivity.setNetworkId(PeopleUtils.convertStringToObjectId(networkId));
        ownershipTransferActivity.setCreatedOn(new DateTime());
        ownershipTransferActivity.setLastUpdatedOn(new DateTime());
        userActivityRepository.save(ownershipTransferActivity);

        // sending notification
        SQSPayload sqsPayload = prepareSQSPayloadForNetwork(network, ownershipTransferActivity, sessionUser);
        queueService.sendPayloadToSQS(new ArrayList<>(Arrays.asList(sqsPayload)));

        // save
        networkMemberRepository.saveAll(updatedNetworkMemberList);

    }

    @Override
    public RecommendedNetworksResponseDTO getRecommendedNetworks(String networkCategory) {
        PeopleUser sessionUser = tokenAuthService.getSessionUser();

        // check category name is valid or not
        checkIfNetworkCatogoryValid(networkCategory);

        RecommendedNetworksResponseDTO responseDTO = new RecommendedNetworksResponseDTO();

        // Populating most popular networks
        List<Network> popularNetworkList =
                networkRepository.getMostPopularNetworksForUserByCategory(sessionUser.getUserId(),
                        networkCategory, recommendedNetworkLimit);

        List<UserNetworkDetails> popularNetworkDetails = new ArrayList<>();
        for (Network network : popularNetworkList) {
            UserNetworkDetails userNetworkDetails = new UserNetworkDetails();
            userNetworkDetails.setNetworkDetails(prepareNetworkDetails(network));
            userNetworkDetails.setNetworkId(network.getNetworkId());
            popularNetworkDetails.add(userNetworkDetails);
        }
        responseDTO.setMostPopular(popularNetworkDetails);

        // Populating suggested networks
        List<RecentActiveNetwork> recentActiveNetworks = recentActiveNetworkRepository.getRecentNetworksByCategory(
                sessionUser.getUserId(), networkCategory, newMemberWeightage, newNetworkWeightage);
        List<UserNetworkDetails> suggestedNetworkDetails =
                getSuggestedNetworkDetailsList(sessionUser.getUserId(), recentActiveNetworks);
        if (!suggestedNetworkDetails.isEmpty()) {
            responseDTO.setSuggestion(suggestedNetworkDetails);
        }

        // fetching local networks
        if (sessionUser.getDeviceLocation() != null) {
            double latitude = sessionUser.getDeviceLocation().getLatitude();
            double longitude = sessionUser.getDeviceLocation().getLongitude();
            List<Network> localNetworks =
                    networkRepository.getLocalNetworksForUserByCategory(sessionUser.getUserId(), networkCategory,
                            latitude, longitude, localNetworkRadiusInMiles, recommendedNetworkLimit);

            List<UserNetworkDetails> localNetworkDetails = new ArrayList<>();
            for (Network network : localNetworks) {
                UserNetworkDetails userNetworkDetails = new UserNetworkDetails();
                userNetworkDetails.setNetworkDetails(prepareNetworkDetails(network));
                userNetworkDetails.setNetworkId(network.getNetworkId());
                localNetworkDetails.add(userNetworkDetails);
            }

            responseDTO.setLocal(localNetworkDetails);
        }
        return responseDTO;
    }

    @Override
    public Set<UserNetworkDetails> getTopRecommendedNetworks() {
        PeopleUser sessionUser = tokenAuthService.getSessionUser();

        Set<UserNetworkDetails> topNetworks = new HashSet<>();

        // fetching top local networks
        if (sessionUser.getDeviceLocation() != null) {
            double latitude = sessionUser.getDeviceLocation().getLatitude();
            double longitude = sessionUser.getDeviceLocation().getLongitude();
            List<Network> topLocalNetworks = networkRepository
                    .getLocalNetworksForUser(sessionUser.getUserId(), latitude, longitude, localNetworkRadiusInMiles,
                            20);

            for (Network network : topLocalNetworks) {
                if (topNetworks.size() >= (toplocalLimit)) {
                    break;
                }
                UserNetworkDetails userNetworkDetails = new UserNetworkDetails();
                userNetworkDetails.setNetworkDetails(prepareNetworkDetails(network));
                userNetworkDetails.setNetworkId(network.getNetworkId());
                topNetworks.add(userNetworkDetails);
            }
        }

        // Populating most popular networks
        List<Network> popularNetworkList =
                networkRepository.getTopMostPopularNetworksForUser(sessionUser.getUserId(), recommendedNetworkLimit);

        List<UserNetworkDetails> topPopularNetworks = new ArrayList<>();
        for (Network network : popularNetworkList) {
            UserNetworkDetails userNetworkDetails = new UserNetworkDetails();
            userNetworkDetails.setNetworkDetails(prepareNetworkDetails(network));
            userNetworkDetails.setNetworkId(network.getNetworkId());
            topPopularNetworks.add(userNetworkDetails);
        }

        // Populating suggested networks
        List<RecentActiveNetwork> recentActiveNetworks = recentActiveNetworkRepository.getTopRecentNetworks(
                sessionUser.getUserId(), newMemberWeightage, newNetworkWeightage);
        List<UserNetworkDetails> topSuggestedNetworks =
                getSuggestedNetworkDetailsList(sessionUser.getUserId(), recentActiveNetworks);

        int maxLimitForSuggestedNetworks = topSuggestedNetworks.size() < topSuggestedLimit ?
                topSuggestedNetworks.size() : topSuggestedLimit;
        topNetworks.addAll(topSuggestedNetworks.subList(0, maxLimitForSuggestedNetworks));

        for (UserNetworkDetails popularNetwork : topPopularNetworks) {
            if (topNetworks.size() >= (topPopularLimit + topSuggestedLimit + toplocalLimit)) {
                break;
            }
            topNetworks.add(popularNetwork);
        }
        return topNetworks;
    }

    private String getUserId() {
        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        return sessionUser.getUserId();
    }

    private JoinNetworkResponseDTO joinPublicNetwork(Network network, PeopleUser sessionUser) {

        // check if network is private
        if (NetworkPrivacyType.PRIVATE.getValue().equalsIgnoreCase(network.getPrivacyType())) {
            throw new BadRequestException(MessageCodes.NETWORK_JOIN_REQUEST_CANNOT_BE_SENT_TO_PRIVATE_NETWORK.getValue());
        }

        JoinNetworkResponseDTO response = new JoinNetworkResponseDTO();

        if (NetworkPrivacyType.DIRECT_JOIN.getValue().equalsIgnoreCase(network.getPrivacyType())) {

            createNetworkMember(network.getNetworkId(), sessionUser.getUserId(), network.getNetworkCategory());
            // increase member count
            setMemberCount(network, 1);

            // preparing response
            response.setResponseMessage(messages.get(MessageConstant.NETWORK_JOIN_SUCCESS));
            response.setIsRequestSent(false);
            return response;

        } else if (NetworkPrivacyType.JOIN_BY_REQUEST.getValue().equalsIgnoreCase(network.getPrivacyType())) {

            List<NetworkMember> ownerAndAdminsList = networkMemberRepository.findByIdAndRole(network.getNetworkId(),
                    Arrays.asList(NetworkMemberRole.OWNER.getValue(), NetworkMemberRole.ADMIN.getValue()));
            // activity and notification to owner and admins
            createNetworkJoinRequest(network, sessionUser, ownerAndAdminsList);

            // preparing response
            response.setResponseMessage(messages.get(MessageConstant.NETWORK_JOIN_REQUEST));
            response.setIsRequestSent(true);
            return response;

        } else {
            throw new BadRequestException(MessageCodes.INVALID_OPERATION.getValue());
        }
    }

    private void createNetworkMember(String networkId, String userId, String networkCategory) {

        NetworkMember networkMember = new NetworkMember();
        networkMember.setMemberRole(NetworkMemberRole.MEMBER.getValue());
        networkMember.setNetworkId(networkId);
        networkMember.setMemberId(userId);

        networkMemberRepository.save(networkMember);

        // Create Entry in Recent Active Network
        createRecentActiveNetwork(networkId, networkCategory, Boolean.FALSE,
                Boolean.TRUE);
    }

    private void createNetworkJoinRequest(Network network, PeopleUser sessionUser, List<NetworkMember> ownerAndAdminsList) {

        List<UserActivity> joinRequestActivityForNetworkAdmins = new ArrayList<>();
        List<SQSPayload> sqsPayloadList = new ArrayList<>();

        // activity type
        ActivityType activityType = new ActivityType();
        activityType.setActionTaken(Action.INITIATED);
        activityType.setRequestType(RequestType.NETWORK_JOIN_REQUEST);

        for (NetworkMember networkMember : ownerAndAdminsList) {
            UserActivity userActivity = new UserActivity();
            userActivity.setActivityById(sessionUser.getUserId());
            userActivity.setActivityForId(networkMember.getMemberId());
            userActivity.setOverallStatus(ActivityStatus.PENDING);
            userActivity.setActivityType(activityType);
            userActivity.setNetworkId(PeopleUtils.convertStringToObjectId(network.getNetworkId()));
            userActivity.setCreatedOn(new DateTime());
            userActivity.setLastUpdatedOn(new DateTime());
            joinRequestActivityForNetworkAdmins.add(userActivity);
        }
        userActivityService.createMultipleRequest(joinRequestActivityForNetworkAdmins);

        // creating sqsPayload
        for (UserActivity activity : joinRequestActivityForNetworkAdmins) {
            sqsPayloadList.add(prepareSQSPayloadForNetwork(network, activity, sessionUser));
        }
        queueService.sendPayloadToSQS(sqsPayloadList);

    }

    private List<UserActivity> promoteToNetworkAdmins(PeopleUser sessionUser, Network network,
                                                      List<String> memberIdList) {

        List<UserActivity> listOfMembersToPromote = new ArrayList<>();
        List<UserActivity> createdActivityList = new ArrayList<>();
        List<SQSPayload> sqsPayloadList = new ArrayList<>();

        ActivityType activityType = new ActivityType();
        activityType.setRequestType(RequestType.NETWORK_ADMIN_PROMOTION);
        activityType.setActionTaken(Action.PROMOTED);

        for (String inviteeId : PeopleUtils.emptyIfNull(memberIdList)) {

            UserActivity promoteMember = new UserActivity();
            promoteMember.setActivityById(sessionUser.getUserId());
            promoteMember.setActivityForId(inviteeId);
            promoteMember.setActivityType(activityType);
            promoteMember.setOverallStatus(ActivityStatus.ACTIVE);
            promoteMember.setNetworkId(PeopleUtils.convertStringToObjectId(network.getNetworkId()));
            promoteMember.setCreatedOn(new DateTime());
            promoteMember.setLastUpdatedOn(new DateTime());

            listOfMembersToPromote.add(promoteMember);
        }

        if (!PeopleUtils.isNullOrEmpty(listOfMembersToPromote)) {
            createdActivityList = userActivityService.createMultipleRequest(listOfMembersToPromote);

            for (UserActivity activity : createdActivityList) {
                sqsPayloadList.add(prepareSQSPayloadForNetwork(network, activity, sessionUser));
            }
        }

        //send notification
        queueService.sendPayloadToSQS(sqsPayloadList);

        return createdActivityList;
    }

    private SQSPayload prepareSQSPayloadForNetwork(Network network, UserActivity userActivity,
                                                   PeopleUser sessionUser) {
        if (!masterService.isPushNotificationEnabledForUser(userActivity.getActivityForId())) {
            return null;
        }
        PushNotificationDTO pushNotificationDTO = new PushNotificationDTO();
        pushNotificationDTO.setActivityId(userActivity.getActivityId());
        pushNotificationDTO.setActivityRequestType(userActivity.getActivityType().getRequestType());
        pushNotificationDTO.setNetworkId(network.getNetworkId());
        pushNotificationDTO.setIsNetworkNotification(true);
        pushNotificationDTO.setInitiatorName(PeopleUtils.getDefaultOrEmpty(sessionUser.getFullName()));
        pushNotificationDTO.setReceiverUserId(userActivity.getActivityForId());
        pushNotificationDTO.setNetworkName(network.getName());

        if (!PeopleUtils.isNullOrEmpty(userActivity.getMessage())) {
            pushNotificationDTO.setActivityMessage(userActivity.getMessage());
        }

        return notificationService.prepareSQSPayloadForNotification(userActivity.getActivityForId(),
                pushNotificationDTO);
    }

    /*
     *
     * Get network if network is active, else throw exception
     */
    private Network getNetworkDetailsIfActive(String networkId) {

        return Optional.ofNullable(networkRepository.findNetworkByIdAndStatus(networkId, NetworkStatus.ACTIVE.getValue()))
                .orElseThrow(() -> new BadRequestException(MessageCodes.INVALID_NETWORK.getValue()));
    }

    /*
     *
     * Get Network Member details if network member is a valid member and active
     *
     */
    private NetworkMember getNetworkMemberDetailsIfActive(String networkId, String memberId) {

        return Optional.ofNullable(networkMemberRepository.findByIdAndUserIdAndStatus(networkId, memberId,
                NetworkMemberStatus.ACTIVE.getValue())).orElseThrow(() -> new BadRequestException(
                        MessageCodes.INVALID_NETWORK_MEMBER.getValue()));
    }

    /*
     *
     * Check if network member is an owner of network
     *
     */
    private Boolean checkIfMemberIsOwner(NetworkMember networkMember) {

        Boolean isOwner = Boolean.FALSE;
        if (networkMember.getMemberRole().equals(NetworkMemberRole.OWNER.getValue())) {
            isOwner = Boolean.TRUE;
        }
        return isOwner;
    }

    /*
     *
     * Check if network member is owner or admin of network
     *
     */
    private Boolean checkIfMemberIsOwnerOrAdmin(NetworkMember networkMember) {

        Boolean isOwnerOrAdmin = Boolean.FALSE;
        if ((networkMember.getMemberRole().equals(NetworkMemberRole.OWNER.getValue())) ||
                (networkMember.getMemberRole().equals(NetworkMemberRole.ADMIN.getValue()))) {
            isOwnerOrAdmin = Boolean.TRUE;
        }
        return isOwnerOrAdmin;
    }

    private NetworkCategory checkIfNetworkCatogoryValid(String networkCategory) {

        return Optional.ofNullable(networkCategoryRepository.findByName(networkCategory))
                .orElseThrow(() -> new BadRequestException(MessageCodes.INVALID_NETWORK_CATEGORY.getValue()));
    }

    /*
     *
     * check if communication type required by network is shared by user
     *
     */
    private Boolean checkIfCommunicationTypeShared(PeopleUser peopleUser, Network network) {

        NetworkPrimaryContactMethod communicationType = network.getPrimaryContactMethod();
        List<String> networkPermissionList = peopleUser.getNetworkSharedValueList();
        Boolean isShared = Boolean.FALSE;

        Map<String, UserProfileData> metadataMap = peopleUser.getMetadataMap();
        for (String value : PeopleUtils.emptyIfNull(networkPermissionList)) {
            if (metadataMap.containsKey(value)) {
                String category = metadataMap.get(value).getCategory();
                switch (category) {
                    case PHONE_NUMBER:
                    case EMAIL:
                        if (category.equalsIgnoreCase(communicationType.getContactCategory())) {
                            isShared = Boolean.TRUE;
                        }
                        break;
                    case SOCIAL_PROFILE:
                        if (metadataMap.get(value).getLabel().equalsIgnoreCase(communicationType.getContactLabel())) {
                            isShared = Boolean.TRUE;
                        }
                        break;
                    default:
                        break;
                }

            }
        }
        return isShared;
    }

    private void setMemberCount(Network network, int increasedBy) {

        int initialCount = network.getMemberCount();
        network.setMemberCount(initialCount + increasedBy);

        networkRepository.save(network);
    }

    private void createRecentActiveNetwork(String networkId, String networkCategory, boolean isnewNetwork,
                                           boolean isNewMemberAdded) {
        RecentActiveNetwork recentActiveNetwork = new RecentActiveNetwork();
        recentActiveNetwork.setNetworkCategory(networkCategory);
        recentActiveNetwork.setNetworkId(networkId);
        recentActiveNetwork.setNewNetwork(isnewNetwork);
        recentActiveNetwork.setNewMember(isNewMemberAdded);
        recentActiveNetwork.setCreatedOn(PeopleUtils.getCurrentTimeInUTC());

        recentActiveNetworkRepository.save(recentActiveNetwork);
    }


    private UserContactData getConnectionObject(PeopleUser sessionUser, NetworkMember networkMember,
                                                NetworkPrimaryContactMethod contactMethod) {

        UserContactData memberContact = new UserContactData();
        UserConnection userConnection = userConnectionRepository.findConnectionByFromIdAndToId(
                sessionUser.getUserId(), networkMember.getMemberId());

        if (userConnection != null) {
            //if UserConnection exist for session user and owner

            UserConnection userContact = userConnectionRepository.getSharedProfileDataForSelectedContact
                    (Arrays.asList(userConnection.getConnectionId())).get(0);

            memberContact = userConnectionService.prepareContactStaticData(sessionUser, userContact);
            UserInformationDTO contactStaticData = memberContact.getStaticProfileData();

            Set<String> registeredNumbersList = masterService.getRegisteredNumberList();
            if (contactStaticData != null && (!registeredNumbersList.isEmpty())) {
                userConnectionService.populateStaticDataWithIsVerifiedInfo(contactStaticData, registeredNumbersList);
            }

            switch (userContact.getConnectionStatus()) {

                case CONNECTED:
                    memberContact = userConnectionService.prepareContactSharedData(
                            sessionUser, userContact);
                    break;
                case NOT_CONNECTED:
                case PENDING:
                default:
                    break;
            }
        } else {
            //if UserConnection do not exist for session user and member
            memberContact.setPublicProfileData(masterService.prepareUserPublicData(
                    peopleUserRepository.findByUserIdAndStatus(networkMember.getMemberId(), UserStatus.ACTIVE)));
            memberContact.setConnectionStatus(ConnectionStatus.NOT_CONNECTED.getValue());
            memberContact.setToUserId(networkMember.getMemberId());
        }

        // adding network shared information of member
        memberContact.setNetworkSharedInformationData(getNetworkSharedData(networkMember, contactMethod));

        return memberContact;
    }

    private NetworkMembersResponseDTO getNetworkMembersResponseDTO(PeopleUser sessionUser, Link link,
                                                                   Page<NetworkMember> listOfNetworkMembers,
                                                                   NetworkPrimaryContactMethod contactMethod) {
        List<NetworkMember> membersList = listOfNetworkMembers.getContent();
        List<UserContactData> membersConnectionObjectList = new ArrayList<>();

        NetworkMembersResponseDTO getNetworkMembersResponseDTO = new NetworkMembersResponseDTO();
        getNetworkMembersResponseDTO.setTotalNumberOfPages(listOfNetworkMembers.getTotalPages());
        getNetworkMembersResponseDTO.setTotalElements(listOfNetworkMembers.getTotalElements());
        if (!listOfNetworkMembers.isLast()) {
            getNetworkMembersResponseDTO.setNextURL(link.getHref());
        }

        Map<String, UserContactData> notConnectedMembers = new HashMap<>();
        /**
         * creating connection object for each network member with respect to session user
         * and adding not connected network members contact data to a map to process connection status.
         */
        for (NetworkMember networkMember : membersList) {
            UserContactData memberContact;
            if (networkMember.getMemberId().equals(sessionUser.getUserId())) {
                memberContact = new UserContactData();
                memberContact.setToUserId(sessionUser.getUserId());
                memberContact.setPublicProfileData(masterService.prepareUserPublicData(sessionUser));
                memberContact.setNetworkSharedInformationData(getNetworkSharedData(networkMember, contactMethod));
                membersConnectionObjectList.add(memberContact);
            } else {
                memberContact = getConnectionObject(sessionUser, networkMember, contactMethod);
                membersConnectionObjectList.add(memberContact);

                if (!memberContact.getConnectionStatus().equalsIgnoreCase(ConnectionStatus.CONNECTED.getValue())) {
                    notConnectedMembers.put(memberContact.getToUserId(), memberContact);
                }
            }
        }

        List<PeopleUser> membersUserData = peopleUserRepository.findByUserIdsAndStatus(
                new ArrayList<>(notConnectedMembers.keySet()), UserStatus.ACTIVE);

        for (PeopleUser user : PeopleUtils.emptyIfNull(membersUserData)) {
            // Check if there is a pending "CONNECTION_REQUEST"
            UserActivity pendingConnectionRequestActivity = userActivityRepository.
                    getPendingConnectionRequestActivity(sessionUser.getUserId(), user.getVerifiedContactNumber());

            /* if the connection request was sent update connection status to PENDING */
            if (pendingConnectionRequestActivity != null) {
                notConnectedMembers.get(user.getUserId()).setConnectionStatus(ConnectionStatus.PENDING.getValue());
            }
        }

        getNetworkMembersResponseDTO.setNetworkMembersList(membersConnectionObjectList);

        return getNetworkMembersResponseDTO;
    }


    private NetworkSharedInformationDTO getNetworkSharedData(NetworkMember networkMember,
                                                             NetworkPrimaryContactMethod networkCommunicationType) {
        // add networkShareData of the network member
        PeopleUser member = peopleUserService.findUserByUserId(networkMember.getMemberId());
        List<String> networkSharedValueList = member.getNetworkSharedValueList();
        Map<String, UserProfileData> metadataMap = member.getMetadataMap();

        NetworkSharedInformationDTO networkSharedData = new NetworkSharedInformationDTO();
        List<UserProfileData> userMetadataList = new ArrayList<>();

        userMetadataList.add(getNetworkSharedValue(networkSharedValueList, metadataMap, networkCommunicationType));

        networkSharedData.setUserNetworkSharedMetadataList(userMetadataList);
        networkSharedData.setMemberRole(networkMember.getMemberRole());
        return networkSharedData;
    }

    private UserProfileData getNetworkSharedValue(List<String> networkSharedValueList, Map<String, UserProfileData> metadataMap,
                                                  NetworkPrimaryContactMethod networkCommunicationType) {

        for (String value : PeopleUtils.emptyIfNull(networkSharedValueList)) {
            if (!metadataMap.containsKey(value))
                continue;
            UserProfileData membersProfileData = metadataMap.get(value);
            membersProfileData.setVerification(UserInformationVerification.VERIFIED);
            String category = membersProfileData.getCategory();

            switch (category) {
                case PHONE_NUMBER:
                case EMAIL:
                    if (category.equalsIgnoreCase(networkCommunicationType.getContactCategory())) {
                        return membersProfileData;
                    }
                    break;
                case SOCIAL_PROFILE:
                    if (membersProfileData.getLabel().equalsIgnoreCase(networkCommunicationType.getContactLabel())) {
                        return membersProfileData;
                    }
                    break;
                default:
                    break;
            }
        }
        return null;
    }

    private NetworkDetails prepareNetworkDetails(Network network) {

        NetworkDetails networkDetails = new NetworkDetails();
        networkDetails.setName(network.getName());
        networkDetails.setImageURL(network.getImageURL());
        networkDetails.setBannerImageURL(network.getBannerImageURL());
        networkDetails.setNetworkLocation(network.getNetworkLocation());
        networkDetails.setTagList(network.getTagList());
        networkDetails.setPrimaryContactMethod(network.getPrimaryContactMethod());
        networkDetails.setPrivacyType(network.getPrivacyType());
        networkDetails.setMemberCount(network.getMemberCount());
        networkDetails.setAdminCount(network.getAdminCount());
        networkDetails.setDescription(network.getDescription());
        networkDetails.setNetworkCategory(network.getNetworkCategory());
        networkDetails.setLastModifiedTime(network.getLastModifiedTime());

        return networkDetails;
    }

    /**
     * @param sortByRole
     * @param fNameOrder
     * @param lNameOrder
     * @param lNamePreferred
     * @return
     */
    private Sort getSortForNetworkMembers(int sortByRole, int fNameOrder, int lNameOrder, boolean lNamePreferred) {
        Set<SortElement> set = new TreeSet<>();

        // assigning priority and adding SortElement to set, if sortByRole has to be considered for sorting
        if (sortByRole == SortingOrder.DESCENDING_ORDER.getValue()
                || sortByRole == SortingOrder.ASCENDING_ORDER.getValue()) {
            set.add(new SortElement(MEMBER_ROLE, sortByRole, 1));
        }

        // assigning priority and adding SortElement to set
        if (!set.isEmpty()) {
            if (lNamePreferred) {
                set.add(new SortElement(NETWORK_MEMBER_LAST_NAME, lNameOrder, 2));
                set.add(new SortElement(NETWORK_MEMBER_FIRST_NAME, fNameOrder, 3));
            } else {
                set.add(new SortElement(NETWORK_MEMBER_FIRST_NAME, fNameOrder, 2));
                set.add(new SortElement(NETWORK_MEMBER_LAST_NAME, lNameOrder, 3));
            }
        } else {
            if (lNamePreferred) {
                set.add(new SortElement(NETWORK_MEMBER_LAST_NAME, lNameOrder, 1));
                set.add(new SortElement(NETWORK_MEMBER_FIRST_NAME, fNameOrder, 2));
            } else {
                set.add(new SortElement(NETWORK_MEMBER_FIRST_NAME, fNameOrder, 1));
                set.add(new SortElement(NETWORK_MEMBER_LAST_NAME, lNameOrder, 2));
            }
        }

        return PeopleUtils.getSort(set);
    }

    private void updateInvitationStatusAndMemberStatus(PeopleUser sessionUser, List<NetworkInviteeContact> userContactList,
                                                       String networkId, boolean isSharedOperation) {

        for (NetworkInviteeContact userContact : PeopleUtils.emptyIfNull(userContactList)) {
            if (userContact.getIsInviteeAPeopleUser()) {
                // update member status and if shared or invited status
                updateNetworkMemberStatusForInviteeOrShare(networkId, userContact.getInviteeUserId(), userContact);
                updateSharedOrInvitedStatus(sessionUser, userContact.getInviteeUserId(), networkId, userContact,
                        isSharedOperation);
            }
        }

    }

    private void updateNetworkMemberStatusForInviteeOrShare(String networkId, String otherUsersId,
                                                            NetworkInviteeContact userContact) {
        NetworkMember networkMember = networkMemberRepository.findByIdAndUserIdAndStatus(networkId,
                otherUsersId, NetworkMemberStatus.ACTIVE.getValue());
        if (networkMember != null) {
            userContact.setIsAlreadyANetworkMember(Boolean.TRUE);
        }
    }

    private void updateSharedOrInvitedStatus(PeopleUser sessionUser, String otherUsersId, String networkId,
                                             NetworkInviteeContact userContact,
                                             boolean isSharedOperation) {
        List<UserActivity> sharedOrInvited;
        if (isSharedOperation) {
            // remove contacts for which network was already shared
            sharedOrInvited = userActivityRepository.findPendingActivityByActivityByIdAndForIdAndRequestType(
                    sessionUser.getUserId(), otherUsersId, networkId, RequestType.NETWORK_SHARE);

        } else {
            // remove contacts for which invitation is already
            sharedOrInvited = userActivityRepository.findPendingActivityByActivityByIdAndForIdAndRequestType(
                    sessionUser.getUserId(), otherUsersId, networkId, RequestType.NETWORK_MEMBER_INVITE);
        }

        if (!PeopleUtils.isNullOrEmpty(sharedOrInvited)) {
            userContact.setIsNetworkSharedOrInviteSent(Boolean.TRUE);
        }
    }

    private List<NetworkInviteeContact> prepareValidContactList(String initiatorId, List<NetworkInviteeContact> inviteeList) {

        // get all valid contact
        Map<String, NetworkInviteeContact> connectionIdToContactMap = new HashMap<>();
        List<NetworkInviteeContact> validUserContactList = new ArrayList<>();

        // mapping each connection Id to its respective contact details
        for (NetworkInviteeContact userContact : PeopleUtils.emptyIfNull(inviteeList)) {
            connectionIdToContactMap.put(userContact.getConnectionId(), userContact);
        }

        // fetching only valid connections from the provided invitee list
        List<UserConnection> validContactList = userConnectionRepository.findContactByConnectionId(initiatorId,
                new ArrayList<>(connectionIdToContactMap.keySet()));

        for (UserConnection contactConnection : PeopleUtils.emptyIfNull(validContactList)) {
            // if already a connection then populate NetworkInviteeContact with userId and peopleUser status
            if (contactConnection.getConnectionStatus().getValue().equalsIgnoreCase(ConnectionStatus.CONNECTED.getValue())) {
                if (connectionIdToContactMap.get(contactConnection.getConnectionId()) != null) {
                    NetworkInviteeContact inviteeConnectedContact = connectionIdToContactMap.get(
                            contactConnection.getConnectionId());


                    inviteeConnectedContact.setIsAlreadyANetworkMember(Boolean.FALSE);
                    inviteeConnectedContact.setIsNetworkSharedOrInviteSent(Boolean.FALSE);
                    inviteeConnectedContact.setInviteeUserId(contactConnection.getConnectionToId());
                    inviteeConnectedContact.setIsInviteeAPeopleUser(Boolean.TRUE);

                    validUserContactList.add(inviteeConnectedContact);

                }
            } else if (connectionIdToContactMap.get(contactConnection.getConnectionId()) != null) {
                NetworkInviteeContact inviteeContact = connectionIdToContactMap.get(contactConnection.getConnectionId());
                inviteeContact.setIsAlreadyANetworkMember(Boolean.FALSE);
                inviteeContact.setIsNetworkSharedOrInviteSent(Boolean.FALSE);

                List<UserProfileData> userProfileData = contactConnection.getContactStaticData().getUserMetadataList();

                boolean isInviteeContactUpdated = masterService.updateContactForStaticContactWithVerifiedNumber(userProfileData, inviteeContact);

                updateInviteeContactIfNotConnected(inviteeContact, isInviteeContactUpdated);

                if (inviteeContact.getContactNumber() == null) {
                    continue;
                }

                validUserContactList.add(inviteeContact);
            }
        }

        return validUserContactList;
    }

    private void updateInviteeContactIfNotConnected(NetworkInviteeContact inviteeContact, boolean isInviteeContactUpdated) {
        if (isInviteeContactUpdated) {
            inviteeContact.setIsInviteeAPeopleUser(Boolean.TRUE);
        } else {
            inviteeContact.setIsInviteeAPeopleUser(Boolean.FALSE);
        }
    }

    private void expireActivities(Network network, List<String> initiatorUserId, boolean requestAccepted) {
        userActivityRepository.updateAllJoinRequestWithActivityByIdsAndNetworkId(
                initiatorUserId, network.getNetworkId(), requestAccepted);
    }

    private List<UserNetworkDetails> getSuggestedNetworkDetailsList(String userId,
                                                                    List<RecentActiveNetwork> recentActiveNetworks) {
        List<String> suggestedNetworkIds = new ArrayList<>();
        for (RecentActiveNetwork recentActiveNetwork : recentActiveNetworks) {
            suggestedNetworkIds.add(recentActiveNetwork.getId().toString());
        }

        List<UserNetworkDetails> suggestedNetworkDetails = new ArrayList<>();
        List<Network> suggestedNetworkList = networkRepository.findActiveNetworksByIdsNewToUser(userId,
                suggestedNetworkIds, recommendedNetworkLimit);
        for (Network network : suggestedNetworkList) {
            UserNetworkDetails userNetworkDetails = new UserNetworkDetails();
            userNetworkDetails.setNetworkDetails(prepareNetworkDetails(network));
            userNetworkDetails.setNetworkId(network.getNetworkId());
            suggestedNetworkDetails.add(userNetworkDetails);
        }
        return suggestedNetworkDetails;
    }

    private void checkUpdatedSharedValueIdList(PeopleUser sessionUser, List<UserActivity> networkJoinRequests,
                                               List<String> newNetworkSharedValueIdList, List<String> userNetworkIds) {
        // Prepare existing shared categories for networks
        List<String> existingNetworkSharedCategories =
                getListOfSharedCategoriesForNetwork(sessionUser, sessionUser.getNetworkSharedValueList());

        // Prepare new shared categories for networks
        List<String> newNetworkSharedCategories =
                getListOfSharedCategoriesForNetwork(sessionUser, newNetworkSharedValueIdList);

        for(UserActivity networkJoinRequestSent : PeopleUtils.emptyIfNull(networkJoinRequests)){
            userNetworkIds.add(networkJoinRequestSent.getNetworkId());
        }

        // Compare both shared network category list
        // And prepare a new list for missing category(s)
        List<String> missingCategoryList =
                compareSharedNetworkCategoryList(existingNetworkSharedCategories, newNetworkSharedCategories);

        List<Network> userNetworks = networkRepository.getAllNetworksById(userNetworkIds);

        for (Network network : userNetworks) {
            String networkCategory = network.getPrimaryContactMethod().getContactCategory().toUpperCase();
            String networkCategoryLabel = network.getPrimaryContactMethod().getContactLabel().toUpperCase();
            if (missingCategoryList.contains(networkCategory) ||
                    (networkCategory.equals(SOCIAL_PROFILE) && missingCategoryList.contains(networkCategoryLabel))) {
                throw new BadRequestException(MessageCodes.NETWORK_SHARED_CONTACTS_CANNOT_BE_REMOVED.getValue());
            }
        }

        sessionUser.setNetworkSharedValueList(newNetworkSharedValueIdList);

        updateAddedNetworkCommunicationDictionary(sessionUser, newNetworkSharedValueIdList);

    }

    private void updateAddedNetworkCommunicationDictionary(PeopleUser sessionUser, List<String> newNetworkSharedValueIdList) {
        Map<String, UserProfileData> userMetaDataMap = sessionUser.getMetadataMap();
        NetworkCommunicationSettingStatus communicationSettingStatus = sessionUser.getNetworkCommunicationTypesSelected();
        for (String networkValue : newNetworkSharedValueIdList) {
            if (userMetaDataMap.get(networkValue) != null) {
                String category = userMetaDataMap.get(networkValue).getCategory();
                String label = userMetaDataMap.get(networkValue).getLabel();
                switch (category) {
                    case EMAIL:
                        communicationSettingStatus.setDefaultEmailAdded(Boolean.TRUE);
                        break;
                    case SOCIAL_PROFILE:
                        updateSocialProfileSettingStatus(label, communicationSettingStatus);
                        break;
                    case PHONE_NUMBER:
                    default:
                        break;
                }
            }
        }
    }

    private void updateSocialProfileSettingStatus(String label, NetworkCommunicationSettingStatus communicationSettingStatus) {
        switch (label) {
            case TWITTER:
                communicationSettingStatus.setDefaultTwitterAccountAdded(Boolean.TRUE);
                break;
            case LINKEDIN:
                communicationSettingStatus.setDefaultLinkedInAccountAdded(Boolean.TRUE);
                break;
            case INSTAGRAM:
                communicationSettingStatus.setDefaultInstagramAccountAdded(Boolean.TRUE);
                break;
            default:
                throw new BadRequestException(MessageCodes.INVALID_OPERATION.getValue());
        }
    }

    private List<String> getListOfSharedCategoriesForNetwork(PeopleUser peopleUser,
                                                             List<String> existingNetworkSharedValueIdList) {
        List<String> networkSharedCategoryList = new ArrayList<>();

        Map<String, UserProfileData> metadataMap = peopleUser.getMetadataMap();
        for (String value : PeopleUtils.emptyIfNull(existingNetworkSharedValueIdList)) {
            if (metadataMap.containsKey(value)) {
                String category = metadataMap.get(value).getCategory();
                switch (category) {
                    case PHONE_NUMBER:
                    case EMAIL:
                        networkSharedCategoryList.add(category);
                        break;
                    case SOCIAL_PROFILE:
                        networkSharedCategoryList.add(metadataMap.get(value).getLabel());
                        break;
                    default:
                        break;
                }

            }
        }
        return networkSharedCategoryList;
    }

    private List<String> compareSharedNetworkCategoryList(List<String> existingNetworkSharedCategories,
                                                          List<String> newNetworkSharedCategories) {
        List<String> missingSharedCategoryList = new ArrayList<>();

        for (String category : existingNetworkSharedCategories) {
            if (!newNetworkSharedCategories.contains(category)) {
                missingSharedCategoryList.add(category);
            }
        }

        return missingSharedCategoryList;
    }


}
