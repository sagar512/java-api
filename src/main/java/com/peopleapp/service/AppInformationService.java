package com.peopleapp.service;

import com.peopleapp.dto.requestresponsedto.AppInformationResponseDTO;

public interface AppInformationService {

    AppInformationResponseDTO fetchAppInformationByDeviceTypeId(String deviceTypeId);
}
