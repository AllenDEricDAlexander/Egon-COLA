package top.egon.cola.component.common.result.model;

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
 * Internal page operation result model.
 *
 * @param success whether the operation succeeded
 * @param code stable enterprise status code
 * @param status stable enterprise status text
 * @param message internal message
 * @param records current page records, never null
 * @param total total record count
 * @param pageNo current page number, starts from 1
 * @param pageSize page size
 * @param pages total page count
 * @param hasNext whether next page exists
 * @param hasPrevious whether previous page exists
 * @param <T> record type
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({
        "success", "code", "status", "message",
        "records", "total", "pageNo", "pageSize", "pages", "hasNext", "hasPrevious"
})
public record PageResultModel<T>(
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
        @JsonProperty("hasPrevious") boolean hasPrevious
) implements Serializable {

    @Serial
    @JsonIgnore
    private static final long serialVersionUID = 1L;

    public PageResultModel {
        records = records == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(records));
    }
}
