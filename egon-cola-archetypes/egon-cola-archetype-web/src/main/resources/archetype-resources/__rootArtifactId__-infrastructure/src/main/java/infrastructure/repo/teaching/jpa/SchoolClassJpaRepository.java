package ${package}.infrastructure.repo.teaching.jpa;

import ${package}.infrastructure.repo.teaching.po.SchoolClassPo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolClassJpaRepository extends JpaRepository<SchoolClassPo, String> {
}
