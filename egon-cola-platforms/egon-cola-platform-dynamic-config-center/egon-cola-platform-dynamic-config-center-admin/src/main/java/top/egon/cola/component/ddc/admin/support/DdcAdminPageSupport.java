package top.egon.cola.component.ddc.admin.support;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.core.pojo.PageResultRecord;

import java.util.List;

public final class DdcAdminPageSupport {

    private DdcAdminPageSupport() {
    }

    public static Pageable pageable(PageQuery query, Sort sort) {
        PageQuery value = query == null ? PageQuery.defaultPage() : query;
        return PageRequest.of(value.pageNo() - 1, value.pageSize(), sort);
    }

    public static Pageable pageable(PageQuery query) {
        return pageable(query, Sort.unsorted());
    }

    public static <T> Page<T> slice(List<T> records, PageQuery query) {
        List<T> values = records == null ? List.of() : List.copyOf(records);
        Pageable pageable = pageable(query);
        int from = (int) Math.min(pageable.getOffset(), values.size());
        int to = Math.min(from + pageable.getPageSize(), values.size());
        return new PageImpl<>(values.subList(from, to), pageable, values.size());
    }

    public static <T> PageResultRecord<T> result(Page<T> page) {
        return PageResultRecord.success(
                page.getContent(),
                page.getTotalElements(),
                page.getNumber() + 1,
                page.getSize()
        );
    }
}
