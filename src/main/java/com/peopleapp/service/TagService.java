package com.peopleapp.service;

import com.peopleapp.dto.GetTagsResponseDTO;
import com.peopleapp.dto.TagData;

import java.util.List;
import java.util.SortedMap;

public interface TagService {

    GetTagsResponseDTO getSuggestedTagList();

    SortedMap<String, TagData> editUserTag(SortedMap<String, TagData> existingTagMap, List<String> tagList);

    SortedMap<String, TagData> createNewTagByUser(SortedMap<String, TagData> existingTagMap, List<String> tagList);
}
