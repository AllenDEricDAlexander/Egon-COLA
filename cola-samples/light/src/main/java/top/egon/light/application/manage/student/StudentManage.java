package top.egon.light.application.manage.student;

import top.egon.light.domain.common.Page;
import top.egon.light.domain.student.model.Student;

public interface StudentManage {
    Student register(String name, String email);

    Student getById(String studentId);

    Page<Student> getPage(int currentPage, int pageSize);
}
