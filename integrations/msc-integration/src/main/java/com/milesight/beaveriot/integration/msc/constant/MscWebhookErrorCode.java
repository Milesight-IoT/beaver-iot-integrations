package com.milesight.beaveriot.integration.msc.constant;

import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import lombok.*;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MscWebhookErrorCode implements ErrorCodeSpec {

    WEBHOOK_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "not_found", "Webhook is not found or disabled.", null),
    WEBHOOK_REQUEST_EXPIRED(HttpStatus.BAD_REQUEST.value(), "request_expired", "Request is expired.", null),
    WEBHOOK_SIGNATURE_INVALID(HttpStatus.UNAUTHORIZED.value(), "signature_invalid", "Signature is invalid.", null),
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
