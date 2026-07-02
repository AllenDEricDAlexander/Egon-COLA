package top.egon.light.domain.teaching.service;

import top.egon.light.domain.teaching.model.Course;

public class CourseDomainService {
    public Course create(String courseId, String name, String description) {
        return Course.create(courseId, name, description);
    }
}
