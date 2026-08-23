#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.exam.service.impl;

import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.entities.ExamPaper;
import ${package}.domain.exam.entities.Score;
import ${package}.domain.exam.enums.ScoreStatus;
import ${package}.domain.exam.service.ScoreDomainService;
import ${package}.domain.exam.validators.ScoreDomainValidator;
import ${package}.domain.exam.vos.ScoreValue;

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
