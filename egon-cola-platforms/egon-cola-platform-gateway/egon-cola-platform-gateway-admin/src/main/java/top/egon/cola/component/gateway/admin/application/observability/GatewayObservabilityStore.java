package top.egon.cola.component.gateway.admin.application.observability;

import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;

import java.time.Instant;
import java.util.List;

public interface GatewayObservabilityStore {

    boolean project(GatewayCallEventV1 event, Instant expiresAt);

    void recordFailure(ConsumeFailure failure);

    Page<TraceSummary> traces(TraceQuery query);

    DashboardSummary dashboard(String env, String namespace, Instant since);

    Page<AuditSummary> audits(AuditQuery query);

    int deleteExpired(Instant now);

    record ConsumeFailure(
            String id,
            String topic,
            int partition,
            long offset,
            String eventId,
            String failureCode,
            String failureMessage,
            String payloadSha256,
            int payloadSize,
            Instant occurredAt
    ) {
    }

    record TraceQuery(
            String env,
            String namespace,
            String traceId,
            String protocol,
            String statusCategory,
            int page,
            int size
    ) {

        public TraceQuery {
            if (page < 1 || size < 1 || size > 200) {
                throw new IllegalArgumentException(
                        "invalid trace page request"
                );
            }
        }
    }

    record AuditQuery(
            String env,
            String namespace,
            String actorId,
            String resourceId,
            String traceId,
            Boolean successful,
            int page,
            int size
    ) {

        public AuditQuery {
            if (page < 1 || size < 1 || size > 200) {
                throw new IllegalArgumentException(
                        "invalid audit page request"
                );
            }
        }
    }

    record TraceSummary(
            String eventId,
            String traceId,
            Instant startedAt,
            long durationMs,
            String protocol,
            String gatewayGroupId,
            String operationKey,
            String statusCategory,
            String engineInstanceId,
            String providerService
    ) {
    }

    record AuditSummary(
            String id,
            String actorId,
            String actorType,
            String source,
            String traceId,
            String resourceType,
            String resourceId,
            String action,
            Object beforeSummary,
            Object afterSummary,
            Long draftRevision,
            String releaseId,
            boolean successful,
            String errorCode,
            Instant occurredAt
    ) {
    }

    record RequestPoint(
            Instant time,
            long requests,
            long errors,
            long p50,
            long p95,
            long p99
    ) {
    }

    record ProtocolCall(String protocol, long value) {
    }

    record DashboardSummary(
            long gatewayGroups,
            long readyEngines,
            long totalEngines,
            long inconsistentGroups,
            long activeProviders,
            long abnormalProviders,
            double releaseSuccessRate,
            List<RequestPoint> requestSeries,
            List<ProtocolCall> protocolCalls,
            String observabilityState
    ) {

        public DashboardSummary {
            requestSeries = List.copyOf(requestSeries);
            protocolCalls = List.copyOf(protocolCalls);
        }
    }

    record Page<T>(List<T> items, int page, int size, long total) {

        public Page {
            items = List.copyOf(items);
        }
    }
}
