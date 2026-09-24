package top.egon.cola.component.yuheng.admin.reporting.repository.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.component.yuheng.admin.catalog.domain.po.GatewayBusinessDomainPO;
import top.egon.cola.component.yuheng.admin.catalog.domain.po.GatewayEntityDomainPO;
import top.egon.cola.component.yuheng.admin.catalog.domain.po.GatewayInterfaceGroupPO;
import top.egon.cola.component.yuheng.admin.catalog.domain.po.GatewayOperationDefinitionRecordPO;
import top.egon.cola.component.yuheng.admin.catalog.domain.po.GatewayOperationRecordPO;
import top.egon.cola.component.yuheng.admin.catalog.repository.mp.GatewayBusinessDomainPersistenceRepository;
import top.egon.cola.component.yuheng.admin.catalog.repository.mp.GatewayEntityDomainPersistenceRepository;
import top.egon.cola.component.yuheng.admin.catalog.repository.mp.GatewayInterfaceGroupPersistenceRepository;
import top.egon.cola.component.yuheng.admin.catalog.repository.mp.GatewayOperationDefinitionPersistenceRepository;
import top.egon.cola.component.yuheng.admin.catalog.repository.mp.GatewayOperationPersistenceRepository;
import top.egon.cola.component.yuheng.admin.reporting.domain.bo.GatewayStoredReportBO;
import top.egon.cola.component.yuheng.admin.reporting.domain.po.GatewayDefinitionSetPO;
import top.egon.cola.component.yuheng.admin.reporting.domain.po.GatewayDefinitionSetOperationPO;
import top.egon.cola.component.yuheng.admin.reporting.repository.GatewayDefinitionReportRepository;
import top.egon.cola.component.yuheng.admin.reporting.repository.mp.GatewayDefinitionSetOperationPersistenceRepository;
import top.egon.cola.component.yuheng.admin.reporting.repository.mp.GatewayDefinitionSetPersistenceRepository;
import top.egon.cola.component.yuheng.contract.reporting.GatewayDefinitionSourceTypeEnum;
import top.egon.cola.component.yuheng.contract.reporting.GatewayInterfaceDefinitionReport;
import top.egon.cola.component.yuheng.contract.reporting.GatewayInterfaceDefinitionReportResult;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 中文说明：{@code MpGatewayDefinitionReportRepository} 是网关定义上报端口的 MyBatis-Plus 门面实现，逐方法替换被删除的手写 JDBC 仓储：
 * 它不写裸 SQL、不引入 JdbcTemplate 或 mapper XML，所有读写只经由各表的受守卫 {@code EgonColaRepository} 持久化边界
 * （租户过滤、{@code deleted_at IS NULL} 活跃读取、乐观锁 CAS、版本化软删），jsonb 列改由 {@code GatewayJsonbTypeHandler}
 * 直接绑定结构化节点，取代原 {@code ?::jsonb} 文本转换。原 {@code SELECT DISTINCT}／聚合连接／{@code ON CONFLICT DO NOTHING}
 * 被分解为同作用域、同排序的受守卫单表读取加内存组装；定义集入库沿业务域→实体域→分组→操作逐级查找或创建，
 * 主键统一由雪花 id 显式分配，业务 {@code revision} 与 MP {@code version} 互不替代，0 行写入永不记为成功，
 * 来源与分组冲突沿用原有 {@code IllegalStateException} 文本码。
 * English summary: {@code MpGatewayDefinitionReportRepository} is the MyBatis-Plus facade implementing the gateway definition-report
 * port, replacing the deleted hand-written JDBC repository method by method. It issues no raw SQL and adds no JdbcTemplate or mapper
 * XML: every read and write goes through the table-specific guarded {@code EgonColaRepository} boundary (tenant filtering, active-only
 * {@code deleted_at IS NULL} reads, optimistic-lock CAS and versioned soft delete), while jsonb columns bind structured nodes through
 * {@code GatewayJsonbTypeHandler} instead of the legacy {@code ?::jsonb} text casts. The original {@code SELECT DISTINCT}, the aggregate
 * join and {@code ON CONFLICT DO NOTHING} are decomposed into same-scope, same-ordering guarded single-table reads assembled in memory;
 * ingestion walks business domain → entity domain → interface group → operation, finding or creating each level. Primary keys are
 * assigned explicitly from the Snowflake generator, the business {@code revision} stays distinct from the MP {@code version}, a
 * zero-row write is never counted as success, and source/group conflicts keep the pre-existing textual {@code IllegalStateException}
 * codes.
 *
 * 用法 / Usage: 经端口 {@code GatewayDefinitionReportRepository} 注入到上报与聚合入库服务，整批写入必须留在调用方事务内；
 * 入参 {@code now} 仍绑定到定义集的业务列 {@code received_at}/{@code completed_at}，而原各表 {@code created_at}/{@code updated_at}
 * 与 {@code gateway_operation_definition.created_by} 已由持久边界的审计与租户列统一承载。/ Inject it through the
 * {@code GatewayDefinitionReportRepository} port into the reporting and aggregation ingestion services and keep the whole batch inside
 * the caller's transaction. The {@code now} argument still binds to the definition-set business columns
 * {@code received_at}/{@code completed_at}, while the legacy per-table {@code created_at}/{@code updated_at} and
 * {@code gateway_operation_definition.created_by} are now carried by the persistence boundary's audit and tenant columns.
 */
@Slf4j
@Validated
@Repository("mpGatewayDefinitionReportRepository")
@RequiredArgsConstructor
public class MpGatewayDefinitionReportRepository implements GatewayDefinitionReportRepository {

    /** 定义集入库后的原 wire 状态字符串。/ Original wire status of an ingested definition set. */
    private static final String VERIFIED_STATUS = "VERIFIED";

    /** 新指向定义的操作原 wire 生命周期状态。/ Original lifecycle status of an operation pointing at a fresh definition. */
    private static final String DISCOVERED_STATUS = "DISCOVERED";

    /** 允许被上报重新指向的操作下线态原 wire 字符串。/ Original wire status of an offlined operation a report may re-point. */
    private static final String OFFLINE_STATUS = "OFFLINE";

    /** starter 操作计数使用的来源类型原 wire 字符串。/ Source-type wire string counted as starter operations. */
    private static final String RPC_DESCRIPTOR_SOURCE = "RPC_DESCRIPTOR";

    /** 迁移历史 HTTP 来源时校验的协议原 wire 字符串。/ Protocol wire string checked when migrating a legacy HTTP source. */
    private static final String HTTP_PROTOCOL = "HTTP";

    /** 协议不唯一时定义集使用的原 wire 汇总值。/ Wire summary value used when a report spans several protocols. */
    private static final String MIXED_PROTOCOL = "MIXED";

    /** {@code gateway_definition_set} 的受守卫持久化边界。/ Guarded store for gateway_definition_set rows. */
    @Qualifier("gatewayDefinitionSetPersistenceRepository")
    private final GatewayDefinitionSetPersistenceRepository definitionSetRepository;

    /** {@code gateway_definition_set_operation} 成员关系的受守卫持久化边界。/ Guarded store for definition-set membership rows. */
    @Qualifier("gatewayDefinitionSetOperationPersistenceRepository")
    private final GatewayDefinitionSetOperationPersistenceRepository membershipRepository;

    /** {@code gateway_business_domain} 的受守卫持久化边界。/ Guarded store for gateway_business_domain rows. */
    @Qualifier("gatewayBusinessDomainPersistenceRepository")
    private final GatewayBusinessDomainPersistenceRepository businessDomainRepository;

