package ${package}.domain.student.repos;

import ${package}.domain.common.Page;
import ${package}.domain.student.model.Student;

import java.util.Optional;

public interface StudentRepository {
    Student save(Student student);

    Optional<Student> findById(String studentId);

    Page<Student> findPage(int currentPage, int pageSize);

    boolean existsByEmail(String email);
}
