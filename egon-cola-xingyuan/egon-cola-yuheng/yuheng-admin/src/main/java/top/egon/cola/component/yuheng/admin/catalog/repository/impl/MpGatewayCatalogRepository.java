package top.egon.cola.component.yuheng.admin.catalog.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.component.yuheng.admin.application.domain.po.GatewayApplicationRecordPO;
import top.egon.cola.component.yuheng.admin.application.repository.mp.GatewayApplicationPersistenceRepository;
import top.egon.cola.component.yuheng.admin.catalog.converter.GatewayOperationDefinitionPersistenceConverter;
import top.egon.cola.component.yuheng.admin.catalog.converter.GatewayOperationPersistenceConverter;
import top.egon.cola.component.yuheng.admin.catalog.domain.bo.GatewayOperationBO;
import top.egon.cola.component.yuheng.admin.catalog.domain.bo.GatewayOperationDefinitionBO;
import top.egon.cola.component.yuheng.admin.catalog.domain.dto.GatewayManualHierarchyDTO;
import top.egon.cola.component.yuheng.admin.catalog.domain.po.GatewayBusinessDomainPO;
import top.egon.cola.component.yuheng.admin.catalog.domain.po.GatewayEntityDomainPO;
import top.egon.cola.component.yuheng.admin.catalog.domain.po.GatewayInterfaceGroupPO;
import top.egon.cola.component.yuheng.admin.catalog.domain.po.GatewayOperationDefinitionRecordPO;
import top.egon.cola.component.yuheng.admin.catalog.domain.po.GatewayOperationRecordPO;
import top.egon.cola.component.yuheng.admin.catalog.domain.vo.GatewayCatalogTreeVO;
import top.egon.cola.component.yuheng.admin.catalog.domain.vo.GatewayCurrentOperationDefinitionVO;
import top.egon.cola.component.yuheng.admin.catalog.domain.vo.GatewayInterfaceGroupScopeVO;
import top.egon.cola.component.yuheng.admin.catalog.domain.vo.GatewayOperationNodeVO;
import top.egon.cola.component.yuheng.admin.catalog.repository.GatewayCatalogRepository;
import top.egon.cola.component.yuheng.admin.catalog.repository.mp.GatewayBusinessDomainPersistenceRepository;
import top.egon.cola.component.yuheng.admin.catalog.repository.mp.GatewayEntityDomainPersistenceRepository;
import top.egon.cola.component.yuheng.admin.catalog.repository.mp.GatewayInterfaceGroupPersistenceRepository;
import top.egon.cola.component.yuheng.admin.catalog.repository.mp.GatewayOperationDefinitionPersistenceRepository;
import top.egon.cola.component.yuheng.admin.catalog.repository.mp.GatewayOperationPersistenceRepository;
import top.egon.cola.component.yuheng.admin.group.domain.po.GatewayGroupRecordPO;
import top.egon.cola.component.yuheng.admin.group.repository.mp.GatewayGroupPersistenceRepository;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminNotFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 中文说明：{@code MpGatewayCatalogRepository} 是网关目录存储的 MyBatis-Plus 实现，取代原手写 JDBC 仓储；它只经由各表的受守卫持久化仓储与 MapStruct 转换器访问 {@code gateway_operation}/{@code gateway_operation_definition} 及目录层级表，保持原有的作用域过滤、活跃读取、修订号 CAS、排序与错误语义。
 * English summary: {@code MpGatewayCatalogRepository} is the MyBatis-Plus gateway catalog store replacing the hand-written JDBC repository; it reaches {@code gateway_operation}/{@code gateway_operation_definition} and the hierarchy tables only through the guarded per-table persistence repositories and the MapStruct converters, preserving the original scope filtering, active-only reads, revision CAS, ordering and error semantics.
 *
 * 用法 / Usage: 由 Spring 容器注入到网关命令/查询服务；/ Injected by the Spring container into the gateway command/query services; the port {@code GatewayCatalogRepository} is the only supported boundary, and no {@code *RecordPO} crosses it.
 */
