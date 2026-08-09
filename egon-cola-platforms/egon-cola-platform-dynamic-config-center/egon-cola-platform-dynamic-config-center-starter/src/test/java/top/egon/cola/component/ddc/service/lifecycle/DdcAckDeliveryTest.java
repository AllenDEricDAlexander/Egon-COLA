package top.egon.cola.component.ddc.service.lifecycle;

import top.egon.cola.component.ddc.autoconfigure.properties.DdcAckDeliveryProperties;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import top.egon.cola.component.ddc.api.client.DdcConfigClient;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.ddc.model.config.DdcAckRequest;
import top.egon.cola.component.ddc.model.config.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.config.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.config.DdcConfigValue;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DdcAckDeliveryTest {

    @Test
    void retriesTransportAndServerFailuresUntilDeliverySucceeds() {
        DdcConfigClient client = mock(DdcConfigClient.class);
        DdcAckRequest request = request("change-1");
        doThrow(new ResourceAccessException("connection reset"))
                .doThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE))
                .doNothing()
                .when(client).ack(request);

        try (DdcAckDelivery delivery = delivery(client, 8, 4)) {
            delivery.start();

            assertThat(delivery.submit(request)).isTrue();

            verify(client, timeout(2000).times(3)).ack(request);
            await(() -> delivery.deliveredCount() == 1);
            assertThat(delivery.retryCount()).isEqualTo(2);
            assertThat(delivery.exhaustedCount()).isZero();
            assertThat(delivery.pendingCount()).isZero();
        }
    }

    @Test
    void doesNotRetryClientFailure() {
        DdcConfigClient client = mock(DdcConfigClient.class);
        DdcAckRequest request = request("change-2");
        doThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST))
                .when(client).ack(request);

        try (DdcAckDelivery delivery = delivery(client, 8, 4)) {
            delivery.start();

            assertThat(delivery.submit(request)).isTrue();

            verify(client, timeout(1000).times(1)).ack(request);
            await(() -> delivery.nonRetryableFailureCount() == 1);
            assertThat(delivery.retryCount()).isZero();
            assertThat(delivery.pendingCount()).isZero();
        }
    }

    @Test
    void deduplicatesPendingAckByChangeInstanceAndLease() throws Exception {
        DdcConfigClient client = mock(DdcConfigClient.class);
        DdcAckRequest request = request("change-3");
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
            entered.countDown();
            release.await(1, TimeUnit.SECONDS);
            return null;
        }).when(client).ack(any());

        try (DdcAckDelivery delivery = delivery(client, 8, 3)) {
            delivery.start();
            assertThat(delivery.submit(request)).isTrue();
            assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();

            assertThat(delivery.submit(request("change-3"))).isTrue();
            assertThat(delivery.pendingCount()).isEqualTo(1);
            assertThat(delivery.deduplicatedCount()).isEqualTo(1);

            release.countDown();
            verify(client, timeout(1000).times(1)).ack(any());
        }
    }

    @Test
    void rejectsWhenBoundedQueueIsSaturated() throws Exception {
        DdcConfigClient client = mock(DdcConfigClient.class);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
            entered.countDown();
            release.await(1, TimeUnit.SECONDS);
            return null;
        }).when(client).ack(any());

        try (DdcAckDelivery delivery = delivery(client, 1, 3)) {
            delivery.start();
            assertThat(delivery.submit(request("change-4"))).isTrue();
            assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();

            assertThat(delivery.submit(request("change-5"))).isFalse();
            assertThat(delivery.saturationCount()).isEqualTo(1);
            assertThat(delivery.pendingCount()).isEqualTo(1);

            release.countDown();
        }
    }

    @Test
    void recordsFinalExhaustionAndStopsItsWorker() {
        DdcConfigClient client = mock(DdcConfigClient.class);
        doThrow(new ResourceAccessException("timeout"))
                .when(client).ack(any());
        DdcAckDelivery delivery = delivery(client, 8, 2);
        delivery.start();

        assertThat(delivery.submit(request("change-6"))).isTrue();

        verify(client, timeout(2000).times(2)).ack(any());
        await(() -> delivery.exhaustedCount() == 1);
        assertThat(delivery.pendingCount()).isZero();

        delivery.stop();
        assertThat(delivery.isRunning()).isFalse();
        assertThat(delivery.submit(request("change-7"))).isFalse();
        assertThat(delivery.isWorkerTerminated()).isTrue();
    }

    @Test
    void capturesSubmitterTraceAndDoesNotLeakWorkerTrace() throws Exception {
        CountDownLatch delivered = new CountDownLatch(2);
        List<String> traces = java.util.Collections.synchronizedList(new ArrayList<>());
        DdcConfigClient client = new NoOpAdminClient() {
            @Override
            public void ack(DdcAckRequest request) {
                traces.add(TraceContext.getTraceId());
                TraceContext.setTraceId("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
                delivered.countDown();
            }
        };
        TraceContext parent = TraceContext.root("request-1");

        try (DdcAckDelivery delivery = delivery(client, 8, 1)) {
            delivery.start();
            try (TraceContext.Scope ignored = parent.open()) {
                assertThat(delivery.submit(request("change-8"))).isTrue();
            }
            TraceContext.clearOwnedKeys();
            assertThat(delivery.submit(request("change-9"))).isTrue();

            assertThat(delivered.await(2, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(traces).hasSize(2);
        assertThat(traces.get(0)).isEqualTo(parent.traceId());
        assertThat(TraceContext.normalizeTraceId(traces.get(1))).isNotNull();
        assertThat(traces.get(1)).isNotEqualTo("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
    }

    private DdcAckDelivery delivery(DdcConfigClient client,
                                    int queueCapacity,
                                    int maxAttempts) {
        DdcAckDeliveryProperties properties = new DdcAckDeliveryProperties();
        properties.setQueueCapacity(queueCapacity);
        properties.setMaxAttempts(maxAttempts);
        properties.setInitialBackoff(Duration.ofMillis(5));
        properties.setMaxBackoff(Duration.ofMillis(20));
        properties.setJitter(0.0);
        properties.setShutdownWait(Duration.ofSeconds(1));
        return new DdcAckDelivery(client, properties);
    }

    private DdcAckRequest request(String changeId) {
        DdcAckRequest request = new DdcAckRequest();
        request.setChangeId(changeId);
        request.setInstanceId("instance-1");
        request.setLeaseId("lease-1");
        request.setTargetVersion(2L);
        request.setResourceChecksum("checksum");
        return request;
    }

    private void await(java.util.function.BooleanSupplier condition) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertThat(condition.getAsBoolean()).isTrue();
    }

    private static class NoOpAdminClient implements DdcConfigClient {

        @Override
        public DdcLeaseSession register(DdcInstanceRegisterRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcLeaseOperationResult heartbeat(DdcHeartbeatRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcLeaseOperationResult offline(DdcHeartbeatRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<DdcConfigValue> pull() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void ack(DdcAckRequest request) {
            throw new UnsupportedOperationException();
        }
    }
}
