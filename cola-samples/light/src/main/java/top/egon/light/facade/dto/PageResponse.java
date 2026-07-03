package top.egon.light.facade.dto;

import java.io.Serializable;
import java.util.List;

public record PageResponse<T>(
        List<T> records,
        int currentPage,
        int totalPages,
        int pageSize,
        long totalCount
) implements Serializable {
    public static <T> PageResponse<T> of(List<T> records, int currentPage, int totalPages, int pageSize, long totalCount) {
        return new PageResponse<>(records, currentPage, totalPages, pageSize, totalCount);
    }
}
