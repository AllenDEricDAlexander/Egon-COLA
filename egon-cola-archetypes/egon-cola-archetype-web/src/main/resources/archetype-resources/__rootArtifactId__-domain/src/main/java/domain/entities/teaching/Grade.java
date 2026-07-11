package ${package}.domain.entities.teaching;

import ${package}.domain.enums.teaching.GradeStatus;
import ${package}.domain.vos.teaching.GradeCode;

public record Grade(String id, GradeCode code, String name, GradeStatus status) {
    public Grade {
        name = name == null ? "" : name.trim();
    }
}
