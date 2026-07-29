package top.egon.cola.component.common.core.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import top.egon.cola.component.common.core.enums.ErrorStatus;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.trace.TraceContext;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * External page response record.
 *
 * @param success whether the operation succeeded
 * @param code stable business result code
 * @param status stable business result status text
 * @param message response message safe for client display
 * @param records current page records, never null
 * @param page page metadata
 * @param traceId trace id from MDC, nullable
 * @param timestamp response created timestamp in milliseconds
 * @param <T> record type
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"success", "code", "status", "message", "records", "page", "traceId", "timestamp"})
public record PageResultRecord<T>(
        @JsonProperty("success") boolean success,
        @JsonProperty("code") int code,
        @JsonProperty("status") String status,
        @JsonProperty("message") String message,
        @JsonProperty("records") List<T> records,
        @JsonProperty("page") PageMetaRecord page,
        @JsonProperty("traceId") String traceId,
        @JsonProperty("timestamp") Long timestamp
) implements Serializable {

    @Serial
    @JsonIgnore
    private static final long serialVersionUID = 1L;

    public PageResultRecord {
        records = records == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(records));
        page = page == null ? PageMetaRecord.of(0, 1, 10) : page;
    }

    public static <T> PageResultRecord<T> success(List<T> records, long total, int pageNo, int pageSize) {
        return result(
                ResultCode.SUCCESS.getCode(),
                ResultCode.SUCCESS.getStatus(),
                ResultCode.SUCCESS.getMessage(),
                true,
                records,
                PageMetaRecord.of(total, pageNo, pageSize)
        );
    }

    public static <T> PageResultRecord<T> failure(ErrorStatus status) {
        return result(status.getCode(), status.getStatus(), status.getMessage(), false, List.of(), PageMetaRecord.of(0, 1, 10));
    }

    public static <T> PageResultRecord<T> result(int code,
                                                 String message,
                                                 boolean success,
                                                 List<T> records,
                                                 PageMetaRecord page) {
        return result(code, null, message, success, records, page);
    }

    public static <T> PageResultRecord<T> result(int code,
                                                 String status,
                                                 String message,
                                                 boolean success,
                                                 List<T> records,
                                                 PageMetaRecord page) {
        return new PageResultRecord<>(success, code, status, message, records, page, TraceContext.getTraceId(), now());
    }

    private static long now() {
        return Instant.now().toEpochMilli();
    }
}
