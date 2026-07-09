package ${package}.infrastructure.teaching.service.impl;

import ${package}.domain.teaching.aggregates.SchoolClassAggregate;
import ${package}.domain.teaching.entities.Course;
import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.teaching.enums.SchoolClassStatus;
import ${package}.domain.teaching.service.SchoolClassDomainService;
import ${package}.domain.teaching.vos.CourseSchedule;
import ${package}.domain.teaching.vos.SchoolClassId;
import ${package}.domain.teaching.vos.Semester;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("schoolClassDomainService")
public class SchoolClassDomainServiceImpl implements SchoolClassDomainService {
    @Override
    public SchoolClass createSchoolClass(String name, Semester semester) {
        return new SchoolClass(
                new SchoolClassId(nextId()), name, semester, SchoolClassStatus.ACTIVE);
    }

    @Override
    public SchoolClassAggregate schedule(
            SchoolClassAggregate schoolClass, Course course, CourseSchedule schedule) {
        schoolClass.schedule(course, schedule);
        return schoolClass;
    }

    private String nextId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
