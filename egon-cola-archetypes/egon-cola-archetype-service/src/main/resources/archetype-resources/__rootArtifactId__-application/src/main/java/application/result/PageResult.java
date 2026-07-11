#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.result;

import java.util.List;

public record PageResult<T>(
        List<T> records,
        int currentPage,
        int totalPages,
        int pageSize,
        long totalCount) {

    public static <T> PageResult<T> of(
            List<T> records, int currentPage, int totalPages, int pageSize, long totalCount) {
        return new PageResult<>(List.copyOf(records), currentPage, totalPages, pageSize, totalCount);
    }
}
