package top.egon.cola.component.yuheng.admin.openapi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.application.domain.po.GatewayApplicationPO;
import top.egon.cola.component.yuheng.admin.application.repository.GatewayApplicationRepository;
import top.egon.cola.component.yuheng.admin.catalog.domain.po.GatewayOperationDefinitionPO;
import top.egon.cola.component.yuheng.admin.catalog.domain.po.GatewayOperationPO;
import top.egon.cola.component.yuheng.admin.catalog.repository.GatewayCatalogRepository;
import top.egon.cola.component.yuheng.admin.openapi.converter.GatewayOpenApi31ContractAdapter;
import top.egon.cola.component.yuheng.admin.openapi.domain.po.GatewayOpenApiSnapshotPO;
import top.egon.cola.component.yuheng.admin.openapi.domain.po.GatewayOpenApiSyncPO;
import top.egon.cola.component.yuheng.admin.openapi.domain.vo.GatewayOpenApiDocumentVO;
import top.egon.cola.component.yuheng.admin.openapi.domain.vo.GatewayOpenApiSyncStateVO;
import top.egon.cola.component.yuheng.admin.openapi.domain.vo.GatewayOperationOpenApiVO;
import top.egon.cola.component.yuheng.admin.openapi.repository.GatewayOpenApiSnapshotRepository;
import top.egon.cola.component.yuheng.admin.openapi.repository.GatewayOpenApiSyncRepository;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminNotFoundException;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayOpenApiSourceNotAvailableException;
import top.egon.cola.component.yuheng.contract.reporting.GatewayDefinitionSourceTypeEnum;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only query facade for OpenAPI synchronization, fragments and snapshots.
 *
 * <p>中文：该 Facade 只组合本地 application/catalog/snapshot 投影；它不触发
 * Tianshu、Provider 网络、同步状态 CAS 或 Definition ingestion。</p>
 */
@Slf4j
@Validated
@Service("gatewayOpenApiQueryService")
public class GatewayOpenApiQueryService {

    private static final TypeReference<Map<String, Object>> OBJECT_MAP =
            new TypeReference<>() {
            };

    private final GatewayOpenApiSyncRepository syncStates;

    private final GatewayOpenApiSnapshotRepository snapshots;

    private final GatewayCatalogRepository catalog;

    private final GatewayApplicationRepository applications;

    private final ObjectMapper objectMapper;

    /** Creates the Spring-managed read-only query facade. */
    public GatewayOpenApiQueryService(
            GatewayOpenApiSyncRepository syncStates,
            GatewayOpenApiSnapshotRepository snapshots,
            GatewayCatalogRepository catalog,
            GatewayApplicationRepository applications,
            @Qualifier("gatewayOpenApiObjectMapper") ObjectMapper objectMapper) {
        this.syncStates = Objects.requireNonNull(syncStates, "syncStates");
        this.snapshots = Objects.requireNonNull(snapshots, "snapshots");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.applications = Objects.requireNonNull(
                applications,
                "applications");
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper");
    }

    /**
     * Lists state rows for active applications in deterministic order.
     *
     * @param bizCode optional business filter
     * @param namespace optional namespace filter
     * @param env optional environment filter
     * @param appCode optional application-code filter
     * @return immutable presentation rows, never {@code null}
     */
    @Transactional(readOnly = true)
    public List<GatewayOpenApiSyncStateVO> listSyncStates(
            @Size(max = 128) String bizCode,
            @Size(max = 128) String namespace,
            @Size(max = 64) String env,
            @Size(max = 128) String appCode) {
        String normalizedBizCode = optionalFilter(bizCode, "bizCode", 128);
        String normalizedNamespace = optionalFilter(
                namespace,
                "namespace",
                128);
        String normalizedEnv = optionalFilter(env, "env", 64);
        String normalizedAppCode = optionalFilter(
                appCode,
                "appCode",
                128);

        List<GatewayOpenApiSyncStateVO> result = new ArrayList<>();
        for (GatewayApplicationPO application
                : applications.findAllByDeletedFalseOrderByCreatedAtDesc()) {
            if (!matches(application,
                    normalizedBizCode,
                    normalizedNamespace,
                    normalizedEnv,
                    normalizedAppCode)) {
                continue;
            }
            List<GatewayOpenApiSyncPO> rows = syncStates.findByApplicationId(
                    application.getId());
            if (rows == null) {
                continue;
            }
            for (GatewayOpenApiSyncPO row : rows) {
                result.add(toSyncState(row));
            }
        }
        result.sort(Comparator
                .comparing(GatewayOpenApiSyncStateVO::applicationId)
                .thenComparing(GatewayOpenApiSyncStateVO::buildId)
                .thenComparing(GatewayOpenApiSyncStateVO::openapiGroup)
                .thenComparing(GatewayOpenApiSyncStateVO::id));
        return List.copyOf(result);
    }

