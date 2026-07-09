package ${package}.domain.teaching.service;

import ${package}.domain.teaching.entities.Course;
import ${package}.domain.teaching.vos.CourseCode;

public interface CourseDomainService {
    Course createCourse(CourseCode code, String name);
}
