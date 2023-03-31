package com.peopleapp.service;

import com.peopleapp.dto.SQSPayload;
import com.peopleapp.dto.requestresponsedto.ConnectRequestDTO;
import com.peopleapp.dto.requestresponsedto.GetAmbassadorDetailsResponseDTO;
import com.peopleapp.dto.requestresponsedto.SendReferralRequestDTO;
import com.peopleapp.exception.BadRequestException;
import com.peopleapp.model.PeopleAmbassador;
import com.peopleapp.model.PeopleUser;
import com.peopleapp.repository.AmbassadorRepository;
import com.peopleapp.repository.PeopleUserRepository;
import com.peopleapp.security.TokenAuthService;
import com.peopleapp.util.PeopleUtils;
import com.peopleapp.util.TokenGenerator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AmbassadorServiceTest {

    @MockBean
    private TokenAuthService tokenAuthService;

    @Inject
    private PeopleUserRepository peopleUserRepository;

    @Inject
    private AmbassadorService ambassadorService;

    @Inject
    private AmbassadorRepository ambassadorRepository;

    @Inject
    private PeopleUserService peopleUserService;

    @MockBean
    private QueueService queueServiceMock;

    private PeopleUser user1;
    private PeopleUser user2;
    private SendReferralRequestDTO requestDTO;


    @Before
    public void setUp() {
        user1 = peopleUserRepository.save(MethodStubs.getUserObject("9888888888", "testuser"));
        user2 = peopleUserRepository.save(MethodStubs.getUserObject("9888888889", "testuser2"));
        requestDTO = MethodStubs.getReferralRequest("88778877");

        mockSQSServices();
    }


    /**
     * Method - sendReferralLink
     * TestCase - Success
     * User can refer others by sending referral link
     */
    @Test
    public void testSendingReferral() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        ambassadorService.sendReferralLink(requestDTO);
        PeopleAmbassador referral = ambassadorRepository.findByAmbassadorIdAndReferredContactNumber(
                PeopleUtils.convertStringToObjectId(user1.getUserId()), requestDTO.getReferredContactNumber().getContactNumberDTO());
        Assert.assertNotNull("Success - User has successfully referred Watu app to 88778877 ", referral);
    }

    /**
     * Method - sendReferralLink
     * TestCase - Failure
     * User can send referral link only once to a contact
     */
    @Test
    public void testSendingReferralTwiceToSameContact() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user2);
        ambassadorService.sendReferralLink(requestDTO);
        PeopleAmbassador referral = ambassadorRepository.findByAmbassadorIdAndReferredContactNumber(
                PeopleUtils.convertStringToObjectId(user2.getUserId()), requestDTO.getReferredContactNumber().getContactNumberDTO());
        Assert.assertNotNull("Success - User has successfully referred Watu app to 88778877 ", referral);

        Exception exception = null;
        try {
            ambassadorService.sendReferralLink(requestDTO);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertTrue("Failure - referral can be sent only once to a contact",
                exception instanceof BadRequestException);
    }

    /**
     * Method - sendReferralLink
     * TestCase - Failure
     * User cannot send referral to existing users
     */
    @Test
    public void testSendingReferralToExistingUser() {
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        SendReferralRequestDTO sendRequest = MethodStubs.getReferralRequest("9888888889");
        Exception exception = null;
        try {
            ambassadorService.sendReferralLink(sendRequest);
        } catch (Exception e) {
            exception = e;
        }
        Assert.assertTrue("Failure - referral can not be sent to existing users",
                exception instanceof BadRequestException);
    }

    /**
     * Method - getAmbassadorDetails
     * TestCase - Success
     * Fetches count of total referrals sent, completed and reward points earned.
     */
    @Test
    public void testFetchingUsersAmbassadorDetails() {
        ConnectRequestDTO requestNumber = MethodStubs.getConnectRequestObj();
        //sending referral link
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);
        ambassadorService.sendReferralLink(MethodStubs.getReferralRequest(
                requestNumber.getContactNumber().getPhoneNumber()));
        ambassadorService.sendReferralLink(requestDTO);


        // generate temp session token
        String tempToken = TokenGenerator.generateTempToken();
        given(this.tokenAuthService.getTempToken()).willReturn(tempToken);
        given(this.tokenAuthService.getTempSessionByTempToken(tempToken))
                .willReturn(MethodStubs.getTempSession(requestNumber.getContactNumber().getContactNumberDTO(), tempToken));

        //using the referral link and signing up
        peopleUserService.verifyOTP(MethodStubs.getVerifyOTPRequest(user1.getReferralCode(), "1111"), "1");

        GetAmbassadorDetailsResponseDTO ambassadorDetails = ambassadorService.getAmbassadorDetails();

        Assert.assertEquals("Success - 2 referral was sent by testUser", 2,
                ambassadorDetails.getNumberOfReferralSent());
        Assert.assertEquals("Success - one referral was completed", 1,
                ambassadorDetails.getNumberOfReferralCompleted());
        Assert.assertEquals("Success - total reward points for testUser is 1", 1,
                ambassadorDetails.getTotalRewardPoints());
    }

    private void mockSQSServices() {
        doNothing().when(queueServiceMock).sendPayloadToSQS(isA(SQSPayload.class));
        doNothing().when(queueServiceMock).sendPayloadToSQS(anyList());
    }

    @After
    public void tearDown() {
        ambassadorRepository.deleteAll();
        peopleUserRepository.deleteAll();
    }

}
