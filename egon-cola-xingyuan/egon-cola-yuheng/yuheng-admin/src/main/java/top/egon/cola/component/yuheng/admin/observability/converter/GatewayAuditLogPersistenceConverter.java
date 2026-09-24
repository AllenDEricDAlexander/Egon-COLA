package top.egon.cola.component.yuheng.admin.observability.converter;

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
import top.egon.cola.component.yuheng.admin.observability.domain.bo.GatewayAuditLogBO;
import top.egon.cola.component.yuheng.admin.observability.domain.po.GatewayAuditLogRecordPO;

/**
 * 中文说明：{@code GatewayAuditLogPersistenceConverter} 是 {@code gateway_audit_log} 在持久边界唯一的 MapStruct 转换器，
 * 负责 {@link GatewayAuditLogRecordPO} 行模型与 {@link GatewayAuditLogBO} 业务载体之间的双向映射：端口主键是十进制文本
 * （旧列 {@code VARCHAR(64)}，由调用方的雪花号生成），迁移后落进 MP 的 bigint {@code id}，因此写方向按十进制解析、
 * 读方向按十进制文本原样还原，二者可逆且与旧 {@code getString("id")} 完全一致；
 * {@code before_summary}/{@code after_summary} 两个可空 jsonb 列经 Spring 托管的 Jackson 在 {@code JsonNode} 与
 * {@code Map<String, Object>} 之间往返，缺省即留空由边界写 SQL NULL，读取时 SQL NULL 与 JSON {@code null} 一律投影为
 * {@code null}；{@code successful} 是 NOT NULL 布尔列，读方向的空列按旧 JDBC 的 {@code getBoolean} 落为 {@code false}；
 * 业务发生时刻保持在自己的列 {@code occurred_at} 上，MP 审计列 {@code create_time} 与 {@code update_time} 只在读方向
 * 由守卫补齐、写方向绝不触碰。
 * English summary: {@code GatewayAuditLogPersistenceConverter} is the only MapStruct converter at the persistence boundary of
 * {@code gateway_audit_log}, mapping {@link GatewayAuditLogRecordPO} rows and {@link GatewayAuditLogBO} carriers both ways: the
 * port key is decimal text (the legacy {@code VARCHAR(64)} column filled by the caller's snowflake generator) that now lands in
 * the MP bigint {@code id}, so the write direction parses it as decimal and the read direction renders it back as the very same
 * decimal text, reversibly and identically to the legacy {@code getString("id")}; the two nullable jsonb columns
 * {@code before_summary} and {@code after_summary} round-trip through the Spring-managed Jackson mapper between a {@code JsonNode}
 * and a {@code Map<String, Object>}, an absent carrier value is left unset so the boundary writes SQL NULL, while a SQL NULL and a
 * JSON {@code null} both project to {@code null} on read; {@code successful} is a NOT NULL boolean column whose absent value reads
 * as {@code false} like the legacy {@code getBoolean}; the business instant keeps its own column {@code occurred_at}, while the MP
 * audit columns {@code create_time} and {@code update_time} are filled by the guard and never written from this side.
 *
 * 用法 / Usage: 由 {@code gateway_audit_log} 的受守卫门面注入（bean 名 {@code gatewayAuditLogPersistenceConverter}）；
 * 写方向不触碰租户、审计、软删与 MP 版本列，因为它们由 {@code EgonColaMetaObjectHandler} 独占；调用方带来的雪花号
 * 必须原样落库，否则审计行标识会被 {@code ASSIGN_ID} 随机取代。JSON 列编解码只在此处经 {@code jacksonObjectMapper} 完成。
 * Injected by the guarded {@code gateway_audit_log} facade under the bean name {@code gatewayAuditLogPersistenceConverter}; the
 * write direction never touches the tenant, audit, soft-delete or MP version columns, because {@code EgonColaMetaObjectHandler}
 * owns them, while the caller-supplied snowflake identifier has to be persisted verbatim or the audit row key would be replaced
 * by a random {@code ASSIGN_ID} value. JSON column codecs run only here, through {@code jacksonObjectMapper}.
 */
