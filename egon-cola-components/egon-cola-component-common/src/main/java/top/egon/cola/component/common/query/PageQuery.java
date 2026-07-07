package top.egon.cola.component.common.query;

import java.io.Serial;

import top.egon.cola.component.common.model.BaseQuery;

/**
 * 分页查询基础参数，不绑定 Bean Validation。
 */
public class PageQuery extends BaseQuery {



    @Serial
    private static final long serialVersionUID = 1L;

    public static final String ASC = "ASC";

    public static final String DESC = "DESC";

    private static final int DEFAULT_PAGE_SIZE = 10;

    private static final int MIN_PAGE_VALUE = 1;

    private int pageNo = MIN_PAGE_VALUE;

    private int pageSize = DEFAULT_PAGE_SIZE;

    private String orderBy;

    private String orderDirection = DESC;

    public int getPageNo() {
        return Math.max(pageNo, MIN_PAGE_VALUE);
    }

    public void setPageNo(int pageNo) {
        this.pageNo = Math.max(pageNo, MIN_PAGE_VALUE);
    }

    public int getPageSize() {
        return Math.max(pageSize, MIN_PAGE_VALUE);
    }

    public void setPageSize(int pageSize) {
        this.pageSize = Math.max(pageSize, MIN_PAGE_VALUE);
    }

    public int getOffset() {
        return (getPageNo() - 1) * getPageSize();
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public String getOrderDirection() {
        return orderDirection;
    }

    public void setOrderDirection(String orderDirection) {
        if (ASC.equalsIgnoreCase(orderDirection)) {
            this.orderDirection = ASC;
            return;
        }
        if (DESC.equalsIgnoreCase(orderDirection)) {
            this.orderDirection = DESC;
        }
    }
}
