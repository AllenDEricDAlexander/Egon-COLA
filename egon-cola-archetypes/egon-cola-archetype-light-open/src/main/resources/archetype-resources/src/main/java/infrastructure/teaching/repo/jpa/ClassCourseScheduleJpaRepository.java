package ${package}.infrastructure.teaching.repo.jpa;

import ${package}.infrastructure.teaching.repo.po.ClassCourseSchedulePO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassCourseScheduleJpaRepository
        extends JpaRepository<ClassCourseSchedulePO, String> {
    List<ClassCourseSchedulePO> findBySchoolClassIdOrderByStartsAt(String schoolClassId);
}