@Slf4j
@Validated
@Repository("mpGatewayCatalogRepository")
@RequiredArgsConstructor
public class MpGatewayCatalogRepository implements GatewayCatalogRepository {

    /** 目录内网关操作及其当前定义的受守卫持久化仓储。/ Guarded store for gateway_operation rows. */
    @Qualifier("gatewayOperationPersistenceRepository")
    private final GatewayOperationPersistenceRepository operationRepository;

    /** 操作定义版本的受守卫持久化仓储。/ Guarded store for gateway_operation_definition rows. */
    @Qualifier("gatewayOperationDefinitionPersistenceRepository")
    private final GatewayOperationDefinitionPersistenceRepository definitionRepository;

    /** 接口分组层的受守卫持久化仓储。/ Guarded store for gateway_interface_group rows. */
    @Qualifier("gatewayInterfaceGroupPersistenceRepository")
    private final GatewayInterfaceGroupPersistenceRepository interfaceGroupRepository;

    /** 业务域层的受守卫持久化仓储。/ Guarded store for gateway_business_domain rows. */
    @Qualifier("gatewayBusinessDomainPersistenceRepository")
    private final GatewayBusinessDomainPersistenceRepository businessDomainRepository;

    /** 实体域层的受守卫持久化仓储。/ Guarded store for gateway_entity_domain rows. */
    @Qualifier("gatewayEntityDomainPersistenceRepository")
    private final GatewayEntityDomainPersistenceRepository entityDomainRepository;

    /** 网关应用受守卫持久化仓储，用于目录作用域解析与存在性校验。/ Guarded store for gateway_application, used for scope resolution and existence checks. */
    @Qualifier("gatewayApplicationPersistenceRepository")
    private final GatewayApplicationPersistenceRepository applicationRepository;

    /** 网关发布组受守卫持久化仓储，用于当前定义查询的 env/namespace 解析。/ Guarded store for gateway_group, used to resolve env/namespace for the current-definition query. */
    @Qualifier("gatewayGroupPersistenceRepository")
    private final GatewayGroupPersistenceRepository gatewayGroupRepository;

    /** {@code GatewayOperationBO} 与 {@code GatewayOperationRecordPO} 的双向转换器。/ Bidirectional converter between GatewayOperationBO and GatewayOperationRecordPO. */
    @Qualifier("gatewayOperationPersistenceConverter")
    private final GatewayOperationPersistenceConverter operationConverter;

    /** {@code GatewayOperationDefinitionBO} 与 {@code GatewayOperationDefinitionRecordPO} 的双向转换器。/ Bidirectional converter between GatewayOperationDefinitionBO and GatewayOperationDefinitionRecordPO. */
    @Qualifier("gatewayOperationDefinitionPersistenceConverter")
    private final GatewayOperationDefinitionPersistenceConverter definitionConverter;

