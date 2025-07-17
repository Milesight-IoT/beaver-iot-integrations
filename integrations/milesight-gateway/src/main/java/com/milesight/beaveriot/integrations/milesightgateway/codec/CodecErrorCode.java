package com.milesight.beaveriot.integrations.milesightgateway.codec;

import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * CodecErrorCode class.
 *
 * @author simon
 * @date 2025/7/15
 */
@Getter
@RequiredArgsConstructor
public enum CodecErrorCode implements ErrorCodeSpec {
    CODEC_RESOURCE_INVALID_URL(HttpStatus.BAD_REQUEST.value(), "codec_resource_invalid_url", "Invalid codec resource url.", null),
    CODEC_RESOURCE_REQUEST_ERROR(HttpStatus.BAD_REQUEST.value(), "codec_resource_request_error", "Request resource error.", null),
    CODEC_RESOURCE_FORMAT_ERROR(HttpStatus.BAD_REQUEST.value(), "codec_resource_format_error", "Invalid resource format.", null),
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
