package top.egon.fable.domain.repos.examing;

import java.util.Optional;

import top.egon.fable.domain.entities.examing.ExamResult;

public interface ExamResultRepository {

    ExamResult save(ExamResult examResult);

    Optional<ExamResult> findById(String examResultId);
}
