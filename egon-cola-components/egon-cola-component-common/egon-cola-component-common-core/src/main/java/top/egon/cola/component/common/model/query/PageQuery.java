package top.egon.cola.component.common.model.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serial;
import java.io.Serializable;

/**
 * Common page query with safe page number and page size normalization.
 *
 * @param pageNo page number, starts from 1
 * @param pageSize page size
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"pageNo", "pageSize"})
public record PageQuery(
        @JsonProperty("pageNo") int pageNo,
        @JsonProperty("pageSize") int pageSize
) implements Serializable {

    @Serial
    @JsonIgnore
    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_PAGE_NO = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 500;

    public PageQuery {
        pageNo = Math.max(pageNo, DEFAULT_PAGE_NO);
        pageSize = normalizePageSize(pageSize);
    }

    public static PageQuery defaultPage() {
        return new PageQuery(DEFAULT_PAGE_NO, DEFAULT_PAGE_SIZE);
    }

    @JsonIgnore
    public int offset() {
        return Math.max((pageNo - 1) * pageSize, 0);
    }

    private static int normalizePageSize(int pageSize) {
        if (pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
