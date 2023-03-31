package com.peopleapp.dto;

public class ApplicationConfigurationDTO {

    private String mongoUri;

    private String s3BaseUrlNetworkCategory;

    private String sqsURL;

    private String snsArnAPNS;

    private String snsArnGCM;

    private String redisHost;

    private String redisPort;

    private String systemEmailId;

    public String getMongoUri() {
        return mongoUri;
    }

    public void setMongoUri(String mongoUri) {
        this.mongoUri = mongoUri;
    }

    public String getRedisHost() {
        return redisHost;
    }

    public void setRedisHost(String redisHost) {
        this.redisHost = redisHost;
    }

    public String getRedisPort() {
        return redisPort;
    }

    public void setRedisPort(String redisPort) {
        this.redisPort = redisPort;
    }

    public String getS3BaseUrlNetworkCategory() {
        return s3BaseUrlNetworkCategory;
    }

    public void setS3BaseUrlNetworkCategory(String s3BaseUrlNetworkCategory) {
        this.s3BaseUrlNetworkCategory = s3BaseUrlNetworkCategory;
    }

    public String getSqsURL() {
        return sqsURL;
    }

    public void setSqsURL(String sqsURL) {
        this.sqsURL = sqsURL;
    }

    public String getSnsArnAPNS() {
        return snsArnAPNS;
    }

    public void setSnsArnAPNS(String snsArnAPNS) {
        this.snsArnAPNS = snsArnAPNS;
    }

    public String getSnsArnGCM() {
        return snsArnGCM;
    }

    public void setSnsArnGCM(String snsArnGCM) {
        this.snsArnGCM = snsArnGCM;
    }

    public String getSystemEmailId() {
        return systemEmailId;
    }

    public void setSystemEmailId(String systemEmailId) {
        this.systemEmailId = systemEmailId;
    }
}
