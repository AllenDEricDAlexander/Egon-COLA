package top.egon.cola.archetype.source.serviceopen.application.course.pojo.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PageCourseQuery(
        @Min(1) int currentPage,
        @Min(1) @Max(200) int pageSize) {
}
