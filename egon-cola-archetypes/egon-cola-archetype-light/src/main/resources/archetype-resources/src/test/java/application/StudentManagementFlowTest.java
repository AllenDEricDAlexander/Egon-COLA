#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.application;

import ${package}.application.manage.student.StudentManage;
import ${package}.application.manage.student.StudentView;
import ${package}.application.manage.teaching.CourseManage;
import ${package}.application.manage.teaching.CourseView;
import ${package}.start.StudentManagementApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = StudentManagementApplication.class)
class StudentManagementFlowTest {
    @Autowired
    private StudentManage studentManage;

    @Autowired
    private CourseManage courseManage;

    @Test
    void register_student_and_assign_course() {
        StudentView student = studentManage.register("Mario", "mario@example.com");
        CourseView course = courseManage.create("Architecture", "Large monolith light domain architecture");

        courseManage.assignCourse(student.id(), course.id());

        StudentView saved = studentManage.getById(student.id());
        assertThat(saved.email()).isEqualTo("mario@example.com");
        assertThat(saved.courseIds()).containsExactly(course.id());
    }
}
