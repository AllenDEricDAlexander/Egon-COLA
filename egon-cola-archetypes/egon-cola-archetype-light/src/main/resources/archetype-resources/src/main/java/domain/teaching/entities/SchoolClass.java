package ${package}.domain.teaching.entities;

import ${package}.domain.teaching.enums.SchoolClassStatus;
import ${package}.domain.teaching.vos.SchoolClassId;
import ${package}.domain.teaching.vos.Semester;

import java.util.Objects;

public record SchoolClass(SchoolClassId id, String name, Semester semester, SchoolClassStatus status) {
    public SchoolClass {
        Objects.requireNonNull(id, "id must not be null");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(semester, "semester must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}
