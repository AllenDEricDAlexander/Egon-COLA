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
 * Common page model for internal application data.
 *
 * @param records current page records, never null
 * @param meta pagination metadata
 * @param <T> record type
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"records", "meta"})
public record PageModel<T>(
        @JsonProperty("records") List<T> records,
        @JsonProperty("meta") PageMeta meta
) implements Serializable {

    @Serial
    @JsonIgnore
    private static final long serialVersionUID = 1L;

    public PageModel {
        records = records == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(records));
        meta = meta == null ? PageMeta.of(0, 1, 10) : meta;
    }

    public static <T> PageModel<T> of(List<T> records, long total, int pageNo, int pageSize) {
        return new PageModel<>(records, PageMeta.of(total, pageNo, pageSize));
    }

    @JsonIgnore
    public long total() {
        return meta.total();
    }

    @JsonIgnore
    public int pageNo() {
        return meta.pageNo();
    }

    @JsonIgnore
    public int pageSize() {
        return meta.pageSize();
    }

    @JsonIgnore
    public long pages() {
        return meta.pages();
    }

    @JsonIgnore
    public boolean hasNext() {
        return meta.hasNext();
    }

    @JsonIgnore
    public boolean hasPrevious() {
        return meta.hasPrevious();
    }
}
