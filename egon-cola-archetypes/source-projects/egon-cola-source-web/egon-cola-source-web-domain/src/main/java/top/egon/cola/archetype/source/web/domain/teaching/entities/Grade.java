package top.egon.cola.archetype.source.web.domain.teaching.entities;

import top.egon.cola.archetype.source.web.domain.teaching.enums.GradeStatus;
import top.egon.cola.archetype.source.web.domain.teaching.vos.GradeCode;

public record Grade(Long id, GradeCode code, String name, GradeStatus status) {
    public Grade {
        name = name == null ? "" : name.trim();
    }
}
