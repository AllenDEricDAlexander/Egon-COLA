#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade.exam.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

public record RecordScoreRequest(
        @NotBlank String examId,
        @NotBlank String studentId,
        @Min(0) @Max(100) int points) implements Serializable {
}
