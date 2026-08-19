package top.egon.cola.component.outbox.dispatch;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;
import top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxProperties;
import top.egon.cola.component.outbox.cleanup.OutboxCleanupJob;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

class OutboxPollerTest {

    @Test
    void shouldSchedulePollingAndEnabledCleanupAndCancelBothOnStop() {
        OutboxDispatcher dispatcher = mock(OutboxDispatcher.class);
        OutboxCleanupJob cleanupJob = mock(OutboxCleanupJob.class);
        TaskScheduler scheduler = mock(TaskScheduler.class);
        @SuppressWarnings("unchecked")
        ScheduledFuture<Object> pollFuture = mock(ScheduledFuture.class);
        @SuppressWarnings("unchecked")
        ScheduledFuture<Object> cleanupFuture = mock(ScheduledFuture.class);
        TransactionalOutboxProperties properties = new TransactionalOutboxProperties();
        properties.getCleanup().setEnabled(true);
        doReturn(pollFuture).when(scheduler).scheduleWithFixedDelay(
                org.mockito.ArgumentMatchers.any(Runnable.class),
                org.mockito.ArgumentMatchers.eq(properties.getPolling().getFixedDelay())
        );
        doReturn(cleanupFuture).when(scheduler).scheduleWithFixedDelay(
                org.mockito.ArgumentMatchers.any(Runnable.class),
                org.mockito.ArgumentMatchers.eq(properties.getCleanup().getFixedDelay())
        );
        OutboxPoller poller = new OutboxPoller(dispatcher, cleanupJob, scheduler, properties);

        poller.start();

        assertThat(poller.isRunning()).isTrue();
        poller.stop();
        assertThat(poller.isRunning()).isFalse();
        verify(pollFuture).cancel(false);
        verify(cleanupFuture).cancel(false);
    }

    @Test
    void shouldNotRunCapturedPollTaskAfterStopOrScheduleDisabledCleanup() {
        OutboxDispatcher dispatcher = mock(OutboxDispatcher.class);
        OutboxCleanupJob cleanupJob = mock(OutboxCleanupJob.class);
        TaskScheduler scheduler = mock(TaskScheduler.class);
        @SuppressWarnings("unchecked")
        ScheduledFuture<Object> pollFuture = mock(ScheduledFuture.class);
        TransactionalOutboxProperties properties = new TransactionalOutboxProperties();
        properties.getCleanup().setEnabled(false);
        ArgumentCaptor<Runnable> pollTask = ArgumentCaptor.forClass(Runnable.class);
        doReturn(pollFuture).when(scheduler).scheduleWithFixedDelay(
                pollTask.capture(),
                org.mockito.ArgumentMatchers.any(Duration.class)
        );
        OutboxPoller poller = new OutboxPoller(dispatcher, cleanupJob, scheduler, properties);

        poller.start();
        poller.stop();
        pollTask.getValue().run();

        verify(dispatcher, never()).submitDue();
        verify(cleanupJob, never()).runOnce();
    }
}
