package top.egon.cola.component.yuheng.admin.catalog.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.component.yuheng.admin.catalog.domain.bo.GatewayOperationBO;
import top.egon.cola.component.yuheng.admin.catalog.domain.po.GatewayOperationRecordPO;

/**
 * 中文说明：{@code GatewayOperationPersistenceConverter} 是 gateway_operation 聚合在持久边界唯一的 MapStruct 转换器，
 * 负责 {@link GatewayOperationRecordPO} 行模型与 {@link GatewayOperationBO} 业务载体之间的双向映射；主键与
 * {@code applicationId}/{@code interfaceGroupId}/{@code currentDefinitionId} 外键以十进制文本往返，
 * {@code provider_service_identity} jsonb 列经 Jackson 在 {@code JsonNode} 与 {@code Map<String, Object>} 之间转换，
 * {@code protocol}/{@code sourceType}/{@code lifecycleStatus} 是开放 varchar 列故保持原 wire 字符串，业务 {@code revision}
 * 与 MP {@code version} 互不替代，{@code deprecatedAt} 只属于行模型。
 * English summary: {@code GatewayOperationPersistenceConverter} is the only MapStruct converter at the persistence boundary of
 * gateway_operation, mapping {@link GatewayOperationRecordPO} rows and {@link GatewayOperationBO} carriers both ways. The key and the
 * {@code applicationId}/{@code interfaceGroupId}/{@code currentDefinitionId} foreign keys round-trip as decimal text, the
 * {@code provider_service_identity} jsonb column is converted through Jackson between {@code JsonNode} and
 * {@code Map<String, Object>}, the open {@code protocol}/{@code sourceType}/{@code lifecycleStatus} columns keep their original wire
 * strings, the business {@code revision} stays separate from the MP {@code version} and {@code deprecatedAt} belongs to the row only.
 *
 * 用法 / Usage: 由 {@code MpGatewayOperationPersistenceRepository} 等受守卫仓储注入使用（bean 名
 * {@code gatewayOperationPersistenceConverter}），RecordPO 只能出现在本转换器与受守卫仓储之间，禁止进入端口或服务签名。
 * Injected by the guarded operation repository under the bean name {@code gatewayOperationPersistenceConverter}; a RecordPO may only
 * meet the business carrier inside this converter and the guarded repository, never in a port or service signature.
 */
