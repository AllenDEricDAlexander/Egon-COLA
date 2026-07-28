package top.egon.cola.component.common.model.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serial;
import java.io.Serializable;
import java.util.Locale;

/**
 * Common sorting query fragment.
 *
 * @param sortBy requested sort field name
 * @param sortDirection requested sort direction, ASC or DESC
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"sortBy", "sortDirection"})
public record SortQuery(
        @JsonProperty("sortBy") String sortBy,
        @JsonProperty("sortDirection") String sortDirection
) implements Serializable {

    @Serial
    @JsonIgnore
    private static final long serialVersionUID = 1L;

    public SortQuery {
        sortBy = normalizeBlank(sortBy);
        sortDirection = normalizeDirection(sortDirection);
    }

    @JsonIgnore
    public boolean hasSort() {
        return sortBy != null && !sortBy.isBlank();
    }

    private static String normalizeDirection(String sortDirection) {
        String value = normalizeBlank(sortDirection);
        if (value == null) {
            return null;
        }
        return "DESC".equals(value.toUpperCase(Locale.ROOT)) ? "DESC" : "ASC";
    }

    private static String normalizeBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
