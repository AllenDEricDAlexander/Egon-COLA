package ${package}.application.manage.student;

import ${package}.domain.common.Page;
import ${package}.domain.student.model.Student;

public interface StudentManage {
    Student register(String name, String email);

    Student getById(String studentId);

    Page<Student> getPage(int currentPage, int pageSize);
}