    /**
     * 中文说明：执行 load目录 操作；按业务/实体/分组/操作四层展开目录树，等价于原 JDBC 四表 LEFT JOIN 且保持 b.code、e.code、g.code、o.operation_key 的排序。
     * English summary: Executes the load catalog operation; expands the catalog tree across business/entity/group/operation, equivalent to the original four-table LEFT JOIN while preserving the b.code, e.code, g.code, o.operation_key ordering.
     *
     * 用法 / Usage: {@code MpGatewayCatalogRepository.loadCatalog(...)}。
     * @param applicationId 参数 applicationId；parameter application id。
     * @return 返回组装后的目录树；returns the assembled catalog tree.
     */
    @Override
    public GatewayCatalogTreeVO loadCatalog(String applicationId) {
        Long application = toLong(applicationId);
        Map<String, GatewayCatalogMutableBusiness> businesses = new LinkedHashMap<>();
        for (GatewayBusinessDomainPO businessRow : businessDomainRepository.list(
                Wrappers.<GatewayBusinessDomainPO>lambdaQuery()
                        .eq(GatewayBusinessDomainPO::getApplicationId, application)
                        .orderByAsc(GatewayBusinessDomainPO::getCode))) {
            GatewayCatalogMutableBusiness business = new GatewayCatalogMutableBusiness(
                    text(businessRow.getId()),
                    businessRow.getCode(),
                    businessRow.getDisplayName());
            businesses.put(text(businessRow.getId()), business);
            for (GatewayEntityDomainPO entityRow : entityDomainRepository.list(
                    Wrappers.<GatewayEntityDomainPO>lambdaQuery()
                            .eq(GatewayEntityDomainPO::getBusinessDomainId, businessRow.getId())
                            .orderByAsc(GatewayEntityDomainPO::getCode))) {
                GatewayCatalogMutableEntity entity = new GatewayCatalogMutableEntity(
                        text(entityRow.getId()),
                        entityRow.getCode(),
                        entityRow.getDisplayName());
                business.entities.put(text(entityRow.getId()), entity);
                for (GatewayInterfaceGroupPO groupRow : interfaceGroupRepository.list(
                        Wrappers.<GatewayInterfaceGroupPO>lambdaQuery()
                                .eq(GatewayInterfaceGroupPO::getEntityDomainId, entityRow.getId())
                                .orderByAsc(GatewayInterfaceGroupPO::getCode))) {
                    GatewayCatalogMutableGroup group = new GatewayCatalogMutableGroup(
                            text(groupRow.getId()),
                            groupRow.getCode(),
                            groupRow.getDisplayName(),
                            groupRow.getSourceType(),
                            groupRow.getClassName());
                    entity.groups.put(text(groupRow.getId()), group);
                    for (GatewayOperationRecordPO operationRow : operationRepository.list(
                            Wrappers.<GatewayOperationRecordPO>lambdaQuery()
                                    .eq(GatewayOperationRecordPO::getInterfaceGroupId, groupRow.getId())
                                    .orderByAsc(GatewayOperationRecordPO::getOperationKey))) {
                        group.operations.add(new GatewayOperationNodeVO(
                                text(operationRow.getId()),
                                operationRow.getOperationKey(),
                                operationRow.getProtocol(),
                                operationRow.getMethodIdentity(),
                                Boolean.TRUE.equals(operationRow.getExternalAccessible()),
                                operationRow.getLifecycleStatus(),
                                operationRow.getSourceType(),
                                operationRow.getRevision() == null ? 0L : operationRow.getRevision()));
                    }
                }
            }
        }
        return new GatewayCatalogTreeVO(
                applicationId,
                businesses.values().stream()
                        .map(GatewayCatalogMutableBusiness::freeze)
                        .toList()
        );
    }

    /**
     * 中文说明：执行 createManualHierarchy 操作；先校验应用存在，再查找或创建业务域、实体域，最后写入 MANUAL 接口分组并返回其 id。
     * English summary: Executes the create manual hierarchy operation; requires the application, finds or creates the business and entity domains, then inserts the MANUAL interface group and returns its id.
     *
     * 用法 / Usage: {@code MpGatewayCatalogRepository.createManualHierarchy(...)}。
     * @param applicationId 参数 applicationId；parameter application id。
     * @param hierarchy 参数 hierarchy；parameter hierarchy。
     * @param now 参数 now；parameter now (audit timestamps are owned by the guarded persistence layer).
     * @return 返回新建接口分组 id；returns the created interface group id.
     */
    @Override
    public String createManualHierarchy(
            String applicationId,
            GatewayManualHierarchyDTO hierarchy,
            Instant now) {
        requireApplication(applicationId);
        String businessId = findOrCreateBusiness(applicationId, hierarchy);
        String entityId = findOrCreateEntity(businessId, hierarchy);
        String interfaceGroupId = SnowflakeIdGenerator.nextId();
        GatewayInterfaceGroupPO group = new GatewayInterfaceGroupPO();
        group.setId(toLong(interfaceGroupId));
        group.setEntityDomainId(toLong(entityId));
        group.setCode(hierarchy.interfaceGroupCode());
        group.setDisplayName(hierarchy.interfaceGroupName());
        group.setSourceType("MANUAL");
        group.setClassName(hierarchy.className());
        group.setDescription(hierarchy.description());
        interfaceGroupRepository.save(group);
        return interfaceGroupId;
    }

