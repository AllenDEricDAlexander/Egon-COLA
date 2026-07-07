package top.egon.cola.component.common.model.page;

import java.io.Serial;
import java.io.Serializable;

/**
 * Pagination metadata shared by page result models.
 */
public class PageMeta implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private long total;

    private int pageNo;

    private int pageSize;

    private long pages;

    private boolean hasNext;

    private boolean hasPrevious;

    public static PageMeta of(long total, int pageNo, int pageSize) {
        int normalizedPageNo = Math.max(pageNo, 1);
        int normalizedPageSize = pageSize <= 0 ? 10 : pageSize;
        long normalizedTotal = Math.max(total, 0);
        long totalPages = normalizedTotal == 0 ? 0 : (normalizedTotal + normalizedPageSize - 1) / normalizedPageSize;

        PageMeta meta = new PageMeta();
        meta.total = normalizedTotal;
        meta.pageNo = normalizedPageNo;
        meta.pageSize = normalizedPageSize;
        meta.pages = totalPages;
        meta.hasPrevious = normalizedPageNo > 1 && totalPages > 0;
        meta.hasNext = totalPages > normalizedPageNo;
        return meta;
    }

    public long getTotal() {
        return total;
    }

    public int getPageNo() {
        return pageNo;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getPages() {
        return pages;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }
}
