package ${package}.infrastructure.teaching.repo.jpa;

import ${package}.infrastructure.teaching.repo.po.SchoolClassPO;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolClassJpaRepository extends JpaRepository<SchoolClassPO, String> {
}