    /**
     * 中文说明：执行 find接口Group 操作；沿分组→实体→业务→应用逐级受守卫读取以还原原 JDBC 四表连接的作用域。
     * English summary: Executes the find interface group operation; walks group→entity→business→application via guarded reads to reconstruct the original four-table join scope.
     *
     * 用法 / Usage: {@code MpGatewayCatalogRepository.findInterfaceGroup(...)}。
     * @param interfaceGroupId 参数 接口GroupId；parameter interface group id。
     * @return 返回分组所属应用作用域；returns the application scope of the group.
     */
    @Override
    public Optional<GatewayInterfaceGroupScopeVO> findInterfaceGroup(String interfaceGroupId) {
        GatewayInterfaceGroupPO group = interfaceGroupRepository.getById(toLong(interfaceGroupId));
        if (group == null) {
            return Optional.empty();
        }
        GatewayEntityDomainPO entity = entityDomainRepository.getById(group.getEntityDomainId());
        if (entity == null) {
            return Optional.empty();
        }
        GatewayBusinessDomainPO business = businessDomainRepository.getById(entity.getBusinessDomainId());
        if (business == null) {
            return Optional.empty();
        }
        GatewayApplicationRecordPO application = applicationRepository.getById(business.getApplicationId());
        if (application == null) {
            return Optional.empty();
        }
        return Optional.of(new GatewayInterfaceGroupScopeVO(
                text(group.getId()),
                text(application.getId()),
                application.getBizCode(),
                application.getApplicationCode(),
                application.getEnv(),
                application.getNamespace()
        ));
    }

    /**
     * 中文说明：执行 find操作 操作；按操作 id 读取活跃操作行并经转换器投影为业务载体。
     * English summary: Executes the find operation operation; loads the active operation row by id and projects it to the business carrier through the converter.
     *
     * 用法 / Usage: {@code MpGatewayCatalogRepository.findOperation(...)}。
     * @param operationId 参数 操作Id；parameter operation id。
     * @return 返回操作业务载体；returns the operation carrier when present.
     */
    @Override
    public Optional<GatewayOperationBO> findOperation(String operationId) {
        return Optional.ofNullable(operationRepository.getById(toLong(operationId)))
                .map(operationConverter::toTarget);
    }

    /**
     * 中文说明：执行 find操作 操作；按应用与 operation_key 读取操作行，等价于原 JDBC 的首行选择。
     * English summary: Executes the find operation operation; loads operations by application and operation_key, mirroring the original JDBC first-row selection.
     *
     * 用法 / Usage: {@code MpGatewayCatalogRepository.findOperation(...)}。
     * @param applicationId 参数 applicationId；parameter application id。
     * @param operationKey 参数 操作键；parameter operation key。
     * @return 返回操作业务载体；returns the operation carrier when present.
     */
    @Override
    public Optional<GatewayOperationBO> findOperation(
            String applicationId,
            String operationKey) {
        return operationRepository.list(
                        Wrappers.<GatewayOperationRecordPO>lambdaQuery()
                                .eq(GatewayOperationRecordPO::getApplicationId, toLong(applicationId))
                                .eq(GatewayOperationRecordPO::getOperationKey, operationKey))
                .stream()
                .findFirst()
                .map(operationConverter::toTarget);
    }

