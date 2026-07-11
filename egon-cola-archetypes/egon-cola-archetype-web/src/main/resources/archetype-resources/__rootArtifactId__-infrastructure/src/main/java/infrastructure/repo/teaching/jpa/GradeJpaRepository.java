package ${package}.infrastructure.repo.teaching.jpa;

import ${package}.infrastructure.repo.teaching.po.GradePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("gradeJpaRepository")
public interface GradeJpaRepository extends JpaRepository<GradePO, String> {
    Optional<GradePO> findByCode(String code);
    boolean existsByCode(String code);
}
