package ${package}.domain.teaching.service;

import ${package}.domain.teaching.model.Course;
import ${package}.domain.teaching.enums.CourseStatus;
import ${package}.domain.teaching.vos.CourseCode;
import ${package}.common.utils.IdUtils;

public class CourseDomainService {
    public Course create(String courseId, String name, String description) {
        return Course.create(courseId, name, description);
    }

    public ${package}.domain.teaching.entities.Course createCourse(CourseCode code, String name) {
        return new ${package}.domain.teaching.entities.Course(
                IdUtils.nextId(), code, name, CourseStatus.ACTIVE);
    }
}
