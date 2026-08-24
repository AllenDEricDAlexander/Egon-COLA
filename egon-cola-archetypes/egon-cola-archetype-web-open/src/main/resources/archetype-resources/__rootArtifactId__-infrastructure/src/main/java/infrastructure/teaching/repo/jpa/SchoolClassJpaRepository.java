package ${package}.infrastructure.teaching.repo.jpa;

import ${package}.infrastructure.teaching.repo.po.SchoolClassPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchoolClassJpaRepository extends JpaRepository<SchoolClassPO, Long> {
    Optional<SchoolClassPO> findByGradeIdAndId(Long gradeId, Long id);

    long countByGradeIdAndNameIgnoreCase(Long gradeId, String name);
}
