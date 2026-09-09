package top.egon.cola.component.yuheng.admin.reporting.service;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.openapi.domain.po.GatewayOpenApiSnapshotPO;
import top.egon.cola.component.yuheng.admin.openapi.repository.GatewayOpenApiSnapshotRepository;
import top.egon.cola.component.yuheng.admin.reporting.domain.dto.GatewayDefinitionIngestionCommandDTO;
import top.egon.cola.component.yuheng.admin.reporting.domain.po.GatewayStoredReportPO;
import top.egon.cola.component.yuheng.admin.reporting.repository.GatewayDefinitionReportRepository;
import top.egon.cola.component.yuheng.contract.reporting.GatewayDefinitionSourceTypeEnum;
import top.egon.cola.component.yuheng.contract.reporting.GatewayInterfaceDefinitionReport;
import top.egon.cola.component.yuheng.contract.reporting.GatewayInterfaceDefinitionReportResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Shared, transport-neutral Definition Set writer for HTTP and RPC sources.
 *
 * <p>中文：该 Facade 只负责本地验证、Definition graph 写入和 OpenAPI snapshot
 * link；它不接触 OAuth、网络请求、sync revision 或 route projection。所有
 * definition/link SQL 都在同一个事务中执行，调用方随后再用 CAS 更新同步行。</p>
 */
@Slf4j
@Validated
@Service("gatewayDefinitionIngestionService")
public class GatewayDefinitionIngestionService {

    private final GatewayDefinitionReportRepository reports;

    private final GatewayOpenApiSnapshotRepository snapshots;

    private final GatewayOperationSchemaValidator schemaValidator;

    private final GatewayReportCanonicalizer canonicalizer;

    private final Clock clock;

    /** Creates the Spring-managed shared ingestion facade. */
    @Autowired
    public GatewayDefinitionIngestionService(
            GatewayDefinitionReportRepository reports,
            GatewayOpenApiSnapshotRepository snapshots,
            ObjectMapper objectMapper) {
        this(
                reports,
                snapshots,
                new GatewayOperationSchemaValidator(objectMapper),
                new GatewayReportCanonicalizer(),
                Clock.systemUTC()
        );
    }

