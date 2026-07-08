package top.egon.cola.component.common.result.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serial;
import java.io.Serializable;

/**
 * External error response DTO.
 *
 * @param code stable enterprise status code
 * @param status stable enterprise status text
 * @param message response message safe for client display
 * @param traceId trace id from MDC, nullable
 * @param timestamp response created timestamp in milliseconds
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"code", "status", "message", "traceId", "timestamp"})
public record ErrorResultDto(
        @JsonProperty("code") int code,
        @JsonProperty("status") String status,
        @JsonProperty("message") String message,
        @JsonProperty("traceId") String traceId,
        @JsonProperty("timestamp") Long timestamp
) implements Serializable {

    @Serial
    @JsonIgnore
    private static final long serialVersionUID = 1L;
}
