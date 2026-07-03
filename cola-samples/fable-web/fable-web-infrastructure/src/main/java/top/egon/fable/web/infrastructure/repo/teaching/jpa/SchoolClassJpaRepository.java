package top.egon.fable.web.infrastructure.repo.teaching.jpa;

import top.egon.fable.web.infrastructure.repo.teaching.po.SchoolClassPo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolClassJpaRepository extends JpaRepository<SchoolClassPo, String> {
}