    /**
     * 中文说明：执行 loadDefinitions 操作；按 operation_id 读取定义版本并按 definition_version 倒序返回。
     * English summary: Executes the load definitions operation; reads definitions by operation_id ordered by definition_version descending.
     *
     * 用法 / Usage: {@code MpGatewayCatalogRepository.loadDefinitions(...)}。
     * @param operationId 参数 操作Id；parameter operation id。
     * @return 返回定义业务载体列表；returns the definition carriers.
     */
    @Override
    public List<GatewayOperationDefinitionBO> loadDefinitions(String operationId) {
        return definitionConverter.toTargetList(definitionRepository.list(
                Wrappers.<GatewayOperationDefinitionRecordPO>lambdaQuery()
                        .eq(GatewayOperationDefinitionRecordPO::getOperationId, toLong(operationId))
                        .orderByDesc(GatewayOperationDefinitionRecordPO::getDefinitionVersion)));
    }

    /**
     * 中文说明：执行 loadCurrent操作Definitions 操作；先解析发布组的 env/namespace，再按应用与操作键顺序读取非 OFFLINE 且存在当前定义的操作。
     * English summary: Executes the load current operation definitions operation; resolves the release group's env/namespace, then reads non-OFFLINE operations that carry a current definition ordered by application_code and operation_key.
     *
     * 用法 / Usage: {@code MpGatewayCatalogRepository.loadCurrentOperationDefinitions(...)}。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回当前操作定义投影列表；returns the current operation-definition projections.
     */
    @Override
    public List<GatewayCurrentOperationDefinitionVO> loadCurrentOperationDefinitions(
            String gatewayGroupId) {
        GatewayGroupRecordPO releaseGroup = gatewayGroupRepository.getById(toLong(gatewayGroupId));
        if (releaseGroup == null) {
            return List.of();
        }
        List<GatewayCurrentOperationDefinitionVO> results = new ArrayList<>();
        for (GatewayApplicationRecordPO application : applicationRepository.list(
                Wrappers.<GatewayApplicationRecordPO>lambdaQuery()
                        .eq(GatewayApplicationRecordPO::getEnv, releaseGroup.getEnv())
                        .eq(GatewayApplicationRecordPO::getNamespace, releaseGroup.getNamespace())
                        .orderByAsc(GatewayApplicationRecordPO::getApplicationCode))) {
            for (GatewayOperationRecordPO operation : operationRepository.list(
                    Wrappers.<GatewayOperationRecordPO>lambdaQuery()
                            .eq(GatewayOperationRecordPO::getApplicationId, application.getId())
                            .ne(GatewayOperationRecordPO::getLifecycleStatus, "OFFLINE")
                            .orderByAsc(GatewayOperationRecordPO::getOperationKey))) {
                Long currentDefinitionId = operation.getCurrentDefinitionId();
                if (currentDefinitionId == null) {
                    continue;
                }
                GatewayOperationDefinitionRecordPO definition =
                        definitionRepository.getById(currentDefinitionId);
                if (definition == null) {
                    continue;
                }
                results.add(new GatewayCurrentOperationDefinitionVO(
                        operationConverter.toTarget(operation),
                        definitionConverter.toTarget(definition)));
            }
        }
        return results;
    }

    /**
     * 中文说明：执行 insert操作 操作；将操作业务载体转换后由受守卫仓储落库，主键与审计/版本列由持久边界负责。
     * English summary: Executes the insert operation operation; converts the carrier and saves it through the guarded store, with primary key and audit/version columns owned by the persistence boundary.
     *
     * 用法 / Usage: {@code MpGatewayCatalogRepository.insertOperation(...)}。
     * @param operation 参数 操作；parameter operation。
     */
    @Override
    public void insertOperation(GatewayOperationBO operation) {
        operationRepository.save(operationConverter.newRow(operation));
    }

