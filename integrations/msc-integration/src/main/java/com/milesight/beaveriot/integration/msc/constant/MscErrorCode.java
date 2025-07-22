package com.milesight.beaveriot.integration.msc.constant;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.msc.sdk.error.MscApiException;
import com.milesight.msc.sdk.error.MscSdkException;
import lombok.*;
import org.springframework.util.StringUtils;

import java.util.Map;


@Getter
@RequiredArgsConstructor
public enum MscErrorCode implements ErrorCodeSpec {

    MSC_API_ERROR("Request to MSC API failed."),
    MSC_ANOTHER_TASK_RUNNING("Another MSC task is running."),
    MSC_CONNECTION_NOT_READY("MSC connection is not ready."),
    ;

    private final String errorCode = name().toLowerCase();

    private final String errorMessage;

    @Override
    public String getDetailMessage() {
        return null;
    }

    @Override
    public String toString() {
        return name();
    }

    public static ServiceException.ServiceExceptionBuilder wrap(MscSdkException e) {
        if (e instanceof MscApiException mscApiException) {
            val msg = StringUtils.hasText(mscApiException.getErrorResponse().getErrMsg())
                    ? mscApiException.getErrorResponse().getErrMsg()
                    : mscApiException.getErrorResponse().getErrCode();
            return MscErrorCode.MSC_API_ERROR.wrap(msg); //NOSONAR
        }
        return ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), e.getMessage());
    }

    public ServiceException.ServiceExceptionBuilder wrap(Throwable e) {
        return wrap(e.getMessage());
    }

    public ServiceException.ServiceExceptionBuilder wrap(String message) {
        return ServiceException.with(this).args(Map.of("msg", message));
    }

}
