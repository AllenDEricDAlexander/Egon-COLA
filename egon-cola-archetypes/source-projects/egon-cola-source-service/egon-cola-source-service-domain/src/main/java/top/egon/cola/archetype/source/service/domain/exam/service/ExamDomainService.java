package top.egon.cola.archetype.source.service.domain.exam.service;

import top.egon.cola.archetype.source.service.domain.course.entities.Course;
import top.egon.cola.archetype.source.service.domain.exam.entities.Exam;
import top.egon.cola.archetype.source.service.domain.exam.entities.ExamPaper;
import top.egon.cola.archetype.source.service.domain.exam.vos.ExamId;
import java.time.Instant;
import java.util.Optional;

public interface ExamDomainService {
    Exam createExam( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull Course course, String title, Instant startsAt, Instant endsAt);
    ExamPaper attachPaper( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull Exam exam, String title, int totalPoints);
    Exam publishExam( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull Exam exam, @jakarta.validation.Valid @jakarta.validation.constraints.NotNull ExamPaper paper);
    Exam save( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull Exam exam);
    Optional<Exam> findById( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull ExamId examId);
    ExamPaper savePaper( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull ExamPaper paper);
    Optional<ExamPaper> findPaperByExamId( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull ExamId examId);
}