    /**
     * Resolves the current OpenAPI definition of one catalog operation.
     *
     * @param operationId Gateway operation identifier
     * @return stored OpenAPI operation fragment
     */
    @Transactional(readOnly = true)
    public GatewayOperationOpenApiVO getOperationOpenApi(String operationId) {
        String normalizedOperationId = required(
                operationId,
                "operationId",
                64);
        GatewayOperationPO operation = catalog.findOperation(
                        normalizedOperationId)
                .orElseThrow(() -> notFound(
                        "Gateway operation was not found"));
        if (!GatewayDefinitionSourceTypeEnum.OPENAPI31.name().equals(
                operation.sourceType())) {
            throw new GatewayOpenApiSourceNotAvailableException();
        }

        GatewayOperationDefinitionPO definition = currentDefinition(
                operation,
                catalog.loadDefinitions(operation.id()));
        Map<String, Object> attributes = definition.attributes() == null
                ? Map.of()
                : definition.attributes();
        String group = attribute(attributes, "openapiGroup");
        String canonicalSha256 = attribute(
                attributes,
                "openapiCanonicalSha256");
        if (canonicalSha256 == null || canonicalSha256.isBlank()) {
            canonicalSha256 = definition.definitionSha256();
        }
        String path = attribute(attributes, "path");
        String method = attribute(attributes, "httpMethod");
        String operationName = attribute(attributes, "openapiOperationId");
        if (group == null || canonicalSha256 == null || path == null
                || method == null || operationName == null) {
            throw notFound("OpenAPI operation provenance was not found");
        }

        GatewayOpenApiSnapshotPO snapshot = snapshots
                .findByApplicationGroupAndCanonicalSha256(
                        operation.applicationId(),
                        group,
                        canonicalSha256)
                .orElseThrow(() -> notFound(
                        "OpenAPI snapshot was not found"));
        Map<String, Object> operationJson = operationJson(
                snapshot.documentJson(),
                path,
                method);
        String normalizedMethod = required(method, "httpMethod", 16)
                .toUpperCase(Locale.ROOT);
        return new GatewayOperationOpenApiVO(
                operation.id(),
                operation.operationKey(),
                snapshot.id(),
                snapshot.openapiVersion(),
                group,
                path,
                normalizedMethod,
                operationName,
                stringList(attributes.get("consumes")),
                stringList(attributes.get("produces")),
                operationJson,
                snapshot.validatedAt());
    }

    /**
     * Returns one immutable snapshot and its parsed JSON value.
     *
     * @param snapshotId immutable snapshot identifier
     * @return snapshot document response
     */
    @Transactional(readOnly = true)
    public GatewayOpenApiDocumentVO getSnapshotDocument(String snapshotId) {
        String normalizedSnapshotId = required(snapshotId, "snapshotId", 64);
        GatewayOpenApiSnapshotPO snapshot = snapshots.findById(
                        normalizedSnapshotId)
                .orElseThrow(() -> notFound(
                        "OpenAPI snapshot was not found"));
        return new GatewayOpenApiDocumentVO(
                snapshot.id(),
                snapshot.applicationId(),
                snapshot.buildId(),
                snapshot.openapiVersion(),
                snapshot.documentSha256(),
                snapshot.canonicalSha256(),
                snapshot.documentJson(),
                snapshot.fetchedAt(),
                snapshot.validatedAt());
    }

