#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade.exam.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

public record PageScoreRequest(
        @NotBlank String examId,
        @Min(1) int currentPage,
        @Min(1) @Max(200) int pageSize) implements Serializable {
}
