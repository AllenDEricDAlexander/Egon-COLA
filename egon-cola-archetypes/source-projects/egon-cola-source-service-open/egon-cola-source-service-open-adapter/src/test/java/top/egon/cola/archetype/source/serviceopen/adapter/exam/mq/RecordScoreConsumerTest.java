package top.egon.cola.archetype.source.serviceopen.adapter.exam.mq;

import top.egon.cola.archetype.source.serviceopen.adapter.exam.dto.RecordScoreMessage;
import top.egon.cola.archetype.source.serviceopen.application.exam.command.RecordScoreCommand;
import top.egon.cola.archetype.source.serviceopen.application.exam.manage.ScoreManage;
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
                "message-1", 4001L, 6001L, 92, Instant.EPOCH);

        consumer.consume(message);

        verify(scoreManage).record(new RecordScoreCommand(4001L, 6001L, 92));
    }
}
