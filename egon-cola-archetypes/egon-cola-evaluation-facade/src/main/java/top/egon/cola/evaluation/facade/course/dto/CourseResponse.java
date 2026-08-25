package top.egon.cola.evaluation.facade.course.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;

public record CourseResponse(
        @NotNull @Positive Long id,
        String code,
        String name,
        int credit,
        String status) implements Serializable {
}