    /**
     * 中文说明：执行 append定义 操作；将定义业务载体转换后由受守卫仓储追加落库。
     * English summary: Executes the append definition operation; converts the carrier and saves it through the guarded store.
     *
     * 用法 / Usage: {@code MpGatewayCatalogRepository.appendDefinition(...)}。
     * @param definition 参数 定义；parameter definition。
     */
    @Override
    public void appendDefinition(GatewayOperationDefinitionBO definition) {
        definitionRepository.save(definitionConverter.newRow(definition));
    }

    /**
     * 中文说明：执行 pointTo定义 操作；载入活跃操作行、写入当前定义指向并自增业务 revision，随后走受守卫 CAS 更新；0 行视为未找到。
     * English summary: Executes the point to definition operation; loads the active operation row, sets the current-definition pointer and increments the business revision, then performs a guarded CAS update; zero rows are treated as not found.
     *
     * 用法 / Usage: {@code MpGatewayCatalogRepository.pointToDefinition(...)}。
     * @param operationId 参数 操作Id；parameter operation id。
     * @param definitionId 参数 定义Id；parameter definition id。
     * @param externalAccessible 参数 externalAccessible；parameter external accessible.
     * @param now 参数 now；parameter now (update timestamp is owned by the guarded persistence layer).
     */
    @Override
    public void pointToDefinition(
            String operationId,
            String definitionId,
            boolean externalAccessible,
            Instant now) {
        GatewayOperationRecordPO operation = operationRepository.getById(toLong(operationId));
        if (operation == null) {
            throw notFound(operationId);
        }
        operation.setCurrentDefinitionId(toLong(definitionId));
        operation.setExternalAccessible(externalAccessible);
        operation.setLifecycleStatus("ACTIVE");
        operation.setRevision(nextRevision(operation.getRevision()));
        if (!operationRepository.updateById(operation)) {
            throw notFound(operationId);
        }
    }

    /**
     * 中文说明：执行 deprecate 操作；载入活跃操作行、置为 DEPRECATED 并写入 deprecated_at 与自增 revision，随后走受守卫 CAS 更新；0 行视为未找到。
     * English summary: Executes the deprecate operation; loads the active operation row, marks it DEPRECATED with deprecated_at and an incremented revision, then performs a guarded CAS update; zero rows are treated as not found.
     *
     * 用法 / Usage: {@code MpGatewayCatalogRepository.deprecate(...)}。
     * @param operationId 参数 操作Id；parameter operation id。
     * @param now 参数 now；parameter now (bound to the deprecated_at business column).
     */
    @Override
    public void deprecate(String operationId, Instant now) {
        GatewayOperationRecordPO operation = operationRepository.getById(toLong(operationId));
        if (operation == null) {
            throw notFound(operationId);
        }
        operation.setLifecycleStatus("DEPRECATED");
        operation.setDeprecatedAt(now);
        operation.setRevision(nextRevision(operation.getRevision()));
        if (!operationRepository.updateById(operation)) {
            throw notFound(operationId);
        }
    }

    /**
     * 中文说明：校验网关应用存在且活跃，等价于原 JDBC 的 count 检查。
     * English summary: Requires the gateway application to exist and be active, equivalent to the original JDBC count check.
     * @param applicationId 参数 applicationId；parameter application id。
     */
    private void requireApplication(String applicationId) {
        if (applicationRepository.count(
                Wrappers.<GatewayApplicationRecordPO>lambdaQuery()
                        .eq(GatewayApplicationRecordPO::getId, toLong(applicationId))) == 0) {
            throw new GatewayAdminNotFoundException(
                    "gateway application " + applicationId + " was not found"
            );
        }
    }

