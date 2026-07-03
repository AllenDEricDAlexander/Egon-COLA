package ${package}.application.manage.teaching;

import ${package}.domain.teaching.model.Course;

public interface CourseManage {
    Course create(String name, String description);

    Course getById(String courseId);

    void assignCourse(String studentId, String courseId);
}
