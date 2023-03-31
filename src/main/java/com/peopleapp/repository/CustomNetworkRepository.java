package com.peopleapp.repository;

import com.mongodb.client.result.UpdateResult;
import com.peopleapp.dto.UserNetworkDetails;
import com.peopleapp.model.Network;
import com.peopleapp.model.NetworkMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomNetworkRepository {

    Page<UserNetworkDetails> getUserNetworks(String userId, Pageable pageable);

    List<UserNetworkDetails> getUserNetworks(String userId);

    Page<NetworkMember> getNetworkMemberDetailsByIdAndRole(String networkId, String searchString,
                                                           List<String> roles, Pageable pageable);

    NetworkMember findByIdAndUserIdAndStatus(String networkId, String userId, String status);

    List<NetworkMember> findByIdAndRole(String networkId, List<String> memberRole);

    /* Update roles of all the members given by list of member ids  from current to expected*/
    UpdateResult updateMemberRolesForANetwork(String networkId, List<String> memberId,String currentRole,  String toBeUpdatedRole);

    List<NetworkMember> findAllActiveNonAdminMembersByNetworkIdAndMemberId(String networkId, List<String> memberId);

    UserNetworkDetails getUserOwnedNetworkByName(String userId, String networkName);

    /* Returns the most popular networks(which user is not part of) sorted descending by Members Count */
    List<Network> getMostPopularNetworksForUserByCategory(String userId, String networkCategory, int limit);

    /* Returns the top most popular networks sorted descending by Members Count */
    List<Network> getTopMostPopularNetworksForUser(String userId, int limit);

    void updateNetworkMembersStatus(String networkId, String status);

    /* Returns list of networks in the defined radius */
    List<Network> getLocalNetworksForUser(String userId, double latitude, double longitude, double distanceInMiles, int limit);

    /* Returns all the networks for a category in the defined radius */
    List<Network> getLocalNetworksForUserByCategory(String userId, String networkCategory, double latitude,
                                                double longitude,
                                              double distanceInMiles, int limit);

    /* Return all network whose name contains searchString */
    Page<Network> searchNetwork(String userId, String searchString, int sortOrder, Pageable pageable);

    /* find network for which user is part of it, based on role*/
    long findCountOfUserEnrolledNetwork(String userId, String memberRole);

    /* Delete the member after they are removed or if they leave network */
    void deleteNetworkMemberByMemberIdsAndNetworkId(List<String> memberIds, String networkId);

    /* Returns list of Networks by NetworkIds for which user is not part of and has not sent invitation request */
    List<Network> findActiveNetworksByIdsNewToUser(String userId, List<String> networkIds, int limit);

    /* Fetches all NetworkMember records of user */
    List<NetworkMember> getNetworkMemberDetailsForUser(String userId);

    /* delete all Network member records of user */
    void removeUserFromNetwork(String userId);

    /* fetches all active network in the given network ids*/
    List<Network> getAllNetworksById(List<String> networkIdList);
}
