package com.peopleapp.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.peopleapp.configuration.LocaleMessageReader;
import com.peopleapp.constant.MessageConstant;
import com.peopleapp.constant.PeopleConstants;
import com.peopleapp.constant.SMSTemplateKeys;
import com.peopleapp.controller.PeopleUserController;
import com.peopleapp.deviceregistration.RegisterDeviceWithSNS;
import com.peopleapp.dto.*;
import com.peopleapp.dto.requestresponsedto.*;
import com.peopleapp.dto.requestresponsedto.QrResponse;
import com.peopleapp.dto.requestresponsedto.VerifyAllDetailsResponse;
import com.peopleapp.enums.*;
import com.peopleapp.exception.BadRequestException;
import com.peopleapp.model.*;
import com.peopleapp.repository.*;
import com.peopleapp.security.TokenAuthService;
import com.peopleapp.service.*;
import com.peopleapp.util.PeopleUtils;
import com.peopleapp.util.TokenGenerator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import javax.inject.Inject;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class PeopleUserServiceImpl implements PeopleUserService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String FIRST_NAME = "firstName.value";
    private static final String LAST_NAME = "lastName.value";

    @Inject
    private LocaleMessageReader messages;

    @Inject
    private TokenAuthService tokenAuthService;

    @Inject
    private PeopleUserRepository peopleUserRepository;

    @Inject
    private UserSessionRepository userSessionRepository;

    @Inject
    private RegisterDeviceWithSNS registerDeviceWithSNS;

    @Inject
    private EmailService emailService;

    @Inject
    private OTPService otpService;

    @Inject
    private VerifyEmailService verifyEmailService;

    @Inject
    private VerifyEmailRepository verifyEmailRepository;

    @Inject
    private PrivacyProfileService privacyProfileService;

    @Inject
    private UserPrivacyProfileRepository userPrivacyProfileRepository;

    @Inject
    private TempSessionService tempSessionService;

    @Inject
    private UserActivityService userActivityService;

    @Inject
    private RegisteredNumberRepository registeredNumberRepository;

    @Inject
    private TemporarySessionRepository temporarySessionRepository;

    @Inject
    private ReportedUserDataRepository reportedUserDataRepository;

    @Inject
    private UserActivityRepository userActivityRepository;

    @Inject
    private UserConnectionRepository userConnectionRepository;

    @Inject
    private MasterService masterService;

    @Inject
    private NotificationService notificationService;

    @Inject
    private TagService tagService;

    @Inject
    private UserConnectionService userConnectionService;

    @Inject
    private AmbassadorRepository ambassadorRepository;

    @Inject
    private QueueService queueService;

    @Inject
    private SystemPrivacyProfileRepository systemPrivacyProfileRepository;

    @Inject
    private NetworkRepository networkRepository;
    
    @Inject
    private SocialMediaCategoryService socialMediaDataService;
    
    @Inject
    private SystemDataService systemDataService;

    @Value("${server.base-path}")
    private String serverBasePath;

    @Value("${app.link}")
    private String appLink;

    @Value("${ambassador.reward.points}")
    private int rewardPoints;

    @Value("${dummy.ios.user.number}")
    private String dummyIOSUserNumber;

    @Value("${dummy.ios.user.otp}")
    private String dummyIOSUserOTP;

    @Value("${dummy.android.user.number}")
    private String dummyAndroidUserNumber;

    @Value("${dummy.android.user.otp}")
    private String dummyAndroidUserOTP;

    @Value("${banch.IO.link}")
    private String banchIOLink;

    @Value("${branch.IO.key}")
    private String branchIOKey;

    @Value("${branch.IO.channel}")
    private String branchIOChannel;

    @Value("${branch.IO.feature}")
    private String branchIOFeature;

    private static final String TWITTER = "PL.02.00";
    private static final String LINKEDIN = "PL.02.01";
    private static final String INSTAGRAM = "PL.02.06";

    @Override
    public PeopleUser findUserByContactNumber(ContactNumberDTO contactNumber) {
        if (contactNumber == null) {
            throw new IllegalArgumentException(MessageConstant.ARGUMENT_NOT_VALID);
        }
        return peopleUserRepository.findByCodeAndNumber(contactNumber.getCountryCode(), contactNumber.getPhoneNumber());
    }

    @Override
    public PeopleUser findUserByUserId(String userId) {
        if (userId == null) {
            throw new IllegalArgumentException(MessageConstant.ARGUMENT_NOT_VALID);
        }
        return peopleUserRepository.findByUserIdAndStatus(userId, UserStatus.ACTIVE);
    }


    @Override
    public String getUserIdByContactNumber(ContactNumberDTO contactNumber) {

        if (contactNumber == null) {
            throw new IllegalArgumentException(MessageConstant.ARGUMENT_NOT_VALID);
        }

        PeopleUser peopleUser = findUserByContactNumber(contactNumber);
        return peopleUser != null ? peopleUser.getUserId() : null;
    }

    @Override
    public SearchByNumberResponseDTO searchGivenContactNumber(ContactNumberDTO numberToBeSearched) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();

        if (sessionUser.getVerifiedContactNumber().getMobileNumber().equals(numberToBeSearched.getMobileNumber())) {
            throw new BadRequestException(MessageCodes.SEARCHED_OWN_CONTACT_NO.getValue());
        }

        PeopleUser existingWatuUser = findUserByContactNumber(numberToBeSearched);

        UserContactData searchedContactData = null;

        List<UserConnection> userConnectionsWithSearchedNumber = null;

        /*  [ PEOP-2908]
         * Step 1. If number being searched is registered with WATU app and if already connected
         *       > will return the connection details without further processing and set searchedContactExist to "TRUE"
         *
         * Step 2. If number being searched is registered with WATU app and not yet connected
         *       > will scan through metaDataList of all static connection in session users list and collect all matching
         *       records, then based on priority will pick the connection which will be returned and
         *       set searchedContactExist to "TRUE"
         *
         * Step 3. If No records found from processing at Step 2 and number is registered with WATU
         *       > will return public data of the user registered with the number and set searchedContactExist to "TRUE"
         *
         * Step 4. If number is not registered
         *       > will scan through metaDataList of all static connection in session users list and collect all matching
         *       records, then based on priority will pick the connection which will be returned and
         *       set searchedContactExist to "TRUE"
         *
         * Step 5. If number is not registered and no match found from Step 4 process
         *       > will set searchedContactExist to "FALSE"
         *
         *       */
        if (existingWatuUser != null) {

            // Step 1,2,3 will be applicable as searched contact is registered with WATU app

            // Step 1
            List<UserConnection> searchedContact = userConnectionRepository.getConnectionDataWithProfileForSelectedToUserIds(
                    sessionUser.getUserId(), Collections.singletonList(existingWatuUser.getUserId()));

            if (!PeopleUtils.isNullOrEmpty(searchedContact)) {
                // connected contact
                searchedContactData = userConnectionService.getUserContactDataList(sessionUser,
                        searchedContact, null, masterService.getRegisteredNumberList()).get(0);
            }

            // Step 2
            if (searchedContactData == null) {
                // connections with searched number entry in metaDataList
                userConnectionsWithSearchedNumber = userConnectionRepository.findByFromIdAndPhoneNumberAndStatus(
                        sessionUser.getUserId(), numberToBeSearched.getPhoneNumber(), Arrays.asList(
                                ConnectionStatus.PENDING.getValue(), ConnectionStatus.NOT_CONNECTED.getValue()));

                searchedContactData = validateConnectionsWithSearchedNumber(sessionUser, numberToBeSearched,
                        userConnectionsWithSearchedNumber);
            }

            // Step 3
            if (searchedContactData == null) {

                // public data of searched contact
                searchedContactData = new UserContactData();
                searchedContactData.setPublicProfileData(masterService.prepareUserPublicData(existingWatuUser));
                searchedContactData.setConnectionStatus(checkAndSetConnectionStatus(sessionUser, existingWatuUser));
                searchedContactData.setIsBlocked(sessionUser.getBlockedUserIdList().contains(existingWatuUser.getUserId()));
            }

        } else {

            // Step 4
            // connections with searched number entry in metaDataList
            userConnectionsWithSearchedNumber = userConnectionRepository.findByFromIdAndPhoneNumberAndStatus(
                    sessionUser.getUserId(), numberToBeSearched.getPhoneNumber(), Arrays.asList(
                            ConnectionStatus.PENDING.getValue(), ConnectionStatus.NOT_CONNECTED.getValue()));

            searchedContactData = validateConnectionsWithSearchedNumber(sessionUser, numberToBeSearched,
                    userConnectionsWithSearchedNumber);
        }

        // prepare response
        SearchByNumberResponseDTO searchResponse = new SearchByNumberResponseDTO();
        
        if(existingWatuUser!=null && sessionUser!=null) {
        	UserActivity otherActivity =
                    userActivityRepository.getPendingConnectionRequestActivity(existingWatuUser.getUserId(),
                    		sessionUser.getUserId());
        	if (otherActivity != null) {
            	searchedContactData.setConnectionStatus(ConnectionStatus.PENDING.getValue());           
            }
        }
        
        if (searchedContactData != null) {
        	if(searchedContactData.getPublicProfileData()!=null) {
        		searchResponse.setSearchedWatuContactDetails(searchedContactData);
        	}else {
        		searchResponse.setSearchedContactDetails(searchedContactData);
        	}
            searchResponse.setSearchedContactExist(true);
        } else {
            // Step 5
            searchResponse.setSearchedContactExist(false);
        }

        return searchResponse;
    }

    /*
     *
     * Method to signup/login
     * @param ConnectRequestDTO
     * @returns ConnectResponseDTO
     *
     */
    @Override
    public ConnectResponseDTO connect(ConnectRequestDTO connectRequestDTO) {

        connectRequestDTO.checkIfValidConnectRequest();
        String emailId = connectRequestDTO.getEmailId();
        ContactNumberDTO contactNumber = null;
        Boolean isExistingUser = Boolean.FALSE;
        if(connectRequestDTO.getContactNumber() != null){
            contactNumber =  connectRequestDTO.getContactNumber().getContactNumberDTO();
        }
        PeopleUser peopleUser = null;

        if (contactNumber != null && PeopleUtils.isNullOrEmpty(contactNumber.getCountryCode())) {
            throw new BadRequestException(MessageCodes.INVALID_PROPERTY.getValue());
        }

        // login with email
        if (!PeopleUtils.isNullOrEmpty(emailId)) {
            peopleUser = findUserByPrimaryEmail(emailId);
            if (peopleUser == null) {
                throw new BadRequestException(MessageCodes.INVALID_EMAIL_ADDRESS.getValue());
            }
            contactNumber = peopleUser.getVerifiedContactNumber();
        } else if (contactNumber != null) {
            peopleUser = peopleUserRepository.findByCodeAndNumber(contactNumber.getCountryCode(), contactNumber.getPhoneNumber());
        }

        // generate temp session token
        String tempToken = TokenGenerator.generateTempToken();

        // generate otp for user
        String otp;
        if (contactNumber != null && contactNumber.getMobileNumber().equals(dummyIOSUserNumber)) {
            otp = dummyIOSUserOTP;
        } else if (contactNumber != null && contactNumber.getMobileNumber().equals(dummyAndroidUserNumber)) {
            otp = dummyAndroidUserOTP;
        } else {
            // generate otp for user
            otp = otpService.generateOtpForUser();
        }

        // persist otp and temp token
        TemporarySession temporarySession = new TemporarySession();
        temporarySession.setOtp(otp);
        temporarySession.setContactNumber(contactNumber);
        temporarySession.setTemporaryToken(tempToken);
        temporarySession.setTokenStatus(TokenStatus.ACTIVE);
        temporarySession.setCreatedOn(PeopleUtils.getCurrentTimeInUTC());
        tempSessionService.persistOTPWithToken(temporarySession);

        // send otp
        otpService.sendOTP(otp, contactNumber);

        if (!PeopleUtils.isNullOrEmpty(emailId)) {
            sendOtpEmail(otp, emailId);
        }

        // prepare connect response
        ConnectResponseDTO connectResponseDTO = new ConnectResponseDTO();
        connectResponseDTO.setIsPhoneNumberVerified(Boolean.FALSE);
        connectResponseDTO.setTempToken(tempToken);
        if(peopleUser!=null) {
        	String email = null;
        	if(peopleUser.getUserMetadataList()!=null) {
        		for(UserProfileData profileData : peopleUser.getUserMetadataList()) {
          			if(profileData.getCategory().equalsIgnoreCase("EMAIL") && profileData.getLabel().equalsIgnoreCase("PL.01.04") && profileData.getIsPrimary()) {
    					for(KeyValueData valueData : profileData.getKeyValueDataList()) {
    						if(valueData.getKey().equalsIgnoreCase("emailAddress")) {
    							email = valueData.getVal();
    						}
    					}
        			}
        		}
        	}
        	if(peopleUser.getFirstNameValue()!=null && !peopleUser.getFirstNameValue().isEmpty() && email!=null) {
	            	isExistingUser = Boolean.TRUE;
        	}
        }
        connectResponseDTO.setIsExistingUser(isExistingUser);
        return connectResponseDTO;
    }

    @Override
    public VerifyOTPResponseDTO verifyOTP(VerifyOTPRequestDTO verifyOTPRequestDTO, String deviceTypeID) {

        String tempToken = tokenAuthService.getTempToken();
        TemporarySession temporarySession = tokenAuthService.getTempSessionByTempToken(tempToken);
        ContactNumberDTO number = temporarySession.getContactNumber();
        Boolean isExistingUser = Boolean.FALSE;
        PeopleUser verifiedUser = new PeopleUser();

        //otpService.checkIfOTPValid(verifyOTPRequestDTO.getOtp(), temporarySession);

        // get user if number already exists
        PeopleUser peopleUser = findUserByContactNumber(number);
        Boolean isNumberExists = Boolean.FALSE;
        if (peopleUser != null) {
            verifiedUser = peopleUser;
            isNumberExists = Boolean.TRUE;
        }

        // check operation
        Boolean isChangeNumberOperation = checkIfChangeNumberOperation(temporarySession);

        // built a bit pattern from isNumberExists & isChangeNumberOperation
        int actualOperation = (isNumberExists ? 2 : 0) | (isChangeNumberOperation ? 1 : 0);

        switch (actualOperation) {
            case 0:
                // new user registration
                verifiedUser = signup(number, verifyOTPRequestDTO.getReferralCode(), verifyOTPRequestDTO.getBluetoothToken());
                break;
            case 1:
                // change primary number
                verifiedUser = findUserByUserId(temporarySession.getUserId());
                updateNumber(verifiedUser, number);
                if(peopleUser!=null) {
                	String email = null;
                	if(peopleUser.getUserMetadataList()!=null) {
                		for(UserProfileData profileData : peopleUser.getUserMetadataList()) {
                  			if(profileData.getCategory().equalsIgnoreCase("EMAIL") && profileData.getLabel().equalsIgnoreCase("PL.01.04") && profileData.getIsPrimary()) {
            					for(KeyValueData valueData : profileData.getKeyValueDataList()) {
            						if(valueData.getKey().equalsIgnoreCase("emailAddress")) {
            							email = valueData.getVal();
            						}
            					}
                			}
                		}
                	}
                	if(peopleUser.getFirstNameValue()!=null && !peopleUser.getFirstNameValue().isEmpty() && email!=null) {
    	            	isExistingUser = Boolean.TRUE;
                	}
                }
                break;
            case 2:
                // login
                if(peopleUser!=null) {
                	String email = null;
                	if(peopleUser.getUserMetadataList()!=null) {
                		for(UserProfileData profileData : peopleUser.getUserMetadataList()) {
                  			if(profileData.getCategory().equalsIgnoreCase("EMAIL") && profileData.getLabel().equalsIgnoreCase("PL.01.04") && profileData.getIsPrimary()) {
            					for(KeyValueData valueData : profileData.getKeyValueDataList()) {
            						if(valueData.getKey().equalsIgnoreCase("emailAddress")) {
            							email = valueData.getVal();
            						}
            					}
                			}
                		}
                	}
                	if(peopleUser.getFirstNameValue()!=null && !peopleUser.getFirstNameValue().isEmpty() && email!=null) {
    	            	isExistingUser = Boolean.TRUE;
                	}
                }
                break;
            default:
                throw new BadRequestException(MessageCodes.INVALID_OPERATION.getValue());
        }

        if(verifiedUser == null) {
            return null;
        }
        expireActiveSession(verifiedUser.getUserId());
        UserSession userSession = createUserSession(verifiedUser.getUserId(), deviceTypeID);

        // expire token
        temporarySession.setTokenStatus(TokenStatus.EXPIRED);
        temporarySessionRepository.save(temporarySession);

        // prepare response
        VerifyOTPResponseDTO response = new VerifyOTPResponseDTO();
        response.setIsExistingUser(isExistingUser);
        response.setSessionToken(userSession.getSessionToken());
        response.setUserId(verifiedUser.getUserId());
        response.setIsPhoneNumberVerified(Boolean.TRUE);
        response.setReferralCode(verifiedUser.getReferralCode());
        response.setIsPushNotificationEnabled(verifiedUser.getIsPushNotificationEnabled());
        response.setBluetoothToken(PeopleUtils.getDefaultOrEmpty(verifiedUser.getBluetoothToken()));
        
        //new code
        PeopleUser sessionUser = tokenAuthService.getUserFromSessionToken(userSession.getSessionToken());
        
        String deviceToken = verifyOTPRequestDTO.getDeviceToken();
        
        expireOtherActiveSessionsByDeviceToken(deviceToken, sessionUser.getUserId());
        
        userSession.setDeviceToken(deviceToken);
        userSession.setEndPointARN(registerDeviceWithSNS.registerDevice(deviceToken, Integer.parseInt(deviceTypeID)));
        userSessionRepository.save(userSession);
        
        VerifyAllDetailsResponse detailsResponse = new VerifyAllDetailsResponse();
        try {
        	detailsResponse.setSocialMediaList(socialMediaDataService.getSocialMediaCategory());
    		ObjectMapper mapper = new ObjectMapper();
    		QrResponse qrResponse =  mapper.readValue(generateQRCodeByUser(sessionUser), QrResponse.class);
    		detailsResponse.setQrResponse(qrResponse);
    		detailsResponse.setUserInfoDetails(getUserDetailNew(sessionUser));
    		detailsResponse.setPredefinedData(systemDataService.getSystemData());
		} catch (Exception e) {
			e.printStackTrace();
		}
        response.setVerifyAllDetails(detailsResponse);
        return response;
    }

    @Override
    public void authenticateEmail(String valueId) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        String userId = sessionUser.getUserId();

        logger.info("userId: {}", userId);

        List<UserProfileData> userProfileDataList = sessionUser.getUserMetadataList();

        // check if user profile data contains the valueId. If yes, set authentication to pending
        UserProfileData toBeAuthenticatedEmail = userProfileDataList.stream()
                .filter(emailData -> valueId.equals(emailData.getValueId()))
                .findAny()
                .orElse(null);

        if (toBeAuthenticatedEmail == null) {
            throw new BadRequestException(MessageCodes.INTERNAL_SERVER_ERROR.getValue());
        }

        String email = toBeAuthenticatedEmail.getSingleValueData();
        Boolean isPrimary = toBeAuthenticatedEmail.getIsPrimary();
        VerifyEmail verifyEmail;

        if (isPrimary) {
            verifyEmail = verifyEmailService.generateAndPersistLinkForPrimaryEmail(userId, email);
        } else {
            verifyEmail = verifyEmailService.generateAndPersistEmailVerificationLink(userId, email);
        }

        // Generate and persist email verification record

        sessionUser.setUserMetadataList(userProfileDataList);
        Map<Integer, UserInformationVerification> emailMap = sessionUser.getEmailAddressMap();
        emailMap.put(email.hashCode(), UserInformationVerification.PENDING);
        sessionUser.setEmailAddressMap(emailMap);
        peopleUserRepository.save(sessionUser);

        //Send verification email
        sendVerificationEmail(sessionUser.getNameValue(), email, verifyEmail.getVerificationLink());

    }

    @Override
    public void linkPrimaryEmail(String email) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        //adding the check to verify if email is already used
        if (checkIfEmailExists(email)) {
            throw new BadRequestException(MessageCodes.EMAIL_ALREADY_EXISTS.getValue());
        }

        // Generate and persist email verification record
        VerifyEmail verifyEmail = verifyEmailService.generateAndPersistLinkForPrimaryEmail(sessionUser.getUserId(), email);

        // add to user profile
        sessionUser.updatePrimaryEmailInMetadata(email);
        sessionUser.setPrimaryEmail(null);

        Map<Integer, UserInformationVerification> emailMap = sessionUser.getEmailAddressMap();
        emailMap.put(email.hashCode(), UserInformationVerification.PENDING);
        sessionUser.setEmailAddressMap(emailMap);
        peopleUserRepository.save(sessionUser);

        // send email link
        sendVerificationEmail(sessionUser.getNameValue(), email, verifyEmail.getVerificationLink());

        // send notifications and update lastModifiedTime for connections
        List<String> profileIds = getPrimaryNumberAndEmailSharedProfileIds(sessionUser,
                UserInfoCategory.EMAIL_ADDRESS.getValue());
        updateAndNotifyConnections(sessionUser, profileIds);

    }

    private PeopleUser findUserByPrimaryEmail(String email) {
        return peopleUserRepository.findByPrimaryEmail(email);
    }

    private Boolean checkIfEmailExists(String email) {
        return findUserByPrimaryEmail(email) != null;
    }

    @Override
    public void verifyEmail(String verificationToken) {

        String link = verifyEmailService.generateEmailVerificationLink(verificationToken);
        VerifyEmail verifyEmail = verifyEmailRepository.findByVerificationLink(link);

        if (verifyEmail == null) {
            throw new BadRequestException(MessageCodes.INVALID_VERIFICATION_TOKEN.getValue());
        }

        String email = verifyEmail.getEmail();
        // check if primary email verify
        if (verifyEmail.getIsPrimary() && checkIfEmailExists(email)) {
            throw new BadRequestException(MessageCodes.EMAIL_ALREADY_EXISTS.getValue());
        }

        PeopleUser sessionUser = findUserByUserId(verifyEmail.getUserId());

        // update primary email if primary email verification
        if (verifyEmail.getIsPrimary()) {
            sessionUser.setPrimaryEmail(email);
            // update networkSettings with default email
            updatePrimaryEmailToNetworkDefaults(sessionUser);
        }

        Map<Integer, UserInformationVerification> emailMap = sessionUser.getEmailAddressMap();
        emailMap.put(email.hashCode(), UserInformationVerification.VERIFIED);
        sessionUser.setEmailAddressMap(emailMap);

        // expire all links sent to the same emailId
        List<VerifyEmail> emailLinkToBeExpired = verifyEmailRepository.findByEmailId(email);
        emailLinkToBeExpired.add(verifyEmail);

        verifyEmailRepository.deleteAll(emailLinkToBeExpired);
        peopleUserRepository.save(sessionUser);

    }

    @Override
    public UpdatePushNotificationSettingResponseDTO updatePushNotificationSetting(UpdatePushNotificationSettingRequestDTO notificationSettingRequestDTO) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();

        Boolean enableStatus = notificationSettingRequestDTO.getEnableSetting();
        String deviceToken = notificationSettingRequestDTO.getDeviceToken();
        Integer deviceTypeId = notificationSettingRequestDTO.getDeviceTypeId();

        // expire other active sessions on this device
        expireOtherActiveSessionsByDeviceToken(deviceToken, sessionUser.getUserId());

        UserSession userSession = userSessionRepository.findActiveSession(PeopleUtils.convertStringToObjectId(sessionUser.getUserId()));

        if (enableStatus && deviceToken != null && deviceTypeId != 0) {

            userSession.setDeviceToken(deviceToken);
            userSession.setDeviceTypeId(deviceTypeId);
            userSession.setEndPointARN(registerDeviceWithSNS.registerDevice(deviceToken, deviceTypeId));

            sessionUser.setIsPushNotificationEnabled(Boolean.TRUE);
        } else if (!enableStatus) {
            sessionUser.setIsPushNotificationEnabled(Boolean.FALSE);
        } else {
            throw new BadRequestException(MessageCodes.INVALID_PROPERTY.getValue());
        }

        userSessionRepository.save(userSession);
        peopleUserRepository.save(sessionUser);

        UpdatePushNotificationSettingResponseDTO notificationSettingResponseDTO = new UpdatePushNotificationSettingResponseDTO();
        notificationSettingResponseDTO.setIsPushNotificationEnabled(sessionUser.getIsPushNotificationEnabled());
        return notificationSettingResponseDTO;
    }

    @Override
    public UpdateUserInfoResponseDTO updateUser(UpdateUserInfoRequestDTO updateUserInfoRequestDTO) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        UserInformationDTO toBeUpdatedDetails = updateUserInfoRequestDTO.getUserDetails();
        Set<String> updatedPrivacyProfileIds = new HashSet<>();

        // update timestamp data by comparing previous values
        if (!PeopleUtils.compareValues(sessionUser.getNameValue(), toBeUpdatedDetails.getName())) {
            sessionUser.setName(toBeUpdatedDetails.getName());
            updatedPrivacyProfileIds.addAll(getAllPrivacyProfileIds(userPrivacyProfileRepository.findAllByUserId(sessionUser.getUserId())));
        }

        if (!PeopleUtils.compareValues(sessionUser.getFirstNameValue(), toBeUpdatedDetails.getFirstName())) {
            sessionUser.setFirstName(toBeUpdatedDetails.getFirstName());
            updatedPrivacyProfileIds.addAll(getAllPrivacyProfileIds(userPrivacyProfileRepository.findAllByUserId(sessionUser.getUserId())));
        }

        if (!PeopleUtils.compareValues(sessionUser.getMiddleNameValue(), toBeUpdatedDetails.getMiddleName())) {
            sessionUser.setMiddleName(toBeUpdatedDetails.getMiddleName());
            updatedPrivacyProfileIds.addAll(getAllPrivacyProfileIds(userPrivacyProfileRepository.findAllByUserId(sessionUser.getUserId())));
        }

        if (!PeopleUtils.compareValues(sessionUser.getLastNameValue(), toBeUpdatedDetails.getLastName())) {
            sessionUser.setLastName(toBeUpdatedDetails.getLastName());
            updatedPrivacyProfileIds.addAll(getAllPrivacyProfileIds(userPrivacyProfileRepository.findAllByUserId(sessionUser.getUserId())));
        }

        if (!PeopleUtils.compareValues(sessionUser.getCompanyValue(), toBeUpdatedDetails.getCompany())) {
            sessionUser.setCompany(toBeUpdatedDetails.getCompany());
            updatedPrivacyProfileIds.addAll(getCompanySharedPrivacyProfileIds(userPrivacyProfileRepository.findAllByUserId(sessionUser.getUserId())));
        }

        if (!PeopleUtils.compareValues(sessionUser.getPositionValue(), toBeUpdatedDetails.getPosition())) {
            sessionUser.setPosition(toBeUpdatedDetails.getPosition());
            updatedPrivacyProfileIds.addAll(getPositionSharedPrivacyProfileIds(userPrivacyProfileRepository.findAllByUserId(sessionUser.getUserId())));
        }

        if (!PeopleUtils.compareValues(sessionUser.getNickNameValue(), toBeUpdatedDetails.getNickName())) {
            sessionUser.setNickName(toBeUpdatedDetails.getNickName());
            updatedPrivacyProfileIds.addAll(getNickNameSharedPrivacyProfileIds(userPrivacyProfileRepository.findAllByUserId(sessionUser.getUserId())));
        }

        if (!PeopleUtils.compareValues(sessionUser.getMaidenNameValue(), toBeUpdatedDetails.getMaidenName())) {
            sessionUser.setMaidenName(toBeUpdatedDetails.getMaidenName());
            updatedPrivacyProfileIds.addAll(getMaidenNameSharedPrivacyProfileIds(userPrivacyProfileRepository.findAllByUserId(sessionUser.getUserId())));
        }


        // update privacy profile image
        updatePrivacyProfileImage(toBeUpdatedDetails, sessionUser);

        // update basic info
        updateUserPrimaryData(toBeUpdatedDetails, sessionUser);

        // update category info

        List<UserProfileData> userProfileDataList = toBeUpdatedDetails.getUserMetadataList();

        List<String> valueIds = getUpdatedValueIds(userProfileDataList, sessionUser);
        updatedPrivacyProfileIds.addAll(getAllPrivacyProfileIds(
                userPrivacyProfileRepository.findAllByUserIdAndValueIds(sessionUser.getUserId(), valueIds)));

        if (!PeopleUtils.isNullOrEmpty(userProfileDataList)) {
            sessionUser.setUserMetadataList(updateUserMetaData(userProfileDataList, sessionUser));

        }

        // update user profile tag data
        if (toBeUpdatedDetails.getTagList() != null) {
        	List<String> tagList = new ArrayList<>();
        	for(String tag : PeopleUtils.emptyIfNull(toBeUpdatedDetails.getTagList())) {
        		tag = tag.replace(".", "");
        		if(!tag.isEmpty()) {
        			tagList.add(tag);	
        		}
        	}
            sessionUser.setTagMap(tagService.editUserTag(sessionUser.getTagMap(), tagList));
        }

        sessionUser.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
        setFullNameForUser(sessionUser);
        PeopleUser updatedUser = peopleUserRepository.save(sessionUser);
        Set<String> blockedUsers = updatedUser.getBlockedUserIdList();//Set of blockedUserIds

        List<SQSPayload> sqsPayloadList = new ArrayList<>();
        List<UserConnection> userConnections = userConnectionRepository.findConnectionByUserIdAndPrivacyProfileId(
                sessionUser.getUserId(), new ArrayList<>(updatedPrivacyProfileIds));
        ActivityType activityType = new ActivityType();
        activityType.setRequestType(RequestType.UPDATE_CONTACT_ACTIVITY);
        activityType.setActionTaken(Action.INITIATED);
        DateTime currentDateTime = PeopleUtils.getCurrentTimeInUTC();

        for (UserConnection userConnection : userConnections) {
            /* Blocked users should not be notified about any profile changes */
            if (blockedUsers.contains(userConnection.getConnectionFromId()) || masterService.isUserBlockedByContact(userConnection.getConnectionFromId(),
                    updatedUser.getUserId())) {
                continue;
            }

            // Create activity for a particular contact
            UserActivity userActivity = new UserActivity();
            userActivity.setActivityForId(userConnection.getConnectionFromId());
            userActivity.setActivityById(userConnection.getConnectionToId());
            userActivity.setActivityType(activityType);
            userActivity.setOverallStatus(ActivityStatus.ACTIVE);
            userActivity.setCreatedOn(currentDateTime);
            userActivity.setLastUpdatedOn(currentDateTime);
            
            List<UserActivity> userActivities = userActivityRepository.getPendingActivitiesByInitiatedByIdAndRequestType(
            		userConnection.getConnectionFromId(), RequestType.UPDATE_CONTACT_ACTIVITY);
            
            if(userActivities!=null) {
                for(UserActivity activity : PeopleUtils.emptyIfNull(userActivities)) {
                	userActivityRepository.deleteById(activity.getActivityId());
                }	
            }
            
            userActivityRepository.save(userActivity);
            
            sqsPayloadList.add(privacyProfileService.prepareSQSPayloadForUpdateContactActivity(userActivity,
                    sessionUser, userConnection.getConnectionId()));
        }
        queueService.sendPayloadToSQS(sqsPayloadList);
        // update default system profiles for new valueIds
        updateDefaultSystemProfiles(updatedUser);

        // prepare response from updatedUser - call get user Details
        UpdateUserInfoResponseDTO response = new UpdateUserInfoResponseDTO();
        response.setUserDetails(getUserDataFromPeopleUserObj(updatedUser));
        return response;
    }
    
    /*
     *
     * verified phone number/ primary number can be changed
     *
     */
    @Override
    public ChangeMobileNumberResponse updatePrimaryNumber(ChangeMobileNumberRequest changeMobileNumberRequest) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();

        ContactNumberDTO newNumber = changeMobileNumberRequest.getNewContactNumber().getContactNumberDTO();

        PeopleUser otherUser = findUserByContactNumber(newNumber);

        if (otherUser != null) {
            if (sessionUser.getUserId().equals(otherUser.getUserId())) {
                throw new BadRequestException(MessageCodes.PHONE_NUMBER_ALREADY_VERIFIED.getValue());
            } else {
                throw new BadRequestException(MessageCodes.PHONE_NUMBER_ALREADY_EXISTS.getValue());
            }
        }
        
        List<UserConnection> userConnections = userConnectionRepository.findAllConnectedContactByPeopleUserToId(sessionUser.getUserId());
        if(userConnections!=null) {
        	for(UserConnection connection : PeopleUtils.emptyIfNull(userConnections)) {
        		connection.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
        		userConnectionRepository.save(connection);
        	}
        }
        
        // generate temp session token
        String tempToken = TokenGenerator.generateTempToken();

        // generate otp for user
        String otp = otpService.generateOtpForUser();

        // persist otp and temp token
        TemporarySession temporarySession = new TemporarySession();
        temporarySession.setOtp(otp);
        temporarySession.setContactNumber(newNumber);
        temporarySession.setTemporaryToken(tempToken);
        temporarySession.setTokenStatus(TokenStatus.ACTIVE);
        temporarySession.setUserId(sessionUser.getUserId());
        temporarySession.setOperation(PeopleConstants.CHANGE_MOBILE_NUMBER);
        tempSessionService.persistOTPWithToken(temporarySession);

        // send otp---
        otpService.sendOTP(otp, newNumber);

        // prepare response
        ChangeMobileNumberResponse response = new ChangeMobileNumberResponse();
        response.setTempToken(tempToken);
        return response;
    }

    /*
	Method to get user related profile information
	@param userId
	@return UserInformationDTO
	 */

    @Override
    public UpdateUserInfoResponseDTO getUserDetails() {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        UserInformationDTO userInformation = getUserDataFromPeopleUserObj(sessionUser);

        UpdateUserInfoResponseDTO response = new UpdateUserInfoResponseDTO();
        response.setUserDetails(userInformation);
        response.setNetworkSharedValues(getNetworkSharedValues(sessionUser));

        return response;
    }
    
    private UpdateUserInfoResponseDTO getUserDetailNew(PeopleUser sessionUser) {

        UserInformationDTO userInformation = getUserDataFromPeopleUserObj(sessionUser);

        UpdateUserInfoResponseDTO response = new UpdateUserInfoResponseDTO();
        response.setUserDetails(userInformation);
        response.setNetworkSharedValues(getNetworkSharedValues(sessionUser));

        return response;
    }

    private List<String> getNetworkSharedValues(PeopleUser sessionUser) {
        List<String> networkSharedValues = new ArrayList<>();
        Set<String> communicationTypesOfUsersNetwork = new HashSet<>();

        List<UserNetworkDetails> userNetworkDetails = networkRepository.getUserNetworks(sessionUser.getUserId());

        for (UserNetworkDetails userNetwork : PeopleUtils.emptyIfNull(userNetworkDetails)) {

            String category = userNetwork.getNetworkDetails().getPrimaryContactMethod().getContactCategory();
            if (category.equalsIgnoreCase(UserInfoCategory.SOCIAL_PROFILE.getValue())) {
                communicationTypesOfUsersNetwork.add(userNetwork.getNetworkDetails().getPrimaryContactMethod()
                        .getContactLabel());
            } else {
                communicationTypesOfUsersNetwork.add(category);
            }

        }
        getNetworkSharedValuesOfPendingJoinRequestToNetwork(sessionUser, communicationTypesOfUsersNetwork);
        List<String> networkSettingValues = sessionUser.getNetworkSharedValueList();
        Map<String, UserProfileData> metaDataMap = sessionUser.getMetadataMap();

        for (String valueId : networkSettingValues) {
            if (metaDataMap.get(valueId) != null) {
                UserProfileData userProfileData = metaDataMap.get(valueId);
                if ((userProfileData.getCategory().equalsIgnoreCase(UserInfoCategory.SOCIAL_PROFILE.getValue())
                        && (communicationTypesOfUsersNetwork.contains(userProfileData.getLabel())))
                        || ((!userProfileData.getCategory().equalsIgnoreCase(UserInfoCategory.SOCIAL_PROFILE.getValue()))
                        && (communicationTypesOfUsersNetwork.contains(userProfileData.getCategory())))) {

                    networkSharedValues.add(valueId);
                }
            }
        }

        return networkSharedValues;
    }

    private void getNetworkSharedValuesOfPendingJoinRequestToNetwork(PeopleUser sessionUser,
                                                                     Set<String> communicationTypesOfUsersNetwork) {
        List<String> networkIdList = new ArrayList<>();
        List<UserActivity> networkJoinRequests = userActivityRepository.getPendingActivitiesByInitiatedByIdAndRequestType(
                sessionUser.getUserId(), RequestType.NETWORK_JOIN_REQUEST);
        for (UserActivity userActivity : PeopleUtils.emptyIfNull(networkJoinRequests)) {
            if (userActivity.getNetworkId() != null) {
                networkIdList.add(userActivity.getNetworkId());
            }
        }
        List<Network> networkList = networkRepository.getAllNetworksById(networkIdList);
        for (Network eachNetwork : PeopleUtils.emptyIfNull(networkList)) {
            String primaryContactCategory = eachNetwork.getPrimaryContactMethod().getContactCategory();
            if (primaryContactCategory.equalsIgnoreCase(UserInfoCategory.SOCIAL_PROFILE.getValue())) {
                communicationTypesOfUsersNetwork.add(eachNetwork.getPrimaryContactMethod().getContactLabel());
            } else {
                communicationTypesOfUsersNetwork.add(primaryContactCategory);
            }
        }
    }

    private Boolean checkIfChangeNumberOperation(TemporarySession temporarySession) {

        String operation = temporarySession.getOperation();
        if (PeopleUtils.isNullOrEmpty(operation)) {
            return Boolean.FALSE;
        }
        if (!PeopleConstants.CHANGE_MOBILE_NUMBER.equals(operation)) {
            throw new BadRequestException(MessageCodes.INVALID_OPERATION.getValue());
        }
        return Boolean.TRUE;
    }

    private PeopleUser signup(ContactNumberDTO contactNumber, String referralCode, String bluetoothToken) {

        // create new user
        PeopleUser newUser = createNewUser(contactNumber, bluetoothToken);

        if (newUser == null) {
            return null;
        }
        if (referralCode != null && (!referralCode.trim().isEmpty())) {
            // add ambassador rewards points to user who referred
            PeopleAmbassador ambassador = ambassadorRepository.findByReferralCodeAndReferredContactNumber(referralCode,
                    contactNumber);

            if (ambassador != null) {
                ambassador.setReferredUserId(newUser.getUserId());
                ambassador.setRewardPoints(rewardPoints);
                ambassador.setReferralStatus(ReferralStatus.COMPLETED);
                ambassador.setReferralCompletedOn(new DateTime());
                ambassadorRepository.save(ambassador);
            }
        }

        // update registered number to registered number repo
        updateRegisteredNumber(contactNumber, null);

        // update request raised for this number
        updateRequestWithUserId(contactNumber, newUser.getUserId());

        return newUser;
    }

    private void updateNumber(PeopleUser peopleUser, ContactNumberDTO contactNumber) {

        // update number
        ContactNumberDTO oldNumber = peopleUser.getVerifiedContactNumber();
        peopleUser.setVerifiedContactNumber(contactNumber);
        peopleUser = peopleUserRepository.save(peopleUser);

        // expire active sessions
        expireActiveSession(peopleUser.getUserId());

        // update registered number to registered number repo
        updateRegisteredNumber(contactNumber, oldNumber);

        // send notifications and update lastModifiedTime for connections
        List<String> profileIds = getPrimaryNumberAndEmailSharedProfileIds(peopleUser, UserInfoCategory.CONTACT_NUMBER.getValue());
        updateAndNotifyConnections(peopleUser, profileIds);
    }

    /* List of profile ids in which the primary number or email is shared is fetched based on userInfoCategory */
    private List<String> getPrimaryNumberAndEmailSharedProfileIds(PeopleUser sessionUser, String userInfoCategory) {
        String valueId = null;
        for (UserProfileData userProfileData : sessionUser.getUserMetadataList()) {
            if (userProfileData.getCategory().equalsIgnoreCase(userInfoCategory) && userProfileData.getIsPrimary()) {
                valueId = userProfileData.getValueId();
                break;
            }
        }
        List<UserPrivacyProfile> userPrivacyProfiles = userPrivacyProfileRepository.findAllByUserIdAndValueIds(
                sessionUser.getUserId(), Arrays.asList(valueId));

        List<String> profileIds = new ArrayList<>();
        for (UserPrivacyProfile privacyProfile : userPrivacyProfiles) {
            profileIds.add(privacyProfile.getPrivacyProfileId());
        }
        return profileIds;
    }

    private PeopleUser createNewUser(ContactNumberDTO contactNumber, String bluetoothToken) {

        PeopleUser newUser = new PeopleUser();
        newUser.setStatus(UserStatus.ACTIVE);
        newUser.setCreatedOn(PeopleUtils.getCurrentTimeInUTC());
        newUser.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
        newUser.setVerifiedContactNumber(contactNumber);
        if (bluetoothToken != null && bluetoothToken != "") {
            PeopleUser bluetoothTokenUser = peopleUserRepository.findByBluetoothToken(bluetoothToken);
            if (bluetoothTokenUser != null) {
                return null;
            } else {
                newUser.setBluetoothToken(bluetoothToken);
            }
        }
        String referralCode;
        PeopleUser peopleUser;

        do {
            referralCode = RandomStringUtils.randomAlphanumeric(12);
            peopleUser = peopleUserRepository.findByReferralCode(referralCode);

        } while (peopleUser != null);
        newUser.setReferralCode(referralCode);
        return peopleUserRepository.save(newUser);

    }

    private UserSession createUserSession(String userId, String deviceTypeID) {

        UserSession userSession = new UserSession();
        userSession.setUserId(userId);
        userSession.setSessionToken(TokenGenerator.generateSessionToken());
        userSession.setStatus(TokenStatus.ACTIVE);
        userSession.setDeviceTypeId(Integer.parseInt(deviceTypeID));
        userSession.setCreatedTime(new DateTime());
        userSessionRepository.save(userSession);
        return userSession;
    }

    private void expireActiveSession(String userId) {
        List<UserSession> activeSessions =
                userSessionRepository.findActiveSession(Arrays.asList(PeopleUtils.convertStringToObjectId(userId)));

        for (UserSession userSession : activeSessions) {
            userSession.setModifiedTime(new DateTime());
            userSession.setStatus(TokenStatus.EXPIRED);
        }
        if (!PeopleUtils.isNullOrEmpty(activeSessions)) {
            userSessionRepository.saveAll(activeSessions);
        }
    }

    private void expireOtherActiveSessionsByDeviceToken(String deviceToken, String currentUserId) {

        List<UserSession> userSessionList = userSessionRepository.findActiveSessionByDeviceToken(deviceToken);
        for (UserSession userSession : PeopleUtils.emptyIfNull(userSessionList)) {
            if (userSession.getUserId().equals(currentUserId)) {
                continue;
            }
            userSession.setModifiedTime(new DateTime());
            userSession.setStatus(TokenStatus.EXPIRED);
        }
        if (!PeopleUtils.isNullOrEmpty(userSessionList)) {
            userSessionRepository.saveAll(userSessionList);
        }
    }

    private void updateRegisteredNumber(ContactNumberDTO newNumber, ContactNumberDTO oldNumber) {

        RegisteredNumber registeredNumber = addNumberToRegisteredNumberRepo(newNumber);
        if (oldNumber != null) {
        	String contactNumber = PeopleUtils.getMobileNumberWithCoutryCode(oldNumber);
            registeredNumber.getRegisteredNumberList().remove(contactNumber);
        }
        registeredNumberRepository.save(registeredNumber);
    }


    private RegisteredNumber addNumberToRegisteredNumberRepo(ContactNumberDTO newNumber) {
    	
    	String contactNumber = PeopleUtils.getMobileNumberWithCoutryCode(newNumber);
    	
        // registered number add
        List<RegisteredNumber> registeredNumberList = registeredNumberRepository.findAll();

        if (PeopleUtils.isNullOrEmpty(registeredNumberList)) {
            RegisteredNumber registeredNumber = new RegisteredNumber();
            List<String> numberList = new ArrayList<>();
            registeredNumber.setRegisteredNumberList(numberList);
            registeredNumberList.add(registeredNumber);
        }

        RegisteredNumber registeredNumber = registeredNumberList.get(0);
        List<String> numberList = registeredNumber.getRegisteredNumberList();
        if (newNumber != null && !numberList.contains(contactNumber)) {
            numberList.add(contactNumber);
        }
        registeredNumber.setRegisteredNumberList(numberList);
        return registeredNumber;
    }

    private void updateRequestWithUserId(ContactNumberDTO contactNumber, String userId) {

        // update all the request raised for this contact number
        List<UserActivity> userActivityList = userActivityService.findByInitiateContactNumber(contactNumber);
        for (UserActivity userActivity : PeopleUtils.emptyIfNull(userActivityList)) {
            userActivity.setActivityForId(userId);
        }
        if (!PeopleUtils.isNullOrEmpty(userActivityList)) {
            userActivityService.createMultipleRequest(userActivityList);
        }
    }

    private UserInformationDTO getUserDataFromPeopleUserObj(PeopleUser peopleUser) {

        UserInformationDTO userInformation = new UserInformationDTO();
        userInformation.setUserId(peopleUser.getUserId());
        userInformation.setFullName(PeopleUtils.getDefaultOrEmpty(peopleUser.getFullName()));
        userInformation.setName(PeopleUtils.getDefaultOrEmpty(peopleUser.getNameValue()));
        userInformation.setFirstName(PeopleUtils.getDefaultOrEmpty(peopleUser.getFirstNameValue()));
        userInformation.setMiddleName(PeopleUtils.getDefaultOrEmpty(peopleUser.getMiddleNameValue()));
        userInformation.setLastName(PeopleUtils.getDefaultOrEmpty(peopleUser.getLastNameValue()));
        userInformation.setDepartment(PeopleUtils.getDefaultOrEmpty(peopleUser.getDepartment()));
        userInformation.setCompany(PeopleUtils.getDefaultOrEmpty(peopleUser.getCompanyValue()));
        userInformation.setPosition(PeopleUtils.getDefaultOrEmpty(peopleUser.getPositionValue()));
        userInformation.setPhoneticFirstName(PeopleUtils.getDefaultOrEmpty(peopleUser.getPhoneticFirstName()));
        userInformation.setPhoneticLastName(PeopleUtils.getDefaultOrEmpty(peopleUser.getPhoneticLastName()));
        userInformation.setPhoneticMiddleName(PeopleUtils.getDefaultOrEmpty(peopleUser.getPhoneticMiddleName()));
        userInformation.setNameSuffix(PeopleUtils.getDefaultOrEmpty(peopleUser.getNameSuffix()));
        userInformation.setNamePrefix(PeopleUtils.getDefaultOrEmpty(peopleUser.getNamePrefix()));
        userInformation.setMaidenName(PeopleUtils.getDefaultOrEmpty(peopleUser.getMaidenNameValue()));
        userInformation.setNickName(PeopleUtils.getDefaultOrEmpty(peopleUser.getNickNameValue()));
        userInformation.setGender(PeopleUtils.getDefaultOrEmpty(peopleUser.getGender()));
        userInformation.setImageURL(PeopleUtils.getDefaultOrEmpty(peopleUser.getDefaultImageUrl()));
        userInformation.setNotes(PeopleUtils.getDefaultOrEmpty(peopleUser.getNotes()));
        userInformation.setTagList(peopleUser.getProfileTags());
        userInformation.setUserMetadataList(peopleUser, peopleUser.getUserMetadataList());
        return userInformation;
    }
    
    /*
      updates user names and primary information like date of birth, gender
     */
    private void updateUserPrimaryData(UserInformationDTO userInformation, PeopleUser sessionUser) {

        sessionUser.setNamePrefix(userInformation.getNamePrefix());
        sessionUser.setPhoneticFirstName(userInformation.getPhoneticFirstName());
        sessionUser.setPhoneticMiddleName(userInformation.getPhoneticMiddleName());
        sessionUser.setPhoneticLastName(userInformation.getPhoneticLastName());
        sessionUser.setNameSuffix(userInformation.getNameSuffix());
        sessionUser.setNotes(userInformation.getNotes());
        sessionUser.setGender(userInformation.getGender());
        sessionUser.setDefaultImageUrl(userInformation.getImageURL());
        sessionUser.setDepartment(userInformation.getDepartment());

    }

    private List<UserProfileData> updateUserMetaData(List<UserProfileData> toBeUpdatedMetadata, PeopleUser peopleUser) {

        // divide into new data and to be updated data
        List<UserProfileData> newDataList = new ArrayList<>();
        Map<String, UserProfileData> updateDataMap = new HashMap<>();

        List<UserProfileData> existingMetadataList = peopleUser.getUserMetadataList();
        for (UserProfileData toBeUpdatedData : PeopleUtils.emptyIfNull(toBeUpdatedMetadata)) {
            if (PeopleUtils.isNullOrEmpty(toBeUpdatedData.getValueId())) {
                newDataList.add(toBeUpdatedData);
            } else {
                updateDataMap.put(toBeUpdatedData.getValueId(), toBeUpdatedData);
            }
        }

        // iterate over user data and update the values
        List<UserProfileData> deletedValues = new ArrayList<>();
        for (UserProfileData existingMetadata : PeopleUtils.emptyIfNull(existingMetadataList)) {
            UserProfileData toBeUpdatedData = updateDataMap.getOrDefault(existingMetadata.getValueId(), null);
            if (checkIfPrimaryNumber(existingMetadata) && checkIfValueUpdate(existingMetadata, toBeUpdatedData)) {
                throw new BadRequestException(MessageCodes.INVALID_OPERATION.getValue());
            }

            if (checkIfPrimaryEmail(existingMetadata) && checkIfValueUpdate(existingMetadata, toBeUpdatedData)) {
                throw new BadRequestException(MessageCodes.INVALID_OPERATION.getValue());
            }


            if (toBeUpdatedData == null) {
                deletedValues.add(existingMetadata);
                continue;
            }
            updateValueIfRequired(existingMetadata, toBeUpdatedData);
        }

        // remove values from the list
        if (!PeopleUtils.isNullOrEmpty(deletedValues)) {
            existingMetadataList.removeAll(deletedValues);
            addDeletedValuesToConnections(peopleUser, deletedValues);
        }

        existingMetadataList.addAll(createProfileData(peopleUser, newDataList));

        return existingMetadataList;
    }

    private void addDeletedValuesToConnections(PeopleUser sessionUser, List<UserProfileData> deletedValues) {
        Map<String, UserProfileData> deletedValueIdMap = new HashMap<>();
        for (UserProfileData profileData : deletedValues) {
            deletedValueIdMap.put(profileData.getValueId(), profileData);
        }
        List<UserPrivacyProfile> userPrivacyProfiles =
                userPrivacyProfileRepository.findAllByUserIdAndValueIds(sessionUser.getUserId(),
                        new ArrayList<>(deletedValueIdMap.keySet()));

        for (UserPrivacyProfile profile : userPrivacyProfiles) {
            UserInformationDTO deletedUserInfoDTO = new UserInformationDTO();
            List<UserProfileData> deletedUserProfilesFromPrivacyProfile = new ArrayList<>();
            Set<String> existingValueIdsInProfile = Sets.intersection(deletedValueIdMap.keySet(),
                    new HashSet<>(profile.getValueIdList()));

            for (String valueId : existingValueIdsInProfile) {
                deletedUserProfilesFromPrivacyProfile.add(deletedValueIdMap.get(valueId));
            }

            deletedUserInfoDTO.setUserMetadataList(deletedUserProfilesFromPrivacyProfile);

            updateConnectionDeletedDataForProfileUsers(sessionUser, profile.getPrivacyProfileId(), deletedUserInfoDTO);
        }

    }

    private void updateConnectionDeletedDataForProfileUsers(PeopleUser sessionUser, String profileId,
                                                            UserInformationDTO deletedUserInfoDTO) {
        List<UserConnection> userConnectionList =
                userConnectionRepository.findConnectionByUserIdAndPrivacyProfileId(sessionUser.getUserId(),
                        Arrays.asList(profileId));
        Set<String> blockedUsers = sessionUser.getBlockedUserIdList();
        for (UserConnection connection : userConnectionList) {
            if (!blockedUsers.contains(connection.getConnectionFromId())) {
                UserInformationDTO informationDTO =
                        Optional.ofNullable(connection.getConnectionDeletedData()).orElse(new UserInformationDTO());
                informationDTO.setUserMetadataList(masterService.mergeMetaList(deletedUserInfoDTO.getUserMetadataList(),
                        informationDTO.getUserMetadataList()));
                connection.setConnectionDeletedData(informationDTO);
                connection.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
            }
        }

        userConnectionRepository.saveAll(userConnectionList);
    }

    private Boolean checkIfValueUpdate(UserProfileData existingData, UserProfileData toBeUpdatedData) {
        return toBeUpdatedData == null || !PeopleUtils.compareValues(existingData.getKeyValueDataList(), toBeUpdatedData.getKeyValueDataList());
    }

    private boolean updateValueIfRequired(UserProfileData existingData, UserProfileData toBeUpdatedData) {

        Boolean isSame = PeopleUtils.compareValues(existingData, toBeUpdatedData);
        if (!isSame) {
            existingData.setKeyValueDataList(toBeUpdatedData.getKeyValueDataList());
            existingData.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
            existingData.setCategory(toBeUpdatedData.getCategory());
            existingData.setLabel(toBeUpdatedData.getLabel());
            existingData.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
            if (toBeUpdatedData.getCategory().equals(UserInfoCategory.SOCIAL_PROFILE.getValue())) {
                VerificationStatusUpdateRequest updateRequest = new VerificationStatusUpdateRequest();
                updateRequest.setVerificationStatus(toBeUpdatedData.getVerification() != null ?
                        toBeUpdatedData.getVerification().getValue() :
                        UserInformationVerification.NOT_VERIFIED.toString());
                updateRequest.setSocialProfileId(toBeUpdatedData.getSocialProfileId());
                updateRequest.setSocialProfileImageURL(toBeUpdatedData.getSocialProfileImageURL());
                updateRequest.setValueId(toBeUpdatedData.getValueId());
                updateRequest.setKeyValueDataList(toBeUpdatedData.getKeyValueDataList());

                updateSocialHandleVerificationStatus(updateRequest);
            }
            return true;
        }
        return false;
    }

    private Boolean checkIfPrimaryNumber(UserProfileData existingData) {
        return UserInfoCategory.CONTACT_NUMBER.getValue().equalsIgnoreCase(existingData.getCategory())
                && existingData.getIsPrimary();
    }

    private Boolean checkIfPrimaryEmail(UserProfileData existingData) {
        return UserInfoCategory.EMAIL_ADDRESS.getValue().equalsIgnoreCase(existingData.getCategory())
                && existingData.getIsPrimary();
    }

    private List<UserProfileData> createProfileData(PeopleUser peopleUser, List<UserProfileData> toBeAddedData) {

        List<UserProfileData> newDataList = new ArrayList<>();
        Map<Integer, UserInformationVerification> socialHandleMap = peopleUser.getSocialHandleMap();
        for (UserProfileData newData : PeopleUtils.emptyIfNull(toBeAddedData)) {

            UserProfileData userProfileData = new UserProfileData();
            userProfileData.setValueId(new ObjectId().toString());
            userProfileData.setCategory(newData.getCategory());
            userProfileData.setLabel(newData.getLabel());
            userProfileData.setKeyValueDataList(newData.getKeyValueDataList());
            userProfileData.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
            if (userProfileData.getCategory().equals(UserInfoCategory.SOCIAL_PROFILE.getValue())) {
                userProfileData.setSocialProfileId(userProfileData.getSocialProfileId());
                userProfileData.setSocialProfileImageURL(userProfileData.getSocialProfileImageURL());
                if (newData.getVerification() != null) {
                    userProfileData.setVerification(newData.getVerification());
                    // updating verification Status in SocialHandle Map
                    String socialhandle = userProfileData.getSingleValueData();
                    socialHandleMap.put(socialhandle.hashCode(),
                            UserInformationVerification.valueOf(newData.getVerification().getValue()));
                    peopleUser.setSocialHandleMap(socialHandleMap);
                }
            }
            newDataList.add(userProfileData);
        }

        return newDataList;
    }

    @Override
    public void updateUserDeviceLocation(Coordinates locationCoordinates) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();

        sessionUser.setDeviceLocation(locationCoordinates);
        peopleUserRepository.save(sessionUser);

    }

    @Override
    public void updateSocialHandleVerificationStatus(VerificationStatusUpdateRequest verificationStatusUpdateRequest) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();

        Map<Integer, UserInformationVerification> socialHandleMap = sessionUser.getSocialHandleMap();
        String valueId = verificationStatusUpdateRequest.getValueId();

        List<UserProfileData> userProfileDataList = sessionUser.getUserMetadataList();

        // check if user profile data contains the valueId. If yes, set authentication to pending
        UserProfileData socialHandle = userProfileDataList.stream()
                .filter(socialHandleData -> valueId.equals(socialHandleData.getValueId()))
                .findAny()
                .orElse(null);

        if (socialHandle == null ||
                !UserInfoCategory.SOCIAL_PROFILE.getValue().equalsIgnoreCase(socialHandle.getCategory())) {
            throw new BadRequestException(MessageCodes.INTERNAL_SERVER_ERROR.getValue());
        }

        // remove social profile Id if the verification status is not "VERIFIED"
        String socialProfileId = verificationStatusUpdateRequest.getVerificationStatus()
                .equalsIgnoreCase(UserInformationVerification.VERIFIED.toString()) ?
                verificationStatusUpdateRequest.getSocialProfileId() : null;

        socialHandle.setSocialProfileId(socialProfileId);
        socialHandle.setSocialProfileImageURL(verificationStatusUpdateRequest.getSocialProfileImageURL());
        socialHandle.setKeyValueDataList(verificationStatusUpdateRequest.getKeyValueDataList());

        String socialhandle = socialHandle.getSingleValueData();
        socialHandleMap.put(socialhandle.hashCode(),
                UserInformationVerification.valueOf(verificationStatusUpdateRequest.getVerificationStatus()));
        // add social account to network settings
        String socialAccount = socialHandle.getLabel();
        NetworkCommunicationSettingStatus networkSettingStatus = sessionUser.getNetworkCommunicationTypesSelected();
        List<String> networkSharedValues = sessionUser.getNetworkSharedValueList();
        switch (socialAccount) {
            case TWITTER:
                if (!networkSettingStatus.isDefaultTwitterAccountAdded()) {
                    networkSharedValues.add(socialHandle.getValueId());
                    networkSettingStatus.setDefaultTwitterAccountAdded(true);
                }
                break;
            case LINKEDIN:
                if (!networkSettingStatus.isDefaultLinkedInAccountAdded()) {
                    networkSharedValues.add(socialHandle.getValueId());
                    networkSettingStatus.setDefaultLinkedInAccountAdded(true);
                }
                break;
            case INSTAGRAM:
                if (!networkSettingStatus.isDefaultInstagramAccountAdded()) {
                    networkSharedValues.add(socialHandle.getValueId());
                    networkSettingStatus.setDefaultInstagramAccountAdded(true);
                }
                break;
            default:
                break;
        }

        sessionUser.setSocialHandleMap(socialHandleMap);
        sessionUser.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
        peopleUserRepository.save(sessionUser);

    }

    @Override
    public void blockUser(BlockUserRequest blockUserRequest) {

        PeopleUser peopleUser = tokenAuthService.getSessionUser();

        if (peopleUser.getUserId().equals(blockUserRequest.getUserId())) {
            throw new BadRequestException(MessageCodes.INVALID_OPERATION.getValue());
        }

        Set<String> blockedUserList = peopleUser.getBlockedUserIdList();
        Boolean isBlocked = blockUserRequest.getIsBlocked();
        String userToBeBlocked = blockUserRequest.getUserId();

        // prepare shared data for blocked connection
        UserConnection userConnection = userConnectionRepository.findConnectionByFromIdAndToId(userToBeBlocked, peopleUser.getUserId());

        if (!isBlocked) {
            blockedUserList.remove(userToBeBlocked);
            peopleUser.setBlockedUserIdList(blockedUserList);
            peopleUserRepository.save(peopleUser);
            if (userConnection != null) {
                userConnection.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
                userConnectionRepository.save(userConnection);
            }
            userActivityRepository.updateBlockedStatusByInitiatorAndReceiverId(blockUserRequest.getUserId(),
                    peopleUser.getUserId());
            return;
        }

        if (!blockedUserList.contains(userToBeBlocked)) {
            blockedUserList.add(userToBeBlocked);
        }

        peopleUser.setBlockedUserIdList(blockedUserList);

        if (userConnection != null) {
            UserPrivacyProfile sharedProfile = userPrivacyProfileRepository.findByProfileIdAndUserId(
                    userConnection.getSharedProfile().getPrivacyProfileId(), peopleUser.getUserId());
            UserInformationDTO profileData = masterService.prepareUserDataBasedOnPrivacyProfile(peopleUser,
                    sharedProfile);

            StaticSharedData staticSharedData = userConnection.getStaticSharedData();
            staticSharedData.setProfileData(profileData);
            userConnection.setStaticSharedData(staticSharedData);
            userConnectionRepository.save(userConnection);

            // Fetch userConnection with UserData and User Profile
            List<UserConnection> userConnectionList = userConnectionRepository.getSharedProfileDataForSelectedContact(
                    new ArrayList<>(Arrays.asList(userConnection.getConnectionId())));

            if (userConnectionList != null && !userConnectionList.isEmpty()) {

                UserConnection updatedUserConnection = userConnectionList.get(0);

                // merging shared data into static object
                UserInformationDTO sharedData = masterService.prepareSharedData1(updatedUserConnection);
                if (updatedUserConnection.getContactStaticData() != null) {
                    masterService.mergeSharedInfoToStaticInfo(sharedData, updatedUserConnection.getContactStaticData());
                } else {
                    updatedUserConnection.setContactStaticData(sharedData);
                }

                userConnectionRepository.save(updatedUserConnection);
            }
        }
        peopleUserRepository.save(peopleUser);
    }

    @Override
    public String reportUser(ReportUserRequest reportUserRequest) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        String userIdToBeReported = reportUserRequest.getUserId();

        if (userIdToBeReported.equals(sessionUser.getUserId())) {
            throw new BadRequestException(MessageCodes.INVALID_OPERATION.getValue());
        }

        /* At first we are checking whether user is already reported,blocked or both
         * and based on that we are making decisions.
         * following cases are :
         *
         * Case 1: if a user is not blocked and not even reported,then after reporting that user, it will be added
         * to blockList as well as to reported Data Repository.
         *
         * Case 2: if a user is already blocked but not reported,then after reporting that user, the reported data will added
         * to repository.
         *
         * Case 3: if a user is not blocked but reported,then after reporting that user,it will get add to blockList and same
         * reported data object will be return.
         *
         * Case 4: if a user is already blocked as well as reported,no action will be taken.
         *
         */

        ReportedData reportedUserData = reportedUserDataRepository.findByReportedByUserIdAndReportedUserId(PeopleUtils.convertStringToObjectId(sessionUser.getUserId()),
                PeopleUtils.convertStringToObjectId(userIdToBeReported));

        Set<String> blockUserIds = sessionUser.getBlockedUserIdList();
        boolean isAddedToBlockList = blockUserIds.add(userIdToBeReported);
        sessionUser.setBlockedUserIdList(blockUserIds);

        if ((!isAddedToBlockList) && (reportedUserData != null)) {
            return messages.get(MessageConstant.ALREADY_REPORTED_USER);
        }

        if (reportedUserData == null) {
            reportedUserData = new ReportedData();
            reportedUserData.setCreatedOn(PeopleUtils.getCurrentTimeInUTC());
            reportedUserData.setReportedByUserId(sessionUser.getUserId());
            reportedUserData.setReportedUserId(userIdToBeReported);
            reportedUserData.setReportMessage(reportUserRequest.getReportMessage());
            reportedUserData.setReportDataType(ReportDataType.USER);
            reportedUserDataRepository.save(reportedUserData);
        }

        if (isAddedToBlockList) {
            peopleUserRepository.save(sessionUser);
        }

        return messages.get(MessageConstant.USER_REPORTED_SUCCESSFULLY);

    }

    @Override
    public InviteByNumberResponseDTO inviteByNumber(InviteByNumberRequest inviteByNumberRequest) {

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        /*
         * Invitee contact number after applying set of validation rules
         * Validation rules
         * 1. Contact must be of USA or Canada
         * 2. Contact must not be registered with watu app
         * 3. Invite can only be sent once
         * */
        ContactNumberDTO inviteeContactNumber = validateInviteeContactAndCheckForSentInvitation(sessionUser,
                inviteByNumberRequest.getInviteeContactInformation());

        // Creating new contact and setting its connection_status as 'PENDING'
        UserConnection newStaticContact = userConnectionService.createNewContact(sessionUser.getUserId(),
                inviteByNumberRequest.getInviteeContactInformation());
        newStaticContact.setConnectionStatus(ConnectionStatus.PENDING);
        newStaticContact = userConnectionRepository.save(newStaticContact);

        SharedProfileInformationData sharedProfileData = Optional.ofNullable(inviteByNumberRequest.getSharedPrivacyProfileKey())
                .orElse(userConnectionService.getDefaultSharedProfileData(sessionUser.getUserId()));

        createActivityForSendingInvite(sessionUser, inviteeContactNumber, newStaticContact.getConnectionId(), sharedProfileData);

        // prepare SMS payload
        Object[] messageParam = new Object[]{sessionUser.getNameValue(), appLink};
        notificationService.prepareSMSPayloadAndSendToQueue(inviteeContactNumber, SMSTemplateKeys.APPLICATION_JOIN_INVITATION,
                messageParam);

        return contactInviteResponse(newStaticContact);
    }

    @Override
    public FetchConnectionListResponseDTO getBlockedUserList(String searchString, Integer fNameOrder, Integer lNameOrder,
                                                             Boolean lNamePreferred, Integer pageNumber, Integer pageSize) {


        Pageable pageable = PageRequest.of(pageNumber, pageSize, getSortOperationForFavouriteContacts(
                fNameOrder, lNameOrder, lNamePreferred));

        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        List<String> blockedList = new ArrayList<>(sessionUser.getBlockedUserIdList());

        List<UserContactData> contactDataList = new ArrayList<>();

        // listing all session users connection to blocked contacts
        List<UserConnection> connectionList = userConnectionRepository.getConnectionDataWithProfileForSelectedToUserIds(
                sessionUser.getUserId(), blockedList);

        // mapping blocked userId with session users connection to blocked contact
        Map<String, UserConnection> blockList = new HashMap<>();
        for (UserConnection userConnection : connectionList) {
            blockList.put(userConnection.getConnectionToId(), userConnection);
        }

        Page<PeopleUser> usersList = peopleUserRepository.getUserDetailsForSelectedUserIds(blockedList,
                searchString.trim(), pageable);

        for (PeopleUser peopleUser : usersList.getContent()) {
            if (blockList.get(peopleUser.getUserId()) != null) {
                //prepare connection details for contact
                UserConnection userContact = blockList.get(peopleUser.getUserId());

                switch (userContact.getConnectionStatus()) {
                    case NOT_CONNECTED:
                    case PENDING:
                        UserContactData contactData = userConnectionService.prepareContactStaticData(sessionUser, userContact);
                        UserInformationDTO contactStaticData = contactData.getStaticProfileData();
                        if (contactStaticData != null) {
                            userConnectionService.populateStaticDataWithIsVerifiedInfo(
                                    contactStaticData, masterService.getRegisteredNumberList());
                        }
                        contactDataList.add(contactData);
                        break;
                    case CONNECTED:
                        contactDataList.add(userConnectionService.prepareContactSharedData(sessionUser, userContact));
                        break;
                    default:
                        break;
                }
            } else {
                // prepare contacts public info
                UserContactData userPublicData = new UserContactData();
                userPublicData.setPublicProfileData(masterService.prepareUserPublicData(peopleUser));
                userPublicData.setIsBlocked(true);
                userPublicData.setToUserId(peopleUser.getUserId());
                contactDataList.add(userPublicData);
            }
        }

        // prepare response
        FetchConnectionListResponseDTO response = new FetchConnectionListResponseDTO();
        response.setContactList(contactDataList);
        response.setLastSyncedTime(PeopleUtils.getCurrentTimeInUTC());
        response.setTotalElements(usersList.getTotalElements());
        response.setTotalNumberOfPages(usersList.getTotalPages());
        if (!usersList.isLast()) {
            response.setNextURL(ControllerLinkBuilder.linkTo(ControllerLinkBuilder
                    .methodOn(PeopleUserController.class)
                    .getBlockedUserList(searchString, fNameOrder, lNameOrder, lNamePreferred,
                            (pageNumber + 1), pageSize, ""))
                    .withSelfRel().getHref());
        }

        return response;
    }

    @Override
    public UserSettingsResponseDTO getUserSettings() {
        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        UserSettingsResponseDTO responseDTO = new UserSettingsResponseDTO();
        responseDTO.setIsPushNotificationEnabled(sessionUser.getIsPushNotificationEnabled());
        return responseDTO;

    }

    private void sendVerificationEmail(String name, String emailId, String link) {
        if (emailId == null) {
            return;
        }
        emailService.sendTemplatedEmail("EmailVerifyTemplateNew", "{\"verification-link\":\"" + link + "\",\"firstName" +
                "\":\"" + name + "\"}", emailId);
    }

    private void sendOtpEmail(String otp, String emailId) {

        if (emailId == null) {
            return;
        }
        emailService.sendTemplatedEmail("OTPTemplateNew", "{\"otp\":\"" + otp + "\"}", emailId);
    }


    private void updatePrivacyProfileImage(UserInformationDTO userInformation, PeopleUser sessionUser) {
        if (userInformation.getImageURL() != null && !userInformation.getImageURL().equals(sessionUser.getDefaultImageUrl())) {
            userPrivacyProfileRepository.updatePrivacyProfileDefaultImage(sessionUser.getUserId(), userInformation.getImageURL());
        } else if (userInformation.getImageURL() == null && sessionUser.getDefaultImageUrl() != null) {
            userPrivacyProfileRepository.updatePrivacyProfileDefaultImage(sessionUser.getUserId(), null);
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

    private List<String> getAllPrivacyProfileIds(List<UserPrivacyProfile> userPrivacyProfiles) {
        List<String> privacyProfileIds = new ArrayList<>();
        for (UserPrivacyProfile userPrivacyProfile : userPrivacyProfiles) {
            privacyProfileIds.add(userPrivacyProfile.getPrivacyProfileId());
        }
        return privacyProfileIds;
    }

    private List<String> getCompanySharedPrivacyProfileIds(List<UserPrivacyProfile> userPrivacyProfiles) {
        List<String> privacyProfileIds = new ArrayList<>();
        for (UserPrivacyProfile userPrivacyProfile : userPrivacyProfiles) {
            if (userPrivacyProfile.getIsCompanyShared()) {
                privacyProfileIds.add(userPrivacyProfile.getPrivacyProfileId());
            }
        }
        return privacyProfileIds;
    }

    private List<String> getPositionSharedPrivacyProfileIds(List<UserPrivacyProfile> userPrivacyProfiles) {
        List<String> privacyProfileIds = new ArrayList<>();
        for (UserPrivacyProfile userPrivacyProfile : userPrivacyProfiles) {
            if (userPrivacyProfile.getIsPositionShared()) {
                privacyProfileIds.add(userPrivacyProfile.getPrivacyProfileId());
            }
        }
        return privacyProfileIds;
    }

    private List<String> getNickNameSharedPrivacyProfileIds(List<UserPrivacyProfile> userPrivacyProfiles) {
        List<String> privacyProfileIds = new ArrayList<>();
        for (UserPrivacyProfile userPrivacyProfile : userPrivacyProfiles) {
            if (userPrivacyProfile.getIsNickNameShared()) {
                privacyProfileIds.add(userPrivacyProfile.getPrivacyProfileId());
            }
        }
        return privacyProfileIds;
    }

    private List<String> getMaidenNameSharedPrivacyProfileIds(List<UserPrivacyProfile> userPrivacyProfiles) {
        List<String> privacyProfileIds = new ArrayList<>();
        for (UserPrivacyProfile userPrivacyProfile : userPrivacyProfiles) {
            if (userPrivacyProfile.getIsMaidenNameShared()) {
                privacyProfileIds.add(userPrivacyProfile.getPrivacyProfileId());
            }
        }
        return privacyProfileIds;
    }

    private List<String> getUpdatedValueIds(List<UserProfileData> toBeUpdatedMetadata, PeopleUser peopleUser) {

        Set<String> updatedValueIds = new HashSet<>();

        // divide into new data and to be updated data
        Map<String, UserProfileData> updateDataMap = new HashMap<>();

        List<UserProfileData> existingMetadataList = peopleUser.getUserMetadataList();
        for (UserProfileData toBeUpdatedData : PeopleUtils.emptyIfNull(toBeUpdatedMetadata)) {
            if (!PeopleUtils.isNullOrEmpty(toBeUpdatedData.getValueId())) {
                updateDataMap.put(toBeUpdatedData.getValueId(), toBeUpdatedData);
            }
        }

        for (UserProfileData existingMetadata : PeopleUtils.emptyIfNull(existingMetadataList)) {
            UserProfileData toBeUpdatedData = updateDataMap.getOrDefault(existingMetadata.getValueId(), null);

            // toBeUpdatedData == null -> deleted valueIds
            // PeopleUtils.compareValues(existingMetadata, toBeUpdatedData) -> data modified
            if (toBeUpdatedData == null || !existingMetadata.getKeyValueDataList().toString().equals(toBeUpdatedData.getKeyValueDataList().toString())) {
                updatedValueIds.add(existingMetadata.getValueId());
            }
        }

        return new ArrayList<>(updatedValueIds);
    }

    // This method will check the connection status
    // between the 'initiator' and 'searched' user
    @Override
    public String checkAndSetConnectionStatus(PeopleUser initiator, PeopleUser searchedUser) {
        // check if already these users are connected
        UserConnection existingUserConnection = userConnectionRepository.findConnectionByFromIdAndToId(
                initiator.getUserId(), searchedUser.getUserId());
        if (existingUserConnection != null) {
            return ConnectionStatus.CONNECTED.getValue();
        }

        // Check if there is a pending "CONNECTION_REQUEST"
        UserActivity pendingConnectionRequestActivity = userActivityRepository.
                getPendingConnectionRequestActivity(initiator.getUserId(), searchedUser.getVerifiedContactNumber());
        if (pendingConnectionRequestActivity != null) {
            return ConnectionStatus.PENDING.getValue();
        }

        return ConnectionStatus.NOT_CONNECTED.getValue();
    }

    private void updateDefaultSystemProfiles(PeopleUser peopleUser) {
        // fetch all pre defined system profiles
        List<SystemPrivacyProfile> systemProfilesList = systemPrivacyProfileRepository.findAll();
        List<UserPrivacyProfile> userProfilesList = userPrivacyProfileRepository.findSystemProfilesForUser(peopleUser.getUserId());

        Map<ProfileKey, List<String>> profileKeyListMap = peopleUser.getProfileKeyMap();

        for (UserPrivacyProfile userPrivacyProfile : userProfilesList) {
            for (SystemPrivacyProfile systemPrivacyProfile : systemProfilesList) {
                if (userPrivacyProfile.getProfileName().equals(systemPrivacyProfile.getProfileName())) {
                    List<String> valueIdList = new ArrayList<>();
                    for (ProfileKey profileKey : PeopleUtils.emptyIfNull(systemPrivacyProfile.getProfileKeyList())) {
                        if (profileKeyListMap.containsKey(profileKey)) {
                            valueIdList.addAll(profileKeyListMap.get(profileKey));
                        }
                    }
                    userPrivacyProfile.setValueIdList(valueIdList);
                }
            }
        }
        userPrivacyProfileRepository.saveAll(userProfilesList);
    }

    private void updateAndNotifyConnections(PeopleUser sessionUser, List<String> profileIds) {
        List<UserConnection> userConnections = userConnectionRepository.getConnectionsByPeopleUserToIdAndSharedProfileIds(
                sessionUser.getUserId(), profileIds);

        List<SQSPayload> sqsPayloadList = new ArrayList<>();
        ActivityType activityType = new ActivityType();
        activityType.setRequestType(RequestType.UPDATE_CONTACT_ACTIVITY);
        activityType.setActionTaken(Action.INITIATED);
        DateTime currentDateTime = PeopleUtils.getCurrentTimeInUTC();
        Set<String> blockedUsers = sessionUser.getBlockedUserIdList();
        for (UserConnection userConnection : userConnections) {
            if (blockedUsers.contains(userConnection.getConnectionFromId()) ||
                    masterService.isUserBlockedByContact(userConnection.getConnectionFromId(), sessionUser.getUserId())) {
                continue;
            }
            // Create activity for a connected contact
              UserActivity userActivity = new UserActivity();
              userActivity.setActivityForId(userConnection.getConnectionFromId());
              userActivity.setActivityById(sessionUser.getUserId());
              userActivity.setActivityType(activityType);
              userActivity.setOverallStatus(ActivityStatus.ACTIVE);
              userActivity.setCreatedOn(currentDateTime);
              userActivity.setLastUpdatedOn(currentDateTime);
              
              List<UserActivity> userActivities = userActivityRepository.getPendingActivitiesByInitiatedByIdAndRequestType(
                      userConnection.getConnectionFromId(), RequestType.UPDATE_CONTACT_ACTIVITY);
              
              if(userActivities!=null) {
                  for(UserActivity activity : PeopleUtils.emptyIfNull(userActivities)) {
                      userActivityRepository.deleteById(activity.getActivityId());
                  }   
              }
              
              userActivityRepository.save(userActivity);

              sqsPayloadList.add(privacyProfileService.prepareSQSPayloadForUpdateContactActivity(userActivity,
            		  sessionUser, userConnection.getConnectionId()));

            // update lastUpdatedOn for all to reflect in delta API
            userConnection.setLastUpdatedOn(currentDateTime);

        }

        userConnectionRepository.saveAll(userConnections);
        queueService.sendPayloadToSQS(sqsPayloadList);
    }

    private void setFullNameForUser(PeopleUser sessionUser) {
        if (sessionUser.getFirstNameValue() != null && sessionUser.getLastNameValue() != null) {
            sessionUser.setFullName(StringUtils.capitalize(sessionUser.getFirstNameValue().toLowerCase()).concat(" ")
                    .concat(StringUtils.capitalize(sessionUser.getLastNameValue().toLowerCase())));
        } else if (sessionUser.getFirstNameValue() != null) {
            sessionUser.setFullName(StringUtils.capitalize(sessionUser.getFirstNameValue().toLowerCase()));
        } else if (sessionUser.getLastNameValue() != null) {
            sessionUser.setFullName(StringUtils.capitalize(sessionUser.getLastNameValue().toLowerCase()));
        }

    }

    private void updatePrimaryEmailToNetworkDefaults(PeopleUser sessionUser) {
        // updating primaryEmail as networkDefault
        List<String> networkSharedValues = sessionUser.getNetworkSharedValueList();
        List<UserProfileData> userProfileDataList = sessionUser.getUserMetadataList();
        Map<String, UserProfileData> metaDataMap = sessionUser.getMetadataMap();
        NetworkCommunicationSettingStatus networkCommunicationSettingStatus = sessionUser.getNetworkCommunicationTypesSelected();
        for (String networkSharedValue : networkSharedValues) {
            if (metaDataMap.get(networkSharedValue) != null) {
                UserProfileData userProfileData = metaDataMap.get(networkSharedValue);
                if (userProfileData.getCategory().equalsIgnoreCase(UserInfoCategory.EMAIL_ADDRESS.getValue())) {
                    return;
                }
            }
        }

        for (UserProfileData userData : userProfileDataList) {
            if (userData.getIsPrimary()
                    && userData.getCategory().equalsIgnoreCase(UserInfoCategory.EMAIL_ADDRESS.getValue())) {
                networkSharedValues.add(userData.getValueId());
                networkCommunicationSettingStatus.setDefaultEmailAdded(Boolean.TRUE);
                break;
            }
        }

    }

    private void createActivityForSendingInvite(PeopleUser sessionUser, ContactNumberDTO inviteeContactNumber,
                                                String connectionId, SharedProfileInformationData sharedProfile) {

        UserActivity userActivity = new UserActivity();
        userActivity.setActivityById(sessionUser.getUserId());

        ActivityType activityType = new ActivityType();
        activityType.setRequestType(RequestType.CONNECTION_REQUEST);
        activityType.setActionTaken(Action.INITIATED);
        userActivity.setActivityType(activityType);
        userActivity.setOverallStatus(ActivityStatus.PENDING);
        userActivity.setSharedProfileInformationData(sharedProfile);

        UserContact initiateDetails = new UserContact();
        initiateDetails.setContactNumber(inviteeContactNumber);
        initiateDetails.setConnectionId(connectionId);
        userActivity.setInitiateDetails(initiateDetails);

        userActivity.setConnectionId(connectionId);
        userActivity.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
        userActivity.setCreatedOn(PeopleUtils.getCurrentTimeInUTC());
        userActivityRepository.save(userActivity);
    }

    private InviteByNumberResponseDTO contactInviteResponse(UserConnection userContact) {
        InviteByNumberResponseDTO response = new InviteByNumberResponseDTO();
        if (userContact.getContactStaticData() != null) {
            // updating verification status of phone number
            userConnectionService.populateStaticDataWithIsVerifiedInfo(userContact.getContactStaticData(),
                    masterService.getRegisteredNumberList());
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

        response.setUserContact(userContactData);
        return response;
    }

    private UserConnection getConnectionFromPriorityMap(Map<String, UserConnection> connectionsWithPriorityMap) {

        UserConnection userConnection = null;
        if (connectionsWithPriorityMap.containsKey(ContactPriority.PRIORITY_LABEL_MOBILE.getValue())) {
            userConnection = connectionsWithPriorityMap.get(ContactPriority.PRIORITY_LABEL_MOBILE.getValue());
        } else if (connectionsWithPriorityMap.containsKey(ContactPriority.PRIORITY_LABEL_IPHONE.getValue())) {
            userConnection = connectionsWithPriorityMap.get(ContactPriority.PRIORITY_LABEL_IPHONE.getValue());
        } else if (connectionsWithPriorityMap.containsKey(ContactPriority.PRIORITY_LABEL_MAIN.getValue())) {
            userConnection = connectionsWithPriorityMap.get(ContactPriority.PRIORITY_LABEL_MAIN.getValue());
        } else if (connectionsWithPriorityMap.containsKey(ContactPriority.PRIORITY_LABEL_HOME.getValue())) {
            userConnection = connectionsWithPriorityMap.get(ContactPriority.PRIORITY_LABEL_HOME.getValue());
        } else if (connectionsWithPriorityMap.containsKey(ContactPriority.PRIORITY_LABEL_WORK.getValue())) {
            userConnection = connectionsWithPriorityMap.get(ContactPriority.PRIORITY_LABEL_WORK.getValue());
        }

        return userConnection;
    }

    private UserContactData validateConnectionsWithSearchedNumber(PeopleUser sessionUser,
                                                                  ContactNumberDTO numberToBeSearched,
                                                                  List<UserConnection> userConnectionsWithSearchedNumber) {
        Map<String, UserConnection> connectionsWithPriorityMap = new HashMap<>();

        for (UserConnection userConnection : userConnectionsWithSearchedNumber) {

            /* this will filter the profile information per user connection and retain only those data which has searched number */
            List<UserProfileData> profileData = userConnection.getContactStaticData().getUserMetadataList().stream()
                    .filter(userProfileData ->
                            userProfileData.getCategory().equalsIgnoreCase("phoneNumber")
                                    && (userProfileData.getContactNumber()
                                    .getMobileNumber().equals(numberToBeSearched.getMobileNumber()))
                    ).collect(Collectors.toList());

            if (!PeopleUtils.isNullOrEmpty(profileData)) {
                setPriorityForConnection(connectionsWithPriorityMap, profileData, userConnection);

                // exit operation if high priority connection data is found
                if (connectionsWithPriorityMap.containsKey(ContactPriority.PRIORITY_LABEL_MOBILE.getValue())) {
                    return userConnectionService.getUserContactDataList(sessionUser, Collections.singletonList(userConnection),
                            null, masterService.getRegisteredNumberList()).get(0);
                }
            }

        }

        // connection with highest priority in the priority map
        UserConnection searchedContact = getConnectionFromPriorityMap(connectionsWithPriorityMap);

        if (searchedContact != null) {
            return userConnectionService.getUserContactDataList(sessionUser, Collections.singletonList(searchedContact),
                    null, masterService.getRegisteredNumberList()).get(0);
        }

        return null;
    }

    private void setPriorityForConnection(Map<String, UserConnection> connectionsWithPriorityMap,
                                          List<UserProfileData> listOfProfileData, UserConnection userConnection) {

        for (UserProfileData userProfileData : listOfProfileData) {
            String label = userProfileData.getLabel();
            // connection prioritized based on labels assigned to its profile data
            if (!connectionsWithPriorityMap.containsKey(label)) {
                connectionsWithPriorityMap.put(userProfileData.getLabel(), userConnection);
            }
        }

    }

    private ContactNumberDTO validateInviteeContactAndCheckForSentInvitation(PeopleUser sessionUser, UserInformationDTO inviteeDetails) {
        List<UserProfileData> inviteeProfile = inviteeDetails.getUserMetadataList();
        ContactNumberDTO inviteeContact = inviteeProfile.get(0).getContactNumber();
        if (!inviteeContact.getCountryCode().equals(CountryCode.US_CANADA_COUNTRY_CODE.getValue()) ||
                (inviteeContact.getPhoneNumber().length() != 10)) {
            throw new BadRequestException(messages.get(MessageCodes.INVALID_COUNTRY_CODE_OR_PHONE_NUMBER.getValue()));
        }
        // check if invitee number is registered and invalidate operation
        PeopleUser peopleUser = findUserByContactNumber(inviteeContact);
        if (peopleUser != null) {
            throw new BadRequestException(MessageCodes.INVALID_OPERATION.getValue());
        }

        // check if invitation already sent to contact
        if (userActivityRepository.getPendingConnectionRequestActivityByContactNumber(sessionUser.getUserId(), inviteeContact) != null) {
            throw new BadRequestException(messages.get(MessageCodes.INVITATION_ALREADY_SENT.getValue()));
        }

        return inviteeContact;
    }

    @Override
    public String generateQRCodeByUserId() throws WriterException, IOException {
        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        String qrCodeLink = generateAppOpenerLink(sessionUser.getUserId());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        generateQRcodewithLogo(qrCodeLink + "?userId=" + sessionUser.getUserId() +
                "&contactNumber=" + sessionUser.getVerifiedContactNumber().getPhoneNumber() +
                "&countryCode=" + sessionUser.getVerifiedContactNumber().getCountryCode(), bos);
        String imageBase64 = Base64.getEncoder().encodeToString(bos.toByteArray()); // base64 encode

        JSONObject json = new JSONObject();
        json.put("qrCodeBase64", imageBase64);
        return json.toString();
    }
    
    private String generateQRCodeByUser(PeopleUser sessionUser) throws WriterException, IOException {
        String qrCodeLink = generateAppOpenerLink(sessionUser.getUserId());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        generateQRcodewithLogo(qrCodeLink + "?userId=" + sessionUser.getUserId() +
                "&contactNumber=" + sessionUser.getVerifiedContactNumber().getPhoneNumber() +
                "&countryCode=" + sessionUser.getVerifiedContactNumber().getCountryCode(), bos);
        String imageBase64 = Base64.getEncoder().encodeToString(bos.toByteArray()); // base64 encode

        JSONObject json = new JSONObject();
        json.put("qrCodeBase64", imageBase64);
        return json.toString();
    }

    public String generateAppOpenerLink(String userId) {
        Map<String, Object> map = new HashMap<>();
        map.put("branch_key", branchIOKey);
        map.put("channel", branchIOChannel);
        map.put("feature", branchIOFeature);
        map.put("data", Collections.singletonMap("userID", userId));

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        RestTemplate restTemplate = new RestTemplate();
        Map<String, String> mapQRCodeLink = restTemplate.exchange(banchIOLink, HttpMethod.POST,
                new HttpEntity<>(map, httpHeaders), Map.class).getBody();
        return mapQRCodeLink.get("url");
    }

//    public void generateQRcode(String data, OutputStream outputStream) throws WriterException, IOException
//    {
//        System.out.println("data : " + data);
//        Map<EncodeHintType, ErrorCorrectionLevel> hashMap = new HashMap<EncodeHintType, ErrorCorrectionLevel>();
//        hashMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
//        BitMatrix matrix = new MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, 200, 200);
//        MatrixToImageWriter.writeToStream(matrix, "png", outputStream);
//    }
    
    /* generateQRcodewithLogo  */
    public void generateQRcodewithLogo(String data, OutputStream outputStream) throws WriterException, IOException {
    	Map<EncodeHintType, ErrorCorrectionLevel> hints = new HashMap<EncodeHintType, ErrorCorrectionLevel>();
		hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

		QRCodeWriter writer = new QRCodeWriter();
		BitMatrix bitMatrix = null;

		bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 200, 200, hints);
		MatrixToImageConfig config = new MatrixToImageConfig(MatrixToImageConfig.BLACK, MatrixToImageConfig.WHITE);
		BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix, config);

		// load logo image
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		InputStream input = classLoader.getResourceAsStream("image/qrlogo.jpg");
		BufferedImage logoImage = ImageIO.read(input);

		int finalImageHeight = qrImage.getHeight() - logoImage.getHeight();
		int finalImageWidth = qrImage.getWidth() - logoImage.getWidth();

		// combine image
		BufferedImage combined = new BufferedImage(qrImage.getHeight(), qrImage.getWidth(),
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) combined.getGraphics();

		// qr code position
		g.drawImage(qrImage, 0, 0, null);
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
		g.drawImage(logoImage, (int) Math.round(finalImageWidth / 2), (int) Math.round(finalImageHeight / 2), null);

		ImageIO.write(combined, "png", outputStream);
	}
}

