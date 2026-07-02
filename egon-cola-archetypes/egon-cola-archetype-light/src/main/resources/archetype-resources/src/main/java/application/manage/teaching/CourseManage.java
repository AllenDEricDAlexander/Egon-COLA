package ${package}.application.manage.teaching;

public interface CourseManage {
    CourseView create(String name, String description);

    CourseView getById(String courseId);

    void assignCourse(String studentId, String courseId);
}
