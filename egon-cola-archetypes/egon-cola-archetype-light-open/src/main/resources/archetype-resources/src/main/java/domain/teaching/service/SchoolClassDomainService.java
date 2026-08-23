package ${package}.domain.teaching.service;

import ${package}.domain.teaching.aggregates.SchoolClassAggregate;
import ${package}.domain.teaching.entities.Course;
import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.teaching.vos.CourseSchedule;
import ${package}.domain.teaching.vos.Semester;

public interface SchoolClassDomainService {
    SchoolClass createSchoolClass(String name, Semester semester);

    SchoolClassAggregate schedule(
            SchoolClassAggregate schoolClass, Course course, CourseSchedule schedule);
}