    /** {@code gateway_entity_domain} 的受守卫持久化边界。/ Guarded store for gateway_entity_domain rows. */
    @Qualifier("gatewayEntityDomainPersistenceRepository")
    private final GatewayEntityDomainPersistenceRepository entityDomainRepository;

    /** {@code gateway_interface_group} 的受守卫持久化边界。/ Guarded store for gateway_interface_group rows. */
    @Qualifier("gatewayInterfaceGroupPersistenceRepository")
    private final GatewayInterfaceGroupPersistenceRepository interfaceGroupRepository;

    /** {@code gateway_operation} 的受守卫持久化边界。/ Guarded store for gateway_operation rows. */
    @Qualifier("gatewayOperationPersistenceRepository")
    private final GatewayOperationPersistenceRepository operationRepository;

    /** {@code gateway_operation_definition} 的受守卫持久化边界。/ Guarded store for gateway_operation_definition rows. */
    @Qualifier("gatewayOperationDefinitionPersistenceRepository")
    private final GatewayOperationDefinitionPersistenceRepository definitionRepository;

    /** 与旧实现同一实例的 Jackson 序列化器，用于把结构化列值交给 jsonb 类型处理器。/ The same Jackson serializer the legacy code injected, used to hand structured column values to the jsonb type handler. */
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    /** 计算定义摘要使用的规范序列化器：属性与映射键按字典序固定，保证摘要稳定。/ Canonical serializer behind the definition digest: properties and map keys are alphabetically ordered so the digest stays stable. */
    private final ObjectMapper canonicalMapper = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();

    /**
     * 中文说明：执行 findBuildFingerprint 操作；等价于原
     * {@code SELECT DISTINCT fingerprint FROM gateway_definition_set WHERE application_id = ? AND build_id = ?} 后取首行，
     * 受守卫读取额外带租户与未软删谓词，并以指纹升序固定原结果集的不确定顺序。
     * English summary: Executes the findBuildFingerprint operation; equal to taking the first row of the original
     * {@code SELECT DISTINCT fingerprint FROM gateway_definition_set WHERE application_id = ? AND build_id = ?}. The guarded read
     * additionally carries the tenant and not-soft-deleted predicates and pins the previously unordered result by ascending fingerprint.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayDefinitionReportRepository.findBuildFingerprint(applicationId, buildId)}。
     * @param applicationId 参数 应用 id；parameter application id。
     * @param buildId 参数 构建 id；parameter build id。
     * @return 返回构建指纹或空；returns the build fingerprint or empty.
     */
    @Override
    public Optional<String> findBuildFingerprint(
            String applicationId,
            String buildId) {
        return firstFingerprint(
                applicationId,
                Wrappers.<GatewayDefinitionSetPO>lambdaQuery()
                        .eq(GatewayDefinitionSetPO::getBuildId, buildId));
    }

    /**
     * 中文说明：执行 findBuildFingerprint 操作；在应用与构建之外按 {@code protocol} 再限定一层作用域，等价于原
     * {@code SELECT DISTINCT fingerprint ... AND protocol = ?}；形参 {@code sourceScope} 与原 SQL 一致地不参与过滤，
     * 仅保留上报来源作用域的入参形状。
     * English summary: Executes the findBuildFingerprint operation scoped by {@code protocol} besides application and build, equal to the
     * original {@code SELECT DISTINCT fingerprint ... AND protocol = ?}; as in the original SQL the {@code sourceScope} parameter is not
     * a filter and only keeps the caller's input shape.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayDefinitionReportRepository.findBuildFingerprint(applicationId, buildId, protocol,
     * sourceScope)}。
     * @param applicationId 参数 应用 id；parameter application id。
     * @param buildId 参数 构建 id；parameter build id。
     * @param protocol 参数 协议；parameter protocol.
     * @param sourceScope 参数 上报来源作用域，不参与过滤；parameter reporting source scope, not a filter.
     * @return 返回构建指纹或空；returns the build fingerprint or empty.
     */
    @Override
    public Optional<String> findBuildFingerprint(
            String applicationId,
            String buildId,
            String protocol,
            String sourceScope) {
        return firstFingerprint(
                applicationId,
                Wrappers.<GatewayDefinitionSetPO>lambdaQuery()
                        .eq(GatewayDefinitionSetPO::getBuildId, buildId)
                        .eq(GatewayDefinitionSetPO::getProtocol, protocol));
    }

    /**
     * 中文说明：执行 definitionSetExists 操作；等价于原
     * {@code SELECT COUNT(*) FROM gateway_definition_set WHERE application_id = ? AND id = ?} 的正则化存在性判定，
     * 迁移后主键是数值列，无法定位主键的标识按不存在如实返回（原 SQL 同样命中 0 行）。
     * English summary: Executes the definitionSetExists operation; the regularized existence verdict of the original
     * {@code SELECT COUNT(*) FROM gateway_definition_set WHERE application_id = ? AND id = ?}. The migrated primary key is numeric, so
     * an identifier that cannot address it is reported as absent, exactly where the original SQL matched zero rows.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayDefinitionReportRepository.definitionSetExists(applicationId, definitionSetId)}。
     * @param applicationId 参数 应用 id；parameter application id。
     * @param definitionSetId 参数 定义集 id；parameter definition set id。
     * @return 返回定义集是否存在；returns whether the definition set exists.
     */
    @Override
    public boolean definitionSetExists(
            String applicationId,
            String definitionSetId) {
        Long application = columnValue(applicationId);
        Long definitionSet = columnValue(definitionSetId);
        if (application == null || definitionSet == null) {
            return false;
        }
        return definitionSetRepository.exists(
                Wrappers.<GatewayDefinitionSetPO>lambdaQuery()
                        .eq(GatewayDefinitionSetPO::getApplicationId, application)
                        .eq(GatewayDefinitionSetPO::getId, definitionSet));
    }

    /**
     * 中文说明：执行 countStarterOperations 操作；等价于原
     * {@code SELECT COUNT(*) FROM gateway_operation WHERE application_id = ? AND source_type = 'RPC_DESCRIPTOR'}，
     * 受守卫计数额外带租户与未软删谓词。
     * English summary: Executes the countStarterOperations operation; equal to the original
     * {@code SELECT COUNT(*) FROM gateway_operation WHERE application_id = ? AND source_type = 'RPC_DESCRIPTOR'}, with the guarded count
     * additionally carrying the tenant and not-soft-deleted predicates.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayDefinitionReportRepository.countStarterOperations(applicationId)}。
     * @param applicationId 参数 应用 id；parameter application id.
     * @return 返回描述符来源操作数；returns the descriptor-backed operation count.
     */
    @Override
    public int countStarterOperations(String applicationId) {
        Long application = columnValue(applicationId);
        if (application == null) {
            return 0;
        }
        return (int) operationRepository.count(
                Wrappers.<GatewayOperationRecordPO>lambdaQuery()
                        .eq(GatewayOperationRecordPO::getApplicationId, application)
                        .eq(GatewayOperationRecordPO::getSourceType, RPC_DESCRIPTOR_SOURCE));
    }

