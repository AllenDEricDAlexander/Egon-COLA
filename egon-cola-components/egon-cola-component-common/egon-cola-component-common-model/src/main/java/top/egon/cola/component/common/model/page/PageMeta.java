package top.egon.cola.component.common.model.page;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serial;
import java.io.Serializable;

/**
 * Pagination metadata shared by page result models.
 *
 * @param total total record count
 * @param pageNo current page number, starts from 1
 * @param pageSize page size
 * @param pages total page count
 * @param hasNext whether next page exists
 * @param hasPrevious whether previous page exists
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"total", "pageNo", "pageSize", "pages", "hasNext", "hasPrevious"})
public record PageMeta(
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

    public static PageMeta of(long total, int pageNo, int pageSize) {
        int normalizedPageNo = Math.max(pageNo, 1);
        int normalizedPageSize = pageSize <= 0 ? 10 : pageSize;
        long normalizedTotal = Math.max(total, 0);
        long totalPages = normalizedTotal == 0 ? 0 : (normalizedTotal + normalizedPageSize - 1) / normalizedPageSize;

        return new PageMeta(
                normalizedTotal,
                normalizedPageNo,
                normalizedPageSize,
                totalPages,
                totalPages > normalizedPageNo,
                normalizedPageNo > 1 && totalPages > 0
        );
    }
}
