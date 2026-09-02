package top.egon.cola.archetype.source.light.facade.teaching.dto;

import java.io.Serializable;

public record CourseDTO(Long id, String code, String name, String status) implements Serializable {
}
