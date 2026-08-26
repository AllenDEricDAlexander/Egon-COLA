package top.egon.cola.component.gateway.admin.openapi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.gateway.admin.openapi.converter.GatewayOpenApi31ContractAdapter;
import top.egon.cola.component.gateway.admin.openapi.converter.GatewayOpenApiDefinitionConverter;
import top.egon.cola.component.gateway.admin.openapi.domain.dto.GatewayOpenApiAggregateDTO;
import top.egon.cola.component.gateway.admin.openapi.domain.dto.GatewayOpenApiDefinitionDTO;
import top.egon.cola.component.gateway.admin.openapi.domain.dto.GatewayOpenApiDocumentDTO;
import top.egon.cola.component.gateway.admin.reporting.domain.dto.GatewayDefinitionIngestionCommandDTO;
import top.egon.cola.component.gateway.admin.reporting.service.GatewayDefinitionIngestionService;
import top.egon.cola.component.gateway.admin.reporting.service.GatewayReportCanonicalizer;
import top.egon.cola.component.gateway.contract.reporting.GatewayDefinitionSourceTypeEnum;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReportResult;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Completeness barrier and single-report coordinator for one OpenAPI build.
 *
 * <p>中文：Coordinator 只组织 Group 集合、漂移和 operation 唯一性；OpenAPI
 * 图遍历由 Adapter 负责，Definition graph 与 snapshot link 由 shared Ingestion
 * Facade 负责。任何 Group 缺失、额外、build 不一致或 operation 冲突都会在
 * Facade 调用前失败。</p>
 */
@Slf4j
@Validated
@Component("gatewayOpenApiAggregateCoordinator")
public class GatewayOpenApiAggregateCoordinator {

    private final GatewayOpenApi31ContractAdapter adapter;

    private final GatewayOpenApiDefinitionConverter converter;

    private final GatewayDefinitionIngestionService ingestion;

    private final GatewayReportCanonicalizer canonicalizer;

    private final Clock clock;

    /** Creates the Spring-managed coordinator. */
    @Autowired
    public GatewayOpenApiAggregateCoordinator(
            GatewayOpenApi31ContractAdapter adapter,
            GatewayOpenApiDefinitionConverter converter,
            GatewayDefinitionIngestionService ingestion) {
        this(
                adapter,
                converter,
                ingestion,
                new GatewayReportCanonicalizer(),
                Clock.systemUTC()
        );
    }

    /** Constructor used by focused deterministic tests. */
    public GatewayOpenApiAggregateCoordinator(
            GatewayOpenApi31ContractAdapter adapter,
            GatewayOpenApiDefinitionConverter converter,
            GatewayDefinitionIngestionService ingestion,
            Clock clock) {
        this(
                adapter,
                converter,
                ingestion,
                new GatewayReportCanonicalizer(),
                clock
        );
    }

    private GatewayOpenApiAggregateCoordinator(
            GatewayOpenApi31ContractAdapter adapter,
            GatewayOpenApiDefinitionConverter converter,
            GatewayDefinitionIngestionService ingestion,
            GatewayReportCanonicalizer canonicalizer,
            Clock clock) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
        this.converter = Objects.requireNonNull(converter, "converter");
        this.ingestion = Objects.requireNonNull(ingestion, "ingestion");
        this.canonicalizer = Objects.requireNonNull(
                canonicalizer,
                "canonicalizer"
        );
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Aggregates all advertised Groups with the trusted Gateway application
     * scope and sends exactly one ingestion command.
     *
     * @param aggregate complete manifest/document/snapshot set
     * @param application trusted Gateway report application scope
     * @return shared Definition Set acknowledgement
     */
    public GatewayInterfaceDefinitionReportResult aggregateAndIngest(
            GatewayOpenApiAggregateDTO aggregate,
            GatewayInterfaceDefinitionReport.Application application) {
        Objects.requireNonNull(aggregate, "aggregate");
        Objects.requireNonNull(application, "application");
        verifyAggregate(aggregate, application);

        List<GatewayInterfaceDefinitionReport> fragments = new ArrayList<>();
        for (String group : aggregate.groups()) {
            GatewayOpenApiDocumentDTO document = aggregate.documents()
                    .get(group);
            GatewayOpenApiDefinitionDTO definition = adapter.adapt(
                    document,
                    new GatewayOpenApiDefinitionDTO.Application(
                            application.bizCode(),
                            application.applicationCode(),
                            application.name(),
                            application.env(),
                            application.namespace()
                    )
            );
            GatewayInterfaceDefinitionReport fragment = converter.toTarget(
                    definition
            );
            if (fragment == null) {
                throw new IllegalStateException(
                        "OpenAPI definition converter returned null"
                );
            }
            fragments.add(fragment);
        }
        GatewayInterfaceDefinitionReport report = merge(
                aggregate,
                application,
                fragments
        );
        GatewayDefinitionIngestionCommandDTO command =
                new GatewayDefinitionIngestionCommandDTO(
                        aggregate.applicationId(),
                        GatewayDefinitionSourceTypeEnum.OPENAPI31,
                        aggregate.aggregateSha256(),
                        report,
                        aggregate.snapshotIds()
                );
        log.info(
                "OpenAPI Group aggregate ready app={} build={} groups={} scope={}",
                aggregate.applicationId(),
                aggregate.buildId(),
                aggregate.groups().size(),
                aggregate.aggregateSha256()
        );
        return ingestion.ingest(command);
    }

