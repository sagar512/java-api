package com.peopleapp.service;

import com.peopleapp.configuration.LocaleMessageReader;
import com.peopleapp.constant.MessageConstant;
import com.peopleapp.dto.APIRequestParamData;
import com.peopleapp.dto.NetworkPendingRequest;
import com.peopleapp.dto.UserNetworkDetails;
import com.peopleapp.dto.UserProfileData;
import com.peopleapp.dto.requestresponsedto.*;
import com.peopleapp.enums.*;
import com.peopleapp.exception.BadRequestException;
import com.peopleapp.model.*;
import com.peopleapp.repository.*;
import com.peopleapp.security.TokenAuthService;
import com.peopleapp.util.PeopleUtils;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.util.*;

import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
@SpringBootTest
public class NetworkServiceTest extends BaseTest {

    @MockBean
    private TokenAuthService tokenAuthService;

    @Inject
    private RecentActiveNetworkRepository recentActiveNetworkRepository;

    @Inject
    private NetworkService networkService;

    @Inject
    private NetworkRepository networkRepository;

    @Inject
    private NetworkMemberRepository networkMemberRepository;

    @Inject
    private LocaleMessageReader message;

    @Inject
    private UserActivityService userActivityService;

    @Inject
    private RegisteredNumberRepository registeredNumberRepository;

    @Inject
    private PeopleUserRepository peopleUserRepository;

    @Inject
    private PeopleUserService peopleUserService;

    @Inject
    private UserConnectionRepository userConnectionRepository;

    @Inject
    private UserPrivacyProfileRepository userPrivacyProfileRepository;

    @Inject
    private UserActivityRepository userActivityRepository;

    @Inject
    private ReportedUserDataRepository reportedUserDataRepository;

    @Inject
    private NetworkCategoryRepository networkCategoryRepository;


    private PeopleUser user1;
    private PeopleUser user2;
    private PeopleUser user3;
    private PeopleUser user4;

    private Network networkCreatedByUser1;
    private Network networkCreatedByUser2;
    private Network privateNetworkByUser3;
    private Network publicNetworkByUser3;
    private Network openNetworkByUser4;

    private NetworkMember networkMember2;
    private UserPrivacyProfile user1Profile;
    private UserPrivacyProfile user2Profile;
    private UserPrivacyProfile user3Profile;
    private UserPrivacyProfile user4Profile;

    private CreateNetworkRequestDTO createNetworkRequest;

    private UserConnection connection_U1_U2;
    private UserConnection connection_U1_U3;
    private UserConnection connection_U2_U1;
    private UserConnection connection_U3_U1;
    private UserConnection connection_U3_U4;
    private UserConnection connection_U4_U3;


    @Before
    public void setUp() {

        // user's initialization
        user1 = peopleUserRepository.save(
                MethodStubs.getWatuUserAccount("8007009001", "testWatuPeople1@mailinator.com",
                        "mockUser1", "", "YML"));
        user2 = peopleUserRepository.save(
                MethodStubs.getWatuUserAccount("8007009002", "testWatuPeople2@mailinator.com",
                        "mockUser2", "", "YML"));
        user3 = peopleUserRepository.save(
                MethodStubs.getWatuUserAccount("8007009003", "testWatuPeople3@mailinator.com",
                        "mockUser3", "", "YML"));
        user4 = peopleUserRepository.save(
                MethodStubs.getWatuUserAccount("8007009004", "testWatuPeople4@mailinator.com",
                        "mockUser4", "", "YML"));


        /*  register numbers to watu */
        registeredNumberRepository.save(MethodStubs.getRegisteredNumber(Arrays.asList("+18007009001", "+18007009002",
                "+18007009003", "+18007009004")));

        /*  creating profiles for users   */
        user1Profile = userPrivacyProfileRepository.save(MethodStubs.getUserPrivacyProfileObj(user1.getUserId(),
                new ArrayList<>(user1.getMetadataMap().keySet())));
        user2Profile = userPrivacyProfileRepository.save(MethodStubs.getUserPrivacyProfileObj(user2.getUserId(),
                new ArrayList<>(user2.getMetadataMap().keySet())));
        user3Profile = userPrivacyProfileRepository.save(MethodStubs.getUserPrivacyProfileObj(user3.getUserId(),
                new ArrayList<>(user3.getMetadataMap().keySet())));
        user4Profile = userPrivacyProfileRepository.save(MethodStubs.getUserPrivacyProfileObj(user4.getUserId(),
                new ArrayList<>(user4.getMetadataMap().keySet())));


        /*  User1 is connected with user2 and user3 */
        connection_U1_U2 = userConnectionRepository.save(MethodStubs.getConnectionWithProfileObj(user1.getUserId(),
                user2.getUserId(), user1Profile.getPrivacyProfileId()));
        connection_U1_U3 = userConnectionRepository.save(MethodStubs.getConnectionWithProfileObj(user1.getUserId(),
                user3.getUserId(), user1Profile.getPrivacyProfileId()));

        connection_U2_U1 = userConnectionRepository.save(MethodStubs.getConnectionWithProfileObj(user2.getUserId(),
                user1.getUserId(), user2Profile.getPrivacyProfileId()));
        connection_U3_U1 = userConnectionRepository.save(MethodStubs.getConnectionWithProfileObj(user3.getUserId(),
                user1.getUserId(), user3Profile.getPrivacyProfileId()));


        /*  user3 and user4 are connected    */
        connection_U4_U3 = userConnectionRepository.save(MethodStubs.getConnectionWithProfileObj(user4.getUserId(),
                user3.getUserId(), user4Profile.getPrivacyProfileId()));

        connection_U3_U4 = userConnectionRepository.save(MethodStubs.getConnectionWithProfileObj(user3.getUserId(),
                user4.getUserId(), user3Profile.getPrivacyProfileId()));


        /*  Creating networks (Public)   */
        networkCreatedByUser1 = networkRepository.save(MethodStubs.getPublicNetworkObj());
        networkCreatedByUser2 = networkRepository.save(MethodStubs.getPublicNetworkObj());

        /*  Creating networks (Private)   */
        privateNetworkByUser3 = networkRepository.save(MethodStubs.getPrivateNetworkObj());
        privateNetworkByUser3.setMemberCount(1);
        privateNetworkByUser3.setAdminCount(1);

        /*  Creating networks (Public)   */
        publicNetworkByUser3 = networkRepository.save(MethodStubs.getPublicNetworkObj());
        publicNetworkByUser3.setMemberCount(1);
        publicNetworkByUser3.setAdminCount(1);

        networkRepository.saveAll(Arrays.asList(privateNetworkByUser3, publicNetworkByUser3));

        /*  Creating networks (Open)   */
        openNetworkByUser4 = networkRepository.save(MethodStubs.getOpenNetworkObj());

        //  Members for network created by user 1
        networkMemberRepository.save(MethodStubs.getNetworkMemberObj(user1.getUserId(),
                networkCreatedByUser1.getNetworkId(), NetworkMemberRole.OWNER.getValue()));
        networkMember2 = networkMemberRepository.save(MethodStubs.getNetworkMemberObj(user2.getUserId(),
                networkCreatedByUser1.getNetworkId(), NetworkMemberRole.MEMBER.getValue()));
        networkCreatedByUser1.setMemberCount(2);
        networkCreatedByUser1.setAdminCount(1);
        networkRepository.save(networkCreatedByUser1);

        //  Members for network created by user 2
        networkMemberRepository.save(MethodStubs.getNetworkMemberObj(user2.getUserId(),
                networkCreatedByUser2.getNetworkId(), NetworkMemberRole.OWNER.getValue()));
        networkMemberRepository.save(MethodStubs.getNetworkMemberObj(user1.getUserId(),
                networkCreatedByUser2.getNetworkId(), NetworkMemberRole.MEMBER.getValue()));
        networkMemberRepository.save(MethodStubs.getNetworkMemberObj(user4.getUserId(),
                networkCreatedByUser2.getNetworkId(), NetworkMemberRole.ADMIN.getValue()));
        networkCreatedByUser2.setMemberCount(3);
        networkCreatedByUser2.setAdminCount(2);
        networkRepository.save(networkCreatedByUser2);

        //  Members for network(private) created by user 3
        networkMemberRepository.save(MethodStubs.getNetworkMemberObj(user3.getUserId(),
                privateNetworkByUser3.getNetworkId(), NetworkMemberRole.OWNER.getValue()));

        //  Members for network (public) created by user 3
        networkMemberRepository.save(MethodStubs.getNetworkMemberObj(user3.getUserId(),
                publicNetworkByUser3.getNetworkId(), NetworkMemberRole.OWNER.getValue()));

        //  Members for network created by user 4
        networkMemberRepository.save(MethodStubs.getNetworkMemberObj(user4.getUserId(),
                openNetworkByUser4.getNetworkId(), NetworkMemberRole.OWNER.getValue()));

        createNetworkRequest = MethodStubs.getCreateNetworkReqObj();
        networkCategoryRepository.save(MethodStubs.getNetworkCategory("Sports"));
    }

