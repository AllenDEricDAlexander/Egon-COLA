package top.egon.light.domain.student.repos;

import top.egon.light.domain.common.Page;
import top.egon.light.domain.student.model.Student;

import java.util.Optional;

public interface StudentRepository {
    Student save(Student student);

    Optional<Student> findById(String studentId);

    Page<Student> findPage(int currentPage, int pageSize);

    boolean existsByEmail(String email);
}
