package top.egon.fable.application.manage.course;

import top.egon.fable.application.view.course.CourseView;

public interface CourseManage {

    CourseView create(String name, int credit);

    CourseView getById(String courseId);
}
