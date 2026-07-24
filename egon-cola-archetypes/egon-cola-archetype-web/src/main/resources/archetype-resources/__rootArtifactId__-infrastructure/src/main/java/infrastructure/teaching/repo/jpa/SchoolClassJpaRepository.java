package ${package}.infrastructure.teaching.repo.jpa;

import ${package}.infrastructure.teaching.repo.po.SchoolClassPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchoolClassJpaRepository extends JpaRepository<SchoolClassPO, String> {
    Optional<SchoolClassPO> findByGradeIdAndId(String gradeId, String id);

    boolean existsByGradeIdAndNameIgnoreCase(String gradeId, String name);
}
