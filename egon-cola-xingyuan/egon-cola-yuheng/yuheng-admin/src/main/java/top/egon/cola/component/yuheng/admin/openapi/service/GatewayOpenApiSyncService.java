package top.egon.cola.component.yuheng.admin.openapi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.tianshu.api.client.DdcManagementClient;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceCatalog;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceInstance;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceKey;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceQuery;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceSnapshot;
import top.egon.cola.component.yuheng.admin.application.domain.po.GatewayApplicationPO;
import top.egon.cola.component.yuheng.admin.application.repository.GatewayApplicationRepository;
import top.egon.cola.component.yuheng.admin.config.properties.GatewayAdminOpenApiProperties;
import top.egon.cola.component.yuheng.admin.openapi.client.GatewayOpenApiFetchException;
import top.egon.cola.component.yuheng.admin.openapi.client.GatewayProviderOpenApiClient;
import top.egon.cola.component.yuheng.admin.openapi.converter.GatewayOpenApi31ContractAdapter;
import top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiAggregateDTO;
import top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiDocumentDTO;
import top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiSyncCandidateDTO;
import top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiSyncKeyDTO;
import top.egon.cola.component.yuheng.admin.openapi.domain.enums.GatewayOpenApiSyncStateEnum;
import top.egon.cola.component.yuheng.admin.openapi.domain.po.GatewayOpenApiSnapshotPO;
import top.egon.cola.component.yuheng.admin.openapi.domain.po.GatewayOpenApiSyncPO;
import top.egon.cola.component.yuheng.admin.openapi.repository.GatewayOpenApiSnapshotRepository;
import top.egon.cola.component.yuheng.admin.openapi.repository.GatewayOpenApiSyncRepository;
import top.egon.cola.component.yuheng.admin.openapi.validation.GatewayOpenApiValidationChain;
import top.egon.cola.component.yuheng.admin.openapi.validation.GatewayOpenApiValidationResult;
import top.egon.cola.component.yuheng.contract.reporting.GatewayInterfaceDefinitionReport;
import top.egon.cola.component.yuheng.contract.reporting.GatewayInterfaceDefinitionReportResult;
import top.egon.cola.component.yuheng.contract.reporting.openapi.GatewayOpenApiGroupManifestDTO;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Durable OpenAPI synchronization application service.
 *
 * <p>中文：该 Facade 只编排 DDC 发现、revision CAS、网络外快照、完整
 * Group 聚合和恢复；HTTP 安全校验、OpenAPI 规则以及 Definition 写入继续
 * 由各自的技术边界负责。任何一个 Group 失败都不会提交半套 Definition。</p>
 */
@Slf4j
@Validated
@Service("gatewayOpenApiSyncService")
@ConditionalOnBean(DdcManagementClient.class)
@ConditionalOnProperty(
        name = "gateway.admin.openapi.enabled",
        havingValue = "true"
)
public class GatewayOpenApiSyncService {

    private static final String OPENAPI_SOURCE = "OPENAPI31";

    private static final String OPENAPI_ENABLED = "true";

    private final DdcManagementClient ddc;

    private final GatewayApplicationRepository applications;

    private final GatewayOpenApiSyncRepository syncStates;

    private final GatewayOpenApiSnapshotRepository snapshots;

    private final GatewayProviderOpenApiClient client;

    private final GatewayOpenApiValidationChain validation;

    private final GatewayOpenApiAggregateCoordinator coordinator;

    private final GatewayAdminOpenApiProperties properties;

    private final ObjectMapper objectMapper;

    private final MeterRegistry meters;

    private final Clock clock;

    /** Creates the Spring-managed synchronization service. */
    @Autowired
    public GatewayOpenApiSyncService(
            DdcManagementClient ddc,
            GatewayApplicationRepository applications,
            GatewayOpenApiSyncRepository syncStates,
            GatewayOpenApiSnapshotRepository snapshots,
            GatewayProviderOpenApiClient client,
            GatewayOpenApiValidationChain validation,
            GatewayOpenApiAggregateCoordinator coordinator,
            GatewayAdminOpenApiProperties properties,
            @Qualifier("gatewayOpenApiObjectMapper")
            ObjectMapper objectMapper,
            MeterRegistry meters) {
        this(
                ddc,
                applications,
                syncStates,
                snapshots,
                client,
                validation,
                coordinator,
                properties,
                objectMapper,
                meters,
                Clock.systemUTC()
        );
    }