@Slf4j
@Component("gatewayAuditLogPersistenceConverter")
@RequiredArgsConstructor
public class GatewayAuditLogPersistenceConverter implements BaseConverter<
        GatewayAuditLogRecordPO,
        GatewayAuditLogBO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只补充 jsonb 编解码、空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds jsonb codec, null guarding
     * and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final GatewayAuditLogMapping MAPPING =
            Mappers.getMapper(GatewayAuditLogMapping.class);

    /**
     * 中文说明：表示 AUDIT_SUMMARY 这一固定值，声明两个 jsonb 摘要列的结构化载体类型，与被替换持久化边界的
     * {@code Map<String, Object>} 读型一致。
     * English summary: Represents the audit summary value, the structured carrier type of the two jsonb summary columns, identical
     * to the {@code Map<String, Object>} read type of the replaced persistence boundary.
     *
     * 用法 / Usage: 仅由本类的摘要解码使用。/ Used only by the summary decoding of this class.
     */
    private static final TypeReference<Map<String, Object>> AUDIT_SUMMARY =
            new TypeReference<>() {
            };

    /**
     * 中文说明：保存 Spring 托管的 Jackson 序列化器，jsonb 列编解码只经它完成。
     * English summary: Holds the Spring-managed Jackson mapper through which every jsonb column encoding and decoding runs.
     */
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：执行 toPersistence 操作，把审计载体渲染为 MP 行模型并编码两个可空 jsonb 摘要列；行标识按十进制解析，
     * 租户、审计、软删与版本列一律留空交由受守卫边界补齐。
     * English summary: Executes the toPersistence operation, rendering an audit carrier into the MyBatis-Plus row model and encoding
     * the two nullable jsonb summary columns; the row identifier is parsed as decimal text while the tenant, audit, soft-delete and
     * version columns stay empty for the guarded boundary to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayAuditLogPersistenceConverter.toPersistence(auditLogBO)}。
     * 传入 {@code null} 返回 {@code null}。/ Passing {@code null} returns {@code null}.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    public GatewayAuditLogRecordPO toPersistence(GatewayAuditLogBO carrier) {
        if (carrier == null) {
            log.debug("gateway audit log business carrier is absent; no row rendered");
            return null;
        }
        GatewayAuditLogRecordPO row = MAPPING.toRow(carrier);
        row.setBeforeSummary(summaryNode(carrier.getBeforeSummary()));
        row.setAfterSummary(summaryNode(carrier.getAfterSummary()));
        return row;
    }

    /**
     * 中文说明：执行 toBusiness 操作，把审计行投影为业务载体：行标识还原为调用方看到的十进制文本，
     * 两个 jsonb 摘要列解码为结构化摘要。
     * English summary: Executes the toBusiness operation, projecting an audit row onto the business carrier: the row identifier is
     * rendered back as the decimal text the caller sees and the two jsonb summary columns decode into their structured form.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayAuditLogPersistenceConverter.toBusiness(row)}。
     * 传入 {@code null} 返回 {@code null}。/ Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    public GatewayAuditLogBO toBusiness(GatewayAuditLogRecordPO row) {
        if (row == null) {
            log.debug("gateway audit log row is absent; no business carrier projected");
            return null;
        }
        GatewayAuditLogBO carrier = MAPPING.toBusiness(row);
        carrier.setBeforeSummary(summary(row.getBeforeSummary()));
        carrier.setAfterSummary(summary(row.getAfterSummary()));
        return carrier;
    }

    /**
     * 中文说明：执行 toPersistenceList 操作，按列表顺序渲染审计载体。
     * English summary: Executes the toPersistenceList operation, rendering every audit carrier in order; null or empty input yields
     * an empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayAuditLogPersistenceConverter.toPersistenceList(carriers)}。
     * @param carriers 参数 业务载体列表；parameter business carriers.
     * @return 返回 行模型列表；returns the row models.
     */
    public List<GatewayAuditLogRecordPO> toPersistenceList(
            List<GatewayAuditLogBO> carriers) {
        return toSourceList(carriers);
    }

    /**
     * 中文说明：执行 toBusinessList 操作，按列表顺序投影审计行。
     * English summary: Executes the toBusinessList operation, projecting every audit row in order; null or empty input yields an
     * empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayAuditLogPersistenceConverter.toBusinessList(rows)}。
     * @param rows 参数 行模型列表；parameter row models.
     * @return 返回 业务载体列表；returns the business carriers.
     */
    public List<GatewayAuditLogBO> toBusinessList(
            List<GatewayAuditLogRecordPO> rows) {
        return toTargetList(rows);
    }

    /**
     * 中文说明：执行 toTarget 操作，即 {@code BaseConverter} 的正向投影（行模型 -> 业务载体）。
     * English summary: Executes the toTarget operation, the forward projection of {@code BaseConverter} (row model to business
     * carrier).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayAuditLogPersistenceConverter.toTarget(row)}。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @Override
    public GatewayAuditLogBO toTarget(GatewayAuditLogRecordPO source) {
        return toBusiness(source);
    }

    /**
     * 中文说明：执行 toSource 操作，即 {@code BaseConverter} 的反向渲染（业务载体 -> 行模型）。
     * English summary: Executes the toSource operation, the reverse rendering of {@code BaseConverter} (business carrier to row
     * model).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayAuditLogPersistenceConverter.toSource(carrier)}。
     * @param target 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @Override
    public GatewayAuditLogRecordPO toSource(GatewayAuditLogBO target) {
        return toPersistence(target);
    }

    /**
     * 中文说明：执行 newRow 操作，是 {@code toPersistence} 的插入别名，语义为「只带业务列的新行」，租户、审计、
     * 软删与版本列一律留空交由受守卫边界补齐；调用方自带的行标识属于业务事实，因此随载体一并写入。
     * English summary: Executes the newRow operation, an insert alias of {@code toPersistence} producing a row carrying business
     * columns only; the tenant, audit, soft-delete and version columns stay empty for the guarded boundary to fill, while the
     * caller-supplied row identifier is business fact and therefore rides the carrier.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayAuditLogPersistenceConverter.newRow(auditLogBO)}。
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public GatewayAuditLogRecordPO newRow(GatewayAuditLogBO carrier) {
        return toPersistence(carrier);
    }

    /**
     * 中文说明：执行 summaryNode 操作，把结构化摘要编码为 jsonb 列值；{@code null} 如实返回 {@code null}，
     * 由受守卫插入省略该可空列（等价旧的 SQL NULL 绑定），编码失败沿用旧文案
     * {@code audit log summary cannot be serialized}。
     * English summary: Executes the summaryNode operation, encoding a structured summary into the jsonb column value; {@code null}
     * truthfully yields {@code null} so the guarded insert omits the nullable column (the equivalent of the replaced SQL NULL
     * binding), and an encoding failure keeps the legacy {@code audit log summary cannot be serialized} message.
     *
     * 用法 / Usage: 仅由 {@link #toPersistence(GatewayAuditLogBO)} 调用。
     * @param value 参数 结构化摘要；parameter structured summary。
     * @return 返回 jsonb 列值或 {@code null}；returns the jsonb column value or {@code null}.
     */
    private JsonNode summaryNode(Map<String, Object> value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.valueToTree(value);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException(
                    "audit log summary cannot be serialized",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 summary 操作，把 jsonb 列值解码为结构化摘要；SQL NULL 与 JSON {@code null} 都投影为 {@code null}，
     * 解码失败沿用旧文案 {@code stored audit log summary is invalid}。
     * English summary: Executes the summary operation, decoding a jsonb column value into the structured summary; a SQL NULL and a
     * JSON {@code null} both project to {@code null}, and a decoding failure keeps the legacy
     * {@code stored audit log summary is invalid} message.
     *
     * 用法 / Usage: 仅由 {@link #toBusiness(GatewayAuditLogRecordPO)} 调用。
     * @param value 参数 jsonb 列值；parameter jsonb column value。
     * @return 返回 结构化摘要或 {@code null}；returns the structured summary or {@code null}.
     */
    private Map<String, Object> summary(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return objectMapper.convertValue(value, AUDIT_SUMMARY);
        } catch (IllegalArgumentException failure) {
            throw new IllegalStateException(
                    "stored audit log summary is invalid",
                    failure
            );
        }
    }
}

/**
 * 中文说明：{@code GatewayAuditLogMapping} 是 {@code gateway_audit_log} 的 MapStruct 结构映射契约，逐列复刻被替换的持久化边界：
 * 端口十进制主键与 MP bigint {@code id} 双向桥接（空白视为未生成，交给 {@code ASSIGN_ID}），两个 jsonb 摘要目标在本接口显式忽略
 * （编解码由外部转换器经 Spring 托管的 {@code jacksonObjectMapper} 完成，映射接口不持有裸序列化器），
 * {@code successful} 的空列按旧 JDBC 的 {@code getBoolean} 读作 {@code false}；业务发生时刻与同名列直接往返，
 * 受保护技术列除 {@code id} 外全部忽略，因为 {@code EgonColaMetaObjectHandler} 独占租户、审计、软删与 MP 版本列；
 * {@code unmappedTargetPolicy=ERROR} 保证任何新增列都必须在此显式表态。
 * English summary: {@code GatewayAuditLogMapping} is the MapStruct structural contract for {@code gateway_audit_log}, mirroring the
 * persistence boundary it replaces column by column: the port's decimal key bridges the MP bigint {@code id} in both directions
 * (a blank value means not yet generated and is left to {@code ASSIGN_ID}), the two jsonb summary targets are ignored explicitly here
 * (their codec runs in the enclosing converter through the Spring-managed {@code jacksonObjectMapper}, so this interface holds no raw
 * serializer), an absent {@code successful} column reads as {@code false} like the legacy {@code getBoolean}, the business instant
 * round-trips with its identically named column, and every protected technical column except {@code id} stays ignored because
 * {@code EgonColaMetaObjectHandler} owns the tenant, audit, soft-delete and MP version columns; {@code unmappedTargetPolicy=ERROR}
 * forces every new column to be stated explicitly here.
 *
 * 用法 / Usage: 由 {@link GatewayAuditLogPersistenceConverter} 通过 {@code Mappers} 持有，不对外暴露，也不登记为 Spring bean。
 * Held by {@link GatewayAuditLogPersistenceConverter} through {@code Mappers}; neither published to callers nor registered as a bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface GatewayAuditLogMapping {

    /**
     * 中文说明：执行 toBusiness 操作，按 {@code GatewayAuditLogBO} 的字段顺序投影审计行。
     * English summary: Executes the toBusiness operation, projecting an audit row onto {@code GatewayAuditLogBO}.
     *
     * 用法 / Usage: 仅由 {@link GatewayAuditLogPersistenceConverter#toBusiness(GatewayAuditLogRecordPO)} 调用。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( text( source.getId() ) )")
    @Mapping(target = "actorId", source = "actorId")
    @Mapping(target = "actorType", source = "actorType")
    @Mapping(target = "source", source = "source")
    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "traceId", source = "traceId")
    @Mapping(target = "resourceType", source = "resourceType")
    @Mapping(target = "resourceId", source = "resourceId")
    @Mapping(target = "action", source = "action")
    @Mapping(target = "beforeSummary", ignore = true)
    @Mapping(target = "afterSummary", ignore = true)
    @Mapping(target = "draftRevision", source = "draftRevision")
    @Mapping(target = "releaseId", source = "releaseId")
    @Mapping(target = "successful", expression = "java( flag( source.getSuccessful() ) )")
    @Mapping(target = "errorCode", source = "errorCode")
    @Mapping(target = "occurredAt", source = "occurredAt")
    GatewayAuditLogBO toBusiness(GatewayAuditLogRecordPO source);

    /**
     * 中文说明：执行 toRow 操作，把审计载体写回行模型；调用方的雪花号写进 {@code id}，两个摘要列与受保护技术列
     * （除 {@code id}）全部忽略。
     * English summary: Executes the toRow operation, writing the audit carrier back onto the row model; the caller's snowflake value
     * fills {@code id}, while the two summary columns and every protected technical column except {@code id} stay ignored.
     *
     * 用法 / Usage: 仅由 {@link GatewayAuditLogPersistenceConverter#toPersistence(GatewayAuditLogBO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( identifier( source.getId() ) )")
    @Mapping(target = "actorId", source = "actorId")
    @Mapping(target = "actorType", source = "actorType")
    @Mapping(target = "source", source = "source")
    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "traceId", source = "traceId")
    @Mapping(target = "resourceType", source = "resourceType")
    @Mapping(target = "resourceId", source = "resourceId")
    @Mapping(target = "action", source = "action")
    @Mapping(target = "beforeSummary", ignore = true)
    @Mapping(target = "afterSummary", ignore = true)
    @Mapping(target = "draftRevision", source = "draftRevision")
    @Mapping(target = "releaseId", source = "releaseId")
    @Mapping(target = "successful", source = "successful")
    @Mapping(target = "errorCode", source = "errorCode")
    @Mapping(target = "occurredAt", source = "occurredAt")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    GatewayAuditLogRecordPO toRow(GatewayAuditLogBO source);

    /**
     * 中文说明：执行 text 操作，把 MP 的 bigint 列按十进制文本投影，等价于旧实现的 {@code getString("id")}；
     * {@code null} 保持 {@code null}。
     * English summary: Executes the text operation, projecting the MP bigint column as decimal text, exactly what the replaced
     * {@code getString("id")} produced; {@code null} stays {@code null}.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 标识；parameter identifier。
     * @return 返回 十进制文本；returns the decimal text.
     */
    default String text(Long value) {
        return value == null ? null : Long.toString(value);
    }

    /**
     * 中文说明：执行 identifier 操作，把业务十进制标识解析为 MP bigint；空白视为未生成，主键交由 {@code ASSIGN_ID}
     * 补位，非法值直接拒绝。
     * English summary: Executes the identifier operation, parsing a business decimal identifier into the MP bigint; a blank value is
     * treated as not yet generated so {@code ASSIGN_ID} can fill the key, while a malformed value is rejected.
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
                    "gateway audit log identifier must be decimal: " + value,
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 flag 操作，把可空的 {@code successful} 列投影为载体的 {@code boolean} 字段，
     * 等价旧 JDBC 的 {@code getBoolean("successful")}（列缺失即 {@code false}）。
     * English summary: Executes the flag operation, projecting the nullable {@code successful} column onto the carrier's
     * {@code boolean} field, equivalent to the legacy {@code getBoolean("successful")} which yields {@code false} for an absent value.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 成功标记列；parameter stored success flag。
     * @return 返回 成功标记；returns the success flag.
     */
    default boolean flag(Boolean value) {
        return Boolean.TRUE.equals(value);
    }
}
