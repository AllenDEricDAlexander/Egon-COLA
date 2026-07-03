#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade.dto.examing;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RecordExamResultRequest(
        @NotBlank(message = "courseId must not be blank")
        String courseId,
        @NotBlank(message = "studentId must not be blank")
        String studentId,
        @Min(value = 0, message = "score must be greater than or equal to 0")
        @Max(value = 100, message = "score must be less than or equal to 100")
        int score
) {
}
