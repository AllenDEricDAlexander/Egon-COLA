package top.egon.fable.infrastructure.repo.examing.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import top.egon.fable.domain.entities.examing.ExamResult;
import top.egon.fable.domain.repos.examing.ExamResultRepository;
import top.egon.fable.infrastructure.repo.examing.converter.ExamResultConverter;
import top.egon.fable.infrastructure.repo.examing.jpa.ExamResultJpaRepository;
import top.egon.fable.infrastructure.repo.examing.po.ExamResultPo;
import org.springframework.stereotype.Repository;

@Repository
public class ExamResultRepositoryImpl implements ExamResultRepository {

    private final ExamResultJpaRepository examResultJpaRepository;

    private final ExamResultConverter examResultConverter;

    public ExamResultRepositoryImpl(ExamResultJpaRepository examResultJpaRepository,
            ExamResultConverter examResultConverter) {
        this.examResultJpaRepository = examResultJpaRepository;
        this.examResultConverter = examResultConverter;
    }

    @Override
    public ExamResult save(ExamResult examResult) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdAt = examResultJpaRepository.findById(examResult.getId())
                .map(ExamResultPo::getCreatedAt)
                .orElse(now);
        ExamResultPo examResultPo = examResultConverter.toPo(examResult, createdAt, now);
        return examResultConverter.toDomain(examResultJpaRepository.save(examResultPo));
    }

    @Override
    public Optional<ExamResult> findById(String examResultId) {
        return examResultJpaRepository.findById(examResultId).map(examResultConverter::toDomain);
    }
}
