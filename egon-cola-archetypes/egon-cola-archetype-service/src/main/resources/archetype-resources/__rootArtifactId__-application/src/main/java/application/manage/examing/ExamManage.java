#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.manage.examing;

import ${package}.domain.entities.examing.ExamResult;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public interface ExamManage {

    ExamResult record(@NotBlank String courseId, @NotBlank String studentId, @Min(0) @Max(100) int score);

    ExamResult getById(@NotBlank String examResultId);
}
