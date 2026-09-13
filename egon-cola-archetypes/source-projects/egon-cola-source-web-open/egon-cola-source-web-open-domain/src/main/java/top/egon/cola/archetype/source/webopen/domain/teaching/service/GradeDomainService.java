package top.egon.cola.archetype.source.webopen.domain.teaching.service;

import top.egon.cola.archetype.source.webopen.domain.teaching.entities.Grade;
import top.egon.cola.archetype.source.webopen.domain.teaching.vos.GradeCode;

import java.util.Optional;

public interface GradeDomainService {
    Grade create(Long gradeId, String code, String name);

    Optional<Grade> findById(Long gradeId);

    Optional<Grade> findByCode( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull GradeCode code);

    boolean existsByCode( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull GradeCode code);

    Grade save( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull Grade grade);
}
