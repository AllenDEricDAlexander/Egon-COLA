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
        ResultDto<T> result = new ResultDto<>();
        fill(result, true, CommonStatus.SUCCESS);
        result.setData(data);
        return result;
    }

    public static <T> ResultDto<T> failure(ErrorStatus status) {
        ResultDto<T> result = new ResultDto<>();
        fill(result, false, status);
        return result;
    }

    public static <T> ResultDto<T> failure(Throwable throwable) {
        if (throwable instanceof EgonException exception) {
            return failure(exception.getCode(), exception.getStatus(), exception.getMessage());
        }
        String message = throwable == null ? CommonStatus.SYSTEM_ERROR.getMessage() : throwable.getMessage();
        return failure(CommonStatus.SYSTEM_ERROR.getCode(), CommonStatus.SYSTEM_ERROR.getStatus(), message);
    }

    public static <T> ResultDto<T> failure(int code, String status, String message) {
        ResultDto<T> result = new ResultDto<>();
        result.setSuccess(false);
        result.setCode(code);
        result.setStatus(status);
        result.setMessage(message);
        result.setTraceId(TraceContext.getTraceId());
        result.setTimestamp(Instant.now().toEpochMilli());
        return result;
    }

    public static <T> PageResultDto<T> page(List<T> records, long total, int pageNo, int pageSize) {
        int normalizedPageNo = Math.max(pageNo, 1);
        int normalizedPageSize = pageSize <= 0 ? 10 : pageSize;
        long normalizedTotal = Math.max(total, 0);
        long totalPages = normalizedTotal == 0 ? 0 : (normalizedTotal + normalizedPageSize - 1) / normalizedPageSize;

        PageResultDto<T> result = new PageResultDto<>();
        result.setSuccess(true);
        result.setCode(CommonStatus.SUCCESS.getCode());
        result.setStatus(CommonStatus.SUCCESS.getStatus());
        result.setMessage(CommonStatus.SUCCESS.getMessage());
        result.setRecords(records);
        result.setTotal(normalizedTotal);
        result.setPageNo(normalizedPageNo);
        result.setPageSize(normalizedPageSize);
        result.setPages(totalPages);
        result.setHasPrevious(normalizedPageNo > 1 && totalPages > 0);
        result.setHasNext(totalPages > normalizedPageNo);
        result.setTraceId(TraceContext.getTraceId());
        result.setTimestamp(Instant.now().toEpochMilli());
        return result;
    }

    private static <T> void fill(ResultDto<T> result, boolean success, ErrorStatus status) {
        result.setSuccess(success);
        result.setCode(status.getCode());
        result.setStatus(status.getStatus());
        result.setMessage(status.getMessage());
        result.setTraceId(TraceContext.getTraceId());
        result.setTimestamp(Instant.now().toEpochMilli());
    }
}
