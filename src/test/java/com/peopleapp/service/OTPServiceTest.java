package com.peopleapp.service;

import com.peopleapp.dto.ContactNumberDTO;
import com.peopleapp.dto.SQSPayload;
import com.peopleapp.model.TemporarySession;
import com.peopleapp.security.TokenAuthService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import javax.inject.Inject;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

@RunWith(SpringRunner.class)
@SpringBootTest
public class OTPServiceTest extends BaseTest {

    @Inject
    private OTPService otpService;

    @MockBean
    private TokenAuthService tokenAuthService;

    @MockBean
    private QueueService queueServiceMock;

    private TemporarySession temporarySession;

    @Before
    public void setUp() {

        ContactNumberDTO contactNumberDTO = MethodStubs.getContactNumberDTO("+1", "1111");
        temporarySession = MethodStubs.getTemporarySessionObj("1111", contactNumberDTO);

        given(this.tokenAuthService.getTempToken()).willReturn(temporarySession.getTemporaryToken());
        given(this.tokenAuthService.getTempSessionByTempToken(anyString())).willReturn(temporarySession);

        doNothing().when(queueServiceMock).sendPayloadToSQS(isA(SQSPayload.class));
        doNothing().when(queueServiceMock).sendPayloadToSQS(anyList());
    }


    /**
     * Method: generateOtpForUser
     * Test: Success Case - Default OTP Flow
     */
    @Test
    public void testGenerateOtpForUserDefault() {
        ReflectionTestUtils.setField(otpService, "defaultOtp", "1111");
        String otp = otpService.generateOtpForUser();
        Assert.assertEquals("Succes - default OTP", "1111", otp);
    }

    /**
     * Method: generateOtpForUser
     * Test: Success Case
     */
    @Test
    public void testGenerateOtpForUser() {
        ReflectionTestUtils.setField(otpService, "defaultOtp", "");

        String otp = otpService.generateOtpForUser();

        Assert.assertNotEquals("Succes - OTP is not default", "1111", otp);
    }

    /**
     * Method: sendOTP
     * Test: Success Case
     */
    public void testSendOTP() {
        ReflectionTestUtils.setField(otpService, "defaultOtp", "");

        ContactNumberDTO contactNumber = MethodStubs.getContactNumberDTO("+1", "11111");
        otpService.sendOTP("1234", contactNumber);
        /* This method is not having any assertion statement,because there is nothing
        to check in the response.
         */
    }

    /**
     * Method: checkIfOTPValid
     * Test: Success Case
     */
    @Test
    public void testCheckIfOTPValid() {
        Exception exception = null;
        ReflectionTestUtils.setField(otpService, "defaultOtp", "");

        try {
            otpService.checkIfOTPValid("1111", temporarySession);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertNull("Success - OTP matches", exception);
    }

    /**
     * Method: checkIfOTPValid
     * Test: Failure Case
     */
    @Test
    public void testCheckIfOTPValidFailure() {
        Exception exception = null;
        ReflectionTestUtils.setField(otpService, "defaultOtp", "");

        try {
            otpService.checkIfOTPValid("1112", temporarySession);
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertNotNull("Success - Expected exception as OTP does not matches", exception);
    }

    /**
     * Method: resendOTP
     * Test: Success Case
     */
    @Test
    public void testResendOTP() {
        Exception exception = null;
        ReflectionTestUtils.setField(otpService, "defaultOtp", "");

        try {
            otpService.resendOTP();
        } catch (Exception e) {
            exception = e;
        }

        Assert.assertNull("Success", exception);
    }

    @After
    public void tearDown() {
    }
}
