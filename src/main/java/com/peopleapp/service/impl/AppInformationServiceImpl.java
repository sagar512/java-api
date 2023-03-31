package com.peopleapp.service.impl;

import com.peopleapp.dto.requestresponsedto.AppInformationResponseDTO;
import com.peopleapp.model.AppInformation;
import com.peopleapp.repository.AppInformationRepository;
import com.peopleapp.service.AppInformationService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class AppInformationServiceImpl implements AppInformationService {

    @Inject
    private AppInformationRepository appInformationRepository;

    @Override
    public AppInformationResponseDTO fetchAppInformationByDeviceTypeId(String deviceTypeId) {
        AppInformation appInformation = appInformationRepository.findByDeviceTypeId(Integer.parseInt(deviceTypeId));

        AppInformationResponseDTO appInformationResponseDTO = new AppInformationResponseDTO();
        appInformationResponseDTO.setDeviceTypeId(appInformation.getDeviceTypeId());
        appInformationResponseDTO.setBuildVersion(appInformation.getBuildVersion());
        appInformationResponseDTO.setBuildNumber(appInformation.getBuildNumber());
        appInformationResponseDTO.setForceUpdateRequired(appInformation.getForceUpdateRequired());

        return appInformationResponseDTO;
    }
}
