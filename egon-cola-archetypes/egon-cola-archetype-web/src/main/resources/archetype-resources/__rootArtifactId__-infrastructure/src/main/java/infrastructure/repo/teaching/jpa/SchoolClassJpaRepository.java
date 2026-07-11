package ${package}.infrastructure.repo.teaching.jpa;

import ${package}.infrastructure.repo.teaching.po.SchoolClassPO;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolClassJpaRepository extends JpaRepository<SchoolClassPO, String> {
    boolean existsByGradeIdAndNameIgnoreCase(String gradeId, String name);
}
