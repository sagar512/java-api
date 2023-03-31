package com.peopleapp.dto;

import lombok.Data;

@Data
public class NetworkCommunicationSettingStatus {

    private boolean defaultPhoneNumberAdded;

    private boolean defaultEmailAdded;

    private boolean defaultTwitterAccountAdded;

    private boolean defaultLinkedInAccountAdded;

    private boolean defaultInstagramAccountAdded;
}
