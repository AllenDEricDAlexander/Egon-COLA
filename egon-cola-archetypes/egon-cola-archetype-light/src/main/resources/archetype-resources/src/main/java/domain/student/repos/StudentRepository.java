package ${package}.domain.student.repos;

import ${package}.domain.student.model.Student;

import java.util.Optional;

public interface StudentRepository {
    Student save(Student student);
    Optional<Student> findById(String studentId);
    boolean existsByEmail(String email);
}
