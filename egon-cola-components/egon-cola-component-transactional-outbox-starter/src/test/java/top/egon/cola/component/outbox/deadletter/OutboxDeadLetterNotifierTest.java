package top.egon.cola.component.outbox.deadletter;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.outbox.event.OutboxDeadLetterEvent;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class OutboxDeadLetterNotifierTest {

    @Test
    void shouldIsolateListenersAndExposeOnlySafeMetadata() {
        List<OutboxDeadLetterEvent> received = new ArrayList<>();
        OutboxDeadLetterNotifier notifier = new OutboxDeadLetterNotifier(List.of(
                event -> {
                    throw new IllegalStateException("listener failed");
                },
                received::add
        ));
        OutboxDeadLetterEvent event = new OutboxDeadLetterEvent(
                "message-1",
                "http",
                "orders",
                10,
                "OUTBOX_RETRY_EXHAUSTED",
                "HTTP_503",
                "trace-1"
        );

        assertThatCode(() -> notifier.notifyDead(event)).doesNotThrowAnyException();

        assertThat(received).containsExactly(event);
        assertThat(OutboxDeadLetterEvent.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .containsExactly(
                        "messageId",
                        "channel",
                        "destination",
                        "attempt",
                        "errorCode",
                        "errorMessage",
                        "traceId"
                );
    }
}
