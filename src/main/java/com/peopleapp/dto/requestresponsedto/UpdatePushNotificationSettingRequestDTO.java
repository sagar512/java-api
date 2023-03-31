package com.peopleapp.dto.requestresponsedto;

import lombok.Data;
import org.springframework.format.annotation.NumberFormat;

import javax.validation.constraints.NotNull;

@Data
public class UpdatePushNotificationSettingRequestDTO {

    @NotNull
    private Boolean enableSetting;

    private String deviceToken;

    @NumberFormat
    private Integer deviceTypeId;

}
