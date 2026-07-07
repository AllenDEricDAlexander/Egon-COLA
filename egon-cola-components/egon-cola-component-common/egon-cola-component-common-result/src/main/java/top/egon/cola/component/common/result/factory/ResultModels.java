package top.egon.cola.component.common.result.factory;

import top.egon.cola.component.common.core.code.CommonStatus;
import top.egon.cola.component.common.core.code.ErrorStatus;
import top.egon.cola.component.common.result.model.PageResultModel;
import top.egon.cola.component.common.result.model.ResultModel;

import java.util.List;

/**
 * Factory methods for internal result models.
 */
public final class ResultModels {

    private ResultModels() {
    }

    public static <T> ResultModel<T> success() {
        return success(null);
    }

    public static <T> ResultModel<T> success(T data) {
        ResultModel<T> result = new ResultModel<>();
        fill(result, true, CommonStatus.SUCCESS);
        result.setData(data);
        return result;
    }

    public static <T> ResultModel<T> failure(ErrorStatus status) {
        ResultModel<T> result = new ResultModel<>();
        fill(result, false, status);
        return result;
    }

    public static <T> PageResultModel<T> page(List<T> records, long total, int pageNo, int pageSize) {
        int normalizedPageNo = Math.max(pageNo, 1);
        int normalizedPageSize = pageSize <= 0 ? 10 : pageSize;
        long normalizedTotal = Math.max(total, 0);
        long totalPages = normalizedTotal == 0 ? 0 : (normalizedTotal + normalizedPageSize - 1) / normalizedPageSize;

        PageResultModel<T> result = new PageResultModel<>();
        result.setSuccess(true);
        result.setCode(CommonStatus.SUCCESS.getCode());
        result.setStatus(CommonStatus.SUCCESS.getStatus());
        result.setMessage(CommonStatus.SUCCESS.getMessage());
        result.setRecords(records);
        result.setTotal(normalizedTotal);
        result.setPageNo(normalizedPageNo);
        result.setPageSize(normalizedPageSize);
        result.setPages(totalPages);
        result.setHasPrevious(normalizedPageNo > 1 && totalPages > 0);
        result.setHasNext(totalPages > normalizedPageNo);
        return result;
    }

    private static <T> void fill(ResultModel<T> result, boolean success, ErrorStatus status) {
        result.setSuccess(success);
        result.setCode(status.getCode());
        result.setStatus(status.getStatus());
        result.setMessage(status.getMessage());
    }
}
