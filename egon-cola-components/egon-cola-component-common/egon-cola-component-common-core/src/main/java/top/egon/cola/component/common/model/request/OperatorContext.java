package top.egon.cola.component.common.model.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serial;
import java.io.Serializable;

/**
 * Operator identity carried by application requests.
 *
 * @param operatorId operator id, nullable
 * @param operatorName operator display name, nullable
 * @param tenantId tenant id, nullable
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"operatorId", "operatorName", "tenantId"})
public record OperatorContext(
        @JsonProperty("operatorId") String operatorId,
        @JsonProperty("operatorName") String operatorName,
        @JsonProperty("tenantId") String tenantId
) implements Serializable {

    @Serial
    @JsonIgnore
    private static final long serialVersionUID = 1L;
}
