package ${package}.infrastructure.repo.teaching.jpa;

import ${package}.infrastructure.repo.teaching.po.GradePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("gradeJpaRepository")
public interface GradeJpaRepository extends JpaRepository<GradePO, String> {
}