    /**
     * 中文说明：执行 ingest 操作；先按原 SQL 的列序写入定义集（状态 {@code VERIFIED}、冲突计数 0、
     * {@code received_at} 与 {@code completed_at} 都取入参时间，操作数与接受数取本次上报的操作总数，协议在全集一致时取该协议、
     * 否则取 {@code MIXED}），再沿业务域→实体域→分组→操作逐级查找或创建并累计变更引用；
     * 定义集主键沿用上报方给出的不透明 id 的数值形式，使后续重复上报的存在性判定命中同一行。
     * English summary: Executes the ingest operation; inserts the definition set in the original column order first (status
     * {@code VERIFIED}, conflict counter 0, both {@code received_at} and {@code completed_at} taken from the argument instant, operation
     * and accepted counts set to the operations this report carries, and the protocol reduced to that protocol or to {@code MIXED}), then
     * walks business domain → entity domain → group → operation, finding or creating each level while accumulating the change
     * references. The definition-set primary key keeps the numeric form of the reporter-supplied opaque id so a repeated report hits the
     * same row on its existence check.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayDefinitionReportRepository.ingest(applicationId, report, now)}；
     * 需要调用方事务，来源冲突抛原有 {@code IllegalStateException} 文本码。
     * @param applicationId 参数 应用 id；parameter application id。
     * @param report 参数 完整定义集报告；parameter the complete definition-set report。
     * @param now 参数 本次上报时间；parameter this reporting instant。
     * @return 返回入库上报载体；returns the stored-report carrier.
     */
    @Override
    public GatewayStoredReportBO ingest(
            String applicationId,
            GatewayInterfaceDefinitionReport report,
            Instant now) {
        Long application = numeric(applicationId, "applicationId");
        Long definitionSetKey = numeric(
                report.definitionSetId(),
                "definitionSetId"
        );
        String definitionSetId = text(definitionSetKey);
        int operationCount = operationCount(report);
        GatewayDefinitionSetPO definitionSet = new GatewayDefinitionSetPO();
        definitionSet.setId(definitionSetKey);
        definitionSet.setApplicationId(application);
        definitionSet.setReportId(report.reportId());
        definitionSet.setBuildId(report.build().buildId());
        definitionSet.setProtocol(protocol(report));
        definitionSet.setFingerprint(report.definitionFingerprint());
        definitionSet.setCompleteSet(report.complete());
        definitionSet.setStatus(VERIFIED_STATUS);
        definitionSet.setOperationCount(operationCount);
        definitionSet.setAcceptedCount(operationCount);
        definitionSet.setConflictCount(0);
        definitionSet.setReceivedAt(now);
        definitionSet.setCompletedAt(now);
        if (!definitionSetRepository.save(definitionSet)) {
            log.warn(
                    "Definition set {} was not inserted for application {}",
                    definitionSetId,
                    applicationId
            );
        }
        GatewayMutableStoredReport stored = new GatewayMutableStoredReport();
        for (GatewayInterfaceDefinitionReport.BusinessDomain business
                : report.businessDomains()) {
            String businessId = businessDomain(
                    application,
                    business.code(),
                    business.name(),
                    business.description()
            );
            for (GatewayInterfaceDefinitionReport.EntityDomain entity
                    : business.entityDomains()) {
                String entityId = entityDomain(
                        businessId,
                        entity.code(),
                        entity.name(),
                        entity.description()
                );
                for (GatewayInterfaceDefinitionReport.InterfaceGroup group
                        : entity.interfaceGroups()) {
                    String groupId = interfaceGroup(
                            entityId,
                            group.code(),
                            group.name(),
                            group.description(),
                            group.className(),
                            group.sourceType()
                    );
                    for (GatewayInterfaceDefinitionReport.Operation operation
                            : group.operations()) {
                        storeOperation(
                                application,
                                groupId,
                                definitionSetId,
                                group.sourceType(),
                                operation,
                                stored
                        );
                    }
                }
            }
        }
        return stored.freeze();
    }

    /**
     * 中文说明：按应用与编码查找活跃业务域，命中时覆写展示名与描述（原 {@code UPDATE ... SET display_name, description,
     * updated_at}，其中时间列现由审计边界承载），缺失时以雪花主键新建；原实现的表名是运行期拼接的动态参数，
     * 迁移后按层级拆成具名方法以禁止裸 SQL。
     * English summary: Finds the active business domain by application and code, overwriting the display name and description on a hit
     * (the original {@code UPDATE ... SET display_name, description, updated_at}, where the timestamp is now owned by the audit
     * boundary) and inserting a Snowflake-keyed row when absent. The legacy implementation interpolated the table name at runtime, so the
     * migration splits the hierarchy into named methods to keep raw SQL out.
     *
     * 用法 / Usage: 仅由 {@link #ingest} 的层级遍历调用。
     * @param applicationId 参数 应用 id；parameter application id.
     * @param code 参数 编码；parameter code.
     * @param name 参数 展示名；parameter display name.
     * @param description 参数 描述；parameter description.
     * @return 返回业务域不透明 id；returns the opaque business-domain id.
     */
    private String businessDomain(
            Long applicationId,
            String code,
            String name,
            String description) {
        List<GatewayBusinessDomainPO> existing = businessDomainRepository.list(
                Wrappers.<GatewayBusinessDomainPO>lambdaQuery()
                        .eq(GatewayBusinessDomainPO::getApplicationId, applicationId)
                        .eq(GatewayBusinessDomainPO::getCode, code)
                        .orderByAsc(GatewayBusinessDomainPO::getId));
        if (!existing.isEmpty()) {
            GatewayBusinessDomainPO business = existing.getFirst();
            business.setDisplayName(name);
            business.setDescription(description);
            businessDomainRepository.updateById(business);
            return text(business.getId());
        }
        String id = SnowflakeIdGenerator.nextId();
        GatewayBusinessDomainPO business = new GatewayBusinessDomainPO();
        business.setId(Long.valueOf(id));
        business.setApplicationId(applicationId);
        business.setCode(code);
        business.setDisplayName(name);
        business.setDescription(description);
        businessDomainRepository.save(business);
        return id;
    }

    /**
     * 中文说明：按业务域与编码查找活跃实体域，命中时覆写展示名与描述，缺失时以雪花主键新建，
     * 与原 {@code gateway_entity_domain} 的两条语句一一对应。
     * English summary: Finds the active entity domain by business domain and code, overwriting the display name and description on a hit
     * and inserting a Snowflake-keyed row when absent, matching the two original {@code gateway_entity_domain} statements one to one.
     *
     * 用法 / Usage: 仅由 {@link #ingest} 的层级遍历调用。
     * @param businessDomainId 参数 业务域不透明 id；parameter opaque business-domain id.
     * @param code 参数 编码；parameter code.
     * @param name 参数 展示名；parameter display name.
     * @param description 参数 描述；parameter description.
     * @return 返回实体域不透明 id；returns the opaque entity-domain id.
     */
    private String entityDomain(
            String businessDomainId,
            String code,
            String name,
            String description) {
        Long business = Long.valueOf(businessDomainId);
        List<GatewayEntityDomainPO> existing = entityDomainRepository.list(
                Wrappers.<GatewayEntityDomainPO>lambdaQuery()
                        .eq(GatewayEntityDomainPO::getBusinessDomainId, business)
                        .eq(GatewayEntityDomainPO::getCode, code)
                        .orderByAsc(GatewayEntityDomainPO::getId));
        if (!existing.isEmpty()) {
            GatewayEntityDomainPO entity = existing.getFirst();
            entity.setDisplayName(name);
            entity.setDescription(description);
            entityDomainRepository.updateById(entity);
            return text(entity.getId());
        }
        String id = SnowflakeIdGenerator.nextId();
        GatewayEntityDomainPO entity = new GatewayEntityDomainPO();
        entity.setId(Long.valueOf(id));
        entity.setBusinessDomainId(business);
        entity.setCode(code);
        entity.setDisplayName(name);
        entity.setDescription(description);
        entityDomainRepository.save(entity);
        return id;
    }

