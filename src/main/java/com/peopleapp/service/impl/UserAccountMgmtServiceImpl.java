package com.peopleapp.service.impl;

import com.peopleapp.configuration.LocaleMessageReader;
import com.peopleapp.dto.StaticSharedData;
import com.peopleapp.dto.UserInformationDTO;
import com.peopleapp.dto.requestresponsedto.DeleteAccountRequest;
import com.peopleapp.enums.*;
import com.peopleapp.exception.BadRequestException;
import com.peopleapp.model.*;
import com.peopleapp.repository.*;
import com.peopleapp.security.TokenAuthService;
import com.peopleapp.service.PeopleUserService;
import com.peopleapp.service.PrivacyProfileService;
import com.peopleapp.service.UserAccountMgmtService;
import com.peopleapp.service.MasterService;
import com.peopleapp.util.PeopleUtils;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserAccountMgmtServiceImpl implements UserAccountMgmtService {

    @Inject
    private PeopleUserRepository peopleUserRepository;

    @Inject
    private UserConnectionRepository userConnectionRepository;

    @Inject
    private UserActivityRepository userActivityRepository;

    @Inject
    private UserGroupRepository userGroupRepository;

    @Inject
    private UserSessionRepository userSessionRepository;

    @Inject
    private LocaleMessageReader message;

    @Inject
    private CustomBaseRepository customBaseRepository;

    @Inject
    private TokenAuthService tokenAuthService;

    @Inject
    private PeopleUserService peopleUserService;

    @Inject
    private PrivacyProfileService privacyProfileService;

    @Inject
    private RegisteredNumberRepository registeredNumberRepository;

    @Inject
    private MasterService masterService;

    @Inject
    private NetworkMemberRepository networkMemberRepository;

    @Inject
    private NetworkRepository networkRepository;

    @Inject
    private UserRestoreConnectionRepository userRestoreConnectionRepository;

    @Override
    public void deleteUserAccount(DeleteAccountRequest deleteAccountRequest) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        String userId = sessionUser.getUserId();

        // User can not delete the network if owner to any network
        long ownerOfNetworks = networkMemberRepository.findCountOfUserEnrolledNetwork(userId,
                NetworkMemberRole.OWNER.getValue());
        if (ownerOfNetworks != 0) {
            throw new BadRequestException(MessageCodes.NETWORK_OWNER_CANNOT_DELETE_ACCOUNT.getValue());
        }

        /* --- clean up tasks, before a user is off boarded from people system --- */
        // exit user out of all networks
        updateUserNetworks(sessionUser);

        // delete all his contacts/connections
        userConnectionRepository.deleteAllConnectionForAUser(userId);

        // make connections static for the deleted user
        makeConnectionsStaticForDeletedContact(userId);

        // expire all activity created for and by user
        userActivityRepository.expireAllActivityRelatedToUser(userId);

        // delete all user groups
        userGroupRepository.deleteAllGroupCreatedByUser(userId);

        // delete all contacts from restore collection
        userRestoreConnectionRepository.deleteAllConnectionByConnectionFromId(new ObjectId(userId));

        // expire session
        UserSession userSession = userSessionRepository.findActiveSession(PeopleUtils.convertStringToObjectId(userId));
        userSession.setModifiedTime(new DateTime());
        userSession.setStatus(TokenStatus.EXPIRED);
        userSessionRepository.save(userSession);

        // change user status to static
        sessionUser.setStatus(UserStatus.DELETED);
        sessionUser.setDeleteAccountReason(deleteAccountRequest.getMessage());
        peopleUserRepository.save(sessionUser);

        // delete from registered number
        List<RegisteredNumber> registeredNumberList = registeredNumberRepository.findAll();

        if (!PeopleUtils.isNullOrEmpty(registeredNumberList)) {
            RegisteredNumber registeredNumber = registeredNumberList.get(0);
            List<String> numberList = registeredNumber.getRegisteredNumberList();
            numberList.remove(sessionUser.getVerifiedContactNumber().getMobileNumber());
            registeredNumberRepository.save(registeredNumber);
        }

    }

    @Override
    public void suspendAccount() {
        PeopleUser peopleUser = tokenAuthService.getSessionUser();

        // Logout user from his active session(s)

        UserSession userSession = userSessionRepository.findActiveSession(
                PeopleUtils.convertStringToObjectId(peopleUser.getUserId()));
        userSession.setModifiedTime(new DateTime());
        userSession.setStatus(TokenStatus.EXPIRED);
        userSessionRepository.save(userSession);

        // Update user status to SUSPEND
        peopleUser.setStatus(UserStatus.DEACTIVATED);
        peopleUserRepository.save(peopleUser);
    }

    @Override
    public void logout() {

        String currentToken = tokenAuthService.getSessionToken();
        // get all active session
        UserSession userSession = userSessionRepository.findBySessionToken(currentToken);
        userSession.setModifiedTime(new DateTime());
        userSession.setStatus(TokenStatus.EXPIRED);
        userSessionRepository.save(userSession);
    }

    /* Makes connections static for the deleted user */
    private void makeConnectionsStaticForDeletedContact(String userId) {
        List<UserConnection> otherConnectionList =
                userConnectionRepository.findAllConnectionConnectedToGivenUserWithProfile(userId);
        List<String> connectionIds = new ArrayList<>();

        // Make him a static contact for all people users with whom he was connected in real time
        for (UserConnection userConnection : PeopleUtils.emptyIfNull(otherConnectionList)) {
            connectionIds.add(userConnection.getConnectionId());

            // save shared data by this contact as static shared data
            StaticSharedData staticSharedData = new StaticSharedData();
            userConnection.setStaticSharedData(staticSharedData);

            UserInformationDTO sharedData = masterService.prepareSharedData1(userConnection);
            userConnection.setStaticSharedProfileData(sharedData);

            // merge shared data snapshot to contact static data
            if (userConnection.getContactStaticData() != null) {
                masterService.mergeSharedInfoToStaticInfo(sharedData, userConnection.getContactStaticData());
            } else {
                userConnection.setContactStaticData(sharedData);
            }

        }

        if (!PeopleUtils.isNullOrEmpty(otherConnectionList)) {
            userConnectionRepository.saveAll(otherConnectionList);
            userConnectionRepository.updateConnectionDataForDeletedAccount(connectionIds, PeopleUtils.getCurrentTimeInUTC());
        }
    }


    private void updateUserNetworks(PeopleUser sessionUser) {
        List<NetworkMember> memberDetailsForUser = networkMemberRepository.getNetworkMemberDetailsForUser(sessionUser.getUserId());
        if(PeopleUtils.isNullOrEmpty(memberDetailsForUser)){
            return;
        }
        Map<String, String> networksForWhichUserIsAdmin = new HashMap<>();
        List<String> userNetworks = new ArrayList<>();
        for (NetworkMember userNetwork : PeopleUtils.emptyIfNull(memberDetailsForUser)) {
            if (userNetwork.getMemberRole().equalsIgnoreCase(NetworkMemberRole.ADMIN.getValue())) {
                networksForWhichUserIsAdmin.put(userNetwork.getNetworkId(), userNetwork.getMemberId());
            }
            userNetworks.add(userNetwork.getNetworkId());
        }

        // update network member and admin count accordingly
        List<Network> networksToBeUpdated = networkMemberRepository.getAllNetworksById(userNetworks);
        for (Network network : PeopleUtils.emptyIfNull(networksToBeUpdated)) {
            int memberCount = network.getMemberCount();
            int adminCount = network.getAdminCount();
            if (networksForWhichUserIsAdmin.get(network.getNetworkId()) != null) {
                network.setAdminCount(adminCount - 1);
            }
            network.setMemberCount(memberCount - 1);
        }

        //delete all network member records for user and update network member counts
        networkMemberRepository.removeUserFromNetwork(sessionUser.getUserId());
        networkRepository.saveAll(networksToBeUpdated);
    }
}
