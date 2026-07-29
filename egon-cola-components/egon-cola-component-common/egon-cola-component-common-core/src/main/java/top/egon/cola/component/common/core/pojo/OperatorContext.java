package top.egon.cola.component.common.core.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serial;
import java.io.Serializable;

/**
 * Operator identity attached to commands, queries, or audit context.
 *
 * @param userId operator user id
 * @param userName operator display name
 * @param tenantId operator tenant id
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"userId", "userName", "tenantId"})
public record OperatorContext(
        @JsonProperty("userId") String userId,
        @JsonProperty("userName") String userName,
        @JsonProperty("tenantId") String tenantId
) implements Serializable {

    @Serial
    @JsonIgnore
    private static final long serialVersionUID = 1L;
}
