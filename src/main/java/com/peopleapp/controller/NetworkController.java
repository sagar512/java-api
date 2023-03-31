package com.peopleapp.controller;

import com.peopleapp.configuration.LocaleMessageReader;
import com.peopleapp.constant.APIParamKeys;
import com.peopleapp.constant.MessageConstant;
import com.peopleapp.dto.APIRequestParamData;
import com.peopleapp.dto.RemoveMemberFromNetworkDTO;
import com.peopleapp.dto.UserNetworkDetails;
import com.peopleapp.dto.requestresponsedto.*;
import com.peopleapp.service.NetworkService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;

@Validated
@RestController
@RequestMapping(value = "/v1.0/network/api")
@Api(value = "network", tags = "Network related operations")
@ApiIgnore
public class NetworkController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final int ASCENDING_ORDER = 1;
    private static final int DESCENDING_ORDER = -1;
    private static final String DEFAULT_PAGENUMBER = "0";

    @Inject
    private NetworkService networkService;

    @Inject
    private LocaleMessageReader message;


    /**
     * Update information that a user will be sharing for the networks they join.
     * By default primary number and email will be shared
     */
    @ApiOperation(
            value = "Update network communication type settings",
            notes = "<div>\n" + "\n" + "This API is used to update the communication type shared in network section of settings.\n" +
                    "\n" + "</div>\n" + "\n" + "<div>\n" + "\n" + "**Note:**\n" + "\n" + "</div>\n" + "\n" + "<div>\n" + "\n" +
                    "If the type selected is shared with any active networks, that value can not be removed.\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                                                            | Error Code |\n" +
                    "| --------------------------------------------------------------------- | ---------- |\n" +
                    "| If communication type shared with a network is being removed.         | 933        |\n" +
                    "| If communication type specified by network is not shared by the user. | 916        |"
    )
    @PostMapping(
            value = "/network-setting/update",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO updateNetworkSetting(@Validated @RequestBody UpdateNetworkSettingDTO updateNetworkSetting,
                                                @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside NetworkController->updateNetworkSetting");

        networkService.updateNetworkSetting(updateNetworkSetting);
        BaseResponseDTO baseResponse = new BaseResponseDTO();
        baseResponse.setMessage(message.get(MessageConstant.NETWORK_SETTING_UPDATE));
        return baseResponse;
    }

    @ApiOperation(
            value = "Network setting values",
            notes = "Shared communication types under network section will be fetched."
    )
    @GetMapping(
            value = "/network-setting",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<UpdateNetworkSettingDTO> getNetworkSetting(@RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside NetworkController->getNetworkSetting");

        List<String> networkDefaultSetting = networkService.getNetworkDefaultSetting();
        BaseResponseDTO<UpdateNetworkSettingDTO> response = new BaseResponseDTO<>();
        UpdateNetworkSettingDTO updateNetworkSettingDTO = new UpdateNetworkSettingDTO();
        updateNetworkSettingDTO.setNetworkSharedValueList(networkDefaultSetting);
        response.setData(updateNetworkSettingDTO);
        return response;
    }


    @ApiOperation(
            value = "List of user networks",
            notes = "All the networks of which user is a member will be fetched."
    )
    @GetMapping(
            value = "/user-networks",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<GetUserNetworksResponseDTO> getUserNetworks(
            @RequestParam(value = APIParamKeys.PAGE_NUMBER, defaultValue = DEFAULT_PAGENUMBER) Integer pageNumber,
            @RequestParam(value = APIParamKeys.PAGE_SIZE, defaultValue = "100") @Max(500) Integer pageSize,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside NetworkController->getUserNetworks");

        GetUserNetworksResponseDTO getUserNetworksResponse = networkService.getUserNetworks(pageNumber, pageSize);

        BaseResponseDTO<GetUserNetworksResponseDTO> baseResponseDTO = new BaseResponseDTO<>();
        baseResponseDTO.setData(getUserNetworksResponse);
        return baseResponseDTO;
    }

    @ApiOperation(
            value = "Create network",
            notes = "<div>\n" + "\n" + "This API is used to create network.\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                                                       | Error Code |\n" +
                    "| ---------------------------------------------------------------- | ---------- |\n" +
                    "| If session user has not shared the communication type specified. | 916        |\n" +
                    "| If trying to create network with duplicate name.                 | 924        |"
    )
    @PostMapping(
            value = "/create",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<CreateOrEditNetworkResponseDTO> createNetwork(@Validated @RequestBody CreateNetworkRequestDTO createNetworkRequest,
                                                                         @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside NetworkController->createNetwork");

        CreateOrEditNetworkResponseDTO createNetworkResponse = networkService.createNetwork(createNetworkRequest);
        BaseResponseDTO<CreateOrEditNetworkResponseDTO> baseResponseDTO = new BaseResponseDTO<>();
        baseResponseDTO.setData(createNetworkResponse);
        baseResponseDTO.setMessage(message.get(MessageConstant.NETWORK_CREATED));
        return baseResponseDTO;
    }

    @ApiOperation(
            value = "Join Network",
            notes = "<div>\n" + "\n" + "This API enables user to send request to join different networks.\n" + "\n" + "</div>\n" +
                    "\n" + "<div>\n" + "\n" + "**Joining network cases:**\n" + "\n" + "</div>\n" + "\n" +
                    "  - OPEN network - User will be joining directly upon making this API call.\n" +
                    "  - PUBLIC network(Join by request type) - User will need to wait for\n" +
                    "    approval to join from Owner or admin of that network.\n" +
                    "  - PRIVATE network - User cannot send request to join network.\n" + "\n" +
                    "| Error Case                                                            | Error Code |\n" +
                    "| --------------------------------------------------------------------- | ---------- |\n" +
                    "| If network is in-active.                                              | 915        |\n" +
                    "| If communication type specified by network is not shared by the user. | 916        |\n" +
                    "| If already a member of the network.                                   | 925        |\n" +
                    "| If request was sent and in pending state.                             | 926        |\n" +
                    "| If trying to send request to a private network.                       | 934        |"
    )
    @PostMapping(
            value = "/join",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<JoinNetworkResponseDTO> joinNetwork(@Validated @RequestBody JoinNetworkRequestDTO joinNetworkRequest,
                                                               @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside NetworkController->joinNetwork");
        BaseResponseDTO<JoinNetworkResponseDTO> baseResponse = new BaseResponseDTO<>();
        baseResponse.setData(networkService.joinNetwork(joinNetworkRequest));
        return baseResponse;
    }

    @ApiOperation(
            value = "Network Details",
            notes = "This API fetches information of specific network and also the details of network owner."
    )
    @GetMapping(
            value = "/details/{networkId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<UserNetworkDetails> getNetworkDetails(@PathVariable("networkId") String networkId,
                                                                 @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside NetworkController->getNetworkDetails");

        UserNetworkDetails userNetworkDetails = networkService.getNetworkDetails(networkId);

        BaseResponseDTO<UserNetworkDetails> baseResponse = new BaseResponseDTO<>();
        baseResponse.setData(userNetworkDetails);
        return baseResponse;
    }

    @ApiOperation(
            value = "Invite Members(For private networks)",
            notes = "<div>\n" + "\n" + "This API is used by owner/admins to invite their contacts to join private networks.\n" +
                    "\n" + "</div>\n" + "\n" +
                    "| Error Case                                                | Error Code |\n" +
                    "| --------------------------------------------------------- | ---------- |\n" +
                    "| If network is in-active.                                  | 915        |\n" +
                    "| If communication type specified by network is not shared. | 916        |\n" +
                    "| If user is not a member of the network.                   | 917        |\n" +
                    "| Only owner or admins can perform this action.             | 919        |"
    )
    @PostMapping(
            value = "/member/invite",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO inviteMember(@Validated @RequestBody NetworkInviteRequestDTO networkInviteRequest,
                                        @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside NetworkController->inviteMember");

        BaseResponseDTO baseResponse = new BaseResponseDTO<>();
        baseResponse.setMessage(networkService.inviteMembersToNetwork(networkInviteRequest));
        return baseResponse;
    }

    @ApiOperation(
            value = "Remove members",
            notes = "<div>\n" + "\n" + "This API is used to remove members from network.\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                                                | Error Code |\n" +
                    "| --------------------------------------------------------- | ---------- |\n" +
                    "| If network is in-active.                                  | 915        |\n" +
                    "| If communication type specified by network is not shared. | 916        |\n" +
                    "| If user is not a member of the network.                   | 917        |\n" +
                    "| Only owner or admins can perform this action.             | 919        |"
    )
    @PostMapping(
            value = "/member/remove",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO removeMember(@Validated @RequestBody RemoveMemberFromNetworkDTO removeMemberFromNetwork,
                                        @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside NetworkController->removeMember");

        BaseResponseDTO baseResponse = new BaseResponseDTO<>();
        baseResponse.setMessage(networkService.removeMemberFromNetwork(removeMemberFromNetwork));
        return baseResponse;
    }

    @ApiOperation(
            value = "Promote members to admins",
            notes = "<div>\n" + "\n" + "This API allows network owner to promote any member to admin.\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                                                | Error Code |\n" +
                    "| --------------------------------------------------------- | ---------- |\n" +
                    "| If network is in-active.                                  | 915        |\n" +
                    "| If communication type specified by network is not shared. | 916        |\n" +
                    "| If user is not a member of the network.                   | 917        |\n" +
                    "| Only owner can perform this action.             | 919        |"
    )
    @PostMapping(
            value = "/admin/promote",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO promoteToAdmin(@Validated @RequestBody NetworkAdminPromoteDTO networkAdminPromote,
                                          @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside NetworkController->promoteToAdmin");


        BaseResponseDTO baseResponse = new BaseResponseDTO<>();
        baseResponse.setMessage(networkService.promoteAdminsToNetwork(networkAdminPromote));
        return baseResponse;
    }

    @ApiOperation(
            value = "Pending network join request",
            notes = "<div>\n" + "\n" + "This API is used by owner or admins to check all the pending request for a network.\n" +
                    "\n" + "</div>\n" + "\n" + "<div>\n" + "\n" + "**API is paginated with default page size of 100 (MAX - 500) and page\n" +
                    "number 0.**\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                                    | Error Code |\n" +
                    "| --------------------------------------------- | ---------- |\n" +
                    "| If network is in-active.                      | 915        |\n" +
                    "| If user is not a member of the network.       | 917        |\n" +
                    "| Only owner or admins can perform this action. | 919        |"
    )
    @GetMapping(
            value = "/all-join-request/{networkId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<NetworkPendingRequestDetailsDTO> fetchAllJoinRequest(
            @PathVariable("networkId") String networkId,
            @RequestParam(value = APIParamKeys.PAGE_NUMBER, defaultValue = DEFAULT_PAGENUMBER) Integer pageNumber,
            @RequestParam(value = APIParamKeys.PAGE_SIZE, defaultValue = "100") @Max(500) Integer pageSize,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside NetworkController->fetchAllJoinRequest");

        NetworkPendingRequestDetailsDTO response = networkService.fetchJoinRequestDetailsForNetwork(networkId,
                pageNumber, pageSize);
        BaseResponseDTO<NetworkPendingRequestDetailsDTO> baseResponse = new BaseResponseDTO<>();
        baseResponse.setData(response);
        return baseResponse;
    }


    @ApiOperation(
            value = "Edit Network",
            notes = "<div>\n" + "\n" + "This API is used to edit network related information.\n" + "\n" + "</div>\n" + "\n" +
                    "<div>\n" + "\n" + "**Note:**\n" + "\n" + "</div>\n" + "\n" +
                    "  - If privacy type is changed from public(join by request) to\n" + "    open(direct join), then all" +
                    " pending request will be automatically\n" + "    approved.\n" + "  - If privacy type is changed from" +
                    " public(join by request) to private,\n" + "    then all pending request will be automatically declined.\n" + "\n" +
                    "| Error Case                                                        | Error Code |\n" +
                    "| ----------------------------------------------------------------- | ---------- |\n" +
                    "| If network is in-active.                                          | 915        |\n" +
                    "| If communication type specified by network is not shared by user. | 916        |\n" +
                    "| If user is not a member of the network.                           | 917        |\n" +
                    "| Only owner or admins can perform this action.                     | 919        |\n" +
                    "| If a network already exist with the edited name.                  | 930        |"
    )
    @PostMapping(
            value = "/edit",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<CreateOrEditNetworkResponseDTO> editNetwork(@Validated @RequestBody EditNetworkRequestDTO editNetworkRequest,
                                                                       @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside NetworkController->editNetwork");

        CreateOrEditNetworkResponseDTO editNetworkResponse = networkService.editNetworkDetails(editNetworkRequest);
        BaseResponseDTO<CreateOrEditNetworkResponseDTO> response = new BaseResponseDTO<>();
        response.setData(editNetworkResponse);
        response.setMessage(message.get(MessageConstant.NETWORK_EDITED));
        return response;
    }

    @ApiOperation(
            value = "Leave Network",
            notes = "<div>\n" + "\n" + "This API is used to unsubscribe from a network. Apart from owner any other\n" +
                    "member of network can perform this action.\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                               | Error Code |\n" +
                    "| ---------------------------------------- | ---------- |\n" +
                    "| If network is in-active.                 | 915        |\n" +
                    "| If user is not a member of the network.  | 917        |\n" +
                    "| If session user is owner of the network. | 921        |"
    )
    @PostMapping(
            value = "/leave",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO leaveNetwork(@Validated @RequestBody LeaveNetworkRequestDTO leaveNetworkRequest,
                                        @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside NetworkController->leaveNetwork");

        networkService.leaveNetwork(leaveNetworkRequest);
        BaseResponseDTO baseResponse = new BaseResponseDTO();
        baseResponse.setMessage(message.get(MessageConstant.NETWORK_UNSUBSCRIBE));
        return baseResponse;
    }

    @ApiOperation(
            value = "Report Network",
            notes = "<div>\n" + "\n" + "This API is used to report on a network.\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case               | Error Code |\n" +
                    "| ------------------------ | ---------- |\n" +
                    "| If network is in-active. | 915        |"
    )
    @PostMapping(
            value = "/report",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO reportNetwork(@Validated @RequestBody ReportNetworkRequestDTO reportNetworkRequest,
                                         @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside NetworkController->reportNetwork");
        BaseResponseDTO baseResponse = new BaseResponseDTO();
        baseResponse.setMessage(networkService.reportNetwork(reportNetworkRequest));
        return baseResponse;
    }

    @ApiOperation(
            value = "Share network",
            notes = "<div>\n" + "\n" + "This API is used to share network to user's contacts.\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                                              | Error Code |\n" +
                    "| ------------------------------------------------------- | ---------- |\n" +
                    "| If network is in-active.                                | 915        |\n" +
                    "| If session user is not an active member of the network. | 917        |\n" +
                    "| Private network can not be shared.                      | 922        |\n" +
                    "| If sharing network with contacts more than once.        | 932        |"
    )
    @PostMapping(
            value = "/share",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO shareNetwork(@Validated @RequestBody ShareNetworkRequestDTO shareNetworkRequest,
                                        @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside NetworkController->shareNetwork");

        BaseResponseDTO baseResponse = new BaseResponseDTO<>();
        baseResponse.setMessage(networkService.shareNetwork(shareNetworkRequest));
        return baseResponse;
    }

    @ApiOperation(
            value = "Delete network",
            notes = "<div>\n" + "\n" + "This API is used to delete the network.\n" + "\n" + "</div>\n" +
                    "\n" + "<div>\n" + "\n" + "**Note:**\n" + "\n" + "</div>\n" + "\n" + "<div>\n" + "\n" + "Only owner can delete the network.\n" +
                    "\n" + "</div>\n" + "\n" +
                    "| Error Case                                              | Error Code |\n" +
                    "| ------------------------------------------------------- | ---------- |\n" +
                    "| If network is in-active.                                | 915        |\n" +
                    "| If the user has not shared the communication type.      | 916        |\n" +
                    "| If session user is not an active member of the network. | 917        |\n" +
                    "| If session user is not owner of the network.            | 919        |"
    )
    @DeleteMapping(
            value = "/delete",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO deleteNetwork(@Validated @RequestBody DeleteNetworkRequestDTO deleteNetworkRequest,
                                         @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside NetworkController->deleteNetwork");

        networkService.deleteNetwork(deleteNetworkRequest);
        BaseResponseDTO baseResponse = new BaseResponseDTO();
        baseResponse.setMessage(message.get(MessageConstant.NETWORK_DELETE));
        return baseResponse;
    }

    @ApiOperation(
            value = "Demote admins",
            notes = "<div>\n" + "\n" + "This API is used to demote admins to member of the network.\n" + "\n" + "</div>\n" +
                    "\n" + "<div>\n" + "\n" + "**Note:**\n" + "\n" + "</div>\n" + "\n" + "<div>\n" + "\n" +
                    "Demote operation can be performed only by owner.\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                                              | Error Code |\n" +
                    "| ------------------------------------------------------- | ---------- |\n" +
                    "| If network is in-active.                                | 915        |\n" +
                    "| If the user has not shared the communication type.      | 916        |\n" +
                    "| If session user is not owner of the network.            | 919        |\n" +
                    "| If session user is not an active member of the network. | 917        |\n" +
                    "| If trying to demote owner of the network.               | 928        |"
    )
    @PostMapping(
            value = "/admin/demote",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO removeAdmins(@Validated @RequestBody DemoteAdminRequestDTO removeAdminRequest,
                                        @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside NetworkController->demoteAdmins");

        BaseResponseDTO baseResponse = new BaseResponseDTO<>();
        baseResponse.setMessage(networkService.demoteAdmins(removeAdminRequest));
        return baseResponse;
    }

    @ApiOperation(
            value = "Transfer ownership",
            notes = "<div>\n" + "\n" + "This API is used to transfer ownership of the network.\n" + "\n" + "</div>\n" +
                    "\n" + "<div>\n" + "\n" + "**Note:**\n" + "\n" + "</div>\n" + "\n" + "<div>\n" + "\n" +
                    "Ownership of a network can be transferred only to admins of the network.\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case  | Error Code |\n" +
                    "| -------------------------------------------------------------------------------------------------" +
                    "------------------------------------------- | ---------- |\n" +
                    "| If network is in-active.                  | 915        |\n" +
                    "| If the user has not shared the communication type.     | 916        |\n" +
                    "| If user transferring ownership is not a network owner. | 919        |\n" +
                    "| If session user is not an active member of the network or if user to whom the ownership is being " +
                    "transferred is not an admin of the network. | 917        |\n" +
                    "| If transferring ownership to self         | 923        |"
    )
    @PostMapping(
            value = "/ownership/transfer",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO transferOwnerShip(@Validated @RequestBody TransferOwnerShipRequestDTO transferOwnerShipRequest,
                                             @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside NetworkController->transferOwnerShip");

        networkService.transferOwnership(transferOwnerShipRequest);
        BaseResponseDTO baseResponse = new BaseResponseDTO<>();
        baseResponse.setMessage(message.get(MessageConstant.NETWORK_OWNERSHIP_TRANSFER));
        return baseResponse;
    }

    @ApiOperation(
            value = "Accept network invitation",
            notes = "<div>\n" + "\n" + "This API is used to accept invitation and join a private network.\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                                         | Error Code |\n" +
                    "| -------------------------------------------------- | ---------- |\n" +
                    "| If request is invalid.                             | 920        |\n" +
                    "| If network is in-active.                           | 915        |\n" +
                    "| If the user has not shared the communication type. | 916        |\n" +
                    "| If user is already a network member.               | 925        |"
    )
    @PostMapping(
            value = "/invitation/accept",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO acceptInvitation(@Validated @RequestBody AcceptInvitationRequestDTO acceptInvitationRequest,
                                            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside NetworkController->acceptInvitation");

        BaseResponseDTO baseResponse = new BaseResponseDTO<>();
        baseResponse.setMessage(networkService.acceptInvitation(acceptInvitationRequest));
        return baseResponse;
    }

    @ApiOperation(
            value = "Accept or Reject network join request",
            notes = "<div>\n" + "\n" + "This API is used to accept or reject network request. **Only Owner and\n" +
                    "admins can perform action.**\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                                                    | Error Code |\n" +
                    "| ------------------------------------------------------------- | ---------- |\n" +
                    "| If request is invalid.                                        | 920        |\n" +
                    "| If network is in-active.                                      | 915        |\n" +
                    "| If the user accepting request is not a active network member. | 917        |\n" +
                    "| If the user is not owner or admin action can not be taken.    | 919        |\n" +
                    "| If the user has not shared the communication type.            | 916        |"
    )
    @PostMapping(
            value = "/join-request/manage",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO handleJoinRequest(@Validated @RequestBody NetworkJoinRequestDTO networkJoinRequest,
                                             @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside NetworkController->handleJoinRequest");

        String response = networkService.handleNetworkJoinRequest(networkJoinRequest);
        BaseResponseDTO baseResponse = new BaseResponseDTO<>();
        baseResponse.setMessage(response);
        return baseResponse;
    }

    @ApiOperation(
            value = "Favourite/ Un-Favourite a network",
            notes = "<div>\n" + "\n" + "With this API a network can be marked as favourite/ un-favourite w.r.t\n" +
                    "session user.\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                                         | Error Code |\n" +
                    "| -------------------------------------------------- | ---------- |\n" +
                    "| If session user in not a active member of network. | 917        |"
    )
    @PostMapping(
            value = "/favourite/update",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO updateNetworkFavouriteStatus(@Validated @RequestBody UpdateNetworkFavouriteRequestDTO updateNetworkFavouriteRequestDTO,
                                                        @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside NetworkController-> updateFavouriteStatus");
        String responseMessage = networkService.updateNetworkFavouriteStatus(updateNetworkFavouriteRequestDTO);
        BaseResponseDTO baseResponse = new BaseResponseDTO();
        baseResponse.setMessage(responseMessage);
        return baseResponse;
    }

    @ApiOperation(
            value = "Fetch Recommended Networks",
            notes = "Returns Most Popular, Local and Suggested Networks for a Category"
    )
    @GetMapping(
            value = "/recommended",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO fetchRecommendedNetworks(@RequestParam(value = "networkCategory") String networkCategory,
                                                    @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside NetworkController-> fetchRecommendedNetworks");
        RecommendedNetworksResponseDTO responseDTO =
                networkService.getRecommendedNetworks(networkCategory);
        BaseResponseDTO baseResponse = new BaseResponseDTO();
        baseResponse.setData(responseDTO);
        return baseResponse;
    }


    @ApiOperation(
            value = "Fetch top Recommended Networks",
            notes = "Returns top recommended network across all categories"
    )
    @GetMapping(
            value = "/top-recommended",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO fetchTopRecommendedNetworks(@RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside NetworkController-> fetchTopRecommendedNetworks");
        BaseResponseDTO baseResponse = new BaseResponseDTO();
        baseResponse.setData(networkService.getTopRecommendedNetworks());
        return baseResponse;
    }

    /**
     * @param networkId
     * @param sortByRole     -> 1 = Ascending, -1 = Descending, 0 = do not consider, default = 0
     * @param fNameOrder     -> 1 = Ascending, -1 = Descending, default = 1
     * @param lNameOrder     -> 1 = Ascending, -1 = Descending, default = 1
     * @param lNamePreferred -> default = false
     * @param pageNumber
     * @param pageSize
     * @param sessionToken
     * @return
     */
    @ApiOperation(
            value = "List of members of a network",
            notes = "<div>\n" + "\n" + "This API will fetch all the members of a network.\n" + "\n" + "</div>\n" + "\n" +
                    "<div>\n" + "\n" + "**Note :**\n" + "\n" + "</div>\n" + "\n" + "<div>\n" + "\n" +
                    "This API is paginated with **default page number 0 and page size 500. Max page size allowed in this API is 500**\n" +
                    "\n" + "</div>\n" + "\n" + "<div>\n" + "\n" + "For fNameOrder and lNameOrder : **1 = Ascending, -1 = Descending,\n" +
                    "default = 1**\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                                                     | Error Code |\n" +
                    "| -------------------------------------------------------------- | ---------- |\n" +
                    "| If network is in-active.          | 915        |\n" +
                    "| If session user is not an active member of the network. | 917        |"
    )
    @GetMapping(
            value = "/members/{networkId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<NetworkMembersResponseDTO> getMembersOfNetwork(
            @PathVariable("networkId") String networkId,
            @RequestParam(value = APIParamKeys.SEARCH_STRING, required = false, defaultValue = "") String searchString,
            @RequestParam(value = APIParamKeys.SORT_BY_ROLE, defaultValue = "0")
            @Min(DESCENDING_ORDER) @Max(ASCENDING_ORDER) Integer sortByRole,
            @RequestParam(value = APIParamKeys.FNAME_SORT_ORDER, defaultValue = "1")
            @Min(DESCENDING_ORDER) @Max(ASCENDING_ORDER) Integer fNameOrder,
            @RequestParam(value = APIParamKeys.LNAME_SORT_ORDER, defaultValue = "1")
            @Min(DESCENDING_ORDER) @Max(ASCENDING_ORDER) Integer lNameOrder,
            @RequestParam(value = APIParamKeys.LAST_NAME_PREFERRED, defaultValue = "false") Boolean lNamePreferred,
            @RequestParam(value = APIParamKeys.PAGE_NUMBER, defaultValue = DEFAULT_PAGENUMBER) Integer pageNumber,
            @RequestParam(value = APIParamKeys.PAGE_SIZE, defaultValue = "500") @Max(500) Integer pageSize,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside NetworkController->getMembersOfNetwork");

        APIRequestParamData apiRequestParamData = new APIRequestParamData();
        apiRequestParamData.setSearchString(searchString);
        apiRequestParamData.setSortByRole(sortByRole);
        apiRequestParamData.setFNameOrder(fNameOrder);
        apiRequestParamData.setLNameOrder(lNameOrder);
        apiRequestParamData.setLastNamePreferred(lNamePreferred);
        apiRequestParamData.setPageNumber(pageNumber);
        apiRequestParamData.setPageSize(pageSize);

        NetworkMembersResponseDTO getNetworkMembersResponseDTO = networkService.getMembersOfNetwork(networkId, apiRequestParamData);

        BaseResponseDTO<NetworkMembersResponseDTO> baseResponseDTO = new BaseResponseDTO<>();
        baseResponseDTO.setData(getNetworkMembersResponseDTO);
        return baseResponseDTO;
    }

    /**
     * @param networkId
     * @param fNameOrder     -> 1 = Ascending, -1 = Descending, default = 1
     * @param lNameOrder     -> 1 = Ascending, -1 = Descending, default = 1
     * @param lNamePreferred -> default = false
     * @param pageNumber
     * @param pageSize
     * @param sessionToken
     * @return
     */
    @ApiOperation(
            value = "List of admins of a network",
            notes = "<div>\n" + "\n" + "This API will fetch all the admins of a network.\n" + "\n" + "</div>\n" + "\n" +
                    "<div>\n" + "\n" + "**Note :**\n" + "\n" + "</div>\n" + "\n" + "<div>\n" + "\n" +
                    "This API is paginated with **default page number 0 and page size 100. Max page size allowed in this API is 500**\n" +
                    "\n" + "</div>\n" + "\n" + "<div>\n" + "\n" + "For fNameOrder and lNameOrder : **1 = Ascending, -1 = Descending,\n" +
                    "default = 1**\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                                                     | Error Code |\n" +
                    "| -------------------------------------------------------------- | ---------- |\n" +
                    "| If network is in-active.          | 915        |\n" +
                    "| If session user is not an active member of the network. | 917        |"
    )
    @GetMapping(
            value = "/admins/{networkId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<NetworkMembersResponseDTO> getAdminsOfNetwork(
            @PathVariable("networkId") String networkId,
            @RequestParam(value = APIParamKeys.SEARCH_STRING, required = false, defaultValue = "") String searchString,
            @RequestParam(value = APIParamKeys.FNAME_SORT_ORDER, defaultValue = "1")
            @Min(DESCENDING_ORDER) @Max(ASCENDING_ORDER) Integer fNameOrder,
            @RequestParam(value = APIParamKeys.LNAME_SORT_ORDER, defaultValue = "1")
            @Min(DESCENDING_ORDER) @Max(ASCENDING_ORDER) Integer lNameOrder,
            @RequestParam(value = APIParamKeys.LAST_NAME_PREFERRED, defaultValue = "false") Boolean lNamePreferred,
            @RequestParam(value = APIParamKeys.PAGE_NUMBER, defaultValue = DEFAULT_PAGENUMBER) Integer pageNumber,
            @RequestParam(value = APIParamKeys.PAGE_SIZE, defaultValue = "100") @Max(500) Integer pageSize,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside NetworkController->getAdminsOfNetwork");

        APIRequestParamData apiRequestParamData = new APIRequestParamData();
        apiRequestParamData.setSearchString(searchString);
        apiRequestParamData.setFNameOrder(fNameOrder);
        apiRequestParamData.setLNameOrder(lNameOrder);
        apiRequestParamData.setLastNamePreferred(lNamePreferred);
        apiRequestParamData.setPageNumber(pageNumber);
        apiRequestParamData.setPageSize(pageSize);

        NetworkMembersResponseDTO getNetworkMembersResponseDTO = networkService.getAdminsOfNetwork(networkId, apiRequestParamData);

        BaseResponseDTO<NetworkMembersResponseDTO> baseResponseDTO = new BaseResponseDTO<>();
        baseResponseDTO.setData(getNetworkMembersResponseDTO);
        return baseResponseDTO;
    }

    @ApiOperation(
            value = "Broadcast message",
            notes = "<div>\n" + "\n" + "With this API **Owner or Admin** can broadcast a message to all network members.\n" +
                    "\n" + "</div>\n" + "\n" +
                    "| Error Case                                                                      | Error Code |\n" +
                    "| ------------------------------------------------------------------------------- | ---------- |\n" +
                    "| If trying to broadcast a message on in-active network.                           | 915        |\n" +
                    "| If message broadcaster is not an active member of the network.                  | 917        |\n" +
                    "| Action denial if broadcaster is not an owner or admin of the network.           | 919        |\n" +
                    "| If broadcaster has stopped sharing the communication type specified by network. | 916        |"
    )
    @PostMapping(
            value = "/broadcast-message",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO messageAllMembers(@Validated @RequestBody MessageNetworkMembersDTO messageNetworkMembersDTO,
                                             @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside NetworkController->messageAllMembers");

        networkService.messageAllMembers(messageNetworkMembersDTO);

        BaseResponseDTO baseResponse = new BaseResponseDTO<>();
        baseResponse.setMessage(message.get(MessageConstant.NETWORK_MESSAGE_SENT));
        return baseResponse;
    }

    /**
     * @param searchString -> performs contains search on Network name with searchString
     * @param pageNumber
     * @param pageSize
     * @param sessionToken
     * @return
     */
    @ApiOperation(
            value = "Search for Network",
            notes = "<div>\n" + "\n" + "Networks will be fetched based on search input.\n" + "\n" + "</div>\n" + "\n" +
                    "<div>\n" + "\n" + "**Note :** Only Non-private networks will be fetched.\n" + "\n" + "</div>\n" + "\n" +
                    "| Error Case                                       | Error Code |\n" +
                    "| ------------------------------------------------ | ---------- |\n" +
                    "| If trying to search for network with empty value | 603        |"
    )
    @GetMapping(
            value = "/search",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BaseResponseDTO<SearchedNetworkResponseDTO> searchNetwork(
            @RequestParam(value = APIParamKeys.SEARCH_STRING) String searchString,
            @RequestParam(value = APIParamKeys.SORT_ORDER, defaultValue = "1")
            @Min(DESCENDING_ORDER) @Max(ASCENDING_ORDER) Integer sortOrder,
            @RequestParam(value = APIParamKeys.PAGE_NUMBER, defaultValue = DEFAULT_PAGENUMBER) Integer pageNumber,
            @RequestParam(value = APIParamKeys.PAGE_SIZE, defaultValue = "100") @Max(500) Integer pageSize,
            @RequestHeader(value = APIParamKeys.SESSION_TOKEN) String sessionToken) {

        logger.info("Inside NetworkController -> searchNetwork");

        SearchedNetworkResponseDTO searchedNetworkResponse = networkService.searchNetwork(searchString, sortOrder,
                pageNumber, pageSize);

        BaseResponseDTO<SearchedNetworkResponseDTO> baseResponseDTO = new BaseResponseDTO<>();
        baseResponseDTO.setData(searchedNetworkResponse);
        return baseResponseDTO;
    }

}
