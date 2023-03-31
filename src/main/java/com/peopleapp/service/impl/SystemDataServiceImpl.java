package com.peopleapp.service.impl;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.peopleapp.dto.ApplicationConfigurationDTO;
import com.peopleapp.dto.SystemTagData;
import com.peopleapp.dto.requestresponsedto.SystemDataResponseDTO;
import com.peopleapp.model.NetworkCategory;
import com.peopleapp.model.SystemTagUser;
import com.peopleapp.repository.NetworkCategoryRepository;
import com.peopleapp.repository.PredefinedLabelsRepository;
import com.peopleapp.repository.TagRepository;
import com.peopleapp.service.SystemDataService;

@Service
public class SystemDataServiceImpl implements SystemDataService {

    @Inject
    private PredefinedLabelsRepository predefinedLabelsRepository;

    @Inject
    private NetworkCategoryRepository networkCategoryRepository;

    @Inject
    private TagRepository tagRepository;

    @Inject
    private ApplicationConfigurationDTO properties;
    
    @Override
    public SystemDataResponseDTO getSystemData(){
        SystemDataResponseDTO responseDTO = new SystemDataResponseDTO();

        // Labels
        responseDTO.setLabels(predefinedLabelsRepository.findAll());

        // Network Categories
        List<NetworkCategory> networkCategoryList = new ArrayList<>();
        for(NetworkCategory networkCategory : networkCategoryRepository.findAll()){
            // Creating complete path for imageUrl
            networkCategory.setImageURL(properties.getS3BaseUrlNetworkCategory() + networkCategory.getImageURL());
            networkCategoryList.add(networkCategory);
        }
        responseDTO.setNetworkCategories(networkCategoryList);

        // Tags
        List<String> tagList = new ArrayList<>();
        List<SystemTagUser> systemTagUserList = tagRepository.findAll();
        if(!systemTagUserList.isEmpty()){
            SystemTagUser systemTagUser = systemTagUserList.get(0);
            for(SystemTagData tagData : systemTagUser.getTagList()){
                tagList.add(tagData.getTag());
            }
        }
        responseDTO.setTagList(tagList);

        return responseDTO;
    }
    
}
