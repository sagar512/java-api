package com.peopleapp.service;

import com.peopleapp.configuration.LocaleMessageReader;
import com.peopleapp.dto.UserGroupData;
import com.peopleapp.dto.requestresponsedto.*;
import com.peopleapp.exception.BadRequestException;
import com.peopleapp.model.PeopleUser;
import com.peopleapp.model.UserConnection;
import com.peopleapp.model.UserGroup;
import com.peopleapp.repository.PeopleUserRepository;
import com.peopleapp.repository.UserConnectionRepository;
import com.peopleapp.repository.UserGroupRepository;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
@SpringBootTest
public class UserGroupServiceTest {

    @Inject
    private PeopleUserRepository peopleUserRepository;

    @Inject
    private UserGroupService userGroupService;

    @Inject
    private UserGroupRepository userGroupRepository;

    @Inject
    private UserConnectionRepository userConnectionRepository;


    @Inject
    private UserConnectionService userConnectionService;

    @Inject
    private LocaleMessageReader messages;

    @MockBean
    private TokenAuthService tokenAuthService;

    private PeopleUser user1;
    private PeopleUser user2;
    private PeopleUser user3;

    private UserConnection userConnection1;
    private UserConnection userConnection2;

    private UserGroup groupCreatedByUser1;

    @Before
    public void setUp() {
        user1 = peopleUserRepository.save(MethodStubs.getWatuUserAccount("9888888888",
                "testuser1@mailinator.com", "TestUser", "1", "Watu"));
        user2 = peopleUserRepository.save(MethodStubs.getWatuUserAccount("9777777777",
                "testuser2@mailinator.com", "TestUser", "2", "Watu"));
        user3 = peopleUserRepository.save(MethodStubs.getWatuUserAccount("9666666666",
                "testuser3@mailinator.com", "TestUser", "3", "Watu"));

        // creating groups for user3
        userGroupRepository.save(MethodStubs.getUserGroupWithContactsAdded(user3.getUserId(), new ArrayList<>()));
        userGroupRepository.save(MethodStubs.getUserGroupWithContactsAdded(user3.getUserId(), new ArrayList<>()));

        userConnection1 = userConnectionRepository.save(MethodStubs.getConnectionObj(user1.getUserId(), user2.getUserId()));
        userConnection2 = userConnectionRepository.save(MethodStubs.getConnectionObj(user2.getUserId(), user1.getUserId()));

        groupCreatedByUser1 = MethodStubs.getUserGroupWithContactsAdded(user1.getUserId(),
                Arrays.asList(userConnection1.getConnectionId()));
        userGroupRepository.save(groupCreatedByUser1);
    }

    @Test
    public void testAddGroup() {
        String message = null;
        AddUserGroupResponseDTO addUserGroupResponseDTO = new AddUserGroupResponseDTO();
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        List<UserGroupData> userGroupDataList = MethodStubs.getUserGroupData();
        UserGroupRequestDTO userGroupRequestDTO = new UserGroupRequestDTO();
        userGroupRequestDTO.setUserGroupList(userGroupDataList);
        Exception exception = null;

        try {
            addUserGroupResponseDTO = userGroupService.addGroups(userGroupRequestDTO);
        } catch (BadRequestException e) {
            exception = e;
        }
        int count = addUserGroupResponseDTO.getUserGroupList().size();
        Assert.assertEquals("failure - expected persisted user group list size 2", 2, count);

    }


    /**
     * Method   -   updateGroupFavouriteValue
     * TestCase -   Success
     * user can mark group created by them as favourite/un-Favourite
     */
    @Test
    public void testUpdateFavouriteGroup() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        Exception exception = null;

        UpdateGroupFavouriteRequestDTO requestDTO = new UpdateGroupFavouriteRequestDTO();
        requestDTO.setFavouriteGroups(Arrays.asList(MethodStubs.getFavouriteGroupDTO(
                groupCreatedByUser1.getGroupId(), true)));
        try {
            userGroupService.updateGroupFavouriteValue(requestDTO);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertNull("Success - Group is marked as favourite.", exception);
    }