    /** Alias retained for callers that separate aggregate from ingest naming. */
    public GatewayInterfaceDefinitionReportResult aggregateAndIngest(
            GatewayOpenApiAggregateDTO aggregate,
            GatewayOpenApiDefinitionDTO.Application application) {
        return aggregateAndIngest(
                aggregate,
                new GatewayInterfaceDefinitionReport.Application(
                        application.bizCode(),
                        application.applicationCode(),
                        application.name(),
                        application.env(),
                        application.namespace()
                )
        );
    }

    private void verifyAggregate(
            GatewayOpenApiAggregateDTO aggregate,
            GatewayInterfaceDefinitionReport.Application application) {
        String expectedScope = GatewayOpenApiAggregateDTO
                .calculateAggregateSha256(
                        aggregate.groups(),
                        aggregate.documents()
                );
        if (!expectedScope.equals(aggregate.aggregateSha256())) {
            throw new IllegalArgumentException(
                    "OpenAPI aggregate sourceScope does not match documents"
            );
        }
        GatewayOpenApiDocumentDTO firstDocument = aggregate.documents()
                .get(aggregate.groups().getFirst());
        String artifactVersion = firstDocument.candidate().artifactVersion();
        String providerServiceName = firstDocument.candidate()
                .providerServiceName();
        String providerGroup = firstDocument.candidate().providerGroup();
        String providerVersion = firstDocument.candidate().providerVersion();
        String resourceUri = firstDocument.candidate().resourceUri().toString();
        for (String group : aggregate.groups()) {
            GatewayOpenApiDocumentDTO document = aggregate.documents().get(group);
            if (!aggregate.applicationId().equals(
                    document.candidate().applicationId()
            )
                    || !aggregate.buildId().equals(
                    document.candidate().buildId()
            )
                    || !group.equals(document.candidate().openapiGroup())
                    || !application.bizCode().equals(
                    document.candidate().bizCode()
            )
                    || !application.applicationCode().equals(
                    document.candidate().applicationCode()
            )
                    || !artifactVersion.equals(
                    document.candidate().artifactVersion()
            )
                    || !providerServiceName.equals(
                    document.candidate().providerServiceName()
            )
                    || !providerGroup.equals(
                    document.candidate().providerGroup()
            )
                    || !providerVersion.equals(
                    document.candidate().providerVersion()
            )
                    || !resourceUri.equals(
                    document.candidate().resourceUri().toString()
            )) {
                throw new IllegalArgumentException(
                        "OpenAPI document candidate does not match aggregate"
                );
            }
        }
    }

