package ${package}.facade.teaching.dto;

import java.io.Serializable;

public record CreateCourseDTO(
        String code,
        String name,
        String operatorId,
        String requestId) implements Serializable {
}
