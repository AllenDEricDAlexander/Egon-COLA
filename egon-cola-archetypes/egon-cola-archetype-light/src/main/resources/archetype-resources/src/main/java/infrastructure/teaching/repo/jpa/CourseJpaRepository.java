package ${package}.infrastructure.teaching.repo.jpa;

import ${package}.infrastructure.teaching.repo.po.CoursePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("teachingCourseJpaRepository")
public interface CourseJpaRepository extends JpaRepository<CoursePO, String> {
    Optional<CoursePO> findByCourseCode(String courseCode);
}
