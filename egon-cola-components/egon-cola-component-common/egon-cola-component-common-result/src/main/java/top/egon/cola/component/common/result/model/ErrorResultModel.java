package top.egon.cola.component.common.result.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serial;
import java.io.Serializable;

/**
 * Internal error description model.
 *
 * @param code stable enterprise status code
 * @param status stable enterprise status text
 * @param message internal message
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"code", "status", "message"})
public record ErrorResultModel(
        @JsonProperty("code") int code,
        @JsonProperty("status") String status,
        @JsonProperty("message") String message
) implements Serializable {

    @Serial
    @JsonIgnore
    private static final long serialVersionUID = 1L;
}
