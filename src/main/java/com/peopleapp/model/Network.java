package com.peopleapp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peopleapp.dto.Address;
import com.peopleapp.dto.NetworkPrimaryContactMethod;
import com.peopleapp.enums.NetworkStatus;
import lombok.Data;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document(collection = "networks")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@TypeAlias("networks")
public class Network {

    @Id
    @Field(value = "_id")
    private ObjectId networkId;

    private String name;

    private String imageURL;

    private String bannerImageURL;

    private String privacyType;

    private NetworkPrimaryContactMethod primaryContactMethod;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private Address networkLocation;

    private int memberCount = 0;

    private int adminCount = 0;

    private List<String> tagList;

    private String description;

    private String networkCategory;

    private String networkStatus = NetworkStatus.ACTIVE.getValue();

    private DateTime lastModifiedTime;

    public String getNetworkId() {
        return this.networkId != null ? this.networkId.toString() : null;
    }

    public void setNetworkId(String networkId) {
        this.networkId = new ObjectId(networkId);
    }

}


