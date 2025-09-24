package com.milesight.beaveriot.integrations.milesightgateway.model;

import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * MilesightGatewayErrorCode class.
 *
 * @author simon
 * @date 2025/7/15
 */
@Getter
@RequiredArgsConstructor
public enum MilesightGatewayErrorCode implements ErrorCodeSpec {
    GATEWAY_RESPOND_ERROR(HttpStatus.BAD_REQUEST.value(), "gateway_respond_error", null, null),
    GATEWAY_REQUEST_TIMEOUT(HttpStatus.INTERNAL_SERVER_ERROR.value(), "gateway_request_timeout", "Request gateway timeout.", null),
    DUPLICATED_GATEWAY_EUI(HttpStatus.BAD_REQUEST.value(), "duplicated_gateway_eui", "Duplicated gateway eui.", null),
    DUPLICATED_DEVICE_EUI(HttpStatus.BAD_REQUEST.value(), "duplicated_device_eui", "Duplicated device eui.", null),
    GATEWAY_NO_APPLICATION(HttpStatus.BAD_REQUEST.value(), "gateway_no_application", "Gateway must have at least one application.", null),
    GATEWAY_NO_DEVICE_PROFILE(HttpStatus.BAD_REQUEST.value(), "gateway_no_device_profile", "Gateway must have at least one device profile.", null),
    TEMPLATE_MISSING_LORA_PROFILE(HttpStatus.BAD_REQUEST.value(), "template_missing_device_profile", "Device template must have \"lora_device_profile_class\" in metadata.", null),
    NO_VALID_PROFILE_FOR_DEVICE(HttpStatus.BAD_REQUEST.value(), "no_valid_profile_for_device", "Gateway has no valid profile for the device", null),
    ;

    private final int status;

    private final String errorCode;

    private final String errorMessage;

    private final String detailMessage;

    @Override
    public String toString() {
        return name();
    }
}
