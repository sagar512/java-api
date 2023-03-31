package com.peopleapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;


@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserNetworkDetails {

    private String networkId;

    private NetworkDetails networkDetails;

    private boolean isFavourite;

    private String memberRole;

    private Long pendingRequestCount;

    private UserContactData networkOwnerContact;

    private Boolean isRequestSent;

    @Override
    public boolean equals(Object otherUserNetworkDetails) {
        if (this == otherUserNetworkDetails)
            return true;
        if (!(otherUserNetworkDetails instanceof UserNetworkDetails))
            return false;
        UserNetworkDetails otherUserNetworkDetailsObj = (UserNetworkDetails) otherUserNetworkDetails;
        return this.networkId.equals(otherUserNetworkDetailsObj.networkId);
    }

    @Override
    public int hashCode() {
        return this.networkId.hashCode();
    }

}