@Slf4j
@Component("gatewayOperationPersistenceConverter")
public class GatewayOperationPersistenceConverter implements BaseConverter<
        GatewayOperationRecordPO,
        GatewayOperationBO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只做空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds null guarding and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final GatewayOperationMapping MAPPING =
            Mappers.getMapper(GatewayOperationMapping.class);

    /**
     * 中文说明：执行 toPersistence 操作，把 operation 业务载体渲染为 MP 行模型。
     * English summary: Executes the toPersistence operation, rendering an operation carrier into the MyBatis-Plus row model; the provider
     * identity map is encoded as a jsonb node and the tenant, audit, soft-delete, version and {@code deprecatedAt} columns are left alone.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayOperationPersistenceConverter.toPersistence(operationBO)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    public GatewayOperationRecordPO toPersistence(GatewayOperationBO carrier) {
        if (carrier == null) {
            log.debug("gateway operation business carrier is absent; no row rendered");
            return null;
        }
        return MAPPING.toRow(carrier);
    }

    /**
     * 中文说明：执行 toBusiness 操作，把 operation 行模型投影为业务载体。
     * English summary: Executes the toBusiness operation, projecting an operation row onto the business carrier, decoding the provider
     * identity jsonb and reading creation/update instants from the MP audit columns.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayOperationPersistenceConverter.toBusiness(row)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    public GatewayOperationBO toBusiness(GatewayOperationRecordPO row) {
        if (row == null) {
            log.debug("gateway operation row is absent; no business carrier projected");
            return null;
        }
        return MAPPING.toBusiness(row);
    }

    /**
     * 中文说明：执行 toPersistenceList 操作，按列表顺序渲染 operation 载体。
     * English summary: Executes the toPersistenceList operation, rendering every operation carrier in order; null or empty input yields
     * an empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayOperationPersistenceConverter.toPersistenceList(carriers)}。
     * @param carriers 参数 业务载体列表；parameter business carriers.
     * @return 返回 行模型列表；returns the row models.
     */
    public List<GatewayOperationRecordPO> toPersistenceList(
            List<GatewayOperationBO> carriers) {
        return toSourceList(carriers);
    }

    /**
     * 中文说明：执行 toBusinessList 操作，按列表顺序投影 operation 行。
     * English summary: Executes the toBusinessList operation, projecting every operation row in order; null or empty input yields an
     * empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayOperationPersistenceConverter.toBusinessList(rows)}。
     * @param rows 参数 行模型列表；parameter row models.
     * @return 返回 业务载体列表；returns the business carriers.
     */
    public List<GatewayOperationBO> toBusinessList(
            List<GatewayOperationRecordPO> rows) {
        return toTargetList(rows);
    }

    /**
     * 中文说明：执行 toTarget 操作，即 {@code BaseConverter} 的正向投影（行模型 -> 业务载体）。
     * English summary: Executes the toTarget operation, the forward projection of {@code BaseConverter} (row model to business carrier).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayOperationPersistenceConverter.toTarget(row)}。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @Override
    public GatewayOperationBO toTarget(GatewayOperationRecordPO source) {
        return toBusiness(source);
    }

    /**
     * 中文说明：执行 toSource 操作，即 {@code BaseConverter} 的反向渲染（业务载体 -> 行模型）。
     * English summary: Executes the toSource operation, the reverse rendering of {@code BaseConverter} (business carrier to row model).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayOperationPersistenceConverter.toSource(carrier)}。
     * @param target 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @Override
    public GatewayOperationRecordPO toSource(GatewayOperationBO target) {
        return toPersistence(target);
    }

    /**
     * 中文说明：执行 newRow 操作，是 {@code toPersistence} 的插入别名，语义为「只带业务列的新行」，
     * {@code deprecatedAt} 与租户、审计、软删、版本列一律留空交由受守卫边界补齐。
     * English summary: Executes the newRow operation, an insert alias of {@code toPersistence} producing a row carrying business columns
     * only; {@code deprecatedAt} plus the tenant, audit, soft-delete and version columns stay empty for the guarded boundary to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayOperationPersistenceConverter.newRow(operationBO)}。
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public GatewayOperationRecordPO newRow(GatewayOperationBO carrier) {
        return toPersistence(carrier);
    }
}

/**
 * 中文说明：{@code GatewayOperationMapping} 是 gateway_operation 的 MapStruct 结构映射契约，逐列复刻被替换的手写 JDBC 读写语义：
 * jsonb 列经 Jackson 在节点与结构化载体之间转换，缺失或 JSON {@code null} 投影为 Java {@code null}，
 * NOT NULL 的 {@code provider_service_identity} 在业务值为空时写入 JSON {@code null} 节点而非 SQL NULL。
 * English summary: {@code GatewayOperationMapping} is the MapStruct structural contract for gateway_operation, mirroring the JDBC it
 * replaces: jsonb travels through Jackson between a node and the structured carrier, a missing or JSON {@code null} column projects to a
 * Java {@code null}, and the NOT NULL {@code provider_service_identity} stores a JSON {@code null} node instead of a SQL NULL when the
 * business value is absent, exactly like the legacy {@code json(null)} binding.
 *
 * 用法 / Usage: 由 {@link GatewayOperationPersistenceConverter} 通过 {@code Mappers} 持有，不对外暴露，也不登记为 Spring bean。
 * Held by {@link GatewayOperationPersistenceConverter} through {@code Mappers}; neither published to callers nor registered as a bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface GatewayOperationMapping {

    /**
     * 中文说明：保存 Jackson 序列化器，禁止 default typing，只做列值与结构化载体之间的转换。
     * English summary: Holds the Jackson converter between the column node and the structured carrier; default typing stays disabled.
     *
     * 用法 / Usage: 仅由本映射的默认方法使用。/ Used only by the default methods of this mapping.
     */
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 中文说明：表示 OBJECT_MAP 这一固定值，声明 jsonb 对象列的结构化载体类型。
     * English summary: Represents the fixed object map value, the structured carrier type of a jsonb object column.
     *
     * 用法 / Usage: 仅由本映射的默认方法使用。/ Used only by the default methods of this mapping.
     */
    TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    /**
     * 中文说明：执行 toBusiness 操作，按 {@code GatewayOperationBO} 的字段顺序投影 gateway_operation 行。
     * English summary: Executes the toBusiness operation, projecting a gateway_operation row onto {@code GatewayOperationBO}.
     *
     * 用法 / Usage: 仅由 {@link GatewayOperationPersistenceConverter#toBusiness(GatewayOperationRecordPO)} 调用。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( text( source.getId() ) )")
    @Mapping(target = "applicationId", expression = "java( text( source.getApplicationId() ) )")
    @Mapping(target = "interfaceGroupId", expression = "java( text( source.getInterfaceGroupId() ) )")
    @Mapping(target = "operationKey", source = "operationKey")
    @Mapping(target = "protocol", source = "protocol")
    @Mapping(target = "methodIdentity", source = "methodIdentity")
    @Mapping(target = "externalAccessible", source = "externalAccessible")
    @Mapping(target = "providerServiceIdentity",
            expression = "java( objectMap( source.getProviderServiceIdentity(), \"provider identity\" ) )")
    @Mapping(target = "sourceType", source = "sourceType")
    @Mapping(target = "lifecycleStatus", source = "lifecycleStatus")
    @Mapping(target = "currentDefinitionId",
            expression = "java( text( source.getCurrentDefinitionId() ) )")
    @Mapping(target = "revision", source = "revision")
    @Mapping(target = "createdAt", source = "createTime")
    @Mapping(target = "updatedAt", source = "updateTime")
    GatewayOperationBO toBusiness(GatewayOperationRecordPO source);

    /**
     * 中文说明：执行 toRow 操作，把 operation 业务载体写回行模型；{@code deprecatedAt} 与受保护技术列全部忽略。
     * English summary: Executes the toRow operation, writing the operation carrier back onto the row model; {@code deprecatedAt} and every
     * protected technical column stay ignored because the deprecation command and the MP guard own them.
     *
     * 用法 / Usage: 仅由 {@link GatewayOperationPersistenceConverter#toPersistence(GatewayOperationBO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( identifier( source.getId() ) )")
    @Mapping(target = "applicationId", expression = "java( identifier( source.getApplicationId() ) )")
    @Mapping(target = "interfaceGroupId", expression = "java( identifier( source.getInterfaceGroupId() ) )")
    @Mapping(target = "operationKey", source = "operationKey")
    @Mapping(target = "protocol", source = "protocol")
    @Mapping(target = "methodIdentity", source = "methodIdentity")
    @Mapping(target = "externalAccessible", source = "externalAccessible")
    @Mapping(target = "providerServiceIdentity",
            expression = "java( requiredNode( source.getProviderServiceIdentity() ) )")
    @Mapping(target = "sourceType", source = "sourceType")
    @Mapping(target = "lifecycleStatus", source = "lifecycleStatus")
    @Mapping(target = "currentDefinitionId",
            expression = "java( identifier( source.getCurrentDefinitionId() ) )")
    @Mapping(target = "revision", source = "revision")
    @Mapping(target = "deprecatedAt", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    GatewayOperationRecordPO toRow(GatewayOperationBO source);

    /**
     * 中文说明：执行 text 操作，把 MP 的 bigint 列按十进制文本投影，等价于原 JDBC 的 {@code getString(...)}。
     * English summary: Executes the text operation, projecting an MP bigint column as decimal text, exactly what the legacy
     * {@code getString(...)} produced; {@code null} stays {@code null}.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 标识；parameter identifier。
     * @return 返回 十进制文本；returns the decimal text.
     */
    default String text(Long value) {
        return value == null ? null : Long.toString(value);
    }

    /**
     * 中文说明：执行 identifier 操作，把业务十进制标识解析为 MP bigint；空白视为未生成或无外键，非法值直接拒绝。
     * English summary: Executes the identifier operation, parsing a business decimal identifier into the MP bigint; blank means not yet
     * generated or no foreign key, and a malformed value is rejected.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 业务标识；parameter business identifier。
     * @return 返回 标识；returns the identifier.
     */
    default Long identifier(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(
                    "gateway operation identifier must be decimal: " + value,
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 objectMap 操作，把 jsonb 对象节点解码为 {@code Map<String, Object>}；节点缺失或为 JSON {@code null} 时返回
     * {@code null}，结构不符按原实现抛出「stored ... is invalid」。
     * English summary: Executes the objectMap operation, decoding a jsonb object node into {@code Map<String, Object>}; an absent or JSON
     * {@code null} node projects to {@code null} and a shape mismatch raises the legacy {@code stored ... is invalid} failure.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 jsonb 节点；parameter jsonb node。
     * @param label 参数 列标签；parameter column label。
     * @return 返回 结构化对象；returns the structured object.
     */
    default Map<String, Object> objectMap(JsonNode value, String label) {
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(value, OBJECT_MAP);
        } catch (IllegalArgumentException failure) {
            throw new IllegalStateException(
                    "stored " + label + " is invalid",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 requiredNode 操作，把结构化对象编码为 jsonb 节点；NOT NULL 列在业务值为空时写入 JSON {@code null} 节点，
     * 与被替换实现的 {@code writeValueAsString(null)} 行为一致。
     * English summary: Executes the requiredNode operation, encoding the structured object into a jsonb node; for a NOT NULL column an
     * absent business value becomes a JSON {@code null} node, matching the replaced {@code writeValueAsString(null)} binding.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 结构化对象；parameter structured object。
     * @return 返回 jsonb 节点；returns the jsonb node.
     */
    default JsonNode requiredNode(Map<String, Object> value) {
        return value == null
                ? OBJECT_MAPPER.nullNode()
                : OBJECT_MAPPER.valueToTree(value);
    }
}