    /**
     * 中文说明：执行 interfaceGroup 操作；按实体域与编码读取活跃分组（投影为 {@code GatewayDefinitionGroupRow}，
     * 与原 {@code SELECT id, source_type ... AND deleted = FALSE} 一致），来源不同时先判定历史来源迁移许可，
     * 不允许则抛原 {@code YUHENG_ADMIN_DEFINITION_SOURCE_CONFLICT: <code>} 文本异常，允许则改写来源，随后覆写
     * 展示名、类名与描述；缺失时以雪花主键新建。
     * English summary: Executes the interfaceGroup operation; reads the active group by entity domain and code (projected to
     * {@code GatewayDefinitionGroupRow}, matching the original {@code SELECT id, source_type ... AND deleted = FALSE}). A differing
     * source first goes through the legacy-source migration test, raising the original
     * {@code YUHENG_ADMIN_DEFINITION_SOURCE_CONFLICT: <code>} on refusal and rewriting the source otherwise, after which the display
     * name, class name and description are overwritten; a missing row is inserted under a Snowflake primary key.
     *
     * 用法 / Usage: 仅由 {@link #ingest} 的层级遍历调用。
     * @param entityDomainId 参数 实体域不透明 id；parameter opaque entity-domain id.
     * @param code 参数 分组编码；parameter group code.
     * @param name 参数 展示名；parameter display name.
     * @param description 参数 描述；parameter description.
     * @param className 参数 类名；parameter class name.
     * @param sourceType 参数 上报来源类型；parameter reported source type.
     * @return 返回分组不透明 id；returns the opaque interface-group id.
     */
    private String interfaceGroup(
            String entityDomainId,
            String code,
            String name,
            String description,
            String className,
            GatewayDefinitionSourceTypeEnum sourceType) {
        Long entity = Long.valueOf(entityDomainId);
        List<GatewayInterfaceGroupPO> existing = interfaceGroupRepository.list(
                Wrappers.<GatewayInterfaceGroupPO>lambdaQuery()
                        .eq(GatewayInterfaceGroupPO::getEntityDomainId, entity)
                        .eq(GatewayInterfaceGroupPO::getCode, code)
                        .orderByAsc(GatewayInterfaceGroupPO::getId));
        if (!existing.isEmpty()) {
            GatewayInterfaceGroupPO stored = existing.getFirst();
            GatewayDefinitionGroupRow row = new GatewayDefinitionGroupRow(
                    text(stored.getId()),
                    stored.getSourceType()
            );
            if (!sourceType.name().equals(row.sourceType())) {
                if (!canMigrateLegacySource(sourceType, row.sourceType())) {
                    throw new IllegalStateException(
                            "YUHENG_ADMIN_DEFINITION_SOURCE_CONFLICT: "
                                    + code
                    );
                }
                stored.setSourceType(sourceType.name());
            }
            stored.setDisplayName(name);
            stored.setClassName(className);
            stored.setDescription(description);
            interfaceGroupRepository.updateById(stored);
            return row.id();
        }
        String id = SnowflakeIdGenerator.nextId();
        GatewayInterfaceGroupPO group = new GatewayInterfaceGroupPO();
        group.setId(Long.valueOf(id));
        group.setEntityDomainId(entity);
        group.setCode(code);
        group.setDisplayName(name);
        group.setSourceType(sourceType.name());
        group.setClassName(className);
        group.setDescription(description);
        interfaceGroupRepository.save(group);
        return id;
    }

    /**
     * 中文说明：执行 storeOperation 操作；把原三表聚合投影
     * （{@code gateway_operation} LEFT JOIN 当前定义与全部历史定义，取 {@code COALESCE(MAX(definition_version),0)}）
     * 分解为按应用与 operation_key 的受守卫读取、当前定义摘要读取与最大版本读取，再复刻原分支：
     * 新操作以雪花主键插入（生命周期 {@code DISCOVERED}、当前定义为空、业务 revision 0）后追加 v1 定义、登记成员关系并指向该定义，计为新增；
     * 来源不同先判历史 HTTP 来源迁移，否则抛 {@code YUHENG_ADMIN_DEFINITION_SOURCE_CONFLICT}；分组不同且不可迁移时抛
     * {@code YUHENG_ADMIN_RPC_DESCRIPTOR_GROUP_CONFLICT}；摘要未变仅补登记成员关系并计为未变；
     * 摘要变化时复用同摘要的既有定义或追加新版本，随后补登记成员关系、指向新定义并计为更新。
     * English summary: Executes the storeOperation operation; the original three-table aggregate projection ({@code gateway_operation}
     * left-joined to its current definition and to all historical definitions for {@code COALESCE(MAX(definition_version),0)}) is
     * decomposed into a guarded read by application and operation_key, a read of the current definition digest and a read of the maximum
     * version, after which the original branches are reproduced: a new operation is inserted under a Snowflake key (lifecycle
     * {@code DISCOVERED}, empty current definition, business revision 0), then gets a version 1 definition, a membership row and the
     * current-definition pointer, counting as created. A differing source first goes through the legacy HTTP-source migration test, else
     * {@code YUHENG_ADMIN_DEFINITION_SOURCE_CONFLICT} is raised; a differing group that cannot migrate raises
     * {@code YUHENG_ADMIN_RPC_DESCRIPTOR_GROUP_CONFLICT}; an unchanged digest only records the membership and counts as unchanged; a
     * changed digest reuses the stored definition with the same digest or appends a new version, records the membership, re-points the
     * operation and counts as updated.
     *
     * 用法 / Usage: 仅由 {@link #ingest} 的分组内操作遍历调用。
     * @param applicationId 参数 应用 id；parameter application id.
     * @param groupId 参数 分组不透明 id；parameter opaque group id.
     * @param definitionSetId 参数 定义集不透明 id；parameter opaque definition-set id.
     * @param sourceType 参数 上报来源类型；parameter reported source type.
     * @param operation 参数 上报操作；parameter reported operation.
     * @param stored 参数 本次上报累计器；parameter this report's accumulator.
     */
    private void storeOperation(
            Long applicationId,
            String groupId,
            String definitionSetId,
            GatewayDefinitionSourceTypeEnum sourceType,
            GatewayInterfaceDefinitionReport.Operation operation,
            GatewayMutableStoredReport stored) {
        GatewayDefinitionOperationRow current = existingOperation(
                applicationId,
                operation.operationKey()
        ).orElse(null);
        String definitionSha = sha256(canonical(operation));
        if (current == null) {
            String operationId = SnowflakeIdGenerator.nextId();
            GatewayOperationRecordPO row = new GatewayOperationRecordPO();
            row.setId(Long.valueOf(operationId));
            row.setApplicationId(applicationId);
            row.setInterfaceGroupId(Long.valueOf(groupId));
            row.setOperationKey(operation.operationKey());
            row.setProtocol(operation.protocol());
            row.setMethodIdentity(operation.methodIdentity());
            row.setExternalAccessible(operation.externalAccessible());
            row.setProviderServiceIdentity(node(operation.providerService()));
            row.setSourceType(sourceType.name());
            row.setLifecycleStatus(DISCOVERED_STATUS);
            row.setRevision(0L);
            operationRepository.save(row);
            String definitionId = appendDefinition(
                    operationId,
                    definitionSetId,
                    1,
                    definitionSha,
                    operation
            );
            linkDefinitionSet(definitionSetId, operationId, definitionId, operation);
            pointPending(operationId, definitionId, operation);
            stored.created++;
            stored.refs.add(ref(operation, operationId, "CREATED"));
            return;
        }
        if (!sourceType.name().equals(current.sourceType())) {
            if (!canMigrateLegacyHttpSource(
                    sourceType,
                    current.sourceType(),
                    operation.protocol()
            )) {
                throw new IllegalStateException(
                        "YUHENG_ADMIN_DEFINITION_SOURCE_CONFLICT: "
                                + operation.operationKey()
                );
            }
            migrateSourceAndGroup(current.id(), sourceType.name(), groupId);
        }
        if (!groupId.equals(current.interfaceGroupId())) {
            if (!canMigrateLegacyHttpSource(
                    sourceType,
                    current.sourceType(),
                    operation.protocol()
            )) {
                throw new IllegalStateException(
                        "YUHENG_ADMIN_RPC_DESCRIPTOR_GROUP_CONFLICT: "
                                + operation.operationKey()
                );
            }
        }
        if (definitionSha.equals(current.definitionSha256())) {
            linkDefinitionSet(
                    definitionSetId,
                    current.id(),
                    current.currentDefinitionId(),
                    operation
            );
            stored.refs.add(ref(operation, current.id(), "UNCHANGED"));
            return;
        }
        String definitionId = findDefinition(current.id(), definitionSha)
                .orElseGet(() -> appendDefinition(
                        current.id(),
                        definitionSetId,
                        current.maxVersion() + 1,
                        definitionSha,
                        operation
                ));
        linkDefinitionSet(definitionSetId, current.id(), definitionId, operation);
        pointPending(current.id(), definitionId, operation);
        stored.updated++;
        stored.refs.add(ref(operation, current.id(), "UPDATED"));
    }

