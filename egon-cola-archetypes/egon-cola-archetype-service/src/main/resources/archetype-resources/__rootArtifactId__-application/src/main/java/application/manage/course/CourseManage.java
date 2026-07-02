#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.manage.course;

import ${package}.application.view.course.CourseView;

public interface CourseManage {

    CourseView create(String name, int credit);

    CourseView getById(String courseId);
}
