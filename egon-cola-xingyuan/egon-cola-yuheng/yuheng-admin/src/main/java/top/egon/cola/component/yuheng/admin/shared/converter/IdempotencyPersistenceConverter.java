package top.egon.cola.component.yuheng.admin.shared.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.component.yuheng.admin.shared.domain.bo.IdempotencyBO;
import top.egon.cola.component.yuheng.admin.shared.domain.po.IdempotencyRecordPO;

/**
 * 中文说明：{@code IdempotencyPersistenceConverter} 是 {@code gateway_idempotency_record} 在持久边界唯一的 MapStruct 转换器，
 * 负责 {@link IdempotencyRecordPO} 行模型与 {@link IdempotencyBO} 业务载体之间的双向映射，逐列复刻被退役的手写 JDBC 投影：
 * 幂等键在端口上叫 {@code key}、落位列是 {@code idempotency_key}；{@code response} 是 NOT NULL 的 {@code response_content}
 * jsonb 列，写入为 {@code null} 时按旧 {@code writeValueAsString(null)} 语义落成 JSON {@code null} 节点而非 SQL NULL，
 * 读取时 SQL NULL 与 JSON {@code null} 一律投影为 {@code null}，结构不符沿用旧文案
 * {@code stored idempotency response is invalid}，编码失败沿用 {@code idempotency response cannot be serialized}；
 * 载体的 {@code createdAt} 是只读投影，取自 MP 审计列 {@code create_time}（写入方向绝不触碰，由
 * {@code EgonColaMetaObjectHandler} 独占盖章），{@code expiresAt} 保持可空。
 * English summary: {@code IdempotencyPersistenceConverter} is the only MapStruct converter at the persistence boundary of
 * {@code gateway_idempotency_record}, mapping {@link IdempotencyRecordPO} rows and {@link IdempotencyBO} carriers both ways and
 * mirroring the retired hand-written JDBC column by column: the port calls the idempotency key {@code key} while the column is
 * {@code idempotency_key}; {@code response} is the NOT NULL {@code response_content} jsonb column, so an absent carrier value is
 * written as a JSON {@code null} node rather than SQL NULL exactly like the replaced {@code writeValueAsString(null)} binding,
 * while a SQL NULL and a JSON {@code null} both project to {@code null} on read, a shape mismatch keeps the legacy
 * {@code stored idempotency response is invalid} message and an encoding failure keeps
 * {@code idempotency response cannot be serialized}; the carrier's {@code createdAt} is a read-only projection of the MP audit
 * column {@code create_time} (never written, because {@code EgonColaMetaObjectHandler} stamps it) and {@code expiresAt} stays nullable.
 *
 * 用法 / Usage: 由 {@code gateway_idempotency_record} 的受守卫门面注入（bean 名 {@code idempotencyPersistenceConverter}）；
 * 写方向不触碰技术主键、租户、审计、软删与 MP 版本列，因为 {@code EgonColaMetaObjectHandler} 与 {@code ASSIGN_ID} 独占它们，
 * 而旧表没有这些列——迁移后的组合业务键 {@code (scope_type, scope_id, idempotency_key)} 仍由数据库唯一约束守护。
 * Injected by the guarded {@code gateway_idempotency_record} facade under the bean name {@code idempotencyPersistenceConverter};
 * the write direction never touches the technical key, tenant, audit, soft-delete or version columns, because
 * {@code EgonColaMetaObjectHandler} and {@code ASSIGN_ID} own them and the legacy table had none of them, while the migrated
 * composite business key {@code (scope_type, scope_id, idempotency_key)} stays guarded by the database constraint.
 */