    /**
     * 中文说明：等价还原原 {@code SELECT o.id, o.interface_group_id, o.source_type, o.current_definition_id,
     * d.definition_sha256, COALESCE(MAX(all_d.definition_version),0) ... WHERE o.application_id = ? AND o.operation_key = ?}
     * 的聚合投影：按应用与 operation_key 受守卫读取（唯一键 {@code (application_id, operation_key)} 保证至多一行，
     * 主键升序只是为无 {@code ORDER BY} 的原查询固定顺序），当前定义摘要经 {@code d.id = o.current_definition_id} 的空安全
     * 左连接语义读取，最大版本取该操作全部定义的 {@code definition_version} 最大值且无定义时为 0。
     * English summary: Reproduces the aggregate projection of the original {@code SELECT o.id, o.interface_group_id, o.source_type,
     * o.current_definition_id, d.definition_sha256, COALESCE(MAX(all_d.definition_version),0) ... WHERE o.application_id = ? AND
     * o.operation_key = ?}: a guarded read by application and operation_key (the {@code (application_id, operation_key)} unique key
     * guarantees at most one row, and ascending primary key only pins the previously unordered query), the current-definition digest
     * under the null-safe left-join semantics of {@code d.id = o.current_definition_id}, and the maximum version as the largest
     * {@code definition_version} across the operation's definitions or zero when there is none.
     *
     * 用法 / Usage: 仅由 {@link #storeOperation} 调用。
     * @param applicationId 参数 应用 id；parameter application id.
     * @param operationKey 参数 操作键；parameter operation key.
     * @return 返回既有操作投影；returns the projection of the stored operation when present.
     */
    private Optional<GatewayDefinitionOperationRow> existingOperation(
            Long applicationId,
            String operationKey) {
        return operationRepository.list(
                        Wrappers.<GatewayOperationRecordPO>lambdaQuery()
                                .eq(GatewayOperationRecordPO::getApplicationId, applicationId)
                                .eq(GatewayOperationRecordPO::getOperationKey, operationKey)
                                .orderByAsc(GatewayOperationRecordPO::getId))
                .stream()
                .findFirst()
                .map(row -> new GatewayDefinitionOperationRow(
                        text(row.getId()),
                        text(row.getInterfaceGroupId()),
                        row.getSourceType(),
                        text(row.getCurrentDefinitionId()),
                        currentDefinitionSha(row.getCurrentDefinitionId()),
                        maxDefinitionVersion(row.getId())));
    }

    /**
     * 中文说明：读取操作当前定义指向的摘要，指针为空或定义行不可见（含已软删）时返回空，
     * 等价于原 {@code LEFT JOIN gateway_operation_definition d ON d.id = o.current_definition_id} 不产生行。
     * English summary: Reads the digest of the definition the operation currently points at, returning null when the pointer is empty or
     * the definition row is invisible (including soft-deleted), exactly as when the original
     * {@code LEFT JOIN gateway_operation_definition d ON d.id = o.current_definition_id} produced no row.
     *
     * 用法 / Usage: 仅由 {@link #existingOperation} 调用。
     * @param currentDefinitionId 参数 当前定义 id；parameter current definition id.
     * @return 返回定义摘要或 {@code null}；returns the definition digest or {@code null}.
     */
    private String currentDefinitionSha(Long currentDefinitionId) {
        if (currentDefinitionId == null) {
            return null;
        }
        GatewayOperationDefinitionRecordPO definition =
                definitionRepository.getById(currentDefinitionId);
        return definition == null ? null : definition.getDefinitionSha256();
    }

    /**
     * 中文说明：取该操作全部定义版本的最大值，无定义时为 0，等价于原 {@code COALESCE(MAX(all_d.definition_version), 0)}。
     * English summary: Takes the largest definition version of this operation, zero when there is none, equal to the original
     * {@code COALESCE(MAX(all_d.definition_version), 0)}.
     *
     * 用法 / Usage: 仅由 {@link #existingOperation} 调用。
     * @param operationId 参数 操作 id；parameter operation id.
     * @return 返回最大定义版本；returns the maximum definition version.
     */
    private long maxDefinitionVersion(Long operationId) {
        return definitionRepository.list(
                        Wrappers.<GatewayOperationDefinitionRecordPO>lambdaQuery()
                                .eq(GatewayOperationDefinitionRecordPO::getOperationId, operationId))
                .stream()
                .map(GatewayOperationDefinitionRecordPO::getDefinitionVersion)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(0L);
    }

    /**
     * 中文说明：执行 migrateSourceAndGroup 操作；等价于原历史 HTTP 来源分支的
     * {@code UPDATE gateway_operation SET source_type = ?, interface_group_id = ?, updated_at = ? WHERE id = ?}，
     * 时间列由审计边界承载，受守卫 CAS 未写成时如实记录告警而不谎报成功。
     * English summary: Executes the migrateSourceAndGroup operation; equal to the original legacy-HTTP-source branch
     * {@code UPDATE gateway_operation SET source_type = ?, interface_group_id = ?, updated_at = ? WHERE id = ?}, where the timestamp is
     * now owned by the audit boundary and a CAS that does not land is logged truthfully instead of being reported as success.
     *
     * 用法 / Usage: 仅由 {@link #storeOperation} 在历史来源迁移许可成立时调用。
     * @param operationId 参数 操作不透明 id；parameter opaque operation id.
     * @param sourceType 参数 目标来源类型；parameter target source type.
     * @param groupId 参数 目标分组不透明 id；parameter opaque target group id.
     */
    private void migrateSourceAndGroup(
            String operationId,
            String sourceType,
            String groupId) {
        GatewayOperationRecordPO operation =
                operationRepository.getById(Long.valueOf(operationId));
        if (operation == null) {
            return;
        }
        operation.setSourceType(sourceType);
        operation.setInterfaceGroupId(Long.valueOf(groupId));
        if (!operationRepository.updateById(operation)) {
            log.warn(
                    "Source migration did not land for operation {}",
                    operationId
            );
        }
    }

