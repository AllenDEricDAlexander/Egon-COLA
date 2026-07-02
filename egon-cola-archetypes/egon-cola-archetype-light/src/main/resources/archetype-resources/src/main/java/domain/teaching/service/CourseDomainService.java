package ${package}.domain.teaching.service;

import ${package}.domain.teaching.model.Course;

public class CourseDomainService {
    public Course create(String courseId, String name, String description) {
        return Course.create(courseId, name, description);
    }
}
