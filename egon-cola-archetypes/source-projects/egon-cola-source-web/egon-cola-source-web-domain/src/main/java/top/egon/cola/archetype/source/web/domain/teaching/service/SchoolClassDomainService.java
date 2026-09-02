package top.egon.cola.archetype.source.web.domain.teaching.service;

import top.egon.cola.archetype.source.web.domain.teaching.entities.Grade;
import top.egon.cola.archetype.source.web.domain.teaching.entities.SchoolClass;
import top.egon.cola.archetype.source.web.domain.teaching.vos.GradeCode;
import top.egon.cola.archetype.source.web.domain.teaching.vos.SchoolClassId;
import top.egon.cola.archetype.source.web.domain.user.vos.UserId;
import top.egon.cola.component.common.mybatis.extension.EgonColaIService;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.util.Optional;

public interface SchoolClassDomainService<P extends EgonModel<P>> extends EgonColaIService<P> {
    SchoolClass create(SchoolClassId schoolClassId, String name, Grade grade);

    Optional<Grade> findGradeByCode(GradeCode code);

    Optional<SchoolClass> findByGradeIdAndId(Long gradeId, SchoolClassId schoolClassId);

    boolean existsByGradeIdAndNameIgnoreCase(Long gradeId, String name);

    SchoolClass save(SchoolClass schoolClass);

    void addUser(Long gradeId, SchoolClassId schoolClassId, UserId userId);

    boolean hasUser(Long gradeId, SchoolClassId schoolClassId, UserId userId);

    SchoolClass assignUser(SchoolClass schoolClass, UserId userId);
}
