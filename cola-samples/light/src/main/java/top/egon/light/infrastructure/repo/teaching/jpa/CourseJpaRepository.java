package top.egon.light.infrastructure.repo.teaching.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.light.infrastructure.repo.teaching.po.CoursePo;

public interface CourseJpaRepository extends JpaRepository<CoursePo, String> {
}
