package top.egon.cola.component.common.model.page;

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
 * Slice model for pagination scenarios where total count is not available.
 *
 * @param records current slice records, never null
 * @param hasNext whether next slice exists
 * @param <T> record type
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"records", "hasNext"})
public record PageSlice<T>(
        @JsonProperty("records") List<T> records,
        @JsonProperty("hasNext") boolean hasNext
) implements Serializable {

    @Serial
    @JsonIgnore
    private static final long serialVersionUID = 1L;

    public PageSlice {
        records = records == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(records));
    }

    public static <T> PageSlice<T> of(List<T> records, boolean hasNext) {
        return new PageSlice<>(records, hasNext);
    }
}
