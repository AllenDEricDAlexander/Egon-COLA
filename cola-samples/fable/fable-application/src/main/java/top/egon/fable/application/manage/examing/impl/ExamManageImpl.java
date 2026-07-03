package top.egon.fable.application.manage.examing.impl;

import top.egon.fable.application.manage.examing.ExamManage;
import top.egon.fable.common.constants.ErrorCodes;
import top.egon.fable.common.exception.NotFoundException;
import top.egon.fable.common.util.IdGenerator;
import top.egon.fable.domain.entities.examing.ExamResult;
import top.egon.fable.domain.repos.examing.ExamResultRepository;
import top.egon.fable.domain.service.examing.ExamDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("examManage")
@RequiredArgsConstructor
public class ExamManageImpl implements ExamManage {

    @Qualifier("examResultRepositoryImpl")
    private final ExamResultRepository examResultRepository;

    @Qualifier("examDomainService")
    private final ExamDomainService examDomainService;

    @Override
    @Transactional
    public ExamResult record(String courseId, String studentId, int score) {
        examDomainService.record(courseId, studentId, score);
        ExamResult examResult = ExamResult.record(IdGenerator.nextId(), courseId, studentId, score);
        return examResultRepository.save(examResult);
    }

    @Override
    @Transactional(readOnly = true)
    public ExamResult getById(String examResultId) {
        return examResultRepository.findById(examResultId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.EXAM_RESULT_NOT_FOUND, "exam result not found"));
    }
}
