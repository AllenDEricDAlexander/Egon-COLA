package top.egon.fable.infrastructure.repo.examing.jpa;

import top.egon.fable.infrastructure.repo.examing.po.ExamResultPo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamResultJpaRepository extends JpaRepository<ExamResultPo, String> {
}
