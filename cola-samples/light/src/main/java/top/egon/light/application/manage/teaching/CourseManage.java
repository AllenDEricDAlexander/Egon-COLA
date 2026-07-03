package top.egon.light.application.manage.teaching;

import top.egon.light.domain.teaching.model.Course;

public interface CourseManage {
    Course create(String name, String description);

    Course getById(String courseId);

    void assignCourse(String studentId, String courseId);
}