    /**
     * Method - updateNetworkSetting
     * TestCase - Success
     * Updating the network shared value list
     */
    @Test
    public void testUpdatingUsersNetworkSharedValues() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        List<String> networkValueList = new ArrayList<>(user1.getNetworkSharedValueList());

        Assert.assertEquals("User has 2 values already selected in network settings",
                2, networkValueList.size());

        for (UserProfileData userProfile : user1.getUserMetadataList()) {
            if (userProfile.getCategory().equalsIgnoreCase(UserInfoCategory.SOCIAL_PROFILE.getValue())) {
                networkValueList.add(userProfile.getValueId());
            }
        }

        networkService.updateNetworkSetting(MethodStubs.getUpdateNetworkSettingsDTO(networkValueList));

        Assert.assertEquals("User has added a value in network settings",
                3, user1.getNetworkSharedValueList().size());
    }

    /**
     * Method - updateNetworkSetting
     * TestCase - Success
     * User can un-select all network shared values if not enrolled to any network
     */
    @Test
    public void testRemovingAllNetworkSharedValues() {
        PeopleUser tempUser = peopleUserRepository.save(MethodStubs.getWatuUserAccount("8007009033",
                "testWatuPeople1@mailinator.com", "tempUser", "", "YML"));
        given(this.tokenAuthService.getSessionUser()).willReturn(tempUser);
        List<String> networkValueList = new ArrayList<>(tempUser.getNetworkSharedValueList());

        Assert.assertEquals("User has 2 values selected in network settings",
                2, networkValueList.size());


        networkService.updateNetworkSetting(MethodStubs.getUpdateNetworkSettingsDTO(new ArrayList<>()));

        List<String> networkDefaultSetting = networkService.getNetworkDefaultSetting();

        Assert.assertEquals("Success - User has removed all values in network settings",
                0, networkDefaultSetting.size());
    }

    /**
     * Method - updateNetworkSetting
     * TestCase - Failure
     * User can un-select all network shared values if not enrolled to any network, if enrolled then error will be thrown
     */
    @Test
    public void testRemovingAllNetworkSharedValuesWhileUserIsPartOfANetwork() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        List<String> networkValueList = new ArrayList<>(user1.getNetworkSharedValueList());

        Assert.assertEquals("User has 2 values selected in network settings",
                2, networkValueList.size());
        Exception exception = null;

        try {
            networkService.updateNetworkSetting(MethodStubs.getUpdateNetworkSettingsDTO(new ArrayList<>()));
        } catch (Exception e) {
            exception = e;
        }

        List<String> networkDefaultSetting = networkService.getNetworkDefaultSetting();

        Assert.assertTrue("Success - User has removed all values in network settings",
                exception instanceof BadRequestException);
        Assert.assertEquals("Network values set earlier were not removed", 2, networkDefaultSetting.size());
    }

    /**
     * Method - updateNetworkSetting
     * TestCase - Failure
     * Description - user can not remove value which is shared with network
     */
    @Test
    public void testRemovingNetworkSharedValue() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        Exception exception = null;
        List<String> networkValueList = new ArrayList<>(user1.getNetworkSharedValueList());

        Assert.assertEquals("User has 2 values already selected in network settings",
                2, networkValueList.size());

        // user1 tries to remove phone number which he has shared with a network
        for (UserProfileData userProfile : user1.getUserMetadataList()) {
            if (userProfile.getCategory().equalsIgnoreCase(UserInfoCategory.CONTACT_NUMBER.getValue())) {
                networkValueList.remove(userProfile.getValueId());
            }
        }

        try {
            networkService.updateNetworkSetting(MethodStubs.getUpdateNetworkSettingsDTO(networkValueList));
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Failure - User1 is not allowed to remove the phone number which is shared " +
                "with network", exception instanceof BadRequestException);
    }


    /**
     * Method - getUserNetworks
     * TestCase - Success
     * Network is created by user1 and it must reflect when he fetches networks
     * for which he is Owner, Admin or member
     */
    @Test
    public void testGetUserNetworks() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);

        int initialPageNumber = 0;
        int pageSize = 1;
        GetUserNetworksResponseDTO userNetworks = networkService.getUserNetworks(initialPageNumber, pageSize);

        Assert.assertEquals("Success - User 1 is member of the 2 networks ", 2,
                userNetworks.getTotalElements());

        Assert.assertNotNull("With page size 1 there will be one more page", userNetworks.getNextURL());

        //increase page number and fetch networks
        userNetworks = networkService.getUserNetworks(initialPageNumber + 1, pageSize);

        Assert.assertNull("This is the last page ", userNetworks.getNextURL());

    }

    /**
     * Method - createNetwork
     * TestCase - Success
     * Creating new network
     */
    public void testCreateNetwork() {
        /* Using collation to check if network with same name is already created by user - collation is not supported in UT*/
//        CreateOrEditNetworkResponseDTO createNetworkResponseDTO = networkService.createNetwork(createNetworkRequest);
//        Optional<Network> network = networkRepository.findById(createNetworkResponseDTO.getNetworkId());
//        Assert.assertNotNull("Success - Network created", network);
    }

    /**
     * Method - createNetwork
     * TestCase - Failure
     * User can not create 2 networks with same name
     */
    public void testCreatingNetworkWithExistingName() {
        /* Using collation to check if network with same name is already created by user - collation is not supported in UT*/
//        Exception exception = null;
//        CreateOrEditNetworkResponseDTO createNetworkResponseDTO = networkService.createNetwork(createNetworkRequest);
//        Assert.assertNotNull("Success - Network created", createNetworkResponseDTO);
//        //trying to create new Network with name in use
//        try {
//            networkService.createNetwork(createNetworkRequest);
//        } catch (Exception e) {
//            exception = e;
//        }
//        Assert.assertTrue("Failure - Expected BadRequestException ", exception instanceof BadRequestException);
//        Assert.assertTrue("Failure - Error status code = 924", exception.getMessage().equals(
//                MessageCodes.NETWORK_NAME_ALREADY_EXIST.getValue()));
    }

    /**
     * Method - updateNetworkFavouriteStatus
     * TestCase - Success
     * Marking Network as Favourite
     */
    @Test
    public void testMarkingNetworkAsFavourite() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        String networkID = networkCreatedByUser1.getNetworkId();

        NetworkMember networkMemberStatusBeforeUpdate = networkMemberRepository.findByIdAndUserIdAndStatus(networkID,
                user1.getUserId(), NetworkMemberStatus.ACTIVE.getValue());
        networkMemberStatusBeforeUpdate.setIsFavourite(Boolean.FALSE);
        networkMemberRepository.save(networkMemberStatusBeforeUpdate);

        networkService.updateNetworkFavouriteStatus(MethodStubs.getNetworkFavouriteRequestDTO(
                networkID, Boolean.TRUE));

        NetworkMember networkMemberStatusAfterUpdate = networkMemberRepository.findByIdAndUserIdAndStatus(networkID,
                user1.getUserId(), NetworkMemberStatus.ACTIVE.getValue());

        Assert.assertFalse("Initially the network is not marked as favourite", networkMemberStatusBeforeUpdate.getIsFavourite());
        Assert.assertTrue("Success - Network marked as favourite  successfully", networkMemberStatusAfterUpdate.getIsFavourite());
    }

    /**
     * Method - updateNetworkFavouriteStatus
     * TestCase - Success
     * Changing favourite status for network from favourite to Un-favourite
     */
    @Test
    public void testMarkingNetworkAsUnFavourite() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        String networkID = networkCreatedByUser1.getNetworkId();

        NetworkMember networkMemberStatusBeforeUpdate = networkMemberRepository.findByIdAndUserIdAndStatus(networkID,
                user1.getUserId(), NetworkMemberStatus.ACTIVE.getValue());
        networkMemberStatusBeforeUpdate.setIsFavourite(Boolean.TRUE);
        networkMemberRepository.save(networkMemberStatusBeforeUpdate);

        networkService.updateNetworkFavouriteStatus(MethodStubs.getNetworkFavouriteRequestDTO(networkID, Boolean.FALSE));

        NetworkMember networkMemberStatusAfterUpdate = networkMemberRepository.findByIdAndUserIdAndStatus(networkID,
                user1.getUserId(), NetworkMemberStatus.ACTIVE.getValue());

        Assert.assertTrue("Initially the network is marked as favourite", networkMemberStatusBeforeUpdate.getIsFavourite());
        Assert.assertFalse("Success - Network is Un-favourite ", networkMemberStatusAfterUpdate.getIsFavourite());
    }

    /**
     * Method - promoteAdminsToNetwork
     * TestCase - Success
     * Promoting network member to admin by owner
     */
    @Test
    public void testPromotingMemberToAdmin() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        NetworkAdminPromoteDTO networkAdminPromoteDTO = MethodStubs.getNetworkAdminPromoteDTO(
                networkCreatedByUser1.getNetworkId(), Collections.singletonList(networkMember2.getMemberId()));
        String response = networkService.promoteAdminsToNetwork(networkAdminPromoteDTO);
        Assert.assertNotNull("Success - on successful promotion to admin activity object is created", response);
        NetworkMember networkMember = networkMemberRepository.findByIdAndUserIdAndStatus(
                networkAdminPromoteDTO.getNetworkId(), user2.getUserId(), NetworkMemberStatus.ACTIVE.getValue());
        Assert.assertTrue("Success - Network member is promoted to Admin", networkMember.getMemberRole()
                .equals(NetworkMemberRole.ADMIN.getValue()));
    }

    /**
     * Method - promoteAdminsToNetwork
     * TestCase - Failure
     * Promoting network member to admin by other members
     */
    @Test
    public void testPromotingMemberToAdminByOtherMembers() {
        Exception exception = null;
        given(this.tokenAuthService.getSessionUser()).willReturn(user3);
        NetworkAdminPromoteDTO networkAdminPromoteDTO = MethodStubs.getNetworkAdminPromoteDTO(
                networkCreatedByUser1.getNetworkId(), Arrays.asList(user2.getUserId()));
        try {
            networkService.promoteAdminsToNetwork(networkAdminPromoteDTO);
        } catch (Exception e) {
            exception = e;
        }
        Assert.assertNotNull(" Failure - Except owners no other members can promote others to admins", exception);
        Assert.assertTrue("BadRequestException is expected", exception instanceof BadRequestException);
    }

    /**
     * Method - promoteAdminsToNetwork
     * TestCase - Failure
     * If network is not in active state then action can not be performed
     */
    @Test
    public void testPromotingMemberToAdminForInActiveNetwork() {
        Exception exception = null;
        networkCreatedByUser1.setNetworkStatus(NetworkStatus.DELETED.getValue());
        networkRepository.save(networkCreatedByUser1);
        NetworkAdminPromoteDTO networkAdminPromoteDTO = MethodStubs.getNetworkAdminPromoteDTO(
                networkCreatedByUser1.getNetworkId(), Arrays.asList(user2.getUserId()));
        try {
            networkService.promoteAdminsToNetwork(networkAdminPromoteDTO);
        } catch (Exception e) {
            exception = e;
        }
        Assert.assertTrue(" Failure (BadRequestException) - Action can be performed only on ACTIVE networks",
                exception instanceof BadRequestException);
        Assert.assertEquals(MessageCodes.INVALID_NETWORK.getValue(), exception.getMessage());
    }

    /**
     * Method - getNetworkDetails
     * TestCase - Success
     * Session user is the owner of the network for which user is fetching details.
     */
    @Test
    public void testFetchingNetworkDetailsForNetworkOwner() {

        UserActivity userActivityForNetworkJoinRequest = MethodStubs.getUserActivityForNetworkJoinRequest(
                user3.getUserId(), networkCreatedByUser1.getNetworkId(), user1.getUserId());
        userActivityRepository.save(userActivityForNetworkJoinRequest);

        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        UserNetworkDetails userNetworkDetails = networkService.getNetworkDetails(networkCreatedByUser1.getNetworkId());

        Assert.assertNotNull("Success - Response is returned", userNetworkDetails);

        Assert.assertEquals("Success - Network has 2 members", 2,
                userNetworkDetails.getNetworkDetails().getMemberCount());

        long pendingRequestCount = userNetworkDetails.getPendingRequestCount();
        Assert.assertEquals("Success - Network has one pending request viewable by owner or admin ", 1,
                pendingRequestCount);

        Assert.assertEquals("Success - Owner will get his own contact detail",
                user1.getUserId(), userNetworkDetails.getNetworkOwnerContact().getToUserId());
    }

    /**
     * Method - getNetworkDetails
     * TestCase - Success
     * Session user is a member of the network for which user is fetching details. And Not in connection with Owner.
     */
    @Test
    public void testFetchingNetworkDetailsForNetworkMemberNotInConnectionWithOwner() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user3);

        UserActivity userActivityForNetworkJoinRequest = MethodStubs.getUserActivityForNetworkJoinRequest(
                user3.getUserId(), networkCreatedByUser1.getNetworkId(), user1.getUserId());
        userActivityRepository.save(userActivityForNetworkJoinRequest);

        UserNetworkDetails userNetworkDetails = networkService.getNetworkDetails(networkCreatedByUser1.getNetworkId());

        Assert.assertNotNull("Success - Response is returned", userNetworkDetails);

        Assert.assertEquals("Success - Network has 2 members", 2,
                userNetworkDetails.getNetworkDetails().getMemberCount());

        Assert.assertNull("Success - Network pending request not visible for members",
                userNetworkDetails.getPendingRequestCount());

        Assert.assertNotNull("Success - Owner contact detail must be available for all users other than owner ",
                userNetworkDetails.getNetworkOwnerContact());
    }

    /**
     * Method - getNetworkDetails
     * TestCase - Success
     * Session user is a member of the network for which user is fetching details and in connection with Owner
     */
    @Test
    public void testFetchingNetworkDetailsForNetworkMemberInConnectionWithOwner() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        UserNetworkDetails userNetworkDetails = networkService.getNetworkDetails(networkCreatedByUser2.getNetworkId());

        Assert.assertNotNull("Success - Response is returned", userNetworkDetails);

        Assert.assertEquals("Success - Network has 3 members", 3,
                userNetworkDetails.getNetworkDetails().getMemberCount());

        Assert.assertNull("Success - Network pending request not visible for members",
                userNetworkDetails.getPendingRequestCount());

        Assert.assertNotNull("Success - Owner contact detail must be available for all users other than owner ",
                userNetworkDetails.getNetworkOwnerContact());
    }

    /**
     * Method - fetchJoinRequestDetailsForNetwork
     * TestCase - Success
     * Fetches details of all the join request for particular network. Only owners and admins can access this.
     */
    @Test
    public void testFetchingNetworkJoinRequestDetails() {
        //creating activity for joining network
        UserActivity userActivityForNetworkJoinRequest = MethodStubs.getUserActivityForNetworkJoinRequest(
                user3.getUserId(), networkCreatedByUser1.getNetworkId(), user1.getUserId());
        userActivityRepository.save(userActivityForNetworkJoinRequest);

        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        NetworkPendingRequestDetailsDTO response = networkService.fetchJoinRequestDetailsForNetwork(
                networkCreatedByUser1.getNetworkId(), 0, 5);

        Assert.assertEquals("Success - only one request is sent to the network ", 1,
                response.getRequestDetailsList().size());

        NetworkPendingRequest networkPendingRequest = response.getRequestDetailsList().get(0);

        Assert.assertEquals("Success - Request was sent from testUser3", user3.getUserId(),
                networkPendingRequest.getPendingRequestDetails().getActivityById());
    }

    /**
     * Method - getMembersOfNetwork
     * TestCase - Success
     * Fetches all members of network including owners, admins and members
     */
    public void testFetchingAllMembers() {

        APIRequestParamData apiRequestParamData = new APIRequestParamData();

        //commenting the test case as embeddedMongo does not support collation feature of mongodb and test case was failing

//        NetworkMembersResponseDTO getNetworkMembersResponseDTO = networkService.getMembersOfNetwork(
//                networkCreatedByUser2.getNetworkId(), apiRequestParamData);

    }

    /**
     * Method - fetchJoinRequestDetailsForNetwork
     * TestCase - Failure
     * Only Owner or Admins can view details of join request
     * testUser2 is just a member of the network created by testUser1
     */
    @Test
    public void testFetchingNetworkJoinRequestDetailsByAMember() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user2);
        //creating activity for joining network
        UserActivity userActivityForNetworkJoinRequest = MethodStubs.getUserActivityForNetworkJoinRequest(
                user3.getUserId(), networkCreatedByUser1.getNetworkId(), user1.getUserId());
        userActivityRepository.save(userActivityForNetworkJoinRequest);

        Exception exception = null;

        try {
            networkService.fetchJoinRequestDetailsForNetwork(networkCreatedByUser1.getNetworkId(), 0, 5);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertNotNull("Exception occurs as a member tries to access pending request details ", exception);

        Assert.assertTrue("BadRequestException expected as this is Unauthorized action",
                exception instanceof BadRequestException);

        Assert.assertEquals(MessageCodes.ACTION_CANNOT_BE_TAKEN.getValue(), exception.getMessage());
    }

    /**
     * Method - getMembersOfNetwork
     * TestCase - Failure
     * Only valid network member can see the list of members
     */
    @Test
    public void testFetchingAllMembersByGuestMember() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user3);
        Exception exception = null;
        APIRequestParamData apiRequestParamData = new APIRequestParamData();
        apiRequestParamData.setSearchString("");
        apiRequestParamData.setSortByRole(0);
        apiRequestParamData.setFNameOrder(1);
        apiRequestParamData.setLNameOrder(1);
        apiRequestParamData.setLastNamePreferred(false);
        apiRequestParamData.setPageNumber(0);
        apiRequestParamData.setPageSize(2);
        try {
            networkService.getMembersOfNetwork(networkCreatedByUser2.getNetworkId(), apiRequestParamData);
        } catch (Exception e) {
            exception = e;
        }
        Assert.assertTrue("BadRequestException Expected - If user is not part of network ",
                exception instanceof BadRequestException);

    }


    /**
     * Method - getAdminsOfNetwork
     * TestCase - Success
     * Fetches only admins of network
     */
    public void testFetchingAllAdmins() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user2);
        // commenting the test case as embeddedMongo does not support collation feature of mongodb and test case was failing

