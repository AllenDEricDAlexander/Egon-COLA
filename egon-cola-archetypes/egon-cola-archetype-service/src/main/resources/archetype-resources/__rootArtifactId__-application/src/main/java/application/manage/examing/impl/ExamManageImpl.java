#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.manage.examing.impl;

import ${package}.application.manage.examing.ExamManage;
import ${package}.application.view.examing.ExamResultView;
import ${package}.common.constants.ErrorCodes;
import ${package}.common.exception.NotFoundException;
import ${package}.common.util.IdGenerator;
import ${package}.domain.entities.examing.ExamResult;
import ${package}.domain.repos.course.CourseRepository;
import ${package}.domain.repos.examing.ExamResultRepository;
import ${package}.domain.service.examing.ExamDomainService;
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
