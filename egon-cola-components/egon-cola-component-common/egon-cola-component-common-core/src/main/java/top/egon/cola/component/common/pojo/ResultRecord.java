package top.egon.cola.component.common.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import top.egon.cola.component.common.enums.ErrorStatus;
import top.egon.cola.component.common.enums.ResultCode;
import top.egon.cola.component.common.exception.CommonException;
import top.egon.cola.component.common.trace.TraceContext;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * External single-object response record.
 *
 * @param success whether the operation succeeded
 * @param code stable business result code
 * @param status stable business result status text
 * @param message response message safe for client display
 * @param data response payload, nullable
 * @param traceId trace id from MDC, nullable
 * @param timestamp response created timestamp in milliseconds
 * @param <T> payload type
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"success", "code", "status", "message", "data", "traceId", "timestamp"})
public record ResultRecord<T>(
        @JsonProperty("success") boolean success,
        @JsonProperty("code") int code,
        @JsonProperty("status") String status,
        @JsonProperty("message") String message,
        @JsonProperty("data") T data,
        @JsonProperty("traceId") String traceId,
        @JsonProperty("timestamp") Long timestamp
) implements Serializable {

    @Serial
    @JsonIgnore
    private static final long serialVersionUID = 1L;

    public static <T> ResultRecord<T> success(T data) {
        return result(ResultCode.SUCCESS, true, data);
    }

    public static <T> ResultRecord<T> failure(ErrorStatus status) {
        return result(status, false, null);
    }

    public static <T> ResultRecord<T> failure(Throwable throwable) {
        if (throwable instanceof CommonException exception) {
            return failure(exception.getCode(), exception.getStatus(), exception.getMessage());
        }
        return failure(ResultCode.SYSTEM_ERROR);
    }

    public static <T> ResultRecord<T> failure(int code, String message) {
        return failure(code, null, message);
    }

    public static <T> ResultRecord<T> failure(int code, String status, String message) {
        return result(code, status, message, false, null);
    }

    public static <T> ResultRecord<T> result(ErrorStatus status, boolean success, T data) {
        return result(status.getCode(), status.getStatus(), status.getMessage(), success, data);
    }

    public static <T> ResultRecord<T> result(int code, String message, boolean success, T data) {
        return result(code, null, message, success, data);
    }

    public static <T> ResultRecord<T> result(int code, String status, String message, boolean success, T data) {
        return new ResultRecord<>(success, code, status, message, data, TraceContext.getTraceId(), now());
    }

    private static long now() {
        return Instant.now().toEpochMilli();
    }
}