//        NetworkMembersResponseDTO getNetworkMembersResponseDTO = networkService.getAdminsOfNetwork(
//                networkCreatedByUser2.getNetworkId(), apiRequestParamData);

    }

    /**
     * Method - getAdminsOfNetwork
     * TestCase - Failure
     * Only valid network member can see the list of admins
     */
    @Test
    public void testFetchingAdminsByGuestMember() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user3);
        Exception exception = null;
        APIRequestParamData apiRequestParamData = new APIRequestParamData();
        apiRequestParamData.setSearchString("");
        apiRequestParamData.setSortByRole(0);
        apiRequestParamData.setFNameOrder(1);
        apiRequestParamData.setLNameOrder(1);
        apiRequestParamData.setLastNamePreferred(false);
        apiRequestParamData.setPageNumber(0);
        apiRequestParamData.setPageSize(2);
        try {
            networkService.getAdminsOfNetwork(networkCreatedByUser2.getNetworkId(), apiRequestParamData);
        } catch (Exception e) {
            exception = e;
        }
        Assert.assertTrue("BadRequestException Expected - If user is not part of network ",
                exception instanceof BadRequestException);
    }

    /**
     * Method - messageAllMembers
     * TestCase - Success
     * Broadcasting message to all members of network, can be initiated by owner or admins
     * messages are sent/received as activity item.
     */
    @Test
    public void testMessagingAllMembers() {
        //forming the message to be sent
        MessageNetworkMembersDTO messageNetworkMembersDTO = new MessageNetworkMembersDTO();
        messageNetworkMembersDTO.setMessageToBroadcast("Message from network owner");
        messageNetworkMembersDTO.setNetworkId(networkCreatedByUser1.getNetworkId());

        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        //invoking the method
        networkService.messageAllMembers(messageNetworkMembersDTO);

        List<UserActivity> userActivity = userActivityRepository.findByInitiatedToId(user2.getUserId());

        Assert.assertEquals(1, userActivity.size());
        Assert.assertEquals("Success - Message is sent to member of the network as activity item",
                RequestType.NETWORK_MESSAGE_BROADCAST, userActivity.get(0).getActivityType().getRequestType());
    }

    /**
     * Method - messageAllMembers
     * TestCase - Failure
     * Other members trying to send messages to network, will result in BadRequestException
     */
    @Test
    public void testMessagingAllMembersByOtherMembers() {
        //forming the message to be sent
        MessageNetworkMembersDTO messageNetworkMembersDTO = new MessageNetworkMembersDTO();
        messageNetworkMembersDTO.setMessageToBroadcast("Message from network member");
        messageNetworkMembersDTO.setNetworkId(networkCreatedByUser2.getNetworkId());
        Exception exception = null;
        //user1 is just a member for the network created by testUser2
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        try {
            //invoking the method
            networkService.messageAllMembers(messageNetworkMembersDTO);
        } catch (Exception e) {
            exception = e;
        }
        Assert.assertTrue("Failure - only owners and admins are allowed to send messages to network",
                exception instanceof BadRequestException);
        Assert.assertEquals("Unauthorized to take action",
                MessageCodes.ACTION_CANNOT_BE_TAKEN.getValue(), exception.getMessage());

    }

    /**
     * Method - deleteNetwork
     * TestCase - Success
     * Only owner of network can delete that network. And all active members status will be updated to deleted state
     */
    @Test
    public void testDeletingNetworkByOwner() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        //testUser is owner of network
        DeleteNetworkRequestDTO deleteNetworkRequest = new DeleteNetworkRequestDTO();
        deleteNetworkRequest.setNetworkId(networkCreatedByUser1.getNetworkId());

        networkService.deleteNetwork(deleteNetworkRequest);

        Optional<Network> network = networkRepository.findById(networkCreatedByUser1.getNetworkId());

        Assert.assertEquals("Success - Network is Deleted", NetworkStatus.DELETED.getValue(),
                network.get().getNetworkStatus());

    }

    /**
     * Method - deleteNetwork
     * TestCase - Failure
     * Only owner of network can delete that network.
     */
    @Test
    public void testDeletingNetworkByOtherMembersOfNetwork() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        Exception exception = null;
        //testUser is not the owner of network
        DeleteNetworkRequestDTO deleteNetworkRequest = new DeleteNetworkRequestDTO();
        deleteNetworkRequest.setNetworkId(networkCreatedByUser2.getNetworkId());
        try {
            networkService.deleteNetwork(deleteNetworkRequest);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("BadRequestException expected", exception instanceof BadRequestException);

    }

    /**
     * Method - handleNetworkJoinRequest
     * TestCase - Success
     * Owner and admin can accept the request to join network
     */
    @Test
    public void testAcceptingRequestToJoinNetwork() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        UserActivity userActivityForNetworkJoinRequest = userActivityRepository.save(MethodStubs
                .getUserActivityForNetworkJoinRequest(user3.getUserId(), networkCreatedByUser1.getNetworkId(), user1.getUserId()));

        NetworkJoinRequestDTO networkJoinRequest = new NetworkJoinRequestDTO();
        networkJoinRequest.setActivityId(userActivityForNetworkJoinRequest.getActivityId());
        networkJoinRequest.setRequestAccepted(Boolean.TRUE);

        NetworkMember networkMember = networkMemberRepository.findByIdAndUserIdAndStatus(
                networkCreatedByUser1.getNetworkId(), user3.getUserId(), NetworkMemberStatus.ACTIVE.getValue());

        Assert.assertNull("testUser3 is not part of the network", networkMember);

        networkService.handleNetworkJoinRequest(networkJoinRequest);

        networkMember = networkMemberRepository.findByIdAndUserIdAndStatus(
                networkCreatedByUser1.getNetworkId(), user3.getUserId(), NetworkMemberStatus.ACTIVE.getValue());

        Assert.assertNotNull("Success - testUser3 is added to the network", networkMember);

    }

    /**
     * Method - handleNetworkJoinRequest
     * TestCase - Success
     * Owner or admin can reject the request to join Network
     */
    @Test
    public void testRejectingRequestToJoinNetwork() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        UserActivity userActivityForNetworkJoinRequest = userActivityRepository.save(MethodStubs
                .getUserActivityForNetworkJoinRequest(user3.getUserId(), networkCreatedByUser1.getNetworkId(), user1.getUserId()));

        NetworkJoinRequestDTO networkJoinRequest = new NetworkJoinRequestDTO();
        networkJoinRequest.setActivityId(userActivityForNetworkJoinRequest.getActivityId());
        networkJoinRequest.setRequestAccepted(Boolean.FALSE);

        NetworkMember networkMember = networkMemberRepository.findByIdAndUserIdAndStatus(
                networkCreatedByUser1.getNetworkId(), user3.getUserId(), NetworkMemberStatus.ACTIVE.getValue());

        Assert.assertNull("testUser3 is not part of the network", networkMember);

        networkService.handleNetworkJoinRequest(networkJoinRequest);

        networkMember = networkMemberRepository.findByIdAndUserIdAndStatus(
                networkCreatedByUser1.getNetworkId(), user3.getUserId(), NetworkMemberStatus.ACTIVE.getValue());

        Assert.assertNull("testUser3 is not part of the network", networkMember);

    }

    /**
     * Method - handleNetworkJoinRequest
     * TestCase - Failure
     * Other members apart from owner or admin cannot Manage pending network join requests
     */
    @Test
    public void testHandlingRequestToJoinNetworkByOtherMembers() {
        //testUser2 is a member and must not be able to handle request
        given(this.tokenAuthService.getSessionUser()).willReturn(user2);
        UserActivity userActivityForNetworkJoinRequest = userActivityRepository.save(MethodStubs
                .getUserActivityForNetworkJoinRequest(user3.getUserId(), networkCreatedByUser1.getNetworkId(), user1.getUserId()));

        NetworkJoinRequestDTO networkJoinRequest = new NetworkJoinRequestDTO();
        networkJoinRequest.setActivityId(userActivityForNetworkJoinRequest.getActivityId());
        networkJoinRequest.setRequestAccepted(Boolean.TRUE);

        Exception exception = null;
        try {
            networkService.handleNetworkJoinRequest(networkJoinRequest);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Expected BadRequestException as members are not allowed to handleJoinRequest ",
                exception instanceof BadRequestException);
    }

    /**
     * Method - editNetworkDetails
     * TestCase - Success
     * Only network owner or admin can edit network details
     */
    public void testEditingANetwork() {
        /* Using collation to check if network with same name is already created by user - collation is not supported in UT*/
//        EditNetworkRequestDTO editNetworkRequest = MethodStubs.getEditNetworkReqObj(networkCreatedByUser1.getNetworkId());
//        String networkNameBeforeEditing = networkCreatedByUser1.getName();
//        //editing the network created by testUser1
//        CreateOrEditNetworkResponseDTO editNetworkResponse = networkService.editNetworkDetails(editNetworkRequest);
//        Assert.assertTrue("Success - Network name is changed", (!editNetworkResponse.getNetworkDetails().getName()
//                .equals(networkNameBeforeEditing) && editNetworkResponse.getNetworkDetails().getName().equals("SportsClub")));
    }

    /**
     * Method - editNetworkDetails
     * TestCase - Failure
     * Exception is thrown if other members(apart from admin and owner) tries to edit network details
     */
    @Test
    public void testEditingANetworkByOtherMembers() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user2);
        EditNetworkRequestDTO editNetworkRequest = MethodStubs.getEditNetworkReqObj(networkCreatedByUser1.getNetworkId());
        Exception exception = null;
        try {
            //editing the network created by testUser1
            networkService.editNetworkDetails(editNetworkRequest);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Failure - Network can not be edited by other members",
                (exception instanceof BadRequestException && exception.getMessage()
                        .equals(MessageCodes.ACTION_CANNOT_BE_TAKEN.getValue())));

    }

    /**
     * Method   -   getNetworkDefaultSetting
     * TestCase -   Success
     */
    @Test
    public void testGettingNetworkPreferences() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user4);
        List<String> networkPreferences = networkService.getNetworkDefaultSetting();

        Assert.assertFalse(PeopleUtils.isNullOrEmpty(networkPreferences));
        Assert.assertEquals("Success - User4 has set primary number and primary email as network preferences ",
                2, networkPreferences.size());
    }

    /**
     * Method   -   joinNetwork
     * TestCase -   Success
     * Description  -   Request sent to public network, needs approval from network owner/admin to be join the network
     */
    @Test
    public void testSendingJoinRequestToPublicNetwork() {
        //  user3 sends join request to network created by user 2
        given(this.tokenAuthService.getSessionUser()).willReturn(user3);
        JoinNetworkResponseDTO joinNetworkResponse = networkService.joinNetwork(MethodStubs.getJoinNetworkRequestDTO(
                networkCreatedByUser2.getNetworkId()));

        Assert.assertTrue("Success - User sent a join request to 'PUBLIC' network ",
                joinNetworkResponse.getIsRequestSent());
    }

    /**
     * Method   -   joinNetwork
     * TestCase -   Success
     * Description  -   Join request to open network makes user instantly a member of that network
     */
    @Test
    public void testSendingJoinRequestToOpenNetwork() {
        //  user1 sends join request to open network created by user 4
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        JoinNetworkResponseDTO joinNetworkResponse = networkService.joinNetwork(MethodStubs.getJoinNetworkRequestDTO(
                openNetworkByUser4.getNetworkId()));

        Assert.assertEquals("Success - On join request user1 becomes part of network",
                joinNetworkResponse.getResponseMessage(), message.get(MessageConstant.NETWORK_JOIN_SUCCESS));

        Assert.assertFalse("Success - User sent a join request to 'OPEN' network which makes them " +
                "members instantly", joinNetworkResponse.getIsRequestSent());
    }

    /**
     * Method   -   joinNetwork
     * TestCase -   Failure
     * Description  -   Join request can be sent only once per network
     */
    @Test
    public void testSendingJoinRequestTwiceToNetwork() {
        //  user3 sends join request to network created by user 2
        given(this.tokenAuthService.getSessionUser()).willReturn(user3);
        JoinNetworkResponseDTO joinNetworkResponse = networkService.joinNetwork(MethodStubs.getJoinNetworkRequestDTO(
                networkCreatedByUser2.getNetworkId()));

        Assert.assertTrue("User sent a join request to a network ", joinNetworkResponse.getIsRequestSent());
        Exception exception = null;
        try {
            networkService.joinNetwork(MethodStubs.getJoinNetworkRequestDTO(networkCreatedByUser2.getNetworkId()));
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Failure - Request to join can be sent only once per network",
                exception instanceof BadRequestException);
    }

    /**
     * Method   -   joinNetwork
     * TestCase -   Failure
     * Description  -   existing members cannot send request again
     */
    @Test
    public void testSendingJoinRequestByNetworkMember() {
        Exception exception = null;
        //  user1 sends join request to network created by user 2
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        try {
            networkService.joinNetwork(MethodStubs.getJoinNetworkRequestDTO(
                    networkCreatedByUser2.getNetworkId()));
        } catch (Exception e) {
            exception = e;
        }
        Assert.assertTrue("Failure - user1 is already member of this networkCreatedByUser2 ",
                exception instanceof BadRequestException);
    }

    /**
     * Method   -   joinNetwork
     * TestCase -   Failure
     * Description  -   To send a join request communication type specified by network has to be shared
     */
    @Test
    public void testSendingJoinRequestByNetworkMemberWithOutSharingCommunicationMethod() {
        Exception exception = null;
        //  tempUser sends join request to network created by user 2 without sharing communication type
        given(this.tokenAuthService.getSessionUser()).willReturn(peopleUserRepository.save(MethodStubs.getUserObject("tempUser")));
        try {
            networkService.joinNetwork(MethodStubs.getJoinNetworkRequestDTO(networkCreatedByUser2.getNetworkId()));
        } catch (Exception e) {
            exception = e;
        }
        Assert.assertTrue("Failure - tempUser has not shared the communication type specified by network ",
                exception instanceof BadRequestException);
    }

    /**
     * Method   -   joinNetwork
     * TestCase -   Failure
     * Description  -   Join request cannot be sent to private networks.
     */
    @Test
    public void testSendingJoinRequestToPrivateNetwork() {
        Exception exception = null;
        //  user1 sends join request to private network created by user 3
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        try {
            networkService.joinNetwork(MethodStubs.getJoinNetworkRequestDTO(privateNetworkByUser3.getNetworkId()));
        } catch (Exception e) {
            exception = e;
        }
        Assert.assertTrue("Failure - Join request cannot be sent to private networks",
                exception instanceof BadRequestException);
    }

    /**
     * Method   -   removeMemberFromNetwork
     * TestCase -   Success
     * Owner or admin can remove the member from the network.
     */
    @Test
    public void testRemovingMemberFromNetwork() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user2);
        UserNetworkDetails userNetworkDetails;

        userNetworkDetails = networkService.getNetworkDetails(networkCreatedByUser2.getNetworkId());

        Assert.assertEquals("user2 created network has 3 members ", 3, userNetworkDetails.getNetworkDetails().getMemberCount());

        networkService.removeMemberFromNetwork(MethodStubs.getRemoveMemberDTO(Arrays.asList(user1.getUserId()),
                networkCreatedByUser2.getNetworkId()));

        userNetworkDetails = networkService.getNetworkDetails(networkCreatedByUser2.getNetworkId());

        Assert.assertEquals("Success - a member has been removed by owner ", 2, userNetworkDetails.getNetworkDetails().getMemberCount());
    }

    /**
     * Method   -   removeMemberFromNetwork
     * TestCase -   Failure
     * Owner or admin can remove the member from the network,
     * If a member tries to remove other members exception will be thrown
     */
    @Test
    public void testRemovingMemberFromNetworkByAnotherMember() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);

        Exception exception = null;
        try {
            networkService.removeMemberFromNetwork(MethodStubs.getRemoveMemberDTO(
                    Arrays.asList(user4.getUserId()), networkCreatedByUser2.getNetworkId()));
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Failure - only owner or admin can remove members from network ",
                exception instanceof BadRequestException);
    }

    /**
     * Method   -   leaveNetwork
     * TestCase -   Success
     */
    @Test
    public void testLeavingNetwork() {

        given(this.tokenAuthService.getSessionUser()).willReturn(user4);
        UserNetworkDetails userNetworkDetails;

        LeaveNetworkRequestDTO leaveNetwork = new LeaveNetworkRequestDTO();
        userNetworkDetails = networkService.getNetworkDetails(networkCreatedByUser2.getNetworkId());
        Assert.assertTrue("User4 is a admin of networkCreated by user2, in networkDetails section user role " +
                "will be admin", userNetworkDetails.getMemberRole().equalsIgnoreCase(
                NetworkMemberRole.ADMIN.getValue()));

        leaveNetwork.setNetworkId(networkCreatedByUser2.getNetworkId());
        networkService.leaveNetwork(leaveNetwork);

        userNetworkDetails = networkService.getNetworkDetails(networkCreatedByUser2.getNetworkId());

        Assert.assertTrue("Success - user has left the network and in network details section it will be" +
                " shown as guest", userNetworkDetails.getMemberRole().equalsIgnoreCase(
                NetworkMemberRole.GUEST.getValue()));

    }

    /**
     * Method   -   leaveNetwork
     * TestCase -   Failure
     * Owner cannot leave network until they transfer ownership to one of the admins of network
     */
    @Test
    public void testLeavingNetworkByOwner() {

        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        UserNetworkDetails userNetworkDetails;

        userNetworkDetails = networkService.getNetworkDetails(networkCreatedByUser1.getNetworkId());
        Assert.assertTrue("User1 is a Owner of network, in networkDetails section user role will be owner",
                userNetworkDetails.getMemberRole().equalsIgnoreCase(NetworkMemberRole.OWNER.getValue()));


        Exception exception = null;
        try {
            networkService.leaveNetwork(MethodStubs.getLeaveNetworkDTO(networkCreatedByUser1.getNetworkId()));
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Owner can not leave the network until they transfer the ownership",
                exception instanceof BadRequestException);

    }

    /**
     * Method   -   reportNetwork
     * TestCase -   Success
     */
    @Test
    public void testReportingNetwork() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user4);

        ReportNetworkRequestDTO reportNetworkRequest = new ReportNetworkRequestDTO();
        reportNetworkRequest.setNetworkId(networkCreatedByUser1.getNetworkId());
        reportNetworkRequest.setReportMessage("No BroadCasts from this network");

        networkService.reportNetwork(reportNetworkRequest);

        List<ReportedData> reportedData = reportedUserDataRepository.findAll();

        Assert.assertTrue("Report has been raised for network by user4",
                reportedData.get(0).getReportDataType().equals(ReportDataType.NETWORK));
    }

    /**
     * Method   -   demoteAdmins
     * TestCase -   Success
     * Owner has the authority to demote admins
     */
    @Test
    public void testDemotingAdmin() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user2);
        UserNetworkDetails userNetworkDetails;

        userNetworkDetails = networkService.getNetworkDetails(networkCreatedByUser2.getNetworkId());
        Assert.assertEquals("User2 is the owner/admin and user4 is the admin, total admin count will be 2 ",
                2, userNetworkDetails.getNetworkDetails().getAdminCount());

        //  user2 being owner demote user4
        networkService.demoteAdmins(MethodStubs.getDemoteAdminDTO(networkCreatedByUser2.getNetworkId(),
                Arrays.asList(user4.getUserId())));

        //  user2 is the only owner/admin
        userNetworkDetails = networkService.getNetworkDetails(networkCreatedByUser2.getNetworkId());
        Assert.assertEquals("user 4 has been demoted by owner of the network", 1,
                userNetworkDetails.getNetworkDetails().getAdminCount());

    }

    /**
     * Method   -   demoteAdmins
     * TestCase -   Failure
     * Only Owner has the authority to demote admins, if any other member tries to perform the action an exception will
     * thrown
     */
    @Test
    public void testDemotingAdminByOtherMembers() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        UserNetworkDetails userNetworkDetails;

        userNetworkDetails = networkService.getNetworkDetails(networkCreatedByUser2.getNetworkId());
        Assert.assertEquals("User2 is the owner/admin and user4 is the admin, total admin count will be 2 ",
                2, userNetworkDetails.getNetworkDetails().getAdminCount());
        Exception exception = null;
        try {
            //  user1 being member tries to demote user4
            networkService.demoteAdmins(MethodStubs.getDemoteAdminDTO(networkCreatedByUser2.getNetworkId(),
                    Arrays.asList(user4.getUserId())));
        } catch (Exception e) {
            exception = e;
        }
        Assert.assertTrue("Only owner can perform this action ", exception instanceof BadRequestException);

    }

    /**
     * Method   -   demoteAdmins
     * TestCase -   Failure
     * Owner cannot be demoted
     */
    @Test
    public void testDemotingOwner() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user2);
        UserNetworkDetails userNetworkDetails;

        userNetworkDetails = networkService.getNetworkDetails(networkCreatedByUser2.getNetworkId());
        Assert.assertEquals("User2 is the owner/admin and user4 is the admin, total admin count will be 2 ",
                2, userNetworkDetails.getNetworkDetails().getAdminCount());
        Exception exception = null;
        try {
            //  user1 being member tries to demote user4
            networkService.demoteAdmins(MethodStubs.getDemoteAdminDTO(networkCreatedByUser2.getNetworkId(),
                    Arrays.asList(user2.getUserId())));
        } catch (Exception e) {
            exception = e;
        }
        Assert.assertTrue("Owner cannot be demoted by any admin/member ",
                exception instanceof BadRequestException);

    }

    /**
     * Method   -   inviteMembersToNetwork
     * TestCase -   Success
     */
    @Test
    public void testInvitingContactsToNetwork() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user3);


        networkService.inviteMembersToNetwork(MethodStubs.getNetworkInvitationDTO(privateNetworkByUser3.getNetworkId(),
                Arrays.asList(connection_U3_U4.getConnectionId())));

        given(this.tokenAuthService.getSessionUser()).willReturn(user4);
        ActivityListResponse activityCreatedForUser = userActivityService.getActivitiesCreatedForUser(0, 5);

        Assert.assertEquals("User4 has received one activity ", 1, activityCreatedForUser.getTotalElements());
        Assert.assertTrue("Success  -   user 4 has received invitation to join network by user 3 ",
                activityCreatedForUser.getUserActivityList().get(0).getActivityDetails().getActivityType()
                        .getRequestType().equals(RequestType.NETWORK_MEMBER_INVITE));

    }

    /**
     * Method   -   inviteMembersToNetwork
     * TestCase -   Failure
     * Only owner or admin can invitee others to network, exception will be thrown if member tries to invitee
     */
    @Test
    public void testInvitingContactsToNetworkByMemberOfNetwork() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        Exception exception = null;

        try {
            networkService.inviteMembersToNetwork(MethodStubs.getNetworkInvitationDTO(
                    networkCreatedByUser2.getNetworkId(), Arrays.asList(connection_U1_U3.getConnectionId())));
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("User1 is just a member of the network and can not send invitee ",
                exception instanceof BadRequestException);

    }

    /**
     * Method   -   inviteMembersToNetwork
     * TestCase -   Failure
     * Only owner or admin can invitee others to network, exception will be thrown if member tries to invitee
     */
    @Test
    public void testInvitingContactsAlreadyMemberOfNetwork() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user2);
        Exception exception = null;

        try {
            networkService.inviteMembersToNetwork(MethodStubs.getNetworkInvitationDTO(
                    networkCreatedByUser2.getNetworkId(), Arrays.asList(connection_U2_U1.getConnectionId())));
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("User1 is already a member of the network  and no invitee will be sent",
                exception instanceof BadRequestException);

    }

    /**
     * Method   -   transferOwnership
     * TestCase -   Success
     * Transfer of network ownership can be done to network admins
     */
    @Test
    public void testTransferringOwnership() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user2);
        UserNetworkDetails userNetworkDetails;

        userNetworkDetails = networkService.getNetworkDetails(networkCreatedByUser2.getNetworkId());
        Assert.assertTrue("User 2 is owner of the network ", userNetworkDetails.getMemberRole().equalsIgnoreCase(
                NetworkMemberRole.OWNER.getValue()));

        networkService.transferOwnership(MethodStubs.getOwnerShipTransferDTO(networkCreatedByUser2.getNetworkId(),
                user4.getUserId()));

        userNetworkDetails = networkService.getNetworkDetails(networkCreatedByUser2.getNetworkId());
        Assert.assertTrue("Success  - user2 after transferring the ownership becomes admin of the network",
                userNetworkDetails.getMemberRole().equalsIgnoreCase(NetworkMemberRole.ADMIN.getValue()));

        given(this.tokenAuthService.getSessionUser()).willReturn(user4);
        userNetworkDetails = networkService.getNetworkDetails(networkCreatedByUser2.getNetworkId());
        Assert.assertTrue("Success - user4 is new owner of the networkCreatedByUser2",
                userNetworkDetails.getMemberRole().equalsIgnoreCase(NetworkMemberRole.OWNER.getValue()));

    }

    /**
     * Method   -   transferOwnership
     * TestCase -   Failure
     * Transfer of network ownership can be done to network admins and not to members, exception will be thrown if
     * transferring to member
     */
    @Test
    public void testTransferringOwnershipToMember() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user2);

        Exception exception = null;
        try {
            /*  User2 is the owner of the network and trying to transfer to user1  who is just a member of the network  */
            networkService.transferOwnership(MethodStubs.getOwnerShipTransferDTO(networkCreatedByUser2.getNetworkId(),
                    user1.getUserId()));
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Failure - ownership can only be transferred to admins",
                exception instanceof BadRequestException);

    }

    /**
     * Method   -   transferOwnership
     * TestCase -   Failure
     */
    @Test
    public void testTransferringOwnershipToCurrentOwner() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user2);

        Exception exception = null;
        try {
            /*  User2 is the owner of the network and trying to transfer to back to them    */
            networkService.transferOwnership(MethodStubs.getOwnerShipTransferDTO(networkCreatedByUser2.getNetworkId(),
                    user2.getUserId()));
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Failure - Owner cannot transfer ownership to themselves",
                exception instanceof BadRequestException);

    }

    /**
     * Method   -   shareNetwork
     * TestCase -   Success
     * Any member can share network with other Watu users
     */
    @Test
    public void testSharingNetwork() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user3);

        networkService.shareNetwork(MethodStubs.getNetworkShareDTO(publicNetworkByUser3.getNetworkId(),
                Arrays.asList(connection_U3_U4.getConnectionId())));

        given(this.tokenAuthService.getSessionUser()).willReturn(user4);
        ActivityListResponse activityCreatedForUser = userActivityService.getActivitiesCreatedForUser(0, 5);

        Assert.assertEquals("User4 has received one activity ", 1,
                activityCreatedForUser.getTotalElements());
        Assert.assertTrue("Success  -   user 4 has received activity of a network being shared by user3 ",
                activityCreatedForUser.getUserActivityList().get(0).getActivityDetails().getActivityType()
                        .getRequestType().equals(RequestType.NETWORK_SHARE));
    }

    /**
     * Method   -   shareNetwork
     * TestCase -   Failure
     * Private network can not be share with any one
     */
    @Test
    public void testSharingPrivateNetwork() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user3);

        Exception exception = null;
        try {
            networkService.shareNetwork(MethodStubs.getNetworkShareDTO(privateNetworkByUser3.getNetworkId(),
                    Arrays.asList(connection_U3_U4.getConnectionId())));
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Failure  -   Private Networks can not be shared  ",
                exception instanceof BadRequestException);
    }

    /**
     * Method   -   shareNetwork
     * TestCase -   Failure
     * Sharing network can be done only once per contact for a network
     */
    @Test
    public void testSharingNetworkTwiceToSameContact() {
        // user 3 shares a network with user4
        given(this.tokenAuthService.getSessionUser()).willReturn(user3);
        networkService.shareNetwork(MethodStubs.getNetworkShareDTO(publicNetworkByUser3.getNetworkId(),
                Arrays.asList(connection_U3_U4.getConnectionId())));

        //user 4 receive the network request activity
        given(this.tokenAuthService.getSessionUser()).willReturn(user4);
        ActivityListResponse activityCreatedForUser = userActivityService.getActivitiesCreatedForUser(0, 5);

        Assert.assertEquals("User4 has received one activity ", 1,
                activityCreatedForUser.getTotalElements());
        Assert.assertTrue("Success  -   user 4 has received activity of a network being shared by user3 ",
                activityCreatedForUser.getUserActivityList().get(0).getActivityDetails().getActivityType()
                        .getRequestType().equals(RequestType.NETWORK_SHARE));

        Exception exception = null;

        try {
            // user3 tries to share the same network again with user 4
            given(this.tokenAuthService.getSessionUser()).willReturn(user3);
            networkService.shareNetwork(MethodStubs.getNetworkShareDTO(publicNetworkByUser3.getNetworkId(),
                    Arrays.asList(connection_U3_U4.getConnectionId())));
        } catch (Exception e) {
            exception = e;
        }
        Assert.assertTrue("Failure - sharing network twice will result in exception",
                exception instanceof BadRequestException);
    }

    /**
     * Method   -   acceptInvitation
     * TestCase -   Success
     * User accepting invitation will become a member of the network
     */
    @Test
    public void testAcceptingInvitationToJoinNetwork() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user3);

        networkService.inviteMembersToNetwork(MethodStubs.getNetworkInvitationDTO(privateNetworkByUser3.getNetworkId(),
                Arrays.asList(connection_U3_U4.getConnectionId())));

        given(this.tokenAuthService.getSessionUser()).willReturn(user4);
        ActivityListResponse activityCreatedForUser = userActivityService.getActivitiesCreatedForUser(0, 5);

        Assert.assertEquals("User4 has received one activity ", 1, activityCreatedForUser.getTotalElements());
        String networkInvitationActivityId = activityCreatedForUser.getUserActivityList().get(0)
                .getActivityDetails().getActivityId();

        AcceptInvitationRequestDTO acceptInvitation = new AcceptInvitationRequestDTO();
        acceptInvitation.setActivityId(networkInvitationActivityId);

        //  user4 accepts invitation
        networkService.acceptInvitation(acceptInvitation);

        UserNetworkDetails userNetworkDetails;

        userNetworkDetails = networkService.getNetworkDetails(privateNetworkByUser3.getNetworkId());

        Assert.assertEquals("Number of member for network is increased by 1 after user 4 accept the invitation",
        2, userNetworkDetails.getNetworkDetails().getMemberCount());
        Assert.assertTrue("Success  -   user 4 is now a member of the private network created by user3",
                userNetworkDetails.getMemberRole().equalsIgnoreCase(NetworkMemberRole.MEMBER.getValue()));

    }

    /**
     * Method   -   acceptInvitation
     * TestCase -   Failure
     */
    @Test
    public void testAcceptingInvalidInvitation() {

        given(this.tokenAuthService.getSessionUser()).willReturn(user4);
        Exception exception = null;
        ActivityListResponse activityCreatedForUser = userActivityService.getActivitiesCreatedForUser(0, 5);

        Assert.assertEquals("User4 has received no activity ", 0, activityCreatedForUser.getTotalElements());

        AcceptInvitationRequestDTO acceptInvitation = new AcceptInvitationRequestDTO();
        acceptInvitation.setActivityId(new ObjectId().toString());

        try {
            //  user4 accepts invitation
            networkService.acceptInvitation(acceptInvitation);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Failure  -   user is trying perform action on invalid activity",
                exception instanceof BadRequestException);

    }

    /**
     * Method   -   getRecommendedNetworks
     * TestCase -   Success
     * Response back with list of recommended networks which is most popular
     */
    @Test
    public void testGetRecommendedNetworksWhichIsMostPopular() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user4);
        RecommendedNetworksResponseDTO recommendedNetworksResponseDTO = networkService.getRecommendedNetworks("Sports");
        Assert.assertEquals("Success - list out the most popular network for category sports", 2,
                recommendedNetworksResponseDTO.getMostPopular().size());
    }

    /**
     * Method   -   getRecommendedNetworks
     * TestCase -   Success
     * Response back with list of recently active networks
     */
    @Test
    public void testGetRecommendedNetworksWhichIsMoreActive() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user4);
        //adding network to recently active
        recentActiveNetworkRepository.save(MethodStubs.getRecentActiveNetworks(
                publicNetworkByUser3.getNetworkId(), publicNetworkByUser3.getNetworkCategory()));
        RecommendedNetworksResponseDTO recommendedNetworksResponseDTO = networkService.getRecommendedNetworks("Sports");
        Assert.assertEquals("Success - list out the networks,which is recently active",
                1, recommendedNetworksResponseDTO.getSuggestion().size());
    }

    @After
    public void tearDown() {
        networkMemberRepository.deleteAll();
        networkRepository.deleteAll();
        userActivityRepository.deleteAll();
        registeredNumberRepository.deleteAll();
        userConnectionRepository.deleteAll();
        userPrivacyProfileRepository.deleteAll();
        peopleUserRepository.deleteAll();
        recentActiveNetworkRepository.deleteAll();
        networkCategoryRepository.deleteAll();
    }
}
