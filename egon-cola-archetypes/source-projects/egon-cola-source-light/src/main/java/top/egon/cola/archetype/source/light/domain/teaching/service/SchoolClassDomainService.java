package top.egon.cola.archetype.source.light.domain.teaching.service;

import top.egon.cola.archetype.source.light.domain.teaching.aggregates.SchoolClassAggregate;
import top.egon.cola.archetype.source.light.domain.teaching.entities.Course;
import top.egon.cola.archetype.source.light.domain.teaching.entities.SchoolClass;
import top.egon.cola.archetype.source.light.domain.teaching.vos.CourseSchedule;
import top.egon.cola.archetype.source.light.domain.teaching.vos.SchoolClassId;
import top.egon.cola.archetype.source.light.domain.teaching.vos.Semester;

import java.util.Optional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Persistence-owning school-class domain service contract. */
public interface SchoolClassDomainService {
    SchoolClass createSchoolClass(String name, @Valid @NotNull Semester semester);

    SchoolClass save(@Valid @NotNull SchoolClass schoolClass);

    Optional<SchoolClassAggregate> findAggregateById(@Valid @NotNull SchoolClassId schoolClassId);

    void saveAggregate(@Valid @NotNull SchoolClassAggregate aggregate);

    SchoolClassAggregate schedule(@Valid @NotNull SchoolClassAggregate schoolClass, @Valid @NotNull Course course, @Valid @NotNull CourseSchedule schedule);
}
