package com.peopleapp.service.impl;

import com.peopleapp.dto.GetTagsResponseDTO;
import com.peopleapp.dto.SystemTagData;
import com.peopleapp.dto.TagData;
import com.peopleapp.model.PeopleUser;
import com.peopleapp.model.SystemTagUser;
import com.peopleapp.repository.PeopleUserRepository;
import com.peopleapp.repository.TagRepository;
import com.peopleapp.repository.UserConnectionRepository;
import com.peopleapp.security.TokenAuthService;
import com.peopleapp.service.PrivacyProfileService;
import com.peopleapp.service.TagService;
import com.peopleapp.service.UserConnectionService;
import com.peopleapp.util.PeopleUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.*;

@Service
public class TagServiceImpl implements TagService {

    @Inject
    private TokenAuthService tokenAuthService;

    @Inject
    private UserConnectionRepository userConnectionRepository;

    @Inject
    @Lazy
    private UserConnectionService userConnectionService;

    @Inject
    private PrivacyProfileService privacyProfileService;

    @Inject
    private PeopleUserRepository peopleUserRepository;

    @Inject
    private TagRepository tagRepository;

    @Value("${tag.suggested-number}")
    private int numberOfTags;

    @Override
    public GetTagsResponseDTO getSuggestedTagList() {

        // prepare the suggested tag list for user
        PeopleUser sessionUser = tokenAuthService.getSessionUser();
        // using linkedHashMap to maintain the insertion order
        Set<String> suggestedTagList = new LinkedHashSet<>();

        // get created tags by user
        SortedMap<String, TagData> userTags = sessionUser.getTagMap();

        List<TagData> userTagList = new ArrayList<>();
        for (Map.Entry<String, TagData> entry : userTags.entrySet()) {
            entry.getValue().setTagName(entry.getKey());
            userTagList.add(entry.getValue());
        }

        if (!PeopleUtils.isNullOrEmpty(userTagList)) {
            userTagList.sort(Comparator.comparing(TagData::getCreatedOn).reversed());

            // add user created tags to suggested list
            for (TagData tagData : PeopleUtils.emptyIfNull(userTagList)) {
                if (suggestedTagList.size() >= numberOfTags) {
                    break;
                }
                suggestedTagList.add(tagData.getTagName());
            }
        }


        SystemTagUser systemTagList = tagRepository.findTags();
        if (systemTagList != null) {
            for (SystemTagData tagData : systemTagList.getTagList()) {
                if (suggestedTagList.size() >= numberOfTags) {
                    break;
                }
                suggestedTagList.add(tagData.getTag());
            }
        }

        GetTagsResponseDTO response = new GetTagsResponseDTO();
        response.setTagList(suggestedTagList);

        return response;
    }


    @Override
    public SortedMap<String, TagData> editUserTag(SortedMap<String, TagData> existingTagMap, List<String> tagList) {

        SortedMap<String, TagData> newTagMap = new TreeMap<>();

        if(existingTagMap == null) {
            existingTagMap = newTagMap;
        }


        for (String tag : PeopleUtils.emptyIfNull(tagList)) {

            TagData tagData;
            if(existingTagMap.containsKey(tag)) {

                tagData = existingTagMap.get(tag);
            } else {

                tagData = new TagData();
                tagData.setIsProfileTag(Boolean.TRUE);
                tagData.setCreatedOn(PeopleUtils.getCurrentTimeInUTC());
                tagData.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
            }

            if(!tagData.getIsProfileTag()) {
                tagData.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
            }
            newTagMap.put(tag, tagData);
        }

        return newTagMap;
    }

    @Override
    public SortedMap<String, TagData> createNewTagByUser(SortedMap<String, TagData> existingTagMap, List<String> tagList) {

        if(existingTagMap == null) {
            existingTagMap = new TreeMap<>();
        }

        for(String tag : tagList) {
            if(tagList.stream().noneMatch(tag::equalsIgnoreCase)) {
                TagData tagData = new TagData();
                tagData.setLastUpdatedOn(PeopleUtils.getCurrentTimeInUTC());
                tagData.setCreatedOn(PeopleUtils.getCurrentTimeInUTC());
                tagData.setTagName(tag);
                tagData.setIsProfileTag(Boolean.FALSE);
                existingTagMap.put(tag, tagData);
            }
        }
        return existingTagMap;
    }
}
