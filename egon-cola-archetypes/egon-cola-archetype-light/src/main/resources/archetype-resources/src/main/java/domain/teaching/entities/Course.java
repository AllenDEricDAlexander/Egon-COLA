package ${package}.domain.teaching.entities;

import ${package}.domain.teaching.enums.CourseStatus;
import ${package}.domain.teaching.vos.CourseCode;

import java.util.Objects;

public record Course(String id, CourseCode code, String name, CourseStatus status) {
    public Course {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        Objects.requireNonNull(code, "code must not be null");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(status, "status must not be null");
    }
}
