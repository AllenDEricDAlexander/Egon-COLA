package ${package}.infrastructure.repo.student.impl;

import ${package}.domain.student.model.Student;
import ${package}.domain.student.repos.StudentRepository;
import ${package}.infrastructure.repo.student.converter.StudentPoConverter;
import ${package}.infrastructure.repo.student.jpa.StudentCourseJpaRepository;
import ${package}.infrastructure.repo.student.jpa.StudentJpaRepository;
import ${package}.infrastructure.repo.student.po.StudentCoursePo;
import ${package}.infrastructure.repo.student.po.StudentPo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository("studentRepositoryImpl")
@RequiredArgsConstructor
public class StudentRepositoryImpl implements StudentRepository {
    @Qualifier("studentJpaRepository")
    private final StudentJpaRepository studentJpaRepository;

    @Qualifier("studentCourseJpaRepository")
    private final StudentCourseJpaRepository studentCourseJpaRepository;

    @Qualifier("studentPoConverter")
    private final StudentPoConverter studentPoConverter;

    @Override
    public Student save(Student student) {
        StudentPo saved = studentJpaRepository.save(studentPoConverter.toPo(student));
        student.getCourseIds().forEach(courseId -> {
            if (!studentCourseJpaRepository.existsByStudentIdAndCourseId(student.getId(), courseId)) {
                studentCourseJpaRepository.save(new StudentCoursePo(student.getId(), courseId, LocalDateTime.now()));
            }
        });
        return studentPoConverter.toDomain(saved, studentCourseJpaRepository.findByStudentId(student.getId()));
    }

    @Override
    public Optional<Student> findById(String studentId) {
        return studentJpaRepository.findById(studentId)
                .map(studentPo -> studentPoConverter.toDomain(studentPo, studentCourseJpaRepository.findByStudentId(studentId)));
    }

    @Override
    public boolean existsByEmail(String email) {
        return studentJpaRepository.existsByEmail(email);
    }
}
