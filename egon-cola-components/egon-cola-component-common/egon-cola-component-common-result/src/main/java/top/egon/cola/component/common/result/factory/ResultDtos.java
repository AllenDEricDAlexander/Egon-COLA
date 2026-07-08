package top.egon.cola.component.common.result.factory;

import top.egon.cola.component.common.core.code.CommonStatus;
import top.egon.cola.component.common.core.code.ErrorStatus;
import top.egon.cola.component.common.core.exception.EgonException;
import top.egon.cola.component.common.result.dto.PageResultDto;
import top.egon.cola.component.common.result.dto.ResultDto;
import top.egon.cola.component.common.trace.TraceContext;

import java.time.Instant;
import java.util.List;

/**
 * Factory methods for external response DTOs.
 */
public final class ResultDtos {

    private ResultDtos() {
    }

    public static <T> ResultDto<T> success() {
        return success(null);
    }

    public static <T> ResultDto<T> success(T data) {
        return new ResultDto<>(
                true,
                CommonStatus.SUCCESS.getCode(),
                CommonStatus.SUCCESS.getStatus(),
                CommonStatus.SUCCESS.getMessage(),
                data,
                TraceContext.getTraceId(),
                now()
        );
    }

    public static <T> ResultDto<T> failure(ErrorStatus status) {
        return failure(status.getCode(), status.getStatus(), status.getMessage());
    }

    public static <T> ResultDto<T> failure(Throwable throwable) {
        if (throwable instanceof EgonException exception) {
            return failure(exception.getCode(), exception.getStatus(), exception.getMessage());
        }
        return failure(
                CommonStatus.SYSTEM_ERROR.getCode(),
                CommonStatus.SYSTEM_ERROR.getStatus(),
                CommonStatus.SYSTEM_ERROR.getMessage()
        );
    }

    public static <T> ResultDto<T> failure(int code, String status, String message) {
        return new ResultDto<>(false, code, status, message, null, TraceContext.getTraceId(), now());
    }

    public static <T> PageResultDto<T> page(List<T> records, long total, int pageNo, int pageSize) {
        int normalizedPageNo = Math.max(pageNo, 1);
        int normalizedPageSize = pageSize <= 0 ? 10 : pageSize;
        long normalizedTotal = Math.max(total, 0);
        long totalPages = normalizedTotal == 0 ? 0 : (normalizedTotal + normalizedPageSize - 1) / normalizedPageSize;

        return new PageResultDto<>(
                true,
                CommonStatus.SUCCESS.getCode(),
                CommonStatus.SUCCESS.getStatus(),
                CommonStatus.SUCCESS.getMessage(),
                records,
                normalizedTotal,
                normalizedPageNo,
                normalizedPageSize,
                totalPages,
                totalPages > normalizedPageNo,
                normalizedPageNo > 1 && totalPages > 0,
                TraceContext.getTraceId(),
                now()
        );
    }

    private static long now() {
        return Instant.now().toEpochMilli();
    }
}
