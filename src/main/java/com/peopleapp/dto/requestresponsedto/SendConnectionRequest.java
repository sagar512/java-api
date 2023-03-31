package com.peopleapp.dto.requestresponsedto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.peopleapp.dto.ContactNumberDTO;
import com.peopleapp.dto.SharedProfileInformationData;
import com.peopleapp.dto.UserContact;
import com.peopleapp.dto.UserInformationDTO;
import com.peopleapp.enums.MessageCodes;
import com.peopleapp.enums.OperationType;
import com.peopleapp.enums.SendConnectionRequestFlow;
import com.peopleapp.exception.BadRequestException;
import com.peopleapp.util.PeopleUtils;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SendConnectionRequest extends InitiatorAndReceiverDetailsDTO {

    @Valid
    private List<UserContact> initiateContactDetailsList;

    private List<String> initiateUserIdList;

    private UserInformationDTO initiateUserInformation;

    private List<String> activityIdList;

    private List<String> activitySubIdList;

    private boolean isStaticContactToBeCreated = Boolean.FALSE;

    @NotNull
    @Valid
    private SharedProfileInformationData sharedPrivacyProfileKey;

    private String message;

    @ApiModelProperty(hidden = true)
    @JsonIgnore
    private ContactNumberDTO receiverNumber;

    @ApiModelProperty(hidden = true)
    @JsonIgnore
    private ContactNumberDTO initiatorNumber;

    @JsonIgnore
    private SendConnectionRequestFlow requestFlow;

    @ApiModelProperty(hidden = true)
    @JsonIgnore
    private OperationType operationType = OperationType.SINGLE;

    public void setRequestFlowAndOperationType() {

        if(!PeopleUtils.isNullOrEmpty(this.initiateUserIdList)) {
            if(this.initiateUserIdList.size() > 1) {
                this.operationType = OperationType.BULK;
            }
            this.requestFlow = SendConnectionRequestFlow.USER_ID_FLOW;

        } else if (!PeopleUtils.isNullOrEmpty(this.activityIdList)) {
            this.requestFlow = SendConnectionRequestFlow.ACTIVITY_ID_FLOW;

        } else if (!PeopleUtils.isNullOrEmpty(this.initiateContactDetailsList)) {
            if (this.initiateContactDetailsList.size() > 1) {
                this.operationType = OperationType.BULK;
            }
            this.requestFlow = SendConnectionRequestFlow.CONNECTION_ID_FLOW;

        } else if (!PeopleUtils.isNullOrEmpty(this.activitySubIdList)) {
            this.requestFlow = SendConnectionRequestFlow.ACTIVITY_SUB_ID_FLOW;
        } else {
            throw new BadRequestException(MessageCodes.INVALID_OPERATION.getValue());
        }
    }

}
