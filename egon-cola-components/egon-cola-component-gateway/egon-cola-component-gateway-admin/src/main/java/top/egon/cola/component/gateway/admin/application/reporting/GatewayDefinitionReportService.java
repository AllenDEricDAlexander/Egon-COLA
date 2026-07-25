package top.egon.cola.component.gateway.admin.application.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.gateway.admin.application.GatewayAdminIdempotencyConflictException;
import top.egon.cola.component.gateway.admin.application.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.admin.application.IdempotencyStore;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReportResult;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GatewayDefinitionReportService {

    private static final String IDEMPOTENCY_SCOPE =
            "GATEWAY_DEFINITION_REPORT";

    private final GatewayDefinitionReportStore reports;

    private final IdempotencyStore idempotency;

    private final ObjectMapper objectMapper;

    private final GatewayReportCanonicalizer canonicalizer =
            new GatewayReportCanonicalizer();

    private final Clock clock;

    public GatewayDefinitionReportService(
            GatewayDefinitionReportStore reports,
            IdempotencyStore idempotency,
            ObjectMapper objectMapper) {
        this(reports, idempotency, objectMapper, Clock.systemUTC());
    }

    GatewayDefinitionReportService(
            GatewayDefinitionReportStore reports,
            IdempotencyStore idempotency,
            ObjectMapper objectMapper,
            Clock clock) {
        this.reports = reports;
        this.idempotency = idempotency;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public GatewayInterfaceDefinitionReportResult accept(
            GatewayReportAuthentication authentication,
            GatewayInterfaceDefinitionReport report,
            String headerReportId,
            String headerContractVersion) {
        validate(
                authentication,
                report,
                headerReportId,
                headerContractVersion
        );
        canonicalizer.verify(report);
        String payloadSha = canonicalizer.payloadSha256(report);
        IdempotencyStore.Record previous = idempotency.find(
                        IDEMPOTENCY_SCOPE,
                        authentication.applicationId(),
                        report.reportId()
                )
                .orElse(null);
        if (previous != null) {
            if (!previous.payloadSha256().equals(payloadSha)) {
                throw new GatewayAdminIdempotencyConflictException();
            }
            return objectMapper.convertValue(
                    previous.response(),
                    GatewayInterfaceDefinitionReportResult.class
            );
        }
        reports.findBuildFingerprint(
                        authentication.applicationId(),
                        report.build().buildId()
                )
                .filter(fingerprint -> !fingerprint.equals(
                        report.definitionFingerprint()
                ))
                .ifPresent(conflict -> {
                    throw new IllegalStateException(
                            "GATEWAY_ADMIN_IMMUTABLE_BUILD_CONFLICT: "
                                    + report.build().buildId()
                    );
                });
        GatewayInterfaceDefinitionReportResult result =
                reports.definitionSetExists(
                        authentication.applicationId(),
                        report.definitionSetId()
                )
                        ? alreadyVerified(authentication, report)
                        : ingest(authentication, report);
        idempotency.save(new IdempotencyStore.Record(
                IDEMPOTENCY_SCOPE,
                authentication.applicationId(),
                report.reportId(),
                payloadSha,
                report.definitionSetId(),
                objectMapper.convertValue(result, Map.class),
                result.receivedAt(),
                null
        ));
        return result;
    }

    @Transactional(readOnly = true)
    public GatewayInterfaceDefinitionReportResult find(
            GatewayReportAuthentication authentication,
            String reportId) {
        return idempotency.find(
                        IDEMPOTENCY_SCOPE,
                        authentication.applicationId(),
                        reportId
                )
                .map(record -> objectMapper.convertValue(
                        record.response(),
                        GatewayInterfaceDefinitionReportResult.class
                ))
                .orElseThrow(() -> new GatewayAdminNotFoundException(
                        "gateway interface report "
                                + reportId
                                + " was not found"
                ));
    }

    private GatewayInterfaceDefinitionReportResult ingest(
            GatewayReportAuthentication authentication,
            GatewayInterfaceDefinitionReport report) {
        int previousCount = reports.countStarterOperations(
                authentication.applicationId()
        );
        Instant now = clock.instant();
        GatewayDefinitionReportStore.StoredReport stored = reports.ingest(
                authentication.applicationId(),
                report,
                now
        );
        DefinitionCounts counts = counts(report);
        int missing = Math.max(0, previousCount - counts.operations);
        return new GatewayInterfaceDefinitionReportResult(
                report.reportId(),
                report.definitionSetId(),
                GatewayInterfaceDefinitionReportResult.Status.ACCEPTED,
                authentication.applicationId(),
                new GatewayInterfaceDefinitionReportResult.Counts(
                        counts.businesses,
                        counts.entities,
                        counts.groups,
                        counts.operations,
                        stored.created(),
                        stored.updated(),
                        missing
                ),
                stored.operationRefs(),
                missing == 0
                        ? List.of()
                        : List.of(warning(
                        "MISSING_FROM_THIS_SET",
                        "interfaces absent from this set are offlined after "
                                + "providers activate it"
                )),
                now
        );
    }

    private GatewayInterfaceDefinitionReportResult alreadyVerified(
            GatewayReportAuthentication authentication,
            GatewayInterfaceDefinitionReport report) {
        DefinitionCounts counts = counts(report);
        return new GatewayInterfaceDefinitionReportResult(
                report.reportId(),
                report.definitionSetId(),
                GatewayInterfaceDefinitionReportResult.Status
                        .ACCEPTED_WITH_WARNINGS,
                authentication.applicationId(),
                new GatewayInterfaceDefinitionReportResult.Counts(
                        counts.businesses,
                        counts.entities,
                        counts.groups,
                        counts.operations,
                        0,
                        0,
                        0
                ),
                List.of(),
                List.of(warning(
                        "DEFINITION_SET_ALREADY_VERIFIED",
                        "the immutable definition set already exists"
                )),
                clock.instant()
        );
    }

    private void validate(
            GatewayReportAuthentication authentication,
            GatewayInterfaceDefinitionReport report,
            String headerReportId,
            String headerContractVersion) {
        if (!"v1".equals(headerContractVersion)
                || !"v1".equals(report.contractVersion())) {
            throw new IllegalArgumentException(
                    "unsupported gateway reporting contract version"
            );
        }
        if (!report.reportId().equals(headerReportId)) {
            throw new IllegalArgumentException(
                    "X-Gateway-Report-Id does not match request body"
            );
        }
        if (!authentication.applicationCode().equals(
                report.application().applicationCode()
        )
                || !authentication.env().equals(report.application().env())
                || !authentication.namespace().equals(
                report.application().namespace()
        )) {
            throw new IllegalArgumentException(
                    "credential scope does not match report application"
            );
        }
        if (!report.complete()) {
            throw new IllegalArgumentException(
                    "only complete definition reports are supported"
            );
        }
        Set<String> operationKeys = new HashSet<>();
        report.businessDomains().forEach(business ->
                business.entityDomains().forEach(entity ->
                        entity.interfaceGroups().forEach(group ->
                                group.operations().forEach(operation -> {
                                    if (!operationKeys.add(
                                            operation.operationKey()
                                    )) {
                                        throw new IllegalArgumentException(
                                                "duplicate operationKey "
                                                        + operation
                                                        .operationKey()
                                        );
                                    }
                                    if (operation.externalAccessible()
                                            && !"SUPPORTED".equals(
                                            operation.gatewaySupport()
                                    )) {
                                        throw new IllegalArgumentException(
                                                "unsupported operation cannot "
                                                        + "be externally "
                                                        + "accessible"
                                        );
                                    }
                                }))));
        validateCodes(report);
    }

    private void validateCodes(GatewayInterfaceDefinitionReport report) {
        String pattern = "[a-zA-Z0-9][a-zA-Z0-9._-]{0,127}";
        report.businessDomains().forEach(business -> {
            if (!business.code().matches(pattern)) {
                throw new IllegalArgumentException(
                        "invalid business domain code " + business.code()
                );
            }
            Set<String> entities = new LinkedHashSet<>();
            business.entityDomains().forEach(entity -> {
                if (!entity.code().matches(pattern)
                        || !entities.add(entity.code())) {
                    throw new IllegalArgumentException(
                            "invalid or duplicate entity domain code "
                                    + entity.code()
                    );
                }
                Set<String> groups = new LinkedHashSet<>();
                entity.interfaceGroups().forEach(group -> {
                    if (group.code().length() > 256
                            || !groups.add(group.code())) {
                        throw new IllegalArgumentException(
                                "invalid or duplicate interface group code "
                                        + group.code()
                        );
                    }
                });
            });
        });
    }

    private DefinitionCounts counts(
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
                for (GatewayInterfaceDefinitionReport.InterfaceGroup group
                        : entity.interfaceGroups()) {
                    operations += group.operations().size();
                }
            }
        }
        return new DefinitionCounts(
                report.businessDomains().size(),
                entities,
                groups,
                operations
        );
    }

    private GatewayInterfaceDefinitionReportResult.Warning warning(
            String code,
            String message) {
        return new GatewayInterfaceDefinitionReportResult.Warning(
                "$",
                code,
                message
        );
    }

    private record DefinitionCounts(
            int businesses,
            int entities,
            int groups,
            int operations
    ) {
    }
}
