package top.egon.cola.component.common.result.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serial;
import java.io.Serializable;

/**
 * Internal single-object operation result model.
 *
 * @param success whether the operation succeeded
 * @param code stable enterprise status code
 * @param status stable enterprise status text
 * @param message internal message
 * @param data internal payload, nullable
 * @param <T> payload type
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"success", "code", "status", "message", "data"})
public record ResultModel<T>(
        @JsonProperty("success") boolean success,
        @JsonProperty("code") int code,
        @JsonProperty("status") String status,
        @JsonProperty("message") String message,
        @JsonProperty("data") T data
) implements Serializable {

    @Serial
    @JsonIgnore
    private static final long serialVersionUID = 1L;
}
