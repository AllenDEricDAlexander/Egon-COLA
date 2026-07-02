package top.egon.fable.application.manage.examing.impl;

import top.egon.fable.application.manage.examing.ExamManage;
import top.egon.fable.application.view.examing.ExamResultView;
import top.egon.fable.common.constants.ErrorCodes;
import top.egon.fable.common.exception.NotFoundException;
import top.egon.fable.common.util.IdGenerator;
import top.egon.fable.domain.entities.examing.ExamResult;
import top.egon.fable.domain.repos.course.CourseRepository;
import top.egon.fable.domain.repos.examing.ExamResultRepository;
import top.egon.fable.domain.service.examing.ExamDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExamManageImpl implements ExamManage {

    private final ExamResultRepository examResultRepository;

    private final ExamDomainService examDomainService;

    public ExamManageImpl(ExamResultRepository examResultRepository, CourseRepository courseRepository) {
        this.examResultRepository = examResultRepository;
        this.examDomainService = new ExamDomainService(courseRepository);
    }

    @Override
    @Transactional
    public ExamResultView record(String courseId, String studentId, int score) {
        examDomainService.record(courseId, studentId, score);
        ExamResult examResult = ExamResult.record(IdGenerator.nextId(), courseId, studentId, score);
        return toView(examResultRepository.save(examResult));
    }

    @Override
    public ExamResultView getById(String examResultId) {
        return examResultRepository.findById(examResultId)
                .map(this::toView)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.EXAM_RESULT_NOT_FOUND, "exam result not found"));
    }

    private ExamResultView toView(ExamResult examResult) {
        return new ExamResultView(
                examResult.getId(),
                examResult.getCourseId(),
                examResult.getStudentId(),
                examResult.getScore(),
                examResult.getStatus().name());
    }
}
