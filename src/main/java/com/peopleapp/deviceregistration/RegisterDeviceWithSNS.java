package com.peopleapp.deviceregistration;

import com.peopleapp.dto.ApplicationConfigurationDTO;
import com.peopleapp.enums.DeviceType;
import com.peopleapp.enums.ErrorCode;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

@Component
public class RegisterDeviceWithSNS {

    @Inject
    private AmazonSNSClientWrapper snsClientWrapper;

    @Inject
    private ApplicationConfigurationDTO properties;


    /**
     * Register your device creates Endpoint ARN
     *
     * @param deviceToken
     * @param platform
     * @return Endpoint ARN associated to your device and your app
     */
    public String registerDevice(String deviceToken, int platform) {

        String endPointArn;
        DeviceType deviceType = DeviceType.fromDeviceTypeId(platform);
        switch (deviceType) {

            case IOS:
                endPointArn = registerAppleSandboxDevice(deviceToken);
                break;

            case ANDROID:
                endPointArn = registerAndroidDevice(deviceToken);
                break;
            default:
                throw new BadRequestException(ErrorCode.BAD_REQUEST.getValue());

        }
        return endPointArn;
    }

    /**
     * Register Android device
     *
     * @param registrationId
     * @return
     */
    private String registerAndroidDevice(String registrationId) {
        return snsClientWrapper.registerDevice(registrationId, properties.getSnsArnGCM());
    }

    /**
     * Register Apple device
     *
     * @param deviceToken
     * @return
     */
    private String registerAppleSandboxDevice(String deviceToken) {
        return snsClientWrapper.registerDevice(deviceToken, properties.getSnsArnAPNS());
    }

}
