#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.mq;
import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.entities.ExamPaper;
import ${package}.domain.exam.entities.Score;
import ${package}.domain.exam.event.ExamEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
@Component
@ConditionalOnProperty(prefix = "app.integrations.rabbitmq", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LocalExamEventPublisher implements ExamEventPublisher {
    public void examPublished(Exam exam, ExamPaper paper) { }
    public void scoreRecorded(Score score) { }
}
