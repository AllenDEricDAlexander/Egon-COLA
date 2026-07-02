package ${package}.infrastructure.repo.student.jpa;

import ${package}.infrastructure.repo.student.po.StudentCoursePo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentCourseJpaRepository extends JpaRepository<StudentCoursePo, Long> {
    List<StudentCoursePo> findByStudentId(String studentId);

    boolean existsByStudentIdAndCourseId(String studentId, String courseId);
}
