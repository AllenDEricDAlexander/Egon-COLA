package top.egon.cola.archetype.source.web.domain.teaching.service;

import top.egon.cola.archetype.source.web.domain.teaching.entities.Grade;
import top.egon.cola.archetype.source.web.domain.teaching.entities.SchoolClass;
import top.egon.cola.archetype.source.web.domain.teaching.vos.GradeCode;
import top.egon.cola.archetype.source.web.domain.teaching.vos.SchoolClassId;
import top.egon.cola.archetype.source.web.domain.user.vos.UserId;

import java.util.Optional;

public interface SchoolClassDomainService {
    SchoolClass create( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull SchoolClassId schoolClassId, String name, @jakarta.validation.Valid @jakarta.validation.constraints.NotNull Grade grade);

    Optional<Grade> findGradeByCode( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull GradeCode code);

    Optional<SchoolClass> findByGradeIdAndId(Long gradeId, @jakarta.validation.Valid @jakarta.validation.constraints.NotNull SchoolClassId schoolClassId);

    boolean existsByGradeIdAndNameIgnoreCase(Long gradeId, String name);

    SchoolClass save( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull SchoolClass schoolClass);

    void addUser(Long gradeId, @jakarta.validation.Valid @jakarta.validation.constraints.NotNull SchoolClassId schoolClassId, @jakarta.validation.Valid @jakarta.validation.constraints.NotNull UserId userId);

    boolean hasUser(Long gradeId, @jakarta.validation.Valid @jakarta.validation.constraints.NotNull SchoolClassId schoolClassId, @jakarta.validation.Valid @jakarta.validation.constraints.NotNull UserId userId);

    SchoolClass assignUser( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull SchoolClass schoolClass, @jakarta.validation.Valid @jakarta.validation.constraints.NotNull UserId userId);
}
