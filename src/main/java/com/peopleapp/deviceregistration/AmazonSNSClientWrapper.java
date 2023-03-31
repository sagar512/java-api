package com.peopleapp.deviceregistration;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class AmazonSNSClientWrapper {

    @Inject
    private AmazonSNS snsClient;

    /**
     * Create an Endpoint. This corresponds to an app on a device.
     *
     * @param platformToken
     * @param platformApplicationArn
     * @return endpointArn
     */
    public String registerDevice(String platformToken, String platformApplicationArn) {
        CreatePlatformEndpointRequest platformEndpointRequest = new CreatePlatformEndpointRequest();
        platformEndpointRequest.setToken(platformToken);
        platformEndpointRequest.setPlatformApplicationArn(platformApplicationArn);
        CreatePlatformEndpointResult platformEndpointResult = snsClient.createPlatformEndpoint(platformEndpointRequest);
        return platformEndpointResult.getEndpointArn();
    }
}
