package top.egon.fable.facade.dto.course;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateCourseRequest(@NotBlank String name, @Min(1) int credit) {
}
