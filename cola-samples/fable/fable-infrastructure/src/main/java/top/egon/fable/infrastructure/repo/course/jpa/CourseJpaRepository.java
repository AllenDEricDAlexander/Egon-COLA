package top.egon.fable.infrastructure.repo.course.jpa;

import top.egon.fable.infrastructure.repo.course.po.CoursePo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseJpaRepository extends JpaRepository<CoursePo, String> {

    boolean existsByName(String name);
}
