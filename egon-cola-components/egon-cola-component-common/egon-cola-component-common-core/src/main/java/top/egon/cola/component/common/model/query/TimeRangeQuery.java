package top.egon.cola.component.common.model.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Common time range query fragment.
 *
 * @param startTime range start time, nullable
 * @param endTime range end time, nullable
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"startTime", "endTime"})
public record TimeRangeQuery(
        @JsonProperty("startTime")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime startTime,
        @JsonProperty("endTime")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime endTime
) implements Serializable {

    @Serial
    @JsonIgnore
    private static final long serialVersionUID = 1L;

    @JsonIgnore
    public boolean hasStartTime() {
        return startTime != null;
    }

    @JsonIgnore
    public boolean hasEndTime() {
        return endTime != null;
    }

    @JsonIgnore
    public boolean hasTimeRange() {
        return startTime != null || endTime != null;
    }
}
