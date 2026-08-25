package ${package}.domain.teaching.entities;

import ${package}.domain.teaching.enums.GradeStatus;
import ${package}.domain.teaching.vos.GradeCode;

public record Grade(Long id, GradeCode code, String name, GradeStatus status) {
    public Grade {
        name = name == null ? "" : name.trim();
    }
}
