package top.egon.fable.facade.dto.examing;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RecordExamResultRequest(@NotBlank String courseId, @NotBlank String studentId, @Min(0) @Max(100) int score) {
}
