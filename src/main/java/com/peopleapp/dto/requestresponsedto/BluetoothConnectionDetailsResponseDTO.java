package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.ContactNumberDTO;

import lombok.Data;

@Data
public class BluetoothConnectionDetailsResponseDTO {

    public ContactNumberDTO contactNumberDTO;

    public String connectionId;
    
    public String connectionStatus;

    public String name;

    public String defaultImageUrl;

    public String bluetoothTokenUserId;
    
    public String position;
}