    /** Constructor used by focused tests with a deterministic clock. */
    public GatewayDefinitionIngestionService(
            GatewayDefinitionReportRepository reports,
            GatewayOpenApiSnapshotRepository snapshots,
            GatewayOperationSchemaValidator schemaValidator,
            GatewayReportCanonicalizer canonicalizer,
            Clock clock) {
        this.reports = Objects.requireNonNull(reports, "reports");
        this.snapshots = Objects.requireNonNull(snapshots, "snapshots");
        this.schemaValidator = Objects.requireNonNull(
                schemaValidator,
                "schemaValidator"
        );
        this.canonicalizer = Objects.requireNonNull(
                canonicalizer,
                "canonicalizer"
        );
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Validates and writes one complete immutable Definition Set.
     *
     * @param command source/provenance-aware ingestion command
     * @return report-shaped acknowledgement for the caller
     */
    @Transactional
    public GatewayInterfaceDefinitionReportResult ingest(
            @Valid GatewayDefinitionIngestionCommandDTO command) {
        Objects.requireNonNull(command, "command");
        GatewayInterfaceDefinitionReport report = command.report();
        validateSourceMatrix(command);
        canonicalizer.verify(report);
        validateOperations(report);
        validateSnapshots(command);

        reports.findBuildFingerprint(
                        command.applicationId(),
                        report.build().buildId(),
                        protocol(report),
                        command.sourceScope()
                )
                .filter(fingerprint -> !fingerprint.equals(
                        report.definitionFingerprint()
                ))
                .ifPresent(conflict -> {
                    throw new IllegalStateException(
                            "YUHENG_ADMIN_IMMUTABLE_BUILD_CONFLICT: "
                                    + report.build().buildId()
                    );
                });

        Instant now = clock.instant();
        GatewayStoredReportPO stored;
        if (reports.definitionSetExists(
                command.applicationId(),
                report.definitionSetId()
        )) {
            stored = new GatewayStoredReportPO(0, 0, List.of());
        } else {
            stored = reports.ingest(
                    command.applicationId(),
                    report,
                    now
            );
        }
        if (!command.snapshotIds().isEmpty()) {
            int linked = snapshots.linkAllToDefinitionSet(
                    command.snapshotIds(),
                    report.definitionSetId()
            );
            if (linked != command.snapshotIds().size()) {
                throw new IllegalStateException(
                        "YUHENG_OPENAPI_SNAPSHOT_LINK_COUNT_MISMATCH"
                );
            }
        }
        log.info(
                "Gateway definition ingested app={} source={} scope={} set={}",
                command.applicationId(),
                command.sourceType(),
                command.sourceScope(),
                report.definitionSetId()
        );
        return result(command, stored, now);
    }

    private void validateSourceMatrix(
            GatewayDefinitionIngestionCommandDTO command) {
        GatewayDefinitionSourceTypeEnum source = command.sourceType();
        List<GatewayInterfaceDefinitionReport.InterfaceGroup> groups =
                groups(command.report());
        if (groups.isEmpty()) {
            throw new IllegalArgumentException(
                    "report must contain at least one interface group"
            );
        }
        Set<GatewayDefinitionSourceTypeEnum> sources = groups.stream()
                .map(GatewayInterfaceDefinitionReport.InterfaceGroup::sourceType)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        Set<String> protocols = groups.stream()
                .map(GatewayInterfaceDefinitionReport.InterfaceGroup::protocol)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (sources.size() != 1 || !sources.contains(source)) {
            throw new IllegalArgumentException(
                    "report sourceType does not match ingestion source"
            );
        }
        if (source == GatewayDefinitionSourceTypeEnum.OPENAPI31) {
            if (!protocols.equals(Set.of("HTTP"))) {
                throw new IllegalArgumentException(
                        "OPENAPI31 ingestion requires HTTP report groups"
                );
            }
            if (command.snapshotIds().isEmpty()
                    || !command.sourceScope().matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException(
                        "OPENAPI31 ingestion requires aggregate sourceScope "
                                + "and snapshotIds"
                );
            }
        } else if (source == GatewayDefinitionSourceTypeEnum.RPC_DESCRIPTOR) {
            if (!protocols.equals(Set.of("RPC"))) {
                throw new IllegalArgumentException(
                        "RPC_DESCRIPTOR ingestion requires RPC report groups"
                );
            }
            if (!command.snapshotIds().isEmpty()) {
                throw new IllegalArgumentException(
                        "RPC_DESCRIPTOR ingestion requires empty snapshotIds"
                );
            }
        } else if (!command.snapshotIds().isEmpty()) {
            throw new IllegalArgumentException(
                    "MANUAL ingestion does not accept OpenAPI snapshots"
            );
        }
        if (!command.report().complete()) {
            throw new IllegalArgumentException(
                    "only complete definition reports are supported"
            );
        }
    }

    private void validateOperations(
            GatewayInterfaceDefinitionReport report) {
        Set<String> operationKeys = new HashSet<>();
        for (GatewayInterfaceDefinitionReport.InterfaceGroup group
                : groups(report)) {
            for (GatewayInterfaceDefinitionReport.Operation operation
                    : group.operations()) {
                if (!operationKeys.add(operation.operationKey())) {
                    throw new IllegalArgumentException(
                            "duplicate operationKey "
                                    + operation.operationKey()
                    );
                }
                schemaValidator.validate(operation);
            }
        }
    }

    private void validateSnapshots(
            GatewayDefinitionIngestionCommandDTO command) {
        if (command.snapshotIds().isEmpty()) {
            return;
        }
        List<GatewayInterfaceDefinitionReport.InterfaceGroup> groups =
                groups(command.report());
        Set<String> groupCodes = groups.stream()
                .map(GatewayInterfaceDefinitionReport.InterfaceGroup::code)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (command.snapshotIds().size() != groupCodes.size()) {
            throw new IllegalArgumentException(
                    "snapshotIds count must equal OpenAPI group count"
            );
        }
        Set<String> snapshotGroups = new HashSet<>();
        for (String snapshotId : command.snapshotIds()) {
            GatewayOpenApiSnapshotPO snapshot = snapshots.findById(snapshotId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "YUHENG_OPENAPI_SNAPSHOT_CONFLICT: snapshot was "
                                    + "not found: " + snapshotId
                    ));
            if (!command.applicationId().equals(snapshot.applicationId())
                    || !command.report().build().buildId().equals(
                    snapshot.buildId()
            )
                    || !"VALID".equals(snapshot.validationStatus())
                    || (snapshot.definitionSetId() != null
                    && !command.report().definitionSetId().equals(
                    snapshot.definitionSetId()
            ))) {
                throw new IllegalArgumentException(
                        "YUHENG_OPENAPI_SNAPSHOT_CONFLICT: snapshot does not "
                                + "belong to the command"
                );
            }
            if (!snapshotGroups.add(snapshot.openapiGroup())) {
                throw new IllegalArgumentException(
                        "snapshotIds contain duplicate OpenAPI groups"
                );
            }
        }
        if (!snapshotGroups.equals(groupCodes)) {
            throw new IllegalArgumentException(
                    "YUHENG_OPENAPI_SNAPSHOT_CONFLICT: snapshot groups do not "
                            + "match the report groups"
            );
        }
    }