    /**
     * 中文说明：允许当前 OpenAPI 分组接管一条来源值早于现行来源枚举的历史分组行，与原实现同名判定一致。
     * English summary: Lets a current OpenAPI group take ownership of a historical group row whose source value predates the current
     * source enum, matching the identically named legacy predicate.
     *
     * 用法 / Usage: 由分组与操作两处历史来源迁移判定复用。
     * @param target 参数 目标来源类型；parameter target source type.
     * @param existingSourceType 参数 现存来源 wire 字符串；parameter stored source wire string.
     * @return 返回是否允许迁移；returns whether migration is allowed.
     */
    private boolean canMigrateLegacySource(
            GatewayDefinitionSourceTypeEnum target,
            String existingSourceType) {
        return target == GatewayDefinitionSourceTypeEnum.OPENAPI31
                && isUnknownSource(existingSourceType);
    }

    /**
     * 中文说明：把历史操作迁移限定在 HTTP/OpenAPI 边界内，现代来源类型仍互斥并失败关闭，与原实现一致。
     * English summary: Restricts historical operation migration to the HTTP/OpenAPI boundary, keeping modern source types mutually
     * exclusive and fail-closed, as the legacy code did.
     *
     * 用法 / Usage: 仅由 {@link #storeOperation} 调用。
     * @param target 参数 目标来源类型；parameter target source type.
     * @param existingSourceType 参数 现存来源 wire 字符串；parameter stored source wire string.
     * @param protocol 参数 上报协议；parameter reported protocol.
     * @return 返回是否允许迁移；returns whether migration is allowed.
     */
    private boolean canMigrateLegacyHttpSource(
            GatewayDefinitionSourceTypeEnum target,
            String existingSourceType,
            String protocol) {
        return HTTP_PROTOCOL.equals(protocol)
                && canMigrateLegacySource(target, existingSourceType);
    }

