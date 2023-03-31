package com.peopleapp.service;

import com.peopleapp.dto.APIRequestParamData;
import com.peopleapp.dto.RemoveMemberFromNetworkDTO;
import com.peopleapp.dto.UserNetworkDetails;
import com.peopleapp.dto.requestresponsedto.*;

import java.util.List;
import java.util.Set;

public interface NetworkService {

    GetUserNetworksResponseDTO getUserNetworks(Integer pageNumber, Integer pageSize);

    CreateOrEditNetworkResponseDTO createNetwork(CreateNetworkRequestDTO createNetworkRequest);

    JoinNetworkResponseDTO joinNetwork(JoinNetworkRequestDTO joinNetworkRequestDTO);

    UserNetworkDetails getNetworkDetails(String networkId);

    String inviteMembersToNetwork(NetworkInviteRequestDTO networkInviteRequest);

    String handleNetworkJoinRequest(NetworkJoinRequestDTO acceptRequest);

    String removeMemberFromNetwork(RemoveMemberFromNetworkDTO removeMemberFromNetwork);

    String promoteAdminsToNetwork(NetworkAdminPromoteDTO promoteToNetworkAdmin);

    String acceptInvitation(AcceptInvitationRequestDTO invitationRequest);

    NetworkPendingRequestDetailsDTO fetchJoinRequestDetailsForNetwork(String networkId, Integer pageNumber,
                                                                      Integer pageSize);

    CreateOrEditNetworkResponseDTO editNetworkDetails(EditNetworkRequestDTO createOrEditNetworkRequest);

    void leaveNetwork(LeaveNetworkRequestDTO leaveNetworkRequest);

    String reportNetwork(ReportNetworkRequestDTO reportNetworkRequest);

    String shareNetwork(ShareNetworkRequestDTO shareNetworkRequest);

    void deleteNetwork(DeleteNetworkRequestDTO deleteNetworkRequest);

    String demoteAdmins(DemoteAdminRequestDTO removeAdminRequest);

    void transferOwnership(TransferOwnerShipRequestDTO transferOwnerShipRequest);

    void updateNetworkSetting(UpdateNetworkSettingDTO updateNetworkSetting);

    List<String> getNetworkDefaultSetting();

    String updateNetworkFavouriteStatus(UpdateNetworkFavouriteRequestDTO updateNetworkFavouriteRequestDTO);

    /* Fetches list of recommended networks for specified Network Category */
    RecommendedNetworksResponseDTO getRecommendedNetworks(String networkCategory);

    /* Fetches top recommended networks */
    Set<UserNetworkDetails> getTopRecommendedNetworks();

    NetworkMembersResponseDTO getMembersOfNetwork(String networkId, APIRequestParamData apiRequestParamData);

    NetworkMembersResponseDTO getAdminsOfNetwork(String networkId, APIRequestParamData apiRequestParamData);

    void messageAllMembers(MessageNetworkMembersDTO messageNetworkMembersDTO);

    /* Searches for Network with name containing input searchString*/
    SearchedNetworkResponseDTO searchNetwork(String searchString, Integer sortOrder, Integer pageNumber, Integer pageSize);

}
