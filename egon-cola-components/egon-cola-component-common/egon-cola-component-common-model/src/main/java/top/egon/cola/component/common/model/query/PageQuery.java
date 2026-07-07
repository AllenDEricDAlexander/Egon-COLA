package top.egon.cola.component.common.model.query;

import java.io.Serial;
import java.io.Serializable;

/**
 * Common page query with safe page number and page size normalization.
 */
public class PageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int pageNo = 1;

    private int pageSize = 10;

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = Math.max(pageNo, 1);
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize <= 0 ? 10 : pageSize;
    }

    public int offset() {
        return (pageNo - 1) * pageSize;
    }
}
