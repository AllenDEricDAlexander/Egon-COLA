package top.egon.light.infrastructure.repo.student.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.light.infrastructure.repo.student.po.StudentCoursePo;

import java.util.List;

public interface StudentCourseJpaRepository extends JpaRepository<StudentCoursePo, Long> {
    List<StudentCoursePo> findByStudentId(String studentId);

    boolean existsByStudentIdAndCourseId(String studentId, String courseId);
}