    @Test
    public void testEditGroup() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);

        /*  Checks before making edit group call */
        FetchUserGroupResponseDTO userGroups = userGroupService.fetchUserGroups(0, 1);

        Assert.assertEquals(" User 1 has created only one group ", 1, userGroups.getUserGroupList().size());
        Assert.assertEquals("Title of the group before editing ", "Group title",
                groupCreatedByUser1.getTitle());

        EditUserGroupRequestDTO editUserGroup = new EditUserGroupRequestDTO();
        editUserGroup.setUserGroupsToBeEdited(Arrays.asList(MethodStubs.getEditGroupDTO(
                userGroups.getUserGroupList().get(0).getGroupId(),
                "Watu group", "watuGroupImage", Arrays.asList(userConnection1.getConnectionId()),
                "This group is used to validate edit api functionality")));

        EditGroupResponse editGroupResponse = userGroupService.editGroup(editUserGroup);

        Assert.assertEquals("Success - number of groups being edited is only one",
                1, editGroupResponse.getGroupDetails().size());
        Assert.assertEquals("Title of the group was edited", "Watu group", editGroupResponse.getGroupDetails().get(0).getTitle());

    }

    /**
     * Method: fetchUserGroups
     * Test Case: Success
     * Testing when next link should not be null
     */
    @Test
    public void testFetchUserGroupsLinkNotNull() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user3);
        FetchUserGroupResponseDTO responseDTO = userGroupService.fetchUserGroups(0, 1);

        Assert.assertNotNull("Success - User Group is present", responseDTO.getUserGroupList());
        Assert.assertEquals("Success - Total groups count matching", 2, responseDTO.getTotalElements());
        Assert.assertNotNull("Success - Next link exists", responseDTO.getNextURL());
    }

    /**
     * Method: fetchUserGroups
     * Test Case: Success
     * Testing when next link should be null
     */
    @Test
    public void testFetchUserGroupsLinkNull() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user3);
        FetchUserGroupResponseDTO responseDTO = userGroupService.fetchUserGroups(0, 2);

        Assert.assertNotNull("Success - User Group is present", responseDTO.getUserGroupList());
        Assert.assertEquals("Success - Total groups count matching", 2, responseDTO.getTotalElements());
        Assert.assertNull("Success - Next link is null", responseDTO.getNextURL());
    }

    /**
     * Method - fetchFavouriteGroups
     * TestCase - success
     * user can list down all the group they have marked as favourite
     */
    @Test
    public void testFetchingFavoriteGroups() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        FetchUserGroupResponseDTO favouriteGroups;
        /*  Before marking group as favourite */
        favouriteGroups = userGroupService.fetchFavouriteGroups();
        Assert.assertTrue("No groups has been marked as favorite",
                PeopleUtils.isNullOrEmpty(favouriteGroups.getUserGroupList()));

        UpdateGroupFavouriteRequestDTO requestDTO = new UpdateGroupFavouriteRequestDTO();
        requestDTO.setFavouriteGroups(Arrays.asList(MethodStubs.getFavouriteGroupDTO(
                groupCreatedByUser1.getGroupId(), true)));

        userGroupService.updateGroupFavouriteValue(requestDTO);

        favouriteGroups = userGroupService.fetchFavouriteGroups();
        Assert.assertTrue("Success - user1 successfully marked group as favourite ",
                favouriteGroups.getUserGroupList().get(0).getIsFavourite());
    }

    /**
     * Method   -   deleteUserGroups
     * TestCase -   Success
     */
    @Test
    public void testDeletingGroup() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        FetchUserGroupResponseDTO userGroups;
        /* user1 has created one group */
        userGroups = userGroupService.fetchUserGroups(0, 1);
        Assert.assertEquals("User has created only one group", 1, userGroups.getUserGroupList().size());

        userGroupService.deleteUserGroups(MethodStubs.getDeleteGroupRequest(
                Arrays.asList(groupCreatedByUser1.getGroupId())));

        userGroups = userGroupService.fetchUserGroups(0, 1);

        Assert.assertEquals("Success - user has successfully deleted the only group that was created",
                0, userGroups.getUserGroupList().size());
    }

    /**
     * Method   -   updateGroupImage
     * TestCase -   Success
     */
    @Test
    public void testUpdatingGroupImage() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        FetchUserGroupResponseDTO userGroups;
        /* user1 has created one group with S3-URL as image url*/
        userGroups = userGroupService.fetchUserGroups(0, 1);
        Assert.assertEquals("Image url before making update call", "S3-URL",
                userGroups.getUserGroupList().get(0).getImageURL());

        UpdateGroupImageRequestDTO updateGroupImageRequest = new UpdateGroupImageRequestDTO();
        updateGroupImageRequest.setListOfGroupImages(Arrays.asList(
                MethodStubs.getGroupImage(groupCreatedByUser1.getGroupId(), "updatedImage")));

        userGroupService.updateGroupImage(updateGroupImageRequest);

        userGroups = userGroupService.fetchUserGroups(0, 1);
        Assert.assertNotEquals("Success - Image url is changes", "S3-URL",
                userGroups.getUserGroupList().get(0).getImageURL());

    }

    @After
    public void tearDown() {
        peopleUserRepository.delete(user1);
        peopleUserRepository.delete(user2);
        peopleUserRepository.delete(user3);
        userConnectionRepository.delete(userConnection1);
        userConnectionRepository.delete(userConnection2);
        userGroupRepository.deleteAll();
    }


}