    private GatewayInterfaceDefinitionReportResult result(
            GatewayDefinitionIngestionCommandDTO command,
            GatewayStoredReportPO stored,
            Instant now) {
        GatewayDefinitionCounts counts = counts(command.report());
        return new GatewayInterfaceDefinitionReportResult(
                command.report().reportId(),
                command.report().definitionSetId(),
                stored.created() == 0 && stored.updated() == 0
                        ? GatewayInterfaceDefinitionReportResult.Status
                        .ACCEPTED_WITH_WARNINGS
                        : GatewayInterfaceDefinitionReportResult.Status.ACCEPTED,
                command.applicationId(),
                new GatewayInterfaceDefinitionReportResult.Counts(
                        counts.businesses(),
                        counts.entities(),
                        counts.groups(),
                        counts.operations(),
                        stored.created(),
                        stored.updated(),
                        0
                ),
                stored.operationRefs(),
                List.of(),
                now
        );
    }

    private GatewayDefinitionCounts counts(
            GatewayInterfaceDefinitionReport report) {
        int entities = 0;
        int groups = 0;
        int operations = 0;
        for (GatewayInterfaceDefinitionReport.BusinessDomain business
                : report.businessDomains()) {
            entities += business.entityDomains().size();
            for (GatewayInterfaceDefinitionReport.EntityDomain entity
                    : business.entityDomains()) {
                groups += entity.interfaceGroups().size();
                operations += entity.interfaceGroups().stream()
                        .mapToInt(group -> group.operations().size())
                        .sum();
            }
        }
        return new GatewayDefinitionCounts(
                report.businessDomains().size(),
                entities,
                groups,
                operations
        );
    }

    private String protocol(GatewayInterfaceDefinitionReport report) {
        return groups(report).stream()
                .map(GatewayInterfaceDefinitionReport.InterfaceGroup::protocol)
                .distinct()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "report must contain a protocol"
                ));
    }

    private List<GatewayInterfaceDefinitionReport.InterfaceGroup> groups(
            GatewayInterfaceDefinitionReport report) {
        List<GatewayInterfaceDefinitionReport.InterfaceGroup> groups =
                new ArrayList<>();
        report.businessDomains().forEach(business ->
                business.entityDomains().forEach(entity ->
                        groups.addAll(entity.interfaceGroups())));
        return List.copyOf(groups);
    }
}
