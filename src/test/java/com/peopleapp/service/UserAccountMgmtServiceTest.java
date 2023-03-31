package com.peopleapp.service;

import com.peopleapp.dto.requestresponsedto.DeleteAccountRequest;
import com.peopleapp.enums.UserStatus;
import com.peopleapp.model.PeopleUser;
import com.peopleapp.model.UserConnection;
import com.peopleapp.repository.PeopleUserRepository;
import com.peopleapp.repository.UserConnectionRepository;
import com.peopleapp.repository.UserSessionRepository;
import com.peopleapp.security.TokenAuthService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;

import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
@SpringBootTest
public class UserAccountMgmtServiceTest extends BaseTest {

    @MockBean
    private TokenAuthService tokenAuthService;

    @Inject
    private UserAccountMgmtService userAccountMgmtService;

    @Inject
    private PeopleUserRepository peopleUserRepository;

    @Inject
    private UserConnectionRepository userConnectionRepository;

    @Inject
    private UserSessionRepository userSessionRepository;

    private PeopleUser user1;

    @Before
    public void setUp() {
        user1 = peopleUserRepository.save(MethodStubs.getUserObject("9000000001", "testuser"));

    }

    /**
     * Method - deleteUserAccount
     * TestCase - Success
     */
    @Test
    public void testDeleteUserAccount() {
        // kept inside the function, since once user deleted, will not be present to be reused
        PeopleUser toBeDeletedUser = peopleUserRepository.save(MethodStubs.getUserObject("9999999991",
                "toBeDeletedUser"));
        userSessionRepository.save(MethodStubs.getUserSession(toBeDeletedUser.getUserId()));
        userConnectionRepository.save(MethodStubs.getConnectionObj(user1.getUserId(), toBeDeletedUser.getUserId()));
        userConnectionRepository.save(MethodStubs.getConnectionObj(toBeDeletedUser.getUserId(), user1.getUserId()));

        given(this.tokenAuthService.getSessionUser()).willReturn(toBeDeletedUser);

        DeleteAccountRequest request = MethodStubs.getDeleteAccountRequest();
        userAccountMgmtService.deleteUserAccount(request);

        PeopleUser resultUser = peopleUserRepository.findByuserId(toBeDeletedUser.getUserId(),
                UserStatus.ACTIVE.getValue());

        Assert.assertNull("Success - User deleted", resultUser);
    }

    @After
    public void tearDown() {
        userConnectionRepository.deleteAll();
        peopleUserRepository.deleteAll();
    }
}
