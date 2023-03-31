package com.peopleapp.service;

import com.peopleapp.dto.ActivityDetails;
import com.peopleapp.dto.SharedProfileInformationData;
import com.peopleapp.dto.UserContactData;
import com.peopleapp.dto.UserInformationDTO;
import com.peopleapp.dto.requestresponsedto.*;
import com.peopleapp.dto.requestresponsedto.ContactContactIDRequest;
import com.peopleapp.dto.requestresponsedto.RestoreCountResponse;
import com.peopleapp.model.PeopleUser;
import com.peopleapp.model.UserConnection;
import io.swagger.models.auth.In;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;
import java.util.Set;


public interface UserConnectionService {

    ContactSyncResponseDTO syncContacts(ContactSyncRequestDTO contactSyncDTO);
    
    void deleteAllContactByPeopleUId();

    /**
     * @param pageNumber first=0 (0 based)
     * @param pageSize preferred=10
     */
    ContactRestoreListDTO restoreContacts(Integer pageNumber, Integer pageSize, boolean isPageable);

    RestoreCountResponse getContactBackupDetail();

    BluetoothConnectionDetailsResponseDTO checkBluetoothConnectionDetails(String bluetoothToken);

    SendConnectionRequestResponse sendConnectionRequest(SendConnectionRequest sendConnectionRequest);

    ActivityDetails moreInfoRequest(RequestMoreInfoDTO requestMoreInfoDTO);

    void introduceContactRequest(SendSingleIntroRequestDTO sendSingleIntroRequest);

    void introduceContactToEachOtherRequest(SendMultiIntroRequestDTO sendMultiIntroRequest);

    String shareContact(ShareContactRequest shareContactRequest);

    void shareLocation(ShareLocationRequest shareLocationRequest);

    /* services related to accept request */
    AcceptConnectionResponseDTO acceptConnectionRequest(AcceptConnectionRequestDTO acceptConnectionRequest);

    /* Services related to update contact/s */
    EditStaticDataResponseDTO updateContactStaticData(EditStaticDataRequestDTO editStaticDataRequest);

    void changePrivacyProfileForConnection(ChangePrivacyProfileRequestDTO changePrivacyProfileRequest);

    DeleteContactResponse deleteContact(DeleteContactRequest deleteContactRequest);

    AddContactsToGroupResponseDTO addContactToGroup(AddContactsToGroupRequestDTO addContactsToGroupRequest);

    ContactListDTO removeContactFromGroup(RemoveContactsFromGroupRequestDTO removeContactsFromGroupRequest);

    FavouriteContactsResponseDTO setFavouriteForContact(UpdateFavouriteRequestDTO updateFavouriteRequest);

    /* Services related to fetch details */
    FetchConnectionListResponseDTO getConnectionList(DateTime lastSyncedTime, Integer pageNumber,
                                                     Integer pageSize, boolean returnOnlyMeta,String sort);

    List<String> updateContactImage(UpdateContactImageRequest updateImageRequest);

    RemoveConnectionResponse removeConnection(RemoveConnectionRequest removeConnectionRequest);

    /* overwrites favourites list with the list provided */
    List<String> manageFavouritesForContact(ManageFavouritesRequestDTO manageFavouritesRequestDTO);

    /* returns the paginated and sorted list of favourite contacts, sorting is done on fName and lName */
    FetchFavouritesListResponseDTO getFavouritesList(Integer fNameOrder, Integer lNameOrder, Boolean lNamePreferred,
                                                     Integer pageNumber, Integer pageSize);

    /* populates static data with verification status */
    void populateStaticDataWithIsVerifiedInfo(UserInformationDTO staticContactData, Set<String> numberList);
    
    /* populates static data with verification status and set UserId*/
    void populateStaticDataWithIsVerifiedInfoSetUserId(UserInformationDTO staticContactData, Set<String> numberList,UserContactData contactData,PeopleUser sessionUser);

    /* prepares shared data for the contact */
    UserContactData prepareContactSharedData(PeopleUser peopleUser, UserConnection userContact);
    
    UserContactData prepareUpdateContactSharedData(PeopleUser peopleUser, UserConnection userContact);

    /* prepares static data for the contact */
    UserContactData prepareContactStaticData(PeopleUser peopleUser, UserConnection userContact);

    void mergeContacts(MergeContactsRequestDTO mergeContactsRequest);

    ConnectionDetailsResponseDTO fetchConnectionDetails(String connectionId);

    Map<String, List<String>> prepareConnectionIdToGroupIdMap(String groupOwnerId);

    DeletedInfoResponseDTO deleteInfo(DeleteInfoRequestDTO requestDTO);

    UserConnection createNewContact(String userId, UserInformationDTO contactData);

    SharedProfileInformationData getDefaultSharedProfileData(String userId);

    List<UserContactData> getUserContactDataList(PeopleUser sessionUser, List<UserConnection> deltaContactList,
                                                 Map<String, List<String>> contactToGroupMap, Set<String> numberList);
    
    ContactContactIDRequest updateContactId(ContactContactIDRequest request);
    
    DeleteContactRequest IdenticalFlagUpdate(DeleteContactRequest contactRequest);

}
