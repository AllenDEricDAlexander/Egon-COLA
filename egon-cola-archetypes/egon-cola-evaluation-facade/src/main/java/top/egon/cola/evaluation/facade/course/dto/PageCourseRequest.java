package top.egon.cola.evaluation.facade.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.io.Serializable;

public record PageCourseRequest(
        @Min(1) int currentPage,
        @Min(1) @Max(200) int pageSize) implements Serializable {
}