    /**
     * 中文说明：按应用与业务编码查找活跃业务域，缺失时以雪花 id 新建。
     * English summary: Finds an active business domain by application and code, creating one with a snowflake id when absent.
     * @param applicationId 参数 applicationId；parameter application id。
     * @param hierarchy 参数 hierarchy；parameter hierarchy。
     * @return 返回业务域 id；returns the business domain id.
     */
    private String findOrCreateBusiness(String applicationId, GatewayManualHierarchyDTO hierarchy) {
        Long application = toLong(applicationId);
        List<GatewayBusinessDomainPO> existing = businessDomainRepository.list(
                Wrappers.<GatewayBusinessDomainPO>lambdaQuery()
                        .eq(GatewayBusinessDomainPO::getApplicationId, application)
                        .eq(GatewayBusinessDomainPO::getCode, hierarchy.businessCode()));
        if (!existing.isEmpty()) {
            return text(existing.get(0).getId());
        }
        String id = SnowflakeIdGenerator.nextId();
        GatewayBusinessDomainPO business = new GatewayBusinessDomainPO();
        business.setId(toLong(id));
        business.setApplicationId(application);
        business.setCode(hierarchy.businessCode());
        business.setDisplayName(hierarchy.businessName());
        business.setDescription(null);
        businessDomainRepository.save(business);
        return id;
    }

    /**
     * 中文说明：按业务域与实体编码查找活跃实体域，缺失时以雪花 id 新建。
     * English summary: Finds an active entity domain by business domain and code, creating one with a snowflake id when absent.
     * @param businessId 参数 businessId；parameter business id.
     * @param hierarchy 参数 hierarchy；parameter hierarchy.
     * @return 返回实体域 id；returns the entity domain id.
     */
    private String findOrCreateEntity(String businessId, GatewayManualHierarchyDTO hierarchy) {
        Long business = toLong(businessId);
        List<GatewayEntityDomainPO> existing = entityDomainRepository.list(
                Wrappers.<GatewayEntityDomainPO>lambdaQuery()
                        .eq(GatewayEntityDomainPO::getBusinessDomainId, business)
                        .eq(GatewayEntityDomainPO::getCode, hierarchy.entityCode()));
        if (!existing.isEmpty()) {
            return text(existing.get(0).getId());
        }
        String id = SnowflakeIdGenerator.nextId();
        GatewayEntityDomainPO entity = new GatewayEntityDomainPO();
        entity.setId(toLong(id));
        entity.setBusinessDomainId(business);
        entity.setCode(hierarchy.entityCode());
        entity.setDisplayName(hierarchy.entityName());
        entity.setDescription(null);
        entityDomainRepository.save(entity);
        return id;
    }

    /**
     * 中文说明：构造与原 JDBC 完全一致的操作未找到异常。
     * English summary: Builds the operation-not-found exception with exactly the original JDBC message.
     * @param operationId 参数 操作Id；parameter operation id.
     * @return 返回异常；returns the exception.
     */
    private static GatewayAdminNotFoundException notFound(String operationId) {
        return new GatewayAdminNotFoundException(
                "gateway operation " + operationId + " was not found"
        );
    }

    /**
     * 中文说明：在业务 revision 上自增 1，null 视为 0，与原 SQL 的 revision = revision + 1 对齐。
     * English summary: Increments the business revision by one treating null as zero, matching the original SQL revision = revision + 1.
     * @param revision 参数 修订；parameter revision.
     * @return 返回自增后的修订；returns the incremented revision.
     */
    private static Long nextRevision(Long revision) {
        return (revision == null ? 0L : revision) + 1L;
    }

    /**
     * 中文说明：将端口边界上的字符串 id 解析为持久层数值主键。
     * English summary: Parses the port-boundary string id into the numeric persistence primary key.
     * @param value 参数 值；parameter value.
     * @return 返回数值 id；returns the numeric id or null.
     */
    private static Long toLong(String value) {
        return value == null ? null : Long.valueOf(value);
    }

    /**
     * 中文说明：将持久层数值主键投影为端口边界上的不透明字符串 id。
     * English summary: Projects the numeric persistence primary key to the opaque string id exposed on the port boundary.
     * @param value 参数 值；parameter value.
     * @return 返回字符串 id；returns the string id or null.
     */
    private static String text(Long value) {
        return value == null ? null : String.valueOf(value);
    }

}
