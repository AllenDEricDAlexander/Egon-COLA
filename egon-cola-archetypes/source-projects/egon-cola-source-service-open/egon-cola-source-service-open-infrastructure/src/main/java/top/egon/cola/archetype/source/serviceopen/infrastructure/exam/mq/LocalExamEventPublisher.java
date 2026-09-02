package top.egon.cola.archetype.source.serviceopen.infrastructure.exam.mq;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.Exam;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.ExamPaper;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.Score;
import top.egon.cola.archetype.source.serviceopen.domain.exam.event.ExamEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
@Component
@ConditionalOnProperty(prefix = "app.integrations.rabbitmq", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LocalExamEventPublisher implements ExamEventPublisher {
    public void examPublished(Exam exam, ExamPaper paper) { }
    public void scoreRecorded(Score score) { }
}
