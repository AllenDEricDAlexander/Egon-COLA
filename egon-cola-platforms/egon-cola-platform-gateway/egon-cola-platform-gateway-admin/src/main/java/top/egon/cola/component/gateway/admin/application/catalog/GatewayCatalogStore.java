package top.egon.cola.component.gateway.admin.application.catalog;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface GatewayCatalogStore {

    CatalogTree loadCatalog(String applicationId);

    String createManualHierarchy(
            String applicationId,
            ManualHierarchy hierarchy,
            Instant now);

    Optional<InterfaceGroupScope> findInterfaceGroup(String interfaceGroupId);

    Optional<OperationRecord> findOperation(String operationId);

    Optional<OperationRecord> findOperation(
            String applicationId,
            String operationKey);

    List<OperationDefinition> loadDefinitions(String operationId);

    List<CurrentOperationDefinition> loadCurrentOperationDefinitions(
            String gatewayGroupId);

    void insertOperation(OperationRecord operation);

    void appendDefinition(OperationDefinition definition);

    void pointToDefinition(
            String operationId,
            String definitionId,
            boolean externalAccessible,
            Instant now);

    void deprecate(String operationId, Instant now);

    record ManualHierarchy(
            String businessCode,
            String businessName,
            String entityCode,
            String entityName,
            String interfaceGroupCode,
            String interfaceGroupName,
            String className,
            String description
    ) {
    }

    record InterfaceGroupScope(
            String interfaceGroupId,
            String applicationId,
            String bizCode,
            String applicationCode,
            String env,
            String namespace
    ) {
    }

    record OperationRecord(
            String id,
            String applicationId,
            String interfaceGroupId,
            String operationKey,
            String protocol,
            String methodIdentity,
            boolean externalAccessible,
            Map<String, Object> providerServiceIdentity,
            String sourceType,
            String lifecycleStatus,
            String currentDefinitionId,
            long revision,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    record OperationDefinition(
            String id,
            String operationId,
            long definitionVersion,
            String definitionSha256,
            String summary,
            List<String> tags,
            Map<String, Object> requestSchema,
            Map<String, Object> responseSchema,
            List<Map<String, Object>> errorSchema,
            Map<String, Object> descriptorSnapshot,
            Map<String, Object> attributes,
            boolean externalAccessible,
            Instant createdAt,
            String createdBy
    ) {
    }

    record CurrentOperationDefinition(
            OperationRecord operation,
            OperationDefinition definition
    ) {
    }

    record CatalogTree(
            String applicationId,
            List<BusinessNode> businessDomains
    ) {
    }

    record BusinessNode(
            String id,
            String code,
            String displayName,
            List<EntityNode> entityDomains
    ) {
    }

    record EntityNode(
            String id,
            String code,
            String displayName,
            List<InterfaceGroupNode> interfaceGroups
    ) {
    }

    record InterfaceGroupNode(
            String id,
            String code,
            String displayName,
            String sourceType,
            String className,
            List<OperationNode> operations
    ) {
    }

    record OperationNode(
            String id,
            String operationKey,
            String protocol,
            String methodIdentity,
            boolean externalAccessible,
            String lifecycleStatus,
            String sourceType,
            long revision
    ) {
    }
}
