package top.egon.fable.web.domain.common;

import java.util.List;

public record Page<T>(
        List<T> records,
        int currentPage,
        int totalPages,
        int pageSize,
        long totalCount
) {
    public static <T> Page<T> of(List<T> records, int currentPage, int totalPages, int pageSize, long totalCount) {
        return new Page<>(records, currentPage, totalPages, pageSize, totalCount);
    }
}
