package top.egon.fable.application.manage.course;

import top.egon.fable.domain.common.Page;
import top.egon.fable.domain.entities.course.Course;

public interface CourseManage {

    Course create(String name, int credit);

    Course getById(String courseId);

    Page<Course> getPage(int currentPage, int pageSize);
}
