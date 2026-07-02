package top.egon.light.domain.student.repos;

import top.egon.light.domain.student.model.Student;

import java.util.Optional;

public interface StudentRepository {
    Student save(Student student);
    Optional<Student> findById(String studentId);
    boolean existsByEmail(String email);
}
