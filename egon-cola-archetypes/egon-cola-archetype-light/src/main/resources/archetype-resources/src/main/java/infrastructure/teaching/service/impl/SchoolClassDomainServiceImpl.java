package ${package}.infrastructure.teaching.service.impl;

import ${package}.domain.teaching.aggregates.SchoolClassAggregate;
import ${package}.domain.teaching.entities.Course;
import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.teaching.enums.SchoolClassStatus;
import ${package}.domain.teaching.service.SchoolClassDomainService;
import ${package}.domain.teaching.vos.CourseSchedule;
import ${package}.domain.teaching.vos.SchoolClassId;
import ${package}.domain.teaching.vos.Semester;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.egon.cola.component.common.id.generator.IdGenerator;

@Service("schoolClassDomainService")
@RequiredArgsConstructor
public class SchoolClassDomainServiceImpl implements SchoolClassDomainService {
    private final IdGenerator idGenerator;

    @Override
    public SchoolClass createSchoolClass(String name, Semester semester) {
        return new SchoolClass(
                new SchoolClassId(idGenerator.nextId()),
                name,
                semester,
                SchoolClassStatus.ACTIVE);
    }

    @Override
    public SchoolClassAggregate schedule(
            SchoolClassAggregate schoolClass, Course course, CourseSchedule schedule) {
        schoolClass.schedule(course, schedule);
        return schoolClass;
    }

}
