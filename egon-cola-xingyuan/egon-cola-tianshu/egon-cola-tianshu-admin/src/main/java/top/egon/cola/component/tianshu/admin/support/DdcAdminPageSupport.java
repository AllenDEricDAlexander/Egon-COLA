package top.egon.cola.component.tianshu.admin.support;

import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;

import java.util.List;

public final class DdcAdminPageSupport {

    private DdcAdminPageSupport() {
    }

    public static <T> DdcPageResult<T> slice(List<T> records, PageQuery query) {
        List<T> values = records == null ? List.of() : List.copyOf(records);
        PageQuery value = query == null ? PageQuery.defaultPage() : query;
        long offset = (long) (value.pageNo() - 1) * value.pageSize();
        int from = (int) Math.min(offset, values.size());
        int to = (int) Math.min(offset + value.pageSize(), values.size());
        return DdcPageResult.<T>builder()
                .records(values.subList(from, to))
                .total(values.size())
                .pageNo(value.pageNo())
                .pageSize(value.pageSize())
                .build();
    }

    public static <T> PageResultRecord<T> result(DdcPageResult<T> page) {
        return PageResultRecord.success(
                page.getRecords(),
                page.getTotal(),
                page.getPageNo(),
                page.getPageSize()
        );
    }
}
