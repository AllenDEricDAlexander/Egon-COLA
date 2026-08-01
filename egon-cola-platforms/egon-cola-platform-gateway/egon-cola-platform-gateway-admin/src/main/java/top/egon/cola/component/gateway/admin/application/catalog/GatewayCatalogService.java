package top.egon.cola.component.gateway.admin.application.catalog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.admin.application.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogRepository;
import top.egon.cola.component.gateway.admin.rule.GatewayRuleCanonicalizer;
import top.egon.cola.component.gateway.contract.identity.GatewayOperationKey;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GatewayCatalogService {

    private final GatewayCatalogStore store;

    private final GatewayAuditLogRepository audits;

    private final ObjectMapper objectMapper;

    private final Clock clock;

    @Autowired
    public GatewayCatalogService(
            GatewayCatalogStore store,
            GatewayAuditLogRepository audits,
            ObjectMapper objectMapper) {
        this(store, audits, objectMapper, Clock.systemUTC());
    }

    GatewayCatalogService(
            GatewayCatalogStore store,
            GatewayAuditLogRepository audits,
            ObjectMapper objectMapper,
            Clock clock) {
        this.store = store;
        this.audits = audits;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public GatewayCatalogStore.CatalogTree catalog(String applicationId) {
        return store.loadCatalog(applicationId);
    }

    @Transactional
    public String createManualInterfaceGroup(
            String applicationId,
            GatewayCatalogStore.ManualHierarchy hierarchy,
            AdminActor actor,
            RequestAuditContext request) {
        validateHierarchy(hierarchy);
        String id = store.createManualHierarchy(
                applicationId,
                hierarchy,
                clock.instant()
        );
        audit(actor, request, "INTERFACE_GROUP", id, "CREATE_MANUAL", Map.of(
                "applicationId", applicationId,
                "businessCode", hierarchy.businessCode(),
                "entityCode", hierarchy.entityCode(),
                "interfaceGroupCode", hierarchy.interfaceGroupCode()
        ));
        return id;
    }

    @Transactional
    public OperationDetail createManualOperation(
            String interfaceGroupId,
            ManualOperation command,
            AdminActor actor,
            RequestAuditContext request) {
        GatewayCatalogStore.InterfaceGroupScope scope =
                store.findInterfaceGroup(interfaceGroupId)
                        .orElseThrow(() -> new GatewayAdminNotFoundException(
                                "interface group "
                                        + interfaceGroupId
                                        + " was not found"
                        ));
        String operationKey = operationKey(scope, command);
        store.findOperation(scope.applicationId(), operationKey)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "operation key already exists with source "
                                    + existing.sourceType()
                    );
                });
        Instant now = clock.instant();
        String operationId = UuidV7.simpleString();
        GatewayCatalogStore.OperationRecord operation =
                new GatewayCatalogStore.OperationRecord(
                        operationId,
                        scope.applicationId(),
                        interfaceGroupId,
                        operationKey,
                        command.protocol().name(),
                        methodIdentity(command),
                        command.externalAccessible(),
                        providerIdentity(scope, command),
                        "MANUAL",
                        "DISCOVERED",
                        null,
                        0,
                        now,
                        now
                );
        store.insertOperation(operation);
        GatewayCatalogStore.OperationDefinition definition =
                definition(
                        operation,
                        1,
                        command.definition(),
                        actor.actorId(),
                        now
                );
        store.appendDefinition(definition);
        store.pointToDefinition(
                operationId,
                definition.id(),
                definition.externalAccessible(),
                now
        );
        audit(actor, request, "OPERATION", operationId, "CREATE_MANUAL",
                Map.of(
                        "operationKey", operationKey,
                        "externalAccessible",
                        command.externalAccessible(),
                        "definitionSha256",
                        definition.definitionSha256()
                ));
        return detail(operationId);
    }

    @Transactional(readOnly = true)
    public OperationDetail detail(String operationId) {
        GatewayCatalogStore.OperationRecord operation =
                requiredOperation(operationId);
        return new OperationDetail(
                operation,
                store.loadDefinitions(operationId)
        );
    }

    @Transactional
    public OperationDetail updateManualDefinition(
            String operationId,
            ManualDefinition definition,
            AdminActor actor,
            RequestAuditContext request) {
        GatewayCatalogStore.OperationRecord operation =
                requiredManualOperation(operationId);
        List<GatewayCatalogStore.OperationDefinition> history =
                store.loadDefinitions(operationId);
        long nextVersion = history.stream()
                .mapToLong(GatewayCatalogStore.OperationDefinition
                        ::definitionVersion)
                .max()
                .orElse(0) + 1;
        GatewayCatalogStore.OperationDefinition appended = definition(
                operation,
                nextVersion,
                definition,
                actor.actorId(),
                clock.instant()
        );
        if (history.stream().anyMatch(existing -> existing.definitionSha256()
                .equals(appended.definitionSha256()))) {
            return new OperationDetail(operation, history);
        }
        store.appendDefinition(appended);
        store.pointToDefinition(
                operationId,
                appended.id(),
                appended.externalAccessible(),
                appended.createdAt()
        );
        audit(actor, request, "OPERATION", operationId,
                "UPDATE_MANUAL_DEFINITION", Map.of(
                        "definitionVersion", nextVersion,
                        "definitionSha256", appended.definitionSha256(),
                        "externalAccessible",
                        appended.externalAccessible()
                ));
        return detail(operationId);
    }

    @Transactional
    public OperationDetail updateMetadata(
            String operationId,
            ManualMetadata metadata,
            AdminActor actor,
            RequestAuditContext request) {
        GatewayCatalogStore.OperationRecord operation =
                requiredManualOperation(operationId);
        List<GatewayCatalogStore.OperationDefinition> history =
                store.loadDefinitions(operationId);
        if (history.isEmpty()) {
            throw new IllegalStateException(
                    "manual operation has no definition"
            );
        }
        GatewayCatalogStore.OperationDefinition current = history.getFirst();
        Map<String, Object> attributes =
                new LinkedHashMap<>(current.attributes());
        if (metadata.owner() == null || metadata.owner().isBlank()) {
            attributes.remove("owner");
        } else {
            attributes.put("owner", metadata.owner().trim());
        }
        return updateManualDefinition(
                operationId,
                new ManualDefinition(
                        metadata.summary(),
                        metadata.tags(),
                        current.requestSchema(),
                        current.responseSchema(),
                        current.errorSchema(),
                        current.descriptorSnapshot(),
                        attributes,
                        current.externalAccessible()
                ),
                actor,
                request
        );
    }

    @Transactional
    public OperationDetail deprecate(
            String operationId,
            AdminActor actor,
            RequestAuditContext request) {
        requiredOperation(operationId);
        store.deprecate(operationId, clock.instant());
        audit(actor, request, "OPERATION", operationId, "DEPRECATE", Map.of(
                "lifecycleStatus", "DEPRECATED"
        ));
        return detail(operationId);
    }

    private GatewayCatalogStore.OperationDefinition definition(
            GatewayCatalogStore.OperationRecord operation,
            long version,
            ManualDefinition value,
            String actorId,
            Instant now) {
        validateDefinition(operation.protocol(), value);
        Map<String, Object> digestMaterial = new LinkedHashMap<>();
        digestMaterial.put("summary", value.summary());
        digestMaterial.put("tags", value.tags());
        digestMaterial.put("requestSchema", value.requestSchema());
        digestMaterial.put("responseSchema", value.responseSchema());
        digestMaterial.put("errorSchema", value.errorSchema());
        digestMaterial.put("descriptorSnapshot", value.descriptorSnapshot());
        digestMaterial.put("attributes", value.attributes());
        digestMaterial.put(
                "externalAccessible",
                value.externalAccessible()
        );
        return new GatewayCatalogStore.OperationDefinition(
                UuidV7.simpleString(),
                operation.id(),
                version,
                GatewayRuleCanonicalizer.sha256(bytes(digestMaterial)),
                value.summary(),
                List.copyOf(value.tags()),
                Map.copyOf(value.requestSchema()),
                Map.copyOf(value.responseSchema()),
                List.copyOf(value.errorSchema()),
                value.descriptorSnapshot() == null
                        ? null
                        : Map.copyOf(value.descriptorSnapshot()),
                Map.copyOf(value.attributes()),
                value.externalAccessible(),
                now,
                actorId
        );
    }

    private String operationKey(
            GatewayCatalogStore.InterfaceGroupScope scope,
            ManualOperation command) {
        return switch (command.protocol()) {
            case HTTP -> GatewayOperationKey.http(
                    scope.applicationCode(),
                    required(command.httpMethod(), "httpMethod"),
                    required(command.path(), "path")
            ).value();
            case RPC -> GatewayOperationKey.rpc(
                    scope.applicationCode(),
                    required(command.serviceName(), "serviceName"),
                    defaultValue(command.group(), "default"),
                    defaultValue(command.version(), "1.0.0"),
                    required(command.fullMethodName(), "fullMethodName")
            ).value();
        };
    }

    private String methodIdentity(ManualOperation command) {
        return command.protocol() == Protocol.HTTP
                ? required(command.httpMethod(), "httpMethod").toUpperCase()
                + " " + required(command.path(), "path")
                : required(command.fullMethodName(), "fullMethodName");
    }

    private Map<String, Object> providerIdentity(
            GatewayCatalogStore.InterfaceGroupScope scope,
            ManualOperation command) {
        return Map.of(
                "bizCode", scope.bizCode(),
                "appCode", scope.applicationCode(),
                "env", scope.env(),
                "namespace", scope.namespace(),
                "protocol", command.protocol().name(),
                "serviceName",
                required(command.providerServiceName(), "providerServiceName"),
                "group", defaultValue(command.group(), "default"),
                "version", defaultValue(command.version(), "1.0.0"),
                "transport",
                defaultValue(
                        command.transport(),
                        command.protocol() == Protocol.HTTP ? "HTTP" : "GRPC"
                )
        );
    }

    private GatewayCatalogStore.OperationRecord requiredOperation(String id) {
        return store.findOperation(id)
                .orElseThrow(() -> new GatewayAdminNotFoundException(
                        "gateway operation " + id + " was not found"
                ));
    }

    private GatewayCatalogStore.OperationRecord requiredManualOperation(
            String id) {
        GatewayCatalogStore.OperationRecord operation =
                requiredOperation(id);
        if (!"MANUAL".equals(operation.sourceType())) {
            throw new IllegalArgumentException(
                    "STARTER operation cannot be modified by manual API"
            );
        }
        return operation;
    }

    private void validateHierarchy(
            GatewayCatalogStore.ManualHierarchy value) {
        required(value.businessCode(), "businessCode");
        required(value.businessName(), "businessName");
        required(value.entityCode(), "entityCode");
        required(value.entityName(), "entityName");
        required(value.interfaceGroupCode(), "interfaceGroupCode");
        required(value.interfaceGroupName(), "interfaceGroupName");
    }

    private void validateDefinition(
            String protocol,
            ManualDefinition value) {
        if (value.requestSchema() == null
                || value.responseSchema() == null
                || value.errorSchema() == null
                || value.tags() == null
                || value.attributes() == null) {
            throw new IllegalArgumentException(
                    "definition schema collections are required"
            );
        }
        if ("RPC".equals(protocol) && value.descriptorSnapshot() == null) {
            throw new IllegalArgumentException(
                    "RPC descriptorSnapshot is required"
            );
        }
    }

    private byte[] bytes(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException(
                    "operation definition cannot be serialized",
                    failure
            );
        }
    }

    private void audit(
            AdminActor actor,
            RequestAuditContext request,
            String resourceType,
            String resourceId,
            String action,
            Map<String, Object> after) {
        audits.save(new GatewayAuditLogEntity(
                UuidV7.simpleString(),
                actor.actorId(),
                actor.actorType().name(),
                "MANAGEMENT_API",
                request.requestId(),
                request.traceId(),
                resourceType,
                resourceId,
                action,
                null,
                after,
                null,
                null,
                true,
                null,
                clock.instant()
        ));
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public enum Protocol {
        HTTP,
        RPC
    }

    public record ManualOperation(
            Protocol protocol,
            String httpMethod,
            String path,
            String serviceName,
            String fullMethodName,
            String providerServiceName,
            String group,
            String version,
            String transport,
            boolean externalAccessible,
            ManualDefinition definition
    ) {
    }

    public record ManualDefinition(
            String summary,
            List<String> tags,
            Map<String, Object> requestSchema,
            Map<String, Object> responseSchema,
            List<Map<String, Object>> errorSchema,
            Map<String, Object> descriptorSnapshot,
            Map<String, Object> attributes,
            boolean externalAccessible
    ) {
    }

    public record ManualMetadata(
            String summary,
            List<String> tags,
            String owner
    ) {
    }

    public record OperationDetail(
            GatewayCatalogStore.OperationRecord operation,
            List<GatewayCatalogStore.OperationDefinition> definitions
    ) {
    }
}
