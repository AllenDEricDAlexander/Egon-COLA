package top.egon.cola.archetype.source.light.domain.teaching.entities;

import top.egon.cola.archetype.source.light.domain.teaching.enums.SchoolClassStatus;
import top.egon.cola.archetype.source.light.domain.teaching.vos.SchoolClassId;
import top.egon.cola.archetype.source.light.domain.teaching.vos.Semester;

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
