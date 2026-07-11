#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.service.exam.impl;

import ${package}.domain.entities.exam.Exam;
import ${package}.domain.entities.exam.ExamPaper;
import ${package}.domain.entities.exam.Score;
import ${package}.domain.enums.exam.ScoreStatus;
import ${package}.domain.service.exam.ScoreDomainService;
import ${package}.domain.validators.exam.ScoreDomainValidator;
import ${package}.domain.vos.exam.ScoreValue;

public final class ScoreDomainServiceImpl implements ScoreDomainService {

    private final ScoreDomainValidator validator = new ScoreDomainValidator();

    @Override
    public Score recordScore(
            String id,
            Exam exam,
            ExamPaper paper,
            String studentId,
            int points,
            boolean duplicate) {
        validator.validate(exam, paper, studentId, points, duplicate);
        return new Score(
                id, exam.getId(), exam.getCourseId(), studentId,
                new ScoreValue(points), ScoreStatus.RECORDED);
    }
}
