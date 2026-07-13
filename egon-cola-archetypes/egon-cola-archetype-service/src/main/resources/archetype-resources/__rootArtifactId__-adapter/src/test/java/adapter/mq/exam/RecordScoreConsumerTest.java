#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.mq.exam;

import ${package}.adapter.dto.exam.RecordScoreMessage;
import ${package}.application.exam.command.RecordScoreCommand;
import ${package}.application.exam.manage.ScoreManage;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RecordScoreConsumerTest {

    @Test
    void shouldDelegateRecordScoreCommand() {
        ScoreManage scoreManage = mock(ScoreManage.class);
        RecordScoreConsumer consumer = new RecordScoreConsumer(scoreManage);
        RecordScoreMessage message = new RecordScoreMessage(
                "message-1", "exam-1", "student-1", 92, Instant.EPOCH);

        consumer.consume(message);

        verify(scoreManage).record(new RecordScoreCommand("exam-1", "student-1", 92));
    }
}
