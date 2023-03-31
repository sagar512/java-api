package com.peopleapp.enums;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum DeviceType {

    ANDROID(1),
    IOS(2);

    @Getter
    @NonNull
    private final Integer type;

    public static DeviceType fromDeviceTypeId(Integer deviceId) {
        DeviceType[] deviceTypes = DeviceType.values();

        // Valid Device ID should be between 1 and No. supported Device Types.
        if ((deviceId == null) || (deviceId <= 0) || (deviceId > deviceTypes.length)) {
            return null;
        }

        return deviceTypes[deviceId - 1]; // Since the Array index is 0-based and DeviceType start from 1.
    }

}
