package top.egon.cola.archetype.source.light.facade.teaching.dto;

import java.io.Serializable;

public record CreateCourseDTO(
        String code,
        String name,
        String operatorId,
        String requestId) implements Serializable {
}
