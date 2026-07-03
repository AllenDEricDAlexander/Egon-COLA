package top.egon.fable.infrastructure.repo.examing.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import top.egon.fable.domain.entities.examing.ExamResult;
import top.egon.fable.domain.repos.examing.ExamResultRepository;
import top.egon.fable.infrastructure.repo.examing.converter.ExamResultConverter;
import top.egon.fable.infrastructure.repo.examing.jpa.ExamResultJpaRepository;
import top.egon.fable.infrastructure.repo.examing.po.ExamResultPo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository("examResultRepositoryImpl")
@RequiredArgsConstructor
public class ExamResultRepositoryImpl implements ExamResultRepository {

    @Qualifier("examResultJpaRepository")
    private final ExamResultJpaRepository examResultJpaRepository;

    @Qualifier("examResultConverter")
    private final ExamResultConverter examResultConverter;

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
