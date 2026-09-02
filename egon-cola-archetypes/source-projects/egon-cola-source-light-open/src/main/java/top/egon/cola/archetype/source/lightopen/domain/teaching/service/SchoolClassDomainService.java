package top.egon.cola.archetype.source.lightopen.domain.teaching.service;

import top.egon.cola.archetype.source.lightopen.domain.teaching.aggregates.SchoolClassAggregate;
import top.egon.cola.archetype.source.lightopen.domain.teaching.entities.Course;
import top.egon.cola.archetype.source.lightopen.domain.teaching.entities.SchoolClass;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.CourseSchedule;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.SchoolClassId;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.Semester;
import top.egon.cola.component.common.mybatis.extension.EgonColaIService;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.util.Optional;

/** Persistence-owning school-class domain service contract. */
public interface SchoolClassDomainService<P extends EgonModel<P>> extends EgonColaIService<P> {
    SchoolClass createSchoolClass(String name, Semester semester);

    SchoolClass save(SchoolClass schoolClass);

    Optional<SchoolClassAggregate> findAggregateById(SchoolClassId schoolClassId);

    void saveAggregate(SchoolClassAggregate aggregate);

    SchoolClassAggregate schedule(
            SchoolClassAggregate schoolClass, Course course, CourseSchedule schedule);
}
