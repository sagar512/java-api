package com.peopleapp.service;

import com.peopleapp.dto.UserInformationDTO;
import com.peopleapp.dto.UserProfileData;
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
public class MasterServiceTest extends BaseTest {
    @Inject
    private UserPrivacyProfileRepository userPrivacyProfileRepository;

    @Inject
    private PeopleUserRepository peopleUserRepository;

    @Inject
    private UserConnectionRepository userConnectionRepository;

    @Inject
    private MasterService masterService;

    @MockBean
    private TokenAuthService tokenAuthService;


    private PeopleUser user1;
    private PeopleUser user2;

    private UserPrivacyProfile userProfile1;
    private UserPrivacyProfile userProfile2;
    private UserConnection userConnection12;
    private UserConnection userConnection21;

    @Before
    public void setUp() {

        user1 = peopleUserRepository.save(MethodStubs.getUserObject("1234567890", "testuser1"));
        user2 = peopleUserRepository.save(MethodStubs.getUserObject("1212121212", "testuser2"));

        // adding valueId in User Obj and User Privacy Profile
        List<String> valueIds = new ArrayList<>();
        List<UserProfileData> existingMetadata = new ArrayList<>();
        List<UserProfileData> metadataList = Collections.singletonList( MethodStubs.getUserProfileDataForContactNumber());
        for (UserProfileData newData : PeopleUtils.emptyIfNull(metadataList)) {
            String valueId = new ObjectId().toString();
            newData.setValueId(valueId);
            newData.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
            existingMetadata.add(newData);
            valueIds.add(valueId);
        }
        user1.setUserMetadataList(existingMetadata);
        user2.setUserMetadataList(existingMetadata);

        userProfile1 = userPrivacyProfileRepository.save(MethodStubs.getUserPrivacyProfileObj(user1.getUserId(), valueIds));
        userProfile2 = userPrivacyProfileRepository.save(MethodStubs.getUserPrivacyProfileObj(user2.getUserId(), valueIds));

        userConnection12 = userConnectionRepository.save(MethodStubs.getConnectionWithProfileAndContactStaticObj(
                user1.getUserId(), user2.getUserId(), userProfile1.getPrivacyProfileId()));
        userConnection21 = userConnectionRepository.save(MethodStubs.getConnectionWithProfileAndContactStaticObj(
                user1.getUserId(), user2.getUserId(), userProfile2.getPrivacyProfileId()));


        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
    }


    /**
     * Method: mergeSharedInfoToStaticInfo
     * Test: Success Case
     */
    @Test
    public void testMergeSharedInfoToStaticInfo(){
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);

        List<UserConnection> userConnections = userConnectionRepository.getConnectionDataWithProfileForSelectedContact(
                        user1.getUserId(), new ArrayList<>(Arrays.asList(userConnection12.getConnectionId())));

        UserConnection userConnection = userConnections.get(0);
        UserInformationDTO sharedInfo = masterService.prepareSharedData(userConnection);
        masterService.mergeSharedInfoToStaticInfo(sharedInfo, userConnection.getContactStaticData());

        Assert.assertEquals("Success - First Name updated from sharedInfo",
                userConnection.getContactStaticData().getFirstName(), sharedInfo.getFirstName()
        );
        Assert.assertEquals("Success - Name updated from sharedInfo",
                userConnection.getContactStaticData().getName(), sharedInfo.getName());
    }

    @After
    public void tearDown() {
        userConnectionRepository.deleteAll();
        userPrivacyProfileRepository.deleteAll();
        peopleUserRepository.deleteAll();
    }
}
