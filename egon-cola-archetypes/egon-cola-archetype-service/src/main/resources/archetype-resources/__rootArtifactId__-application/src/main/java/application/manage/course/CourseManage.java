#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.manage.course;

import ${package}.domain.entities.course.Course;

public interface CourseManage {

    Course create(String name, int credit);

    Course getById(String courseId);
}
