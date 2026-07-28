package top.egon.cola.component.common.result.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * External page response DTO.
 *
 * <p>Keep field name {@code pages} for backward compatibility with the JSON contract.</p>
 *
 * @param success whether the operation succeeded
 * @param code stable enterprise status code
 * @param status stable enterprise status text
 * @param message response message safe for client display
 * @param records current page records, never null
 * @param total total record count
 * @param pageNo current page number, starts from 1
 * @param pageSize page size
 * @param pages total page count
 * @param hasNext whether next page exists
 * @param hasPrevious whether previous page exists
 * @param traceId trace id from MDC, nullable
 * @param timestamp response created timestamp in milliseconds
 * @param <T> record type
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({
        "success", "code", "status", "message",
        "records", "total", "pageNo", "pageSize", "pages", "hasNext", "hasPrevious",
        "traceId", "timestamp"
})
public record PageResultDto<T>(
        @JsonProperty("success") boolean success,
        @JsonProperty("code") int code,
        @JsonProperty("status") String status,
        @JsonProperty("message") String message,
        @JsonProperty("records") List<T> records,
        @JsonProperty("total") long total,
        @JsonProperty("pageNo") int pageNo,
        @JsonProperty("pageSize") int pageSize,
        @JsonProperty("pages") long pages,
        @JsonProperty("hasNext") boolean hasNext,
        @JsonProperty("hasPrevious") boolean hasPrevious,
        @JsonProperty("traceId") String traceId,
        @JsonProperty("timestamp") Long timestamp
) implements Serializable {

    @Serial
    @JsonIgnore
    private static final long serialVersionUID = 1L;

    public PageResultDto {
        records = records == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(records));
    }
}
