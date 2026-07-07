package top.egon.cola.component.common.model.page;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Common page model for internal application data.
 */
public class PageModel<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<T> records = Collections.emptyList();

    private PageMeta meta;

    public static <T> PageModel<T> of(List<T> records, long total, int pageNo, int pageSize) {
        PageModel<T> page = new PageModel<>();
        page.records = records == null ? Collections.emptyList() : new ArrayList<>(records);
        page.meta = PageMeta.of(total, pageNo, pageSize);
        return page;
    }

    public List<T> getRecords() {
        return records;
    }

    public PageMeta getMeta() {
        return meta;
    }

    public long getTotal() {
        return meta.getTotal();
    }

    public int getPageNo() {
        return meta.getPageNo();
    }

    public int getPageSize() {
        return meta.getPageSize();
    }

    public long getPages() {
        return meta.getPages();
    }

    public boolean isHasNext() {
        return meta.isHasNext();
    }

    public boolean isHasPrevious() {
        return meta.isHasPrevious();
    }
}
