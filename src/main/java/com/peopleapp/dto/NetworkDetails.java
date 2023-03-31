package com.peopleapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.joda.time.DateTime;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NetworkDetails {

    private String name;

    private String imageURL;

    private String bannerImageURL;

    private String privacyType;

    private NetworkPrimaryContactMethod primaryContactMethod;

    private Address networkLocation;

    private int memberCount;

    private int adminCount;

    private List<String> tagList;

    private String description;

    private String networkCategory;

    private DateTime lastModifiedTime;

}