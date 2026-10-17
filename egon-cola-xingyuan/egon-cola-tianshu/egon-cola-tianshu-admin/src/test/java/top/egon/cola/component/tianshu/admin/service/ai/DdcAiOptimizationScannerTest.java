package top.egon.cola.component.tianshu.admin.service.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantBindingDTO;
import top.egon.cola.component.tianshu.admin.service.ai.job.DdcAiOptimizationScanner;
import top.egon.cola.component.tianshu.admin.service.ai.job.DdcAiTenantJobRunner;
import top.egon.cola.component.tianshu.admin.service.ai.validation.DdcAiSystemActor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcAiOptimizationScannerTest {

    private static final Instant NOW = Instant.parse("2026-10-17T06:00:00Z");

    private final DdcAiOptimizationService optimizationService = mock(DdcAiOptimizationService.class);
    private final DdcAiTenantJobRunner runner = mock(DdcAiTenantJobRunner.class);
    private final DdcTenantBindingDTO tenant = binding(42L, true);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void runInlineAsTenant() {
        when(runner.bindings()).thenReturn(List.of(tenant));
        when(runner.inTenant(any(DdcTenantBindingDTO.class), eq(DdcAiSystemActor.OPTIMIZER), any(Supplier.class)))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(2)).get());
    }

    @Test
    void scanReclaimsEvaluatesThenClaimsAndExecutesOnExecutor() {
        when(optimizationService.evaluateTenant(NOW)).thenReturn(List.of(11L, 12L));
        when(optimizationService.pendingRunIds(anyInt())).thenReturn(List.of(12L, 13L));
        when(optimizationService.claimRun(11L, "admin-1", NOW)).thenReturn(true);
        when(optimizationService.claimRun(12L, "admin-1", NOW)).thenReturn(false);
        when(optimizationService.claimRun(13L, "admin-1", NOW)).thenReturn(true);

        assertThat(scanner(4, new InlineExecutor()).scanOptimization()).isEqualTo(2);

        InOrder order = inOrder(optimizationService);
        order.verify(optimizationService).reclaimExpiredRuns(NOW);
        order.verify(optimizationService).evaluateTenant(NOW);
        order.verify(optimizationService).pendingRunIds(50);
        order.verify(optimizationService).claimRun(11L, "admin-1", NOW);
        order.verify(optimizationService).executeRun(11L);
        verify(optimizationService, never()).executeRun(12L);
        verify(optimizationService).executeRun(13L);
        verify(optimizationService, times(1)).claimRun(12L, "admin-1", NOW);
    }

    @Test
    void claimFailureSkipsExecutionAndExecutionFailureIsContained() {
        when(optimizationService.evaluateTenant(NOW)).thenReturn(List.of(11L, 12L));
        when(optimizationService.claimRun(11L, "admin-1", NOW)).thenThrow(new IllegalStateException("db"));
        when(optimizationService.claimRun(12L, "admin-1", NOW)).thenReturn(true);
        doThrow(new IllegalStateException("model")).when(optimizationService).executeRun(12L);

        assertThat(scanner(4, new InlineExecutor()).scanOptimization()).isEqualTo(1);

        verify(optimizationService, never()).executeRun(11L);
    }

    @Test
    void saturatedSlotsLeaveRemainingRunsForTheNextScan() {
        when(optimizationService.evaluateTenant(NOW)).thenReturn(List.of(11L, 12L, 13L));
        when(optimizationService.claimRun(anyLong(), anyString(), any())).thenReturn(true);
        HoldingExecutor executor = new HoldingExecutor();

        assertThat(scanner(2, executor).scanOptimization()).isEqualTo(2);

        verify(optimizationService, never()).claimRun(eq(13L), anyString(), any());
        assertThat(executor.held).hasSize(2);
    }

    @Test
    void rejectedSubmissionReleasesTheSlotAndStopsTheTenantScan() {
        when(optimizationService.evaluateTenant(NOW)).thenReturn(List.of(11L, 12L));
        when(optimizationService.claimRun(anyLong(), anyString(), any())).thenReturn(true);

        assertThat(scanner(1, new RejectingExecutor()).scanOptimization()).isZero();

        verify(optimizationService, never()).claimRun(eq(12L), anyString(), any());
    }

    @Test
    void tenantBindingChangeIsLoggedAndOtherTenantsContinue() {
        DdcTenantBindingDTO changed = binding(43L, true);
        when(runner.bindings()).thenReturn(List.of(changed, binding(44L, false), tenant));
        when(runner.inTenant(eq(changed), anyString(), any()))
                .thenThrow(new IllegalStateException("TENANT_BINDING_CHANGED_DURING_SCAN"));
        when(optimizationService.evaluateTenant(NOW)).thenReturn(List.of(11L));
        when(optimizationService.claimRun(11L, "admin-1", NOW)).thenReturn(true);

        assertThat(scanner(4, new InlineExecutor()).scanOptimization()).isEqualTo(1);

        verify(optimizationService).executeRun(11L);
    }

    private DdcAiOptimizationScanner scanner(int slots, ExecutorService executor) {
        return new DdcAiOptimizationScanner(optimizationService, executor, runner, slots, "admin-1",
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static DdcTenantBindingDTO binding(long tenantId, boolean enabled) {
        DdcTenantBindingDTO binding = new DdcTenantBindingDTO();
        binding.setTenantId(tenantId);
        binding.setIdentityTenantId("idp-" + tenantId);
        binding.setEnabled(enabled);
        return binding;
    }

    private static class InlineExecutor extends AbstractExecutorService {
        @Override public void execute(Runnable command) { command.run(); }
        @Override public void shutdown() { }
        @Override public List<Runnable> shutdownNow() { return List.of(); }
        @Override public boolean isShutdown() { return false; }
        @Override public boolean isTerminated() { return false; }
        @Override public boolean awaitTermination(long timeout, TimeUnit unit) { return true; }
    }

    private static final class HoldingExecutor extends InlineExecutor {
        private final java.util.List<Runnable> held = new java.util.ArrayList<>();
        @Override public void execute(Runnable command) { held.add(command); }
    }

    private static final class RejectingExecutor extends InlineExecutor {
        @Override public void execute(Runnable command) { throw new RejectedExecutionException("full"); }
    }
}
