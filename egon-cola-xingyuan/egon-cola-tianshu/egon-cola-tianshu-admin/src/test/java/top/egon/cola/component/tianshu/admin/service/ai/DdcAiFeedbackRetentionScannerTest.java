package top.egon.cola.component.tianshu.admin.service.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.tianshu.admin.service.ai.job.DdcAiFeedbackRetentionScanner;
import top.egon.cola.component.tianshu.admin.service.ai.job.DdcAiTenantJobRunner;
import top.egon.cola.component.tianshu.admin.service.ai.validation.DdcAiSystemActor;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcAiFeedbackRetentionScannerTest {

    private final DdcAiFeedbackService feedbackService = mock(DdcAiFeedbackService.class);
    private final DdcAiTenantJobRunner runner = mock(DdcAiTenantJobRunner.class);
    private final DdcAiFeedbackRetentionScanner scanner = new DdcAiFeedbackRetentionScanner(feedbackService, runner);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void runTheTenantBodyInline() {
        when(runner.forEachTenant(eq(DdcAiSystemActor.RETENTION), anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> ((Supplier<Integer>) invocation.getArgument(2)).get());
    }

    @Test
    void purgesInBatchesUntilBelowLimit() {
        when(feedbackService.purgeExpired(1000)).thenReturn(1000, 1000, 7);

        assertThat(scanner.scanRetention()).isEqualTo(2007);

        verify(feedbackService, times(3)).purgeExpired(1000);
    }

    @Test
    void stopsAfterTheRoundCapEvenWhenEveryBatchIsFull() {
        when(feedbackService.purgeExpired(1000)).thenReturn(1000);

        assertThat(scanner.scanRetention()).isEqualTo(20_000);

        verify(feedbackService, times(20)).purgeExpired(1000);
    }
}
