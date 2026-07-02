package ${package}.infrastructure.repo.student.impl;

import ${package}.domain.student.model.Student;
import ${package}.domain.student.repos.StudentRepository;
import ${package}.infrastructure.repo.student.converter.StudentPoConverter;
import ${package}.infrastructure.repo.student.jpa.StudentCourseJpaRepository;
import ${package}.infrastructure.repo.student.jpa.StudentJpaRepository;
import ${package}.infrastructure.repo.student.po.StudentCoursePo;
import ${package}.infrastructure.repo.student.po.StudentPo;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class StudentRepositoryImpl implements StudentRepository {
    private final StudentJpaRepository studentJpaRepository;
    private final StudentCourseJpaRepository studentCourseJpaRepository;

    public StudentRepositoryImpl(StudentJpaRepository studentJpaRepository,
                                 StudentCourseJpaRepository studentCourseJpaRepository) {
        this.studentJpaRepository = studentJpaRepository;
        this.studentCourseJpaRepository = studentCourseJpaRepository;
    }

    @Override
    public Student save(Student student) {
        StudentPo saved = studentJpaRepository.save(StudentPoConverter.toPo(student));
        student.getCourseIds().forEach(courseId -> {
            if (!studentCourseJpaRepository.existsByStudentIdAndCourseId(student.getId(), courseId)) {
                studentCourseJpaRepository.save(new StudentCoursePo(student.getId(), courseId, LocalDateTime.now()));
            }
        });
        return StudentPoConverter.toDomain(saved, studentCourseJpaRepository.findByStudentId(student.getId()));
    }

    @Override
    public Optional<Student> findById(String studentId) {
        return studentJpaRepository.findById(studentId)
                .map(studentPo -> StudentPoConverter.toDomain(studentPo, studentCourseJpaRepository.findByStudentId(studentId)));
    }

    @Override
    public boolean existsByEmail(String email) {
        return studentJpaRepository.existsByEmail(email);
    }
}
