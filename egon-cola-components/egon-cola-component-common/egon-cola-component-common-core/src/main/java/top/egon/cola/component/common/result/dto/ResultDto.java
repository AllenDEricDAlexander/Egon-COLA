package top.egon.cola.component.common.result.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serial;
import java.io.Serializable;

/**
 * External single-object response DTO.
 *
 * @param success whether the operation succeeded
 * @param code stable enterprise status code
 * @param status stable enterprise status text
 * @param message response message safe for client display
 * @param data response payload, nullable
 * @param traceId trace id from MDC, nullable
 * @param timestamp response created timestamp in milliseconds
 * @param <T> payload type
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"success", "code", "status", "message", "data", "traceId", "timestamp"})
public record ResultDto<T>(
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
}
