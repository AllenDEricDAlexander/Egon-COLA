package top.egon.cola.component.gateway.admin.observability.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionService;
import top.egon.cola.component.gateway.admin.observability.repository.GatewayObservabilityRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatewayObservabilityQueryServiceTest {

    @Test
    void mergesRegistryAndEngineProjectionCountsIntoDashboard() {
        GatewayObservabilityRepository store =
                mock(GatewayObservabilityRepository.class);
        GatewayProjectionService projections =
                mock(GatewayProjectionService.class);
        when(store.dashboard(eq("test"), eq("gateway"), any()))
                .thenReturn(summary("AVAILABLE"));
        when(projections.scopeCounts(
                "test-biz", "orders", "test", "gateway"
        ))
                .thenReturn(new top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionCounts(
                        2,
                        3,
                        1,
                        4,
                        1,
                        false
                ));
        GatewayObservabilityQueryService service =
                new GatewayObservabilityQueryService(
                        store,
                        Clock.fixed(
                                Instant.parse("2026-07-25T08:00:00Z"),
                                ZoneOffset.UTC
                        ),
                        projections
                );

        top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO result =
                service.dashboard(
                        "test-biz", "orders", "test", "gateway"
                );

        assertThat(result.readyEngines()).isEqualTo(2);
        assertThat(result.totalEngines()).isEqualTo(3);
        assertThat(result.inconsistentGroups()).isEqualTo(1);
        assertThat(result.activeProviders()).isEqualTo(4);
        assertThat(result.abnormalProviders()).isEqualTo(1);
        assertThat(result.observabilityState()).isEqualTo("AVAILABLE");
    }

    @Test
    void keepsDatabaseSummaryWhenProjectionIsUnavailable() {
        GatewayObservabilityRepository store =
                mock(GatewayObservabilityRepository.class);
        GatewayProjectionService projections =
                mock(GatewayProjectionService.class);
        when(store.dashboard(eq("test"), eq("gateway"), any()))
                .thenReturn(summary("NO_DATA"));
        when(projections.scopeCounts(
                "test-biz", "orders", "test", "gateway"
        ))
                .thenThrow(new IllegalStateException("DDC unavailable"));
        GatewayObservabilityQueryService service =
                new GatewayObservabilityQueryService(
                        store,
                        Clock.systemUTC(),
                        projections
                );

        assertThat(service.dashboard(
                "test-biz", "orders", "test", "gateway"
        )
                .observabilityState()).isEqualTo(
                        "PROJECTION_UNAVAILABLE"
        );
    }

    private top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO summary(String state) {
        return new top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO(
                1,
                0,
                0,
                0,
                0,
                0,
                1D,
                List.of(),
                List.of(),
                state
        );
    }
}