    /**
     * 中文说明：判断存储的来源值是否早于现行枚举，空值按已知处理，与原实现的异常捕获语义一致。
     * English summary: Reports whether a stored source value predates the current enum, treating blank as known, matching the legacy
     * exception-capture behavior.
     *
     * 用法 / Usage: 仅由两处历史来源迁移判定调用。
     * @param value 参数 来源 wire 字符串；parameter source wire string.
     * @return 返回是否为未知历史值；returns whether the value is an unknown historical one.
     */
    private boolean isUnknownSource(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            GatewayDefinitionSourceTypeEnum.valueOf(value);
            return false;
        } catch (IllegalArgumentException ignored) {
            return true;
        }
    }

    /**
     * 中文说明：执行 appendDefinition 操作；以雪花主键追加一个定义版本，列为原 {@code INSERT INTO gateway_operation_definition}
     * 的同名业务列，原 {@code ?::jsonb} 文本转换改由 jsonb 类型处理器绑定节点，描述符快照为空时如实写空；
     * 原 {@code created_by} 记来源类型的写法已由受守卫边界的租户/创建者列取代，不再由业务代码填充。
     * English summary: Executes the appendDefinition operation; adds one definition version under a Snowflake primary key, filling the
     * same business columns as the original {@code INSERT INTO gateway_operation_definition}. The legacy {@code ?::jsonb} text casts
     * become structured nodes bound by the jsonb type handler, an absent descriptor snapshot stays null, and the legacy
     * {@code created_by}-as-source-type write is now carried by the guarded boundary's tenant and creator columns instead of business
     * code.
     *
     * 用法 / Usage: 由新操作与定义变化两条分支调用。
     * @param operationId 参数 操作不透明 id；parameter opaque operation id.
     * @param definitionSetId 参数 定义集不透明 id；parameter opaque definition-set id.
     * @param version 参数 定义版本；parameter definition version.
     * @param definitionSha 参数 定义摘要；parameter definition digest.
     * @param operation 参数 上报操作；parameter reported operation.
     * @return 返回新定义不透明 id；returns the opaque id of the appended definition.
     */
    private String appendDefinition(
            String operationId,
            String definitionSetId,
            long version,
            String definitionSha,
            GatewayInterfaceDefinitionReport.Operation operation) {
        String id = SnowflakeIdGenerator.nextId();
        GatewayOperationDefinitionRecordPO definition =
                new GatewayOperationDefinitionRecordPO();
        definition.setId(Long.valueOf(id));
        definition.setOperationId(Long.valueOf(operationId));
        definition.setDefinitionSetId(Long.valueOf(definitionSetId));
        definition.setDefinitionVersion(version);
        definition.setDefinitionSha256(definitionSha);
        definition.setSummary(operation.summary());
        definition.setTags(node(operation.tags()));
        definition.setRequestSchema(node(operation.requestSchema()));
        definition.setResponseSchema(node(operation.responseSchema()));
        definition.setErrorSchema(node(operation.errorSchema()));
        definition.setDescriptorSnapshot(operation.descriptorSnapshot() == null
                ? null
                : node(operation.descriptorSnapshot()));
        definition.setAttributes(node(attributes(operation)));
        definition.setExternalAccessible(operation.externalAccessible());
        if (!definitionRepository.save(definition)) {
            log.warn(
                    "Definition version {} for operation {} was not appended",
                    version,
                    operationId
            );
        }
        return id;
    }

    /**
     * 中文说明：执行 findDefinition 操作；等价于原
     * {@code SELECT id FROM gateway_operation_definition WHERE operation_id = ? AND definition_sha256 = ?} 取首行，
     * 用于在同摘要定义已存在时复用而非重复追加。
     * English summary: Executes the findDefinition operation; equal to taking the first row of the original
     * {@code SELECT id FROM gateway_operation_definition WHERE operation_id = ? AND definition_sha256 = ?}, reusing an existing
     * definition with the same digest instead of appending a duplicate.
     *
     * 用法 / Usage: 仅由 {@link #storeOperation} 的定义变化分支调用。
     * @param operationId 参数 操作不透明 id；parameter opaque operation id.
     * @param definitionSha 参数 定义摘要；parameter definition digest.
     * @return 返回既有定义不透明 id 或空；returns the stored definition id or empty.
     */
    private Optional<String> findDefinition(
            String operationId,
            String definitionSha) {
        return definitionRepository.list(
                        Wrappers.<GatewayOperationDefinitionRecordPO>lambdaQuery()
                                .eq(GatewayOperationDefinitionRecordPO::getOperationId,
                                        Long.valueOf(operationId))
                                .eq(GatewayOperationDefinitionRecordPO::getDefinitionSha256,
                                        definitionSha)
                                .orderByAsc(GatewayOperationDefinitionRecordPO::getDefinitionVersion))
                .stream()
                .findFirst()
                .map(row -> text(row.getId()));
    }

    /**
     * 中文说明：执行 linkDefinitionSet 操作；等价于原
     * {@code INSERT INTO gateway_definition_set_operation(...) VALUES (...) ON CONFLICT (definition_set_id, operation_id) DO NOTHING}：
     * 先按同一对唯一键做受守卫存在性判定，已存在即跳过（0 行影响正是原 {@code DO NOTHING} 的期望结果），缺失时以雪花主键插入成员关系。
     * English summary: Executes the linkDefinitionSet operation; equal to the original
     * {@code INSERT INTO gateway_definition_set_operation(...) VALUES (...) ON CONFLICT (definition_set_id, operation_id) DO NOTHING}. A
     * guarded existence check on the same unique key pair skips the insert when the row is already there — zero affected rows being
     * exactly what {@code DO NOTHING} produced — and a missing row is inserted under a Snowflake primary key.
     *
     * 用法 / Usage: 由新增、未变与更新三条分支调用。
     * @param definitionSetId 参数 定义集不透明 id；parameter opaque definition-set id.
     * @param operationId 参数 操作不透明 id；parameter opaque operation id.
     * @param definitionId 参数 定义不透明 id；parameter opaque definition id.
     * @param operation 参数 上报操作；parameter reported operation.
     */
    private void linkDefinitionSet(
            String definitionSetId,
            String operationId,
            String definitionId,
            GatewayInterfaceDefinitionReport.Operation operation) {
        Long setKey = Long.valueOf(definitionSetId);
        Long storedOperation = Long.valueOf(operationId);
        if (membershipRepository.exists(
                Wrappers.<GatewayDefinitionSetOperationPO>lambdaQuery()
                        .eq(GatewayDefinitionSetOperationPO::getDefinitionSetId, setKey)
                        .eq(GatewayDefinitionSetOperationPO::getOperationId, storedOperation))) {
            return;
        }
        GatewayDefinitionSetOperationPO membership =
                new GatewayDefinitionSetOperationPO();
        membership.setId(SnowflakeIdGenerator.nextLongId());
        membership.setDefinitionSetId(setKey);
        membership.setOperationId(storedOperation);
        membership.setDefinitionId(definitionId == null ? null : Long.valueOf(definitionId));
        membership.setMethodIdentity(operation.methodIdentity());
        membership.setProviderServiceIdentity(node(operation.providerService()));
        membership.setExternalAccessible(operation.externalAccessible());
        membership.setDeprecated(operation.deprecated());
        membershipRepository.save(membership);
    }

    /**
     * 中文说明：执行 pointPending 操作；等价于原
     * {@code UPDATE gateway_operation SET current_definition_id = ?, method_identity = ?, external_accessible = ?,
     * provider_service_identity = ?::jsonb, lifecycle_status = 'DISCOVERED', deprecated_at = NULL, revision = revision + 1,
     * updated_at = ? WHERE id = ? AND lifecycle_status IN ('DISCOVERED','OFFLINE')}：
     * 先按 id 做受守卫读取并按同一生命周期集合过滤（不满足即与原 0 行一致地什么都不做），
     * 满足时在载入行上改写五列、自增业务 {@code revision} 并走乐观锁 CAS；原 SQL 的 {@code deprecated_at = NULL}
     * 无法由默认 NOT_NULL 更新策略下发，故清空列成为已知缺口。
     * English summary: Executes the pointPending operation; equal to the original
     * {@code UPDATE gateway_operation SET current_definition_id = ?, method_identity = ?, external_accessible = ?,
     * provider_service_identity = ?::jsonb, lifecycle_status = 'DISCOVERED', deprecated_at = NULL, revision = revision + 1, updated_at =
     * ? WHERE id = ? AND lifecycle_status IN ('DISCOVERED','OFFLINE')}. The row is first read through the guarded boundary and filtered
     * by the same lifecycle set — behaving like the original zero-row update when it does not qualify — and otherwise the five columns
     * are rewritten on the loaded row, the business {@code revision} is incremented and an optimistic-lock CAS is issued. The original
     * {@code deprecated_at = NULL} cannot be issued under the default NOT_NULL update strategy, which stays a known gap.
     *
     * 用法 / Usage: 由新增与更新两条分支调用；调用方不消费返回值，与原实现忽略受影响行数一致。
     * @param operationId 参数 操作不透明 id；parameter opaque operation id.
     * @param definitionId 参数 定义不透明 id；parameter opaque definition id.
     * @param operation 参数 上报操作；parameter reported operation.
     */
    private void pointPending(
            String operationId,
            String definitionId,
            GatewayInterfaceDefinitionReport.Operation operation) {
        GatewayOperationRecordPO row =
                operationRepository.getById(Long.valueOf(operationId));
        if (row == null
                || !(DISCOVERED_STATUS.equals(row.getLifecycleStatus())
                || OFFLINE_STATUS.equals(row.getLifecycleStatus()))) {
            return;
        }
        row.setCurrentDefinitionId(Long.valueOf(definitionId));
        row.setMethodIdentity(operation.methodIdentity());
        row.setExternalAccessible(operation.externalAccessible());
        row.setProviderServiceIdentity(node(operation.providerService()));
        row.setLifecycleStatus(DISCOVERED_STATUS);
        row.setRevision(row.getRevision() == null ? 1L : row.getRevision() + 1L);
        if (!operationRepository.updateById(row)) {
            log.warn(
                    "Re-pointing operation {} at definition {} did not land",
                    operationId,
                    definitionId
            );
        }
    }

    /**
     * 中文说明：构造定义行的属性快照：先取上报属性，再固定写入名称、描述、负责人、网关支持与弃用标记，
     * 空字符串代替空值，与原实现完全一致。
     * English summary: Builds the definition attribute snapshot: the reported attributes first, then the fixed name, description, owner,
     * gateway-support and deprecated entries, with empty values written as empty strings exactly as the legacy code did.
     *
     * 用法 / Usage: 仅由 {@link #appendDefinition} 调用。
     * @param operation 参数 上报操作；parameter reported operation.
     * @return 返回属性快照；returns the attribute snapshot.
     */
    private Map<String, Object> attributes(
            GatewayInterfaceDefinitionReport.Operation operation) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.putAll(operation.attributes());
        attributes.put("name", nullable(operation.name()));
        attributes.put("description", nullable(operation.description()));
        attributes.put("owner", nullable(operation.owner()));
        attributes.put("gatewaySupport", operation.gatewaySupport());
        attributes.put("deprecated", operation.deprecated());
        return attributes;
    }

    /**
     * 中文说明：把空值规范化为空字符串，与原属性快照写入的取值保持一致。
     * English summary: Normalizes null to the empty string, keeping the values the attribute snapshot wrote.
     *
     * 用法 / Usage: 仅由 {@link #attributes} 调用。
     * @param value 参数 值；parameter value.
     * @return 返回非空字符串；returns a non-null string.
     */
    private String nullable(String value) {
        return value == null ? "" : value;
    }

    /**
     * 中文说明：构造上报结果中的操作变更引用，携带操作键、操作 id 与变更类型，与原实现一致。
     * English summary: Builds the operation change reference of the report result, carrying the operation key, operation id and change
     * type exactly as the legacy code did.
     *
     * 用法 / Usage: 由新增、未变与更新三条分支调用。
     * @param operation 参数 上报操作；parameter reported operation.
     * @param operationId 参数 操作不透明 id；parameter opaque operation id.
     * @param changeType 参数 变更类型；parameter change type.
     * @return 返回操作变更引用；returns the operation change reference.
     */
    private GatewayInterfaceDefinitionReportResult.OperationRef ref(
            GatewayInterfaceDefinitionReport.Operation operation,
            String operationId,
            String changeType) {
        return new GatewayInterfaceDefinitionReportResult.OperationRef(
                operation.operationKey(),
                operationId,
                changeType
        );
    }

    /**
     * 中文说明：统计本次上报覆盖的操作总数，与原实现的三层展开一致。
     * English summary: Counts the operations this report covers, using the same three-level expansion as the legacy code.
     *
     * 用法 / Usage: 仅由 {@link #ingest} 调用。
     * @param report 参数 完整定义集报告；parameter the complete definition-set report.
     * @return 返回操作总数；returns the total operation count.
     */
    private int operationCount(GatewayInterfaceDefinitionReport report) {
        return report.businessDomains().stream()
                .flatMap(business -> business.entityDomains().stream())
                .flatMap(entity -> entity.interfaceGroups().stream())
                .mapToInt(group -> group.operations().size())
                .sum();
    }

    /**
     * 中文说明：把全集协议归一为唯一协议，否则记为 {@code MIXED}，与原 {@code protocol(report)} 判定一致。
     * English summary: Reduces the report's protocols to the single protocol present or to {@code MIXED}, matching the legacy
     * {@code protocol(report)} decision.
     *
     * 用法 / Usage: 仅由 {@link #ingest} 调用。
     * @param report 参数 完整定义集报告；parameter the complete definition-set report.
     * @return 返回协议或 {@code MIXED}；returns the protocol or {@code MIXED}.
     */
    private String protocol(GatewayInterfaceDefinitionReport report) {
        List<String> protocols = report.businessDomains().stream()
                .flatMap(business -> business.entityDomains().stream())
                .flatMap(entity -> entity.interfaceGroups().stream())
                .flatMap(group -> group.operations().stream())
                .map(GatewayInterfaceDefinitionReport.Operation::protocol)
                .distinct()
                .toList();
        return protocols.size() == 1 ? protocols.getFirst() : MIXED_PROTOCOL;
    }

    /**
     * 中文说明：按字典序固定属性与映射键后序列化操作，作为定义摘要的字节输入，保持摘要与旧实现逐字节一致。
     * English summary: Serializes the operation with alphabetically fixed properties and map keys as the digest input, keeping the digest
     * byte-identical to the legacy computation.
     *
     * 用法 / Usage: 仅由 {@link #storeOperation} 调用。
     * @param value 参数 值；parameter value.
     * @return 返回规范字节；returns the canonical bytes.
     */
    private byte[] canonical(Object value) {
        try {
            return canonicalMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException(
                    "gateway definition cannot be canonicalized",
                    failure
            );
        }
    }

    /**
     * 中文说明：按 SHA-256 计算十六进制摘要，与原 {@code sha256(byte[])} 实现一致。
     * English summary: Computes the hexadecimal SHA-256 digest, identical to the legacy {@code sha256(byte[])} helper.
     *
     * 用法 / Usage: 仅由 {@link #storeOperation} 调用。
     * @param value 参数 规范字节；parameter canonical bytes.
     * @return 返回摘要文本；returns the digest text.
     */
    private String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value)
            );
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /**
     * 中文说明：把结构化列值交给 jsonb 类型处理器；原实现用 {@code json(...)} 生成文本并以 {@code ?::jsonb} 绑定，
     * 迁移后同一序列化器生成节点，落库文本保持一致，序列化失败沿用原
     * {@code gateway definition cannot be serialized} 非法参数异常。
     * English summary: Hands a structured column value to the jsonb type handler; the legacy code rendered text with {@code json(...)}
     * and bound it as {@code ?::jsonb}, while the migration produces a node from the same serializer so the stored text stays identical.
     * A serialization failure keeps the legacy {@code gateway definition cannot be serialized} illegal-argument behavior.
     *
     * 用法 / Usage: 仅由各 jsonb 列写入调用。
     * @param value 参数 结构化值；parameter structured value.
     * @return 返回 jsonb 列节点；returns the jsonb column node.
     */
    private JsonNode node(Object value) {
        return objectMapper.valueToTree(value);
    }

    /**
     * 中文说明：按定义集与构建读取候选指纹并以指纹升序去重后取首个，等价于原 {@code SELECT DISTINCT ...} 的首行选择。
     * English summary: Reads the candidate fingerprints by definition-set scope and build, de-duplicates them in ascending fingerprint
     * order and returns the first, which is the original {@code SELECT DISTINCT ...} first-row choice.
     *
     * 用法 / Usage: 由两个 {@code findBuildFingerprint} 重载共用。
     * @param applicationId 参数 应用 id；parameter application id.
     * @param scope 参数 已限定构建与协议的查询条件；parameter query already scoped by build and, when needed, protocol.
     * @return 返回构建指纹或空；returns the build fingerprint or empty.
     */
    private Optional<String> firstFingerprint(
            String applicationId,
            LambdaQueryWrapper<GatewayDefinitionSetPO> scope) {
        Long application = columnValue(applicationId);
        if (application == null) {
            return Optional.empty();
        }
        return definitionSetRepository
                .list(scope.eq(GatewayDefinitionSetPO::getApplicationId, application)
                        .orderByAsc(GatewayDefinitionSetPO::getFingerprint))
                .stream()
                .map(GatewayDefinitionSetPO::getFingerprint)
                .filter(Objects::nonNull)
                .distinct()
                .findFirst();
    }

    /**
     * 中文说明：把端口边界上的不透明 id 解析为数值主键，空白或非数值返回 {@code null}，
     * 因为这类标识在数值主键下本就无法命中任何行。
     * English summary: Parses a port-boundary opaque id into the numeric primary key, returning {@code null} for blank or non-numeric
     * input since such identifiers cannot match a numeric key at all.
     *
     * 用法 / Usage: 由各读取与存在性分支使用。
     * @param opaqueId 参数 不透明标识；parameter opaque identifier.
     * @return 返回数值主键或 {@code null}；returns the numeric key or {@code null}.
     */
    private static Long columnValue(String opaqueId) {
        if (opaqueId == null || opaqueId.isBlank()) {
            return null;
        }
        try {
            long parsed = Long.parseLong(opaqueId.trim());
            return parsed > 0L ? parsed : null;
        } catch (NumberFormatException invalidOpaqueId) {
            return null;
        }
    }

    /**
     * 中文说明：把写入路径必填的不透明数值标识转换为列值，无法表示时按调用方契约抛出
     * {@code <field> must be a numeric identifier}。
     * English summary: Converts a mandatory write-path opaque numeric identifier into a column value, raising
     * {@code <field> must be a numeric identifier} when it cannot be represented.
     *
     * 用法 / Usage: 仅由写入分支使用。
     * @param opaqueId 参数 不透明标识；parameter opaque identifier.
     * @param field 参数 字段名；parameter field name.
     * @return 返回数值列值；returns the numeric column value.
     */
    private static Long numeric(String opaqueId, String field) {
        Long column = columnValue(opaqueId);
        if (column == null) {
            throw new IllegalArgumentException(
                    field + " must be a numeric identifier"
            );
        }
        return column;
    }

    /**
     * 中文说明：把持久层数值主键投影为端口与引用载体使用的不透明字符串 id。
     * English summary: Projects a numeric persistence primary key into the opaque string id used on port and reference carriers.
     *
     * 用法 / Usage: 由各投影构造调用。
     * @param value 参数 数值主键；parameter numeric key.
     * @return 返回字符串 id 或 {@code null}；returns the string id or {@code null}.
     */
    private static String text(Long value) {
        return value == null ? null : String.valueOf(value);
    }
}
