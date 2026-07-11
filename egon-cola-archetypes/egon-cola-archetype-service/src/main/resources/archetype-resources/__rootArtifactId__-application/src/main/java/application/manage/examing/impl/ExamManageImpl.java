#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.manage.examing.impl;

import ${package}.application.manage.examing.ExamManage;
import ${package}.common.constants.ErrorCodes;
import ${package}.common.exception.NotFoundException;
import ${package}.common.util.IdGenerator;
import ${package}.domain.client.examing.ExamResultClient;
import ${package}.domain.entities.examing.ExamResult;
import ${package}.domain.service.examing.ExamDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service("examManage")
@Validated
@RequiredArgsConstructor
public class ExamManageImpl implements ExamManage {

    @Qualifier("examResultClientImpl")
    private final ExamResultClient examResultClient;

    @Qualifier("legacyExamDomainService")
    private final ExamDomainService examDomainService;

    @Override
    @Transactional
    public ExamResult record(String courseId, String studentId, int score) {
        examDomainService.record(courseId, studentId, score);
        ExamResult examResult = ExamResult.record(IdGenerator.nextId(), courseId, studentId, score);
        return examResultClient.save(examResult);
    }

    @Override
    @Transactional(readOnly = true)
    public ExamResult getById(String examResultId) {
        return examResultClient.findById(examResultId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.EXAM_RESULT_NOT_FOUND, "exam result not found"));
    }
}
