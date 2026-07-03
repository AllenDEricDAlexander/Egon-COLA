package top.egon.light.infrastructure.repo.student.impl;

import top.egon.light.domain.common.Page;
import top.egon.light.domain.student.model.Student;
import top.egon.light.domain.student.repos.StudentRepository;
import top.egon.light.infrastructure.repo.student.converter.StudentPoConverter;
import top.egon.light.infrastructure.repo.student.jpa.StudentCourseJpaRepository;
import top.egon.light.infrastructure.repo.student.jpa.StudentJpaRepository;
import top.egon.light.infrastructure.repo.student.po.StudentCoursePo;
import top.egon.light.infrastructure.repo.student.po.StudentPo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    public Page<Student> findPage(int currentPage, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(currentPage, 1) - 1, pageSize);
        org.springframework.data.domain.Page<StudentPo> page = studentJpaRepository.findAll(pageable);
        return Page.of(
                page.getContent().stream()
                        .map(studentPo -> studentPoConverter.toDomain(
                                studentPo,
                                studentCourseJpaRepository.findByStudentId(studentPo.getId())))
                        .toList(),
                currentPage,
                page.getTotalPages(),
                pageSize,
                page.getTotalElements());
    }

    @Override
    public boolean existsByEmail(String email) {
        return studentJpaRepository.existsByEmail(email);
    }
}
