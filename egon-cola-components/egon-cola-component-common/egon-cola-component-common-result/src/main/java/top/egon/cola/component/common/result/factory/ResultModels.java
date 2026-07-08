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
        return new ResultModel<>(
                true,
                CommonStatus.SUCCESS.getCode(),
                CommonStatus.SUCCESS.getStatus(),
                CommonStatus.SUCCESS.getMessage(),
                data
        );
    }

    public static <T> ResultModel<T> failure(ErrorStatus status) {
        return new ResultModel<>(false, status.getCode(), status.getStatus(), status.getMessage(), null);
    }

    public static <T> PageResultModel<T> page(List<T> records, long total, int pageNo, int pageSize) {
        int normalizedPageNo = Math.max(pageNo, 1);
        int normalizedPageSize = pageSize <= 0 ? 10 : pageSize;
        long normalizedTotal = Math.max(total, 0);
        long totalPages = normalizedTotal == 0 ? 0 : (normalizedTotal + normalizedPageSize - 1) / normalizedPageSize;

        return new PageResultModel<>(
                true,
                CommonStatus.SUCCESS.getCode(),
                CommonStatus.SUCCESS.getStatus(),
                CommonStatus.SUCCESS.getMessage(),
                records,
                normalizedTotal,
                normalizedPageNo,
                normalizedPageSize,
                totalPages,
                totalPages > normalizedPageNo,
                normalizedPageNo > 1 && totalPages > 0
        );
    }
}
