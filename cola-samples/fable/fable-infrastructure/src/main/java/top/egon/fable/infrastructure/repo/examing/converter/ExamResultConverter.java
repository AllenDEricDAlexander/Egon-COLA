package top.egon.fable.infrastructure.repo.examing.converter;

import java.time.LocalDateTime;

import top.egon.fable.domain.entities.examing.ExamResult;
import top.egon.fable.domain.enums.ExamResultStatus;
import top.egon.fable.infrastructure.repo.examing.po.ExamResultPo;
import org.springframework.stereotype.Component;

@Component
public class ExamResultConverter {

    public ExamResultPo toPo(ExamResult examResult, LocalDateTime createdAt, LocalDateTime updatedAt) {
        ExamResultPo examResultPo = new ExamResultPo();
        examResultPo.setId(examResult.getId());
        examResultPo.setCourseId(examResult.getCourseId());
        examResultPo.setStudentId(examResult.getStudentId());
        examResultPo.setScore(examResult.getScore());
        examResultPo.setStatus(examResult.getStatus().name());
        examResultPo.setCreatedAt(createdAt);
        examResultPo.setUpdatedAt(updatedAt);
        return examResultPo;
    }

    public ExamResult toDomain(ExamResultPo examResultPo) {
        ExamResult examResult = new ExamResult();
        examResult.setId(examResultPo.getId());
        examResult.setCourseId(examResultPo.getCourseId());
        examResult.setStudentId(examResultPo.getStudentId());
        examResult.setScore(examResultPo.getScore());
        examResult.setStatus(ExamResultStatus.valueOf(examResultPo.getStatus()));
        return examResult;
    }
}