    private GatewayInterfaceDefinitionReport merge(
            GatewayOpenApiAggregateDTO aggregate,
            GatewayInterfaceDefinitionReport.Application application,
            List<GatewayInterfaceDefinitionReport> fragments) {
        if (fragments.isEmpty()) {
            throw new IllegalArgumentException(
                    "OpenAPI aggregate must contain at least one fragment"
            );
        }
        GatewayInterfaceDefinitionReport first = fragments.getFirst();
        if (!application.equals(first.application())
                || !aggregate.buildId().equals(first.build().buildId())) {
            throw new IllegalArgumentException(
                    "OpenAPI fragment application/build drift detected"
            );
        }
        Map<String, Map<String, Map<String,
                GatewayInterfaceDefinitionReport.InterfaceGroup>>> graph =
                new LinkedHashMap<>();
        Set<String> operationKeys = new HashSet<>();
        for (GatewayInterfaceDefinitionReport fragment : fragments) {
            if (!application.equals(fragment.application())
                    || !aggregate.buildId().equals(
                    fragment.build().buildId()
            )) {
                throw new IllegalArgumentException(
                        "OpenAPI fragments disagree on application/build"
                );
            }
            fragment.businessDomains().forEach(business -> {
                Map<String, Map<String,
                        GatewayInterfaceDefinitionReport.InterfaceGroup>>
                        entities = graph.computeIfAbsent(
                                business.code(),
                                ignored -> new LinkedHashMap<>()
                        );
                business.entityDomains().forEach(entity -> {
                    Map<String, GatewayInterfaceDefinitionReport.InterfaceGroup>
                            groups = entities.computeIfAbsent(
                                    entity.code(),
                                    ignored -> new LinkedHashMap<>()
                            );
                    entity.interfaceGroups().forEach(group -> {
                        if (groups.putIfAbsent(group.code(), group) != null) {
                            throw new IllegalArgumentException(
                                    "duplicate OpenAPI group " + group.code()
                            );
                        }
                        group.operations().forEach(operation -> {
                            if (!operationKeys.add(
                                    operation.operationKey()
                            )) {
                                throw new IllegalArgumentException(
                                        "duplicate operationKey "
                                                + operation.operationKey()
                                );
                            }
                        });
                    });
                });
            });
        }

        List<GatewayInterfaceDefinitionReport.BusinessDomain> businesses =
                graph.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(business -> new GatewayInterfaceDefinitionReport.BusinessDomain(
                                business.getKey(),
                                businessName(fragments, business.getKey()),
                                businessDescription(fragments, business.getKey()),
                                business.getValue().entrySet().stream()
                                        .sorted(Map.Entry.comparingByKey())
                                        .map(entity -> new GatewayInterfaceDefinitionReport.EntityDomain(
                                                entity.getKey(),
                                                entityName(fragments, business.getKey(), entity.getKey()),
                                                entityDescription(fragments, business.getKey(), entity.getKey()),
                                                entity.getValue().values().stream()
                                                        .sorted(Comparator.comparing(
                                                                GatewayInterfaceDefinitionReport.InterfaceGroup::code
                                                        ))
                                                        .toList()
                                        ))
                                        .toList()
                        ))
                        .toList();

        Map<String, String> metadata = new LinkedHashMap<>(
                first.build().metadata()
        );
        metadata.put("openapiAggregateSha256", aggregate.aggregateSha256());
        GatewayInterfaceDefinitionReport.Build build =
                new GatewayInterfaceDefinitionReport.Build(
                        first.build().artifactVersion(),
                        first.build().buildId(),
                        metadata
                );
        String fingerprint = canonicalizer.definitionFingerprint(
                application,
                build,
                true,
                businesses
        );
        String definitionSetId = canonicalizer.definitionSetId(
                application,
                build,
                fingerprint
        );
        GatewayInterfaceDefinitionReport report =
                new GatewayInterfaceDefinitionReport(
                        "v2",
                        "openapi-" + aggregate.aggregateSha256(),
                        clock.instant(),
                        application,
                        build,
                        true,
                        definitionSetId,
                        fingerprint,
                        businesses
                );
        canonicalizer.verify(report);
        return report;
    }

    private String businessName(
            List<GatewayInterfaceDefinitionReport> fragments,
            String code) {
        return fragments.stream()
                .flatMap(fragment -> fragment.businessDomains().stream())
                .filter(domain -> code.equals(domain.code()))
                .map(GatewayInterfaceDefinitionReport.BusinessDomain::name)
                .findFirst()
                .orElse(code);
    }

    private String businessDescription(
            List<GatewayInterfaceDefinitionReport> fragments,
            String code) {
        return fragments.stream()
                .flatMap(fragment -> fragment.businessDomains().stream())
                .filter(domain -> code.equals(domain.code()))
                .map(GatewayInterfaceDefinitionReport.BusinessDomain::description)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String entityName(
            List<GatewayInterfaceDefinitionReport> fragments,
            String businessCode,
            String entityCode) {
        return fragments.stream()
                .flatMap(fragment -> fragment.businessDomains().stream())
                .filter(domain -> businessCode.equals(domain.code()))
                .flatMap(domain -> domain.entityDomains().stream())
                .filter(domain -> entityCode.equals(domain.code()))
                .map(GatewayInterfaceDefinitionReport.EntityDomain::name)
                .findFirst()
                .orElse(entityCode);
    }

    private String entityDescription(
            List<GatewayInterfaceDefinitionReport> fragments,
            String businessCode,
            String entityCode) {
        return fragments.stream()
                .flatMap(fragment -> fragment.businessDomains().stream())
                .filter(domain -> businessCode.equals(domain.code()))
                .flatMap(domain -> domain.entityDomains().stream())
                .filter(domain -> entityCode.equals(domain.code()))
                .map(GatewayInterfaceDefinitionReport.EntityDomain::description)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