    private GatewayOpenApiSyncStateVO toSyncState(
            GatewayOpenApiSyncPO row) {
        GatewayOpenApiSnapshotPO snapshot = row.latestSnapshotId() == null
                ? null
                : snapshots.findById(row.latestSnapshotId()).orElse(null);
        return new GatewayOpenApiSyncStateVO(
                row.id(),
                row.applicationId(),
                row.buildId(),
                row.artifactVersion(),
                row.openapiGroup(),
                row.status().name(),
                row.latestSnapshotId(),
                row.definitionSetId(),
                snapshot == null ? null : GatewayOpenApi31ContractAdapter.operationCount(
                        objectMapper.valueToTree(snapshot.documentJson())),
                snapshot == null ? null : snapshot.schemaCount(),
                snapshot == null ? null : snapshot.canonicalSha256(),
                row.lastErrorCode(),
                row.lastErrorMessage(),
                GatewayDefinitionSourceTypeEnum.OPENAPI31.name(),
                row.lastAttemptAt(),
                row.lastSuccessAt(),
                row.nextRetryAt());
    }

    private GatewayOperationDefinitionPO currentDefinition(
            GatewayOperationPO operation,
            List<GatewayOperationDefinitionPO> definitions) {
        if (definitions == null || definitions.isEmpty()
                || operation.currentDefinitionId() == null) {
            throw notFound("Current operation definition was not found");
        }
        return definitions.stream()
                .filter(Objects::nonNull)
                .filter(definition -> definition.id().equals(
                        operation.currentDefinitionId()))
                .findFirst()
                .orElseThrow(() -> notFound(
                        "Current operation definition was not found"));
    }

    private Map<String, Object> operationJson(
            Map<String, Object> document,
            String path,
            String method) {
        Object pathsValue = document == null ? null : document.get("paths");
        Map<?, ?> paths = asMap(pathsValue);
        Map<?, ?> pathItem = asMap(paths.get(path));
        Object operationValue = pathItem.get(
                method.toLowerCase(Locale.ROOT));
        if (operationValue == null) {
            operationValue = pathItem.get(method.toUpperCase(Locale.ROOT));
        }
        Map<String, Object> operation = asObjectMap(operationValue);
        if (operation.isEmpty()) {
            throw notFound("OpenAPI operation fragment was not found");
        }
        return operation;
    }

    private Map<String, Object> asObjectMap(Object value) {
        if (value == null) {
            throw notFound("OpenAPI operation fragment was not found");
        }
        try {
            return objectMapper.convertValue(value, OBJECT_MAP);
        } catch (IllegalArgumentException error) {
            log.debug("OpenAPI fragment conversion failed: {}",
                    error.getClass().getSimpleName());
            throw notFound("OpenAPI operation fragment was not found");
        }
    }

    private Map<?, ?> asMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            throw notFound("OpenAPI operation fragment was not found");
        }
        return map;
    }

    private List<String> stringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof String string) {
            return List.of(string);
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(item -> Objects.requireNonNull(item, "media type"))
                    .map(Object::toString)
                    .toList();
        }
        return List.of(value.toString());
    }

    private boolean matches(
            GatewayApplicationPO application,
            String bizCode,
            String namespace,
            String env,
            String appCode) {
        return (bizCode == null || bizCode.equals(application.getBizCode()))
                && (namespace == null
                || namespace.equals(application.getNamespace()))
                && (env == null || env.equals(application.getEnv()))
                && (appCode == null
                || appCode.equals(application.getApplicationCode()));
    }

    private String attribute(Map<String, Object> attributes, String name) {
        if (attributes == null) {
            return null;
        }
        Object value = attributes.get(name);
        return value == null ? null : value.toString().trim();
    }

    private String optionalFilter(String value, String field, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return required(value, field, max);
    }

    private String required(String value, String field, int max) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        if (normalized.length() > max) {
            throw new IllegalArgumentException(
                    field + " exceeds " + max + " characters");
        }
        return normalized;
    }

    private GatewayAdminNotFoundException notFound(String message) {
        return new GatewayAdminNotFoundException(message);
    }
}