    /** Constructor used by deterministic focused tests. */
    public GatewayOpenApiSyncService(
            DdcManagementClient ddc,
            GatewayApplicationRepository applications,
            GatewayOpenApiSyncRepository syncStates,
            GatewayOpenApiSnapshotRepository snapshots,
            GatewayProviderOpenApiClient client,
            GatewayOpenApiValidationChain validation,
            GatewayOpenApiAggregateCoordinator coordinator,
            GatewayAdminOpenApiProperties properties,
            MeterRegistry meters,
            Clock clock) {
        this(
                ddc,
                applications,
                syncStates,
                snapshots,
                client,
                validation,
                coordinator,
                properties,
                new ObjectMapper(),
                meters,
                clock
        );
    }

    private GatewayOpenApiSyncService(
            DdcManagementClient ddc,
            GatewayApplicationRepository applications,
            GatewayOpenApiSyncRepository syncStates,
            GatewayOpenApiSnapshotRepository snapshots,
            GatewayProviderOpenApiClient client,
            GatewayOpenApiValidationChain validation,
            GatewayOpenApiAggregateCoordinator coordinator,
            GatewayAdminOpenApiProperties properties,
            ObjectMapper objectMapper,
            MeterRegistry meters,
            Clock clock) {
        this.ddc = Objects.requireNonNull(ddc, "ddc");
        this.applications = Objects.requireNonNull(
                applications,
                "applications"
        );
        this.syncStates = Objects.requireNonNull(syncStates, "syncStates");
        this.snapshots = Objects.requireNonNull(snapshots, "snapshots");
        this.client = Objects.requireNonNull(client, "client");
        this.validation = Objects.requireNonNull(validation, "validation");
        this.coordinator = Objects.requireNonNull(
                coordinator,
                "coordinator"
        );
        this.properties = Objects.requireNonNull(properties, "properties");
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
        this.meters = Objects.requireNonNull(meters, "meters");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Runs one bounded reconciliation tick.
     *
     * @return number of rows claimed for work; the scheduler does not expose
     * a public response
     */
    public int reconcile() {
        properties.validate();
        Instant now = clock.instant();
        recoverExpiredClaims(now);
        Discovery discovery = discover(now);
        if (discovery.aborted()) {
            count("SKIPPED", "DDC_STALE");
            return 0;
        }

        Map<BuildKey, List<ClaimedWork>> workByBuild = new LinkedHashMap<>();
        int claimed = 0;
        for (GatewayOpenApiSyncPO state
                : syncStates.findDue(now, properties.getBatchSize())) {
            GroupContext context = discovery.contexts().get(
                    new GroupKey(
                            state.applicationId(),
                            state.buildId(),
                            state.openapiGroup()
                    )
            );
            if (context == null) {
                // A stale/missing manifest is not safe evidence for a network
                // call. It is handled on a later fresh DDC observation.
                continue;
            }
            Optional<ClaimedWork> work;
            try {
                work = process(
                        state,
                        context.candidates(state.openapiGroup()),
                        now
                );
            } catch (RuntimeException failure) {
                log.warn(
                        "OpenAPI synchronization candidate failed safely "
                                + "app={} build={} group={}",
                        state.applicationId(),
                        state.buildId(),
                        state.openapiGroup()
                );
                count("FAILED", "CANDIDATE_EXCEPTION");
                work = Optional.empty();
            }
            if (work.isPresent()) {
                claimed++;
                workByBuild.computeIfAbsent(
                        new BuildKey(state.applicationId(), state.buildId()),
                        ignored -> new ArrayList<>()
                ).add(work.get());
            }
        }

        workByBuild.forEach((key, work) -> completeAggregate(
                key,
                work,
                discovery.contextsByBuild().get(key),
                now
        ));
        repairLinkedRows(now);
        return claimed;
    }

    /**
     * Synchronizes one trusted candidate. This entry point is useful for a
     * deterministic/manual invocation and uses a one-Group completeness
     * context; scheduled reconciliation supplies the complete manifest.
     *
     * @param candidate DDC-derived candidate
     */
    public void synchronize(GatewayOpenApiSyncCandidateDTO candidate) {
        Objects.requireNonNull(candidate, "candidate");
        properties.validate();
        Instant now = clock.instant();
        GatewayOpenApiSyncKeyDTO key = new GatewayOpenApiSyncKeyDTO(
                candidate.applicationId(),
                candidate.buildId(),
                candidate.openapiGroup()
        );
        Optional<GatewayOpenApiSyncPO> state = syncStates.findByKey(key);
        if (state.isEmpty()) {
            return;
        }
        GroupContext context = application(candidate.applicationId())
                .map(app -> GroupContext.single(app, candidate))
                .orElse(null);
        if (context == null) {
            count("SKIPPED", "APPLICATION_NOT_FOUND");
            return;
        }
        Optional<ClaimedWork> work = process(
                state.get(),
                List.of(candidate),
                now
        );
        work.ifPresent(value -> completeAggregate(
                new BuildKey(candidate.applicationId(), candidate.buildId()),
                List.of(value),
                context,
                now
        ));
        repairLinkedRows(now);
    }

    private Discovery discover(Instant now) {
        Map<GroupKey, GroupContext> contexts = new LinkedHashMap<>();
        Map<BuildKey, GroupContext> byBuild = new LinkedHashMap<>();
        boolean aborted = false;
        for (GatewayApplicationPO application
                : applications.findAllByDeletedFalseOrderByCreatedAtDesc()) {
            boolean applicationAborted = false;
            DdcManagementServiceCatalog catalog;
            try {
                catalog = ddc.getServiceKeys(new DdcManagementServiceQuery(
                        application.getBizCode(),
                        application.getNamespace(),
                        application.getEnv(),
                        application.getApplicationCode(),
                        "HTTP_PROVIDER",
                        properties.isAllowDevelopmentHttp()
                                ? null
                                : "https",
                        null,
                        null,
                        null
                ));
            } catch (RuntimeException unavailable) {
                log.warn(
                        "OpenAPI DDC discovery unavailable app={}",
                        application.getId()
                );
                aborted = true;
                applicationAborted = true;
                continue;
            }
            if (catalog == null || !fresh(catalog.observedAt(), now)) {
                log.warn(
                        "OpenAPI DDC discovery is stale app={}",
                        application.getId()
                );
                aborted = true;
                applicationAborted = true;
                continue;
            }
            List<Observation> observations = new ArrayList<>();
            for (DdcManagementServiceKey service
                    : catalog.services() == null
                    ? List.<DdcManagementServiceKey>of()
                    : catalog.services()) {
                DdcManagementServiceSnapshot snapshot;
                try {
                    snapshot = ddc.getInstances(new DdcManagementServiceQuery(
                            service.bizCode(),
                            application.getNamespace(),
                            service.env(),
                            service.appCode(),
                            service.serviceKind(),
                            service.protocol(),
                            service.serviceName(),
                            service.group(),
                            service.version()
                    ));
                } catch (RuntimeException unavailable) {
                    log.warn(
                            "OpenAPI DDC instance observation unavailable "
                                    + "app={} service={}",
                            application.getId(),
                            service.serviceName()
                    );
                    aborted = true;
                    applicationAborted = true;
                    continue;
                }
                if (snapshot == null || !fresh(snapshot.observedAt(), now)) {
                    aborted = true;
                    applicationAborted = true;
                    continue;
                }
                for (DdcManagementServiceInstance instance
                        : snapshot.instances() == null
                        ? List.<DdcManagementServiceInstance>of()
                        : snapshot.instances()) {
                    Optional<GatewayOpenApiGroupManifestDTO> manifest =
                            manifest(instance);
                    if (manifest.isEmpty()) {
                        continue;
                    }
                    List<GatewayOpenApiSyncCandidateDTO> candidates =
                            candidates(
                                    application,
                                    snapshot,
                                    instance,
                                    manifest.get()
                            );
                    if (!candidates.isEmpty()) {
                        observations.add(new Observation(
                                service,
                                snapshot,
                                manifest.get(),
                                candidates
                        ));
                    }
                }
            }
            if (applicationAborted) {
                continue;
            }
            buildContexts(application, observations, contexts, byBuild, now);
        }
        return new Discovery(aborted, contexts, byBuild);
    }

    private void buildContexts(
            GatewayApplicationPO application,
            List<Observation> observations,
            Map<GroupKey, GroupContext> contexts,
            Map<BuildKey, GroupContext> byBuild,
            Instant now) {
        Map<BuildKey, List<Observation>> grouped = observations.stream()
                .collect(Collectors.groupingBy(
                        value -> new BuildKey(
                                application.getId(),
                                value.manifest().buildId()
                        ),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        Set<GroupKey> discovered = new HashSet<>();
        grouped.forEach((build, values) -> {
            Map<String, List<Observation>> manifests = values.stream()
                    .collect(Collectors.groupingBy(
                            value -> manifestSignature(value.manifest()),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));
            if (manifests.size() != 1) {
                markManifestDrift(application.getId(), build.buildId(), now);
                count("INCONSISTENT_BUILD", "MANIFEST_DRIFT");
                return;
            }
            GatewayOpenApiGroupManifestDTO manifest = values.getFirst()
                    .manifest();
            GroupContext context = new GroupContext(
                    application,
                    manifest,
                    values.stream()
                            .flatMap(value -> value.candidates().stream())
                            .collect(Collectors.groupingBy(
                                    GatewayOpenApiSyncCandidateDTO::openapiGroup,
                                    LinkedHashMap::new,
                                    Collectors.collectingAndThen(
                                            Collectors.toList(),
                                            list -> list.stream()
                                                    .sorted(Comparator.comparing(
                                                            GatewayOpenApiSyncCandidateDTO::instanceId
                                                    ))
                                                    .toList()
                                    )
                            ))
            );
            byBuild.put(build, context);
            for (String group : manifest.groups()) {
                GroupKey key = new GroupKey(
                        application.getId(),
                        manifest.buildId(),
                        group
                );
                contexts.put(key, context);
                discovered.add(key);
                upsertDiscovered(context, group, now);
            }
        });
        markMissingRowsStale(application.getId(), discovered, now);
    }

    private Optional<ClaimedWork> process(
            GatewayOpenApiSyncPO observed,
            List<GatewayOpenApiSyncCandidateDTO> candidates,
            Instant now) {
        if (!syncStates.claim(
                observed.id(),
                observed.revision(),
                now
        )) {
            count("SKIPPED", "CLAIM_LOST");
            return Optional.empty();
        }
        long revision = observed.revision() + 1;
        GatewayOpenApiFetchException lastFailure = null;
        GatewayOpenApiDocumentDTO document = null;
        GatewayOpenApiSyncCandidateDTO selected = null;
        int attempts = Math.min(
                properties.getMaximumInstanceAttempts(),
                candidates == null ? 0 : candidates.size()
        );
        for (int index = 0; index < attempts; index++) {
            GatewayOpenApiSyncCandidateDTO candidate = candidates.get(index);
            try {
                document = client.fetch(candidate);
                selected = candidate;
                break;
            } catch (GatewayOpenApiFetchException failure) {
                lastFailure = failure;
                if (!failure.retryable()) {
                    break;
                }
            } catch (RuntimeException failure) {
                lastFailure = new GatewayOpenApiFetchException(
                        "GATEWAY_OPENAPI_FETCH_FAILED",
                        true,
                        "provider OpenAPI fetch failed"
                );
            }
        }
        if (document == null || selected == null) {
            String errorCode = lastFailure == null
                    ? "GATEWAY_OPENAPI_PROVIDER_UNAVAILABLE"
                    : lastFailure.errorCode();
            String message = lastFailure == null
                    ? "no healthy OpenAPI provider instance was available"
                    : safeMessage(lastFailure.getMessage());
            syncStates.markFailure(
                    observed.id(),
                    revision,
                    GatewayOpenApiSyncStateEnum.FETCH_FAILED,
                    errorCode,
                    message,
                    retryAt(observed, now),
                    now
            );
            count("FETCH_FAILED", errorCode);
            return Optional.empty();
        }
        if (!syncStates.transition(
                observed.id(),
                revision,
                GatewayOpenApiSyncStateEnum.FETCHING,
                GatewayOpenApiSyncStateEnum.VALIDATING,
                now
        )) {
            count("SKIPPED", "FETCH_CAS_LOST");
            return Optional.empty();
        }
        revision++;

        GatewayOpenApiValidationResult result;
        try {
            result = validation.validate(document);
        } catch (RuntimeException invalid) {
            result = GatewayOpenApiValidationResult.invalid(
                    "GATEWAY_OPENAPI_VALIDATION_FAILED",
                    "OpenAPI document validation failed"
            );
        }
        if (!result.valid()) {
            Optional<GatewayOpenApiSnapshotPO> invalidSnapshot;
            try {
                invalidSnapshot = persistSnapshot(
                        document,
                        GatewayOpenApiSyncStateEnum.INVALID.name(),
                        List.of(result.code() + ": " + safeMessage(result.message()))
                );
            } catch (RuntimeException failure) {
                transitionToIngestFailure(
                        observed.id(),
                        revision,
                        now,
                        "GATEWAY_OPENAPI_SNAPSHOT_FAILED",
                        "OpenAPI snapshot persistence failed",
                        observed
                );
                count("INGEST_FAILED", "GATEWAY_OPENAPI_SNAPSHOT_FAILED");
                return Optional.empty();
            }
            if (invalidSnapshot.isPresent()) {
                syncStates.markFailure(
                    observed.id(),
                    revision,
                    GatewayOpenApiSyncStateEnum.INVALID,
                    result.code(),
                    safeMessage(result.message()),
                    null,
                    now
                );
            }
            count("INVALID", result.code());
            return Optional.empty();
        }

        GatewayOpenApiSnapshotPO snapshot;
        try {
            snapshot = persistSnapshot(
                    document,
                    GatewayOpenApiSyncStateEnum.VALID.name(),
                    List.of()
            ).orElseThrow(() -> new IllegalStateException(
                    "OpenAPI snapshot persistence returned no row"
            ));
        } catch (RuntimeException failure) {
            transitionToIngestFailure(
                    observed.id(),
                    revision,
                    now,
                    "GATEWAY_OPENAPI_SNAPSHOT_FAILED",
                    "OpenAPI snapshot persistence failed",
                    observed
            );
            count("INGEST_FAILED", "GATEWAY_OPENAPI_SNAPSHOT_FAILED");
            return Optional.empty();
        }
        if (!syncStates.transition(
                observed.id(),
                revision,
                GatewayOpenApiSyncStateEnum.VALIDATING,
                GatewayOpenApiSyncStateEnum.INGESTING,
                now
        )) {
            count("SKIPPED", "VALIDATION_CAS_LOST");
            return Optional.empty();
        }
        revision++;
        return Optional.of(new ClaimedWork(
                observed,
                selected,
                document,
                snapshot,
                revision
        ));
    }

    private void completeAggregate(
            BuildKey build,
            List<ClaimedWork> work,
            GroupContext context,
            Instant now) {
        if (context == null || work.isEmpty()) {
            return;
        }
        Map<String, ClaimedWork> byGroup = work.stream()
                .collect(Collectors.toMap(
                        value -> value.candidate().openapiGroup(),
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        if (!byGroup.keySet().containsAll(context.manifest().groups())) {
            work.forEach(value -> markAggregateIncomplete(value, now));
            return;
        }
        Map<String, GatewayOpenApiDocumentDTO> documents = new LinkedHashMap<>();
        Map<String, String> snapshotIds = new LinkedHashMap<>();
        for (String group : context.manifest().groups()) {
            ClaimedWork value = byGroup.get(group);
            documents.put(group, value.document());
            snapshotIds.put(group, value.snapshot().id());
        }
        GatewayOpenApiAggregateDTO aggregate = GatewayOpenApiAggregateDTO.of(
                build.applicationId(),
                build.buildId(),
                context.manifest().groups(),
                documents,
                snapshotIds
        );
        GatewayInterfaceDefinitionReportResult result;
        try {
            result = coordinator.aggregateAndIngest(
                    aggregate,
                    reportApplication(context.application())
            );
        } catch (RuntimeException failure) {
            log.warn(
                    "OpenAPI aggregate ingestion failed app={} build={} "
                            + "groups={}",
                    build.applicationId(),
                    build.buildId(),
                    context.manifest().groups().size()
            );
            work.forEach(value -> syncStates.markFailure(
                    value.observed().id(),
                    value.revision(),
                    GatewayOpenApiSyncStateEnum.INGEST_FAILED,
                    "GATEWAY_OPENAPI_INGEST_FAILED",
                    "OpenAPI Definition ingestion failed",
                    retryAt(value.observed(), now),
                    now
            ));
            count("INGEST_FAILED", "GATEWAY_OPENAPI_INGEST_FAILED");
            return;
        }
        if (result == null || result.definitionSetId() == null
                || result.definitionSetId().isBlank()) {
            work.forEach(value -> syncStates.markFailure(
                    value.observed().id(),
                    value.revision(),
                    GatewayOpenApiSyncStateEnum.INGEST_FAILED,
                    "GATEWAY_OPENAPI_INGEST_RESULT_INVALID",
                    "OpenAPI Definition ingestion returned no set",
                    retryAt(value.observed(), now),
                    now
            ));
            count("INGEST_FAILED", "GATEWAY_OPENAPI_INGEST_RESULT_INVALID");
            return;
        }
        for (String group : context.manifest().groups()) {
            ClaimedWork value = byGroup.get(group);
            if (!syncStates.setValid(
                    value.observed().id(),
                    value.revision(),
                    value.snapshot().id(),
                    result.definitionSetId(),
                    now
            )) {
                count("SKIPPED", "SUCCESS_CAS_LOST");
            } else {
                count("VALID", "AGGREGATE_COMMITTED");
            }
        }
    }

    private void markAggregateIncomplete(
            ClaimedWork work,
            Instant now) {
        syncStates.markFailure(
                work.observed().id(),
                work.revision(),
                GatewayOpenApiSyncStateEnum.INGEST_FAILED,
                "GATEWAY_OPENAPI_GROUP_SET_INCOMPLETE",
                "OpenAPI Group set is incomplete; waiting for all groups",
                retryAt(work.observed(), now),
                now
        );
        count("INGEST_FAILED", "GATEWAY_OPENAPI_GROUP_SET_INCOMPLETE");
    }

    private void repairLinkedRows(Instant now) {
        for (GatewayOpenApiSyncPO row
                : syncStates.findByStatus(
                GatewayOpenApiSyncStateEnum.INGESTING
        )) {
            if (row.latestSnapshotId() == null) {
                continue;
            }
            Optional<GatewayOpenApiSnapshotPO> snapshot = snapshots.findById(
                    row.latestSnapshotId()
            );
            if (snapshot.isEmpty() || snapshot.get().definitionSetId() == null) {
                continue;
            }
            if (syncStates.setValid(
                    row.id(),
                    row.revision(),
                    snapshot.get().id(),
                    snapshot.get().definitionSetId(),
                    now
            )) {
                count("VALID", "LINKED_SET_REPAIRED");
            }
        }
    }

    /**
     * Turns abandoned network/ingestion claims into ordinary retryable
     * failures after the configured lease window. The database state machine
     * remains the sole owner of the revision transition.
     */
    private void recoverExpiredClaims(Instant now) {
        Instant cutoff = now.minus(properties.getClaimTimeout());
        for (GatewayOpenApiSyncPO row
                : syncStates.findByStatus(
                GatewayOpenApiSyncStateEnum.FETCHING
        )) {
            if (row.updatedAt().isBefore(cutoff)) {
                syncStates.markFailure(
                        row.id(),
                        row.revision(),
                        GatewayOpenApiSyncStateEnum.FETCH_FAILED,
                        "GATEWAY_OPENAPI_CLAIM_EXPIRED",
                        "OpenAPI synchronization claim expired",
                        retryAt(row, now),
                        now
                );
                count("FETCH_FAILED", "GATEWAY_OPENAPI_CLAIM_EXPIRED");
            }
        }
        for (GatewayOpenApiSyncPO row
                : syncStates.findByStatus(
                GatewayOpenApiSyncStateEnum.INGESTING
        )) {
            if (row.updatedAt().isBefore(cutoff)
                    && row.latestSnapshotId() == null) {
                syncStates.markFailure(
                        row.id(),
                        row.revision(),
                        GatewayOpenApiSyncStateEnum.INGEST_FAILED,
                        "GATEWAY_OPENAPI_CLAIM_EXPIRED",
                        "OpenAPI ingestion claim expired",
                        retryAt(row, now),
                        now
                );
                count("INGEST_FAILED", "GATEWAY_OPENAPI_CLAIM_EXPIRED");
            }
        }
    }

    private void upsertDiscovered(
            GroupContext context,
            String group,
            Instant now) {
        List<GatewayOpenApiSyncCandidateDTO> candidates = context.candidates(
                group
        );
        if (candidates.isEmpty()) {
            return;
        }
        GatewayOpenApiSyncCandidateDTO candidate = candidates.getFirst();
        GatewayOpenApiSyncKeyDTO key = new GatewayOpenApiSyncKeyDTO(
                context.application().getId(),
                context.manifest().buildId(),
                group
        );
        if (syncStates.findByKey(key).isPresent()) {
            return;
        }
        syncStates.upsertDiscovered(new GatewayOpenApiSyncPO(
                UuidV7.simpleString(),
                context.application().getId(),
                context.manifest().buildId(),
                context.manifest().artifactVersion(),
                group,
                candidate.providerServiceName(),
                candidate.providerGroup(),
                candidate.providerVersion(),
                GatewayOpenApiSyncStateEnum.DISCOVERED,
                null,
                null,
                candidate.instanceId(),
                0,
                null,
                null,
                now,
                null,
                null,
                null,
                0,
                now
        ));
    }

    private void markMissingRowsStale(
            String applicationId,
            Set<GroupKey> discovered,
            Instant now) {
        for (GatewayOpenApiSyncPO row
                : syncStates.findByApplicationId(applicationId)) {
            GroupKey key = new GroupKey(
                    row.applicationId(),
                    row.buildId(),
                    row.openapiGroup()
            );
            if (!discovered.contains(key)
                    && row.status() != GatewayOpenApiSyncStateEnum.STALE) {
                syncStates.markFailure(
                        row.id(),
                        row.revision(),
                        GatewayOpenApiSyncStateEnum.STALE,
                        "GATEWAY_OPENAPI_PROVIDER_STALE",
                        "OpenAPI provider observation is no longer healthy",
                        null,
                        now
                );
            }
        }
    }

    private void markManifestDrift(
            String applicationId,
            String buildId,
            Instant now) {
        for (GatewayOpenApiSyncPO row
                : syncStates.findByApplicationId(applicationId)) {
            if (!row.buildId().equals(buildId)
                    || row.status() == GatewayOpenApiSyncStateEnum.VALID
                    || row.status() == GatewayOpenApiSyncStateEnum.STALE) {
                continue;
            }
            if (row.status() == GatewayOpenApiSyncStateEnum.VALIDATING) {
                syncStates.markFailure(
                        row.id(),
                        row.revision(),
                        GatewayOpenApiSyncStateEnum.INCONSISTENT_BUILD,
                        "GATEWAY_OPENAPI_MANIFEST_DRIFT",
                        "same-build OpenAPI Group manifest drifted",
                        null,
                        now
                );
            }
        }
    }

    private void transitionToIngestFailure(
            String id,
            long revision,
            Instant now,
            String errorCode,
            String message,
            GatewayOpenApiSyncPO observed) {
        if (syncStates.transition(
                id,
                revision,
                GatewayOpenApiSyncStateEnum.VALIDATING,
                GatewayOpenApiSyncStateEnum.INGESTING,
                now
        )) {
            syncStates.markFailure(
                    id,
                    revision + 1,
                    GatewayOpenApiSyncStateEnum.INGEST_FAILED,
                    errorCode,
                    message,
                    retryAt(observed, now),
                    now
            );
        }
    }

    private Optional<GatewayOpenApiSnapshotPO> persistSnapshot(
            GatewayOpenApiDocumentDTO document,
            String status,
            List<String> messages) {
        try {
            Map<String, Object> json = objectMapper.convertValue(
                    document.documentJson(),
                    new TypeReference<>() {
                    }
            );
            // Snapshot lookup and operation provenance must use the same canonical contract.
            String canonicalSha = new GatewayOpenApi31ContractAdapter()
                    .canonicalSha256(document);
            JsonNode root = document.documentJson();
            int operations = GatewayOpenApi31ContractAdapter.operationCount(root);
            int schemas = root.path("components").path("schemas").isObject()
                    ? root.path("components").path("schemas").size()
                    : 0;
            GatewayOpenApiSnapshotPO snapshot = new GatewayOpenApiSnapshotPO(
                    UuidV7.simpleString(),
                    document.candidate().applicationId(),
                    null,
                    document.candidate().buildId(),
                    document.candidate().artifactVersion(),
                    document.candidate().openapiGroup(),
                    root.path("openapi").asText("3.1.0"),
                    document.documentSha256(),
                    canonicalSha,
                    json,
                    status,
                    messages,
                    operations,
                    schemas,
                    document.candidate().instanceId(),
                    document.fetchedAt(),
                    clock.instant(),
                    clock.instant()
            );
            return Optional.of(snapshots.insertOrReuse(snapshot));
        } catch (IllegalArgumentException failure) {
            throw new IllegalStateException(
                    "OpenAPI snapshot JSON cannot be serialized",
                    failure
            );
        }
    }

    private List<GatewayOpenApiSyncCandidateDTO> candidates(
            GatewayApplicationPO application,
            DdcManagementServiceSnapshot snapshot,
            DdcManagementServiceInstance instance,
            GatewayOpenApiGroupManifestDTO manifest) {
        List<GatewayOpenApiSyncCandidateDTO> result = new ArrayList<>();
        for (String group : manifest.groups()) {
            try {
                result.add(GatewayOpenApiSyncCandidateDTO.from(
                        application.getId(),
                        snapshot,
                        instance,
                        manifest,
                        group,
                        properties.isAllowDevelopmentHttp()
                ));
            } catch (RuntimeException invalid) {
                log.warn(
                        "OpenAPI candidate rejected app={} group={} instance={}",
                        application.getId(),
                        group,
                        instance.instanceId()
                );
            }
        }
        return result;
    }

    private Optional<GatewayOpenApiGroupManifestDTO> manifest(
            DdcManagementServiceInstance instance) {
        Map<String, String> metadata = instance.metadata();
        if (!OPENAPI_SOURCE.equals(metadata.get("gateway.definition-source"))
                || !OPENAPI_ENABLED.equalsIgnoreCase(
                metadata.get("gateway.openapi.enabled")
        )) {
            return Optional.empty();
        }
        String groups = metadata.get("gateway.openapi.groups");
        String pathTemplate = metadata.get("gateway.openapi.path-template");
        String spec = metadata.get("gateway.openapi.spec");
        if (!"3.1".equals(spec) || groups == null || pathTemplate == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new GatewayOpenApiGroupManifestDTO(
                    List.of(groups.split(",", -1)),
                    pathTemplate,
                    metadata.get("gateway.openapi.resource-uri"),
                    metadata.get("gateway.artifact-version"),
                    metadata.get("gateway.build-id")
            ));
        } catch (RuntimeException invalid) {
            return Optional.empty();
        }
    }

    private Optional<GatewayApplicationPO> application(String id) {
        return applications.findByIdAndDeletedFalse(id);
    }

    private GatewayInterfaceDefinitionReport.Application reportApplication(
            GatewayApplicationPO application) {
        return new GatewayInterfaceDefinitionReport.Application(
                application.getBizCode(),
                application.getApplicationCode(),
                application.getDisplayName(),
                application.getEnv(),
                application.getNamespace()
        );
    }

    private boolean fresh(Instant observedAt, Instant now) {
        return observedAt != null
                && !observedAt.isBefore(
                now.minus(properties.getDriftSampleInterval())
        );
    }

    private String manifestSignature(GatewayOpenApiGroupManifestDTO manifest) {
        return String.join(
                "|",
                String.join(",", manifest.groups()),
                manifest.pathTemplate(),
                manifest.resourceUri(),
                manifest.artifactVersion(),
                manifest.buildId()
        );
    }

    private Instant retryAt(GatewayOpenApiSyncPO row, Instant now) {
        long initial = properties.getRetryInitialDelay().toMillis();
        long maximum = properties.getRetryMaximumDelay().toMillis();
        int attempt = Math.max(1, row.attemptCount());
        long multiplier = 1L;
        for (int index = 1; index < attempt; index++) {
            if (multiplier > maximum / 2L) {
                multiplier = maximum;
                break;
            }
            multiplier *= 2L;
        }
        long base = Math.min(maximum, Math.max(1L, initial * multiplier));
        long hash = Math.floorMod(
                Objects.hash(row.id(), attempt),
                10_001
        );
        double normalized = hash / 10_000.0d;
        double factor = 1.0d
                + ((normalized * 2.0d) - 1.0d)
                * properties.getRetryJitter();
        long delay = Math.max(1L, Math.min(
                maximum,
                Math.round(base * factor)
        ));
        return now.plusMillis(delay);
    }

    private void count(String status, String reason) {
        meters.counter(
                "gateway_openapi_sync_total",
                "status",
                safeTag(status),
                "reason",
                safeTag(reason)
        ).increment();
    }

    private static String safeTag(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.length() <= 64
                ? value
                : value.substring(0, 64);
    }

    private static String safeMessage(String value) {
        if (value == null || value.isBlank()) {
            return "OpenAPI synchronization failed";
        }
        String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
        return normalized.length() <= 1024
                ? normalized
                : normalized.substring(0, 1024);
    }

    private record BuildKey(String applicationId, String buildId) {
    }

    private record GroupKey(
            String applicationId,
            String buildId,
            String group) {
    }

    private record Observation(
            DdcManagementServiceKey service,
            DdcManagementServiceSnapshot snapshot,
            GatewayOpenApiGroupManifestDTO manifest,
            List<GatewayOpenApiSyncCandidateDTO> candidates) {
    }

    private record Discovery(
            boolean aborted,
            Map<GroupKey, GroupContext> contexts,
            Map<BuildKey, GroupContext> contextsByBuild) {
    }

    private record ClaimedWork(
            GatewayOpenApiSyncPO observed,
            GatewayOpenApiSyncCandidateDTO candidate,
            GatewayOpenApiDocumentDTO document,
            GatewayOpenApiSnapshotPO snapshot,
            long revision) {
    }

    private static final class GroupContext {

        private final GatewayApplicationPO application;

        private final GatewayOpenApiGroupManifestDTO manifest;

        private final Map<String, List<GatewayOpenApiSyncCandidateDTO>> candidates;

        private GroupContext(
                GatewayApplicationPO application,
                GatewayOpenApiGroupManifestDTO manifest,
                Map<String, List<GatewayOpenApiSyncCandidateDTO>> candidates) {
            this.application = Objects.requireNonNull(application, "application");
            this.manifest = Objects.requireNonNull(manifest, "manifest");
            this.candidates = candidates.entrySet().stream().collect(
                    Collectors.toUnmodifiableMap(
                            Map.Entry::getKey,
                            entry -> List.copyOf(entry.getValue())
                    )
            );
        }

        private static GroupContext single(
                GatewayApplicationPO application,
                GatewayOpenApiSyncCandidateDTO candidate) {
            GatewayOpenApiGroupManifestDTO manifest =
                    new GatewayOpenApiGroupManifestDTO(
                            List.of(candidate.openapiGroup()),
                            candidate.pathTemplate(),
                            candidate.resourceUri().toString(),
                            candidate.artifactVersion(),
                            candidate.buildId()
                    );
            return new GroupContext(
                    application,
                    manifest,
                    Map.of(candidate.openapiGroup(), List.of(candidate))
            );
        }

        private List<GatewayOpenApiSyncCandidateDTO> candidates(String group) {
            return candidates.getOrDefault(group, List.of());
        }

        private GatewayApplicationPO application() {
            return application;
        }

        private GatewayOpenApiGroupManifestDTO manifest() {
            return manifest;
        }
    }

}
