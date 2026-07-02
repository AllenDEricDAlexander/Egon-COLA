package top.egon.light.infrastructure.repo.teaching.jpa;

import top.egon.light.infrastructure.repo.teaching.po.CoursePo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseJpaRepository extends JpaRepository<CoursePo, String> {
}
