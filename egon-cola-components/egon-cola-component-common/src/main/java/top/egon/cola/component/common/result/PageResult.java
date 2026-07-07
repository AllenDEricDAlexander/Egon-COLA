package top.egon.cola.component.common.result;

import java.io.Serial;

import top.egon.cola.component.common.exception.ErrorCodes;
import top.egon.cola.component.common.trace.TraceContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 通用分页返回结果，自动计算分页元数据并携带 traceId。
 */
public class PageResult<T> implements Serializable {



    @Serial
    private static final long serialVersionUID = 1L;

    private boolean success;

    private String code;

    private String message;

    private List<T> records = new ArrayList<>();

    private long total;

    private int pageNo;

    private int pageSize;

    private long pages;

    private boolean hasNext;

    private boolean hasPrevious;

    private String traceId;

    public static <T> PageResult<T> success(List<T> records, long total, int pageNo, int pageSize) {
        int normalizedPageNo = Math.max(pageNo, 1);
        int normalizedPageSize = Math.max(pageSize, 1);
        long normalizedTotal = Math.max(total, 0);
        long totalPages = normalizedTotal == 0 ? 0 : (normalizedTotal + normalizedPageSize - 1) / normalizedPageSize;

        PageResult<T> result = new PageResult<>();
        result.success = true;
        result.code = ErrorCodes.SUCCESS;
        result.message = "success";
        result.records = records == null ? Collections.emptyList() : new ArrayList<>(records);
        result.total = normalizedTotal;
        result.pageNo = normalizedPageNo;
        result.pageSize = normalizedPageSize;
        result.pages = totalPages;
        result.hasPrevious = normalizedPageNo > 1 && totalPages > 0;
        result.hasNext = totalPages > normalizedPageNo;
        result.traceId = TraceContext.getTraceId();
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public List<T> getRecords() {
        return records;
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

    public String getTraceId() {
        return traceId;
    }
}