@Slf4j
@Component("idempotencyPersistenceConverter")
@RequiredArgsConstructor
public class IdempotencyPersistenceConverter implements BaseConverter<
        IdempotencyRecordPO,
        IdempotencyBO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只补充 jsonb 编解码、空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds jsonb codec, null guarding
     * and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final IdempotencyMapping MAPPING =
            Mappers.getMapper(IdempotencyMapping.class);

    /**
     * 中文说明：表示 IDEMPOTENCY_RESPONSE 这一固定值，声明 {@code response_content} jsonb 列的结构化载体类型，
     * 与被替换的 JDBC 读型 {@code Map<String, Object>} 完全一致。
     * English summary: Represents the idempotency response value, the structured carrier type of the {@code response_content}
     * jsonb column, identical to the {@code Map<String, Object>} read type of the replaced JDBC boundary.
     *
     * 用法 / Usage: 仅由本类的负载解码使用。/ Used only by the payload decoding of this class.
     */
    private static final TypeReference<Map<String, Object>> IDEMPOTENCY_RESPONSE =
            new TypeReference<>() {
            };

    /**
     * 中文说明：保存 Spring 托管的 Jackson 序列化器，jsonb 列编解码只经它完成。
     * English summary: Holds the Spring-managed Jackson mapper through which every jsonb column encoding and decoding runs.
     */
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：执行 toPersistence 操作，把幂等记录载体渲染为 MP 行模型并编码 {@code response_content} jsonb 列；
     * 技术主键、租户、审计、软删与版本列一律留空，由受守卫边界补齐。
     * English summary: Executes the toPersistence operation, rendering an idempotency carrier into the MyBatis-Plus row model and
     * encoding the {@code response_content} jsonb column; the technical key, tenant, audit, soft-delete and version columns stay
     * empty for the guarded boundary to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code idempotencyPersistenceConverter.toPersistence(idempotencyBO)}。
     * 传入 {@code null} 返回 {@code null}。/ Passing {@code null} returns {@code null}.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    public IdempotencyRecordPO toPersistence(IdempotencyBO carrier) {
        if (carrier == null) {
            log.debug("gateway idempotency business carrier is absent; no row rendered");
            return null;
        }
        IdempotencyRecordPO row = MAPPING.toRow(carrier);
        row.setResponseContent(responseNode(carrier.getResponse()));
        return row;
    }

    /**
     * 中文说明：执行 toBusiness 操作，把幂等记录行投影为业务载体：{@code idempotency_key} 还原为端口的 {@code key}、
     * {@code response_content} 解码为结构化响应、{@code create_time} 投影为 {@code createdAt}。
     * English summary: Executes the toBusiness operation, projecting an idempotency row onto the business carrier: the
     * {@code idempotency_key} column returns as the port's {@code key}, {@code response_content} decodes into the structured
     * response and {@code create_time} projects onto {@code createdAt}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code idempotencyPersistenceConverter.toBusiness(row)}。
     * 传入 {@code null} 返回 {@code null}。/ Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    public IdempotencyBO toBusiness(IdempotencyRecordPO row) {
        if (row == null) {
            log.debug("gateway idempotency row is absent; no business carrier projected");
            return null;
        }
        IdempotencyBO carrier = MAPPING.toBusiness(row);
        carrier.setResponse(response(row.getResponseContent()));
        return carrier;
    }

    /**
     * 中文说明：执行 toPersistenceList 操作，按列表顺序渲染幂等记录载体。
     * English summary: Executes the toPersistenceList operation, rendering every idempotency carrier in order; null or empty
     * input yields an empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code idempotencyPersistenceConverter.toPersistenceList(carriers)}。
     * @param carriers 参数 业务载体列表；parameter business carriers.
     * @return 返回 行模型列表；returns the row models.
     */
    public List<IdempotencyRecordPO> toPersistenceList(List<IdempotencyBO> carriers) {
        return toSourceList(carriers);
    }

    /**
     * 中文说明：执行 toBusinessList 操作，按列表顺序投影幂等记录行。
     * English summary: Executes the toBusinessList operation, projecting every idempotency row in order; null or empty input
     * yields an empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code idempotencyPersistenceConverter.toBusinessList(rows)}。
     * @param rows 参数 行模型列表；parameter row models.
     * @return 返回 业务载体列表；returns the business carriers.
     */
    public List<IdempotencyBO> toBusinessList(List<IdempotencyRecordPO> rows) {
        return toTargetList(rows);
    }

    /**
     * 中文说明：执行 toTarget 操作，即 {@code BaseConverter} 的正向投影（行模型 -> 业务载体）。
     * English summary: Executes the toTarget operation, the forward projection of {@code BaseConverter} (row model to business
     * carrier).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code idempotencyPersistenceConverter.toTarget(row)}。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @Override
    public IdempotencyBO toTarget(IdempotencyRecordPO source) {
        return toBusiness(source);
    }

    /**
     * 中文说明：执行 toSource 操作，即 {@code BaseConverter} 的反向渲染（业务载体 -> 行模型）。
     * English summary: Executes the toSource operation, the reverse rendering of {@code BaseConverter} (business carrier to row
     * model).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code idempotencyPersistenceConverter.toSource(carrier)}。
     * @param target 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @Override
    public IdempotencyRecordPO toSource(IdempotencyBO target) {
        return toPersistence(target);
    }

    /**
     * 中文说明：执行 newRow 操作，是 {@code toPersistence} 的插入别名，语义为「只带业务列的新行」，技术主键、租户、
     * 审计、软删与版本列一律留空交由受守卫边界补齐。
     * English summary: Executes the newRow operation, an insert alias of {@code toPersistence} producing a row carrying business
     * columns only; the technical identifier and the tenant, audit, soft-delete and version columns stay empty for the guarded
     * boundary to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code idempotencyPersistenceConverter.newRow(idempotencyBO)}。
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public IdempotencyRecordPO newRow(IdempotencyBO carrier) {
        return toPersistence(carrier);
    }

    /**
     * 中文说明：执行 responseNode 操作，把结构化响应编码为 {@code response_content} 的 jsonb 列值；该列 NOT NULL，
     * 因此载体缺省时写入 JSON {@code null} 节点（与被替换的 {@code writeValueAsString(null)} 绑定一致），
     * 编码失败沿用旧文案 {@code idempotency response cannot be serialized}。
     * English summary: Executes the responseNode operation, encoding a structured response into the {@code response_content}
     * jsonb column value; the column is NOT NULL, so an absent carrier value is written as a JSON {@code null} node (the
     * equivalent of the replaced {@code writeValueAsString(null)} binding) and an encoding failure keeps the legacy
     * {@code idempotency response cannot be serialized} message.
     *
     * 用法 / Usage: 仅由 {@link #toPersistence(IdempotencyBO)} 调用。
     * @param value 参数 结构化响应；parameter structured response.
     * @return 返回 jsonb 列值；returns the jsonb column value.
     */
    public JsonNode responseNode(Map<String, Object> value) {
        if (value == null) {
            return objectMapper.nullNode();
        }
        try {
            return objectMapper.valueToTree(value);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException(
                    "idempotency response cannot be serialized",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 response 操作，把 {@code response_content} jsonb 列值解码为结构化响应；SQL NULL 与 JSON
     * {@code null} 都投影为 {@code null}，解码失败沿用旧文案 {@code stored idempotency response is invalid}。
     * English summary: Executes the response operation, decoding the {@code response_content} jsonb column value into the
     * structured response; a SQL NULL and a JSON {@code null} both project to {@code null}, and a decoding failure keeps the
     * legacy {@code stored idempotency response is invalid} message.
     *
     * 用法 / Usage: 仅由 {@link #toBusiness(IdempotencyRecordPO)} 调用。
     * @param value 参数 jsonb 列值；parameter jsonb column value。
     * @return 返回 结构化响应或 {@code null}；returns the structured response or {@code null}.
     */
    private Map<String, Object> response(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return objectMapper.convertValue(value, IDEMPOTENCY_RESPONSE);
        } catch (IllegalArgumentException failure) {
            throw new IllegalStateException(
                    "stored idempotency response is invalid",
                    failure
            );
        }
    }
}

/**
 * 中文说明：{@code IdempotencyMapping} 是 {@code gateway_idempotency_record} 的 MapStruct 结构映射契约，逐列复刻被替换的
 * 手写 JDBC：端口 {@code key} 与列 {@code idempotency_key} 互桥，{@code createdAt} 只读投影自 MP 审计列
 * {@code create_time}，jsonb 目标 {@code responseContent}/{@code response} 在本接口显式忽略（编解码由外部转换器经
 * Spring 托管的 {@code jacksonObjectMapper} 完成，映射接口不持有裸序列化器）；受保护技术列全部忽略，因为
 * {@code EgonColaMetaObjectHandler} 与 {@code ASSIGN_ID} 独占租户、审计、软删、版本与技术主键，而旧表根本没有这些列；
 * {@code unmappedTargetPolicy=ERROR} 保证任何新增列都必须在此显式表态。
 * English summary: {@code IdempotencyMapping} is the MapStruct structural contract for {@code gateway_idempotency_record},
 * mirroring the hand-written JDBC it replaces column by column: the port's {@code key} bridges the {@code idempotency_key}
 * column, {@code createdAt} is a read-only projection of the MP audit column {@code create_time}, and the jsonb targets
 * {@code responseContent}/{@code response} are ignored explicitly here (their codec runs in the enclosing converter through the
 * Spring-managed {@code jacksonObjectMapper}, so this interface holds no raw serializer); every protected technical column stays
 * ignored because {@code EgonColaMetaObjectHandler} and {@code ASSIGN_ID} own the tenant, audit, soft-delete, version and
 * technical key columns that the legacy table never had; {@code unmappedTargetPolicy=ERROR} forces every new column to be stated
 * explicitly here.
 *
 * 用法 / Usage: 由 {@link IdempotencyPersistenceConverter} 通过 {@code Mappers} 持有，不对外暴露，也不登记为 Spring bean。
 * Held by {@link IdempotencyPersistenceConverter} through {@code Mappers}; neither published to callers nor registered as a bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface IdempotencyMapping {

    /**
     * 中文说明：执行 toBusiness 操作，按 {@code IdempotencyBO} 的字段顺序投影幂等记录行。
     * English summary: Executes the toBusiness operation, projecting an idempotency row onto {@code IdempotencyBO}.
     *
     * 用法 / Usage: 仅由 {@link IdempotencyPersistenceConverter#toBusiness(IdempotencyRecordPO)} 调用。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "scopeType", source = "scopeType")
    @Mapping(target = "scopeId", source = "scopeId")
    @Mapping(target = "key", source = "idempotencyKey")
    @Mapping(target = "payloadSha256", source = "payloadSha256")
    @Mapping(target = "resourceId", source = "resourceId")
    @Mapping(target = "response", ignore = true)
    @Mapping(target = "createdAt", source = "createTime")
    @Mapping(target = "expiresAt", source = "expiresAt")
    IdempotencyBO toBusiness(IdempotencyRecordPO source);

    /**
     * 中文说明：执行 toRow 操作，把幂等记录载体写回行模型；技术主键留空由 {@code ASSIGN_ID} 生成，幂等键写进
     * 业务列 {@code idempotency_key}，创建时刻由边界时钟盖章，受保护技术列全部忽略。
     * English summary: Executes the toRow operation, writing the idempotency carrier back onto the row model; the technical key
     * stays empty for {@code ASSIGN_ID}, the idempotency key lands in the business column {@code idempotency_key}, the creation
     * instant is stamped by the boundary clock, and every protected technical column stays ignored.
     *
     * 用法 / Usage: 仅由 {@link IdempotencyPersistenceConverter#toPersistence(IdempotencyBO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "scopeType", source = "scopeType")
    @Mapping(target = "scopeId", source = "scopeId")
    @Mapping(target = "idempotencyKey", source = "key")
    @Mapping(target = "payloadSha256", source = "payloadSha256")
    @Mapping(target = "resourceId", source = "resourceId")
    @Mapping(target = "responseContent", ignore = true)
    @Mapping(target = "expiresAt", source = "expiresAt")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    IdempotencyRecordPO toRow(IdempotencyBO source);
}
