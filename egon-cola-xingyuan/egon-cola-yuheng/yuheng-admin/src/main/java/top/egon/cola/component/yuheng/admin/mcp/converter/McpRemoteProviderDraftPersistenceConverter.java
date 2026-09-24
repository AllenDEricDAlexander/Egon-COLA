package top.egon.cola.component.yuheng.admin.mcp.converter;

import java.util.LinkedHashMap;
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
import top.egon.cola.component.yuheng.admin.mcp.domain.bo.McpRemoteProviderDraftBO;
import top.egon.cola.component.yuheng.admin.mcp.domain.po.McpRemoteProviderDraftRecordPO;

/**
 * 中文说明：{@code McpRemoteProviderDraftPersistenceConverter} 是 {@code gateway_mcp_remote_provider} 在持久边界唯一的 MapStruct
 * 转换器：旧实现把提供方属性收进一个 jsonb 语义的 {@code content} 映射，新表已把它们展开成独立列，本转换器负责这层「列 <-> content
 * 键」的双向收放，并逐字保留旧语义——读取按 displayName、dialect、transportType、endpointReference、authProfileReference、
 * tlsProfileReference、capabilityFingerprint、status 的规范顺序入表且可空列缺失时不写键，写入对前四项强制非空（异常文案
 * {@code <key> is required}）、对三项空白转 NULL、对 status 缺省为 {@code CONFIGURED}。
 * English summary: {@code McpRemoteProviderDraftPersistenceConverter} is the only MapStruct converter at the persistence boundary of
 * {@code gateway_mcp_remote_provider}: the legacy code kept the provider attributes in one jsonb-shaped {@code content} map while the
 * new table spreads them over dedicated columns, so this converter performs that column-to-key spread in both directions. It keeps the
 * legacy semantics verbatim: reads assemble the canonical key order displayName, dialect, transportType, endpointReference,
 * authProfileReference, tlsProfileReference, capabilityFingerprint, status and omit absent nullable keys; writes enforce the first four
 * as non-blank (message {@code <key> is required}), trim the three optional keys to NULL when blank, and default status to
 * {@code CONFIGURED}.
 *
 * 用法 / Usage: 由 {@code MpMcpRemoteProviderRepository} 注入使用（bean 名
 * {@code mcpRemoteProviderDraftPersistenceConverter}）；RecordPO 只能出现在本转换器与受守卫仓储之间，禁止进入端口或服务签名。
 * Injected by the guarded remote provider draft repository under the bean name {@code mcpRemoteProviderDraftPersistenceConverter}; a
 * RecordPO may only meet the business carrier inside this converter and the guarded repository, never a port or service signature.
 */
@Slf4j
@Component("mcpRemoteProviderDraftPersistenceConverter")
public class McpRemoteProviderDraftPersistenceConverter implements BaseConverter<
        McpRemoteProviderDraftRecordPO,
        McpRemoteProviderDraftBO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只做空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds null guarding and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final McpRemoteProviderDraftMapping MAPPING =
            Mappers.getMapper(McpRemoteProviderDraftMapping.class);

    /**
     * 中文说明：执行 toBusiness 操作，把提供方行模型收拢为携带 {@code content} 映射的业务载体。
     * English summary: Executes the toBusiness operation, gathering a provider row into the business carrier that carries the
     * {@code content} map.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpRemoteProviderDraftPersistenceConverter.toBusiness(row)}。传入 {@code null} 返回
     * {@code null}。/ Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    public McpRemoteProviderDraftBO toBusiness(McpRemoteProviderDraftRecordPO row) {
        if (row == null) {
            log.debug("gateway_mcp_remote_provider row is absent; no business carrier projected");
            return null;
        }
        return MAPPING.toBusiness(row);
    }

    /**
     * 中文说明：执行 toPersistence 操作，把业务载体的 {@code content} 映射展开为提供方行的独立列，技术列留给受守卫边界补齐。
     * English summary: Executes the toPersistence operation, spreading the carrier's {@code content} map over the provider row's
     * dedicated columns while the technical columns stay for the guarded boundary to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpRemoteProviderDraftPersistenceConverter.toPersistence(providerBO)}。传入
     * {@code null} 返回 {@code null}。/ Passing {@code null} returns {@code null}.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    public McpRemoteProviderDraftRecordPO toPersistence(McpRemoteProviderDraftBO carrier) {
        if (carrier == null) {
            log.debug("gateway_mcp_remote_provider business carrier is absent; no row rendered");
            return null;
        }
        return MAPPING.toRow(carrier);
    }

    /**
     * 中文说明：执行 toBusinessList 操作，按列表顺序投影行模型；null 或空输入返回空列表而非 {@code null}。
     * English summary: Executes the toBusinessList operation, projecting every row in order; null or empty input yields an empty list
     * and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpRemoteProviderDraftPersistenceConverter.toBusinessList(rows)}。
     * @param rows 参数 行模型列表；parameter row models.
     * @return 返回 业务载体列表；returns the business carriers.
     */
    public List<McpRemoteProviderDraftBO> toBusinessList(
            List<McpRemoteProviderDraftRecordPO> rows) {
        return toTargetList(rows);
    }

    /**
     * 中文说明：执行 toPersistenceList 操作，按列表顺序渲染业务载体；null 或空输入返回空列表而非 {@code null}。
     * English summary: Executes the toPersistenceList operation, rendering every carrier in order; null or empty input yields an empty
     * list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpRemoteProviderDraftPersistenceConverter.toPersistenceList(carriers)}。
     * @param carriers 参数 业务载体列表；parameter business carriers.
     * @return 返回 行模型列表；returns the row models.
     */
    public List<McpRemoteProviderDraftRecordPO> toPersistenceList(
            List<McpRemoteProviderDraftBO> carriers) {
        return toSourceList(carriers);
    }

    /**
     * 中文说明：执行 toTarget 操作，即 {@code BaseConverter} 的正向投影（行模型 -> 业务载体）。
     * English summary: Executes the toTarget operation, the forward projection of {@code BaseConverter} (row model to business
     * carrier).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpRemoteProviderDraftPersistenceConverter.toTarget(row)}。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @Override
    public McpRemoteProviderDraftBO toTarget(McpRemoteProviderDraftRecordPO source) {
        return toBusiness(source);
    }

    /**
     * 中文说明：执行 toSource 操作，即 {@code BaseConverter} 的反向渲染（业务载体 -> 行模型）。
     * English summary: Executes the toSource operation, the reverse rendering of {@code BaseConverter} (business carrier to row
     * model).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpRemoteProviderDraftPersistenceConverter.toSource(carrier)}。
     * @param target 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @Override
    public McpRemoteProviderDraftRecordPO toSource(McpRemoteProviderDraftBO target) {
        return toPersistence(target);
    }

    /**
     * 中文说明：执行 newRow 操作，是 {@code toPersistence} 的插入别名，语义为「只带业务列的新行」。
     * English summary: Executes the newRow operation, an insert alias of {@code toPersistence} producing a row carrying business
     * columns only.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpRemoteProviderDraftPersistenceConverter.newRow(providerBO)}。
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public McpRemoteProviderDraftRecordPO newRow(McpRemoteProviderDraftBO carrier) {
        return toPersistence(carrier);
    }
}

/**
 * 中文说明：{@code McpRemoteProviderDraftMapping} 是 gateway_mcp_remote_provider 的 MapStruct 结构映射契约，逐列复刻被替换的手写
 * SQL 读写语义；{@code unmappedTargetPolicy=ERROR} 保证任何新增列都必须在此显式表态。
 * English summary: {@code McpRemoteProviderDraftMapping} is the MapStruct structural contract for gateway_mcp_remote_provider,
 * mirroring the hand-written SQL it replaces column by column; {@code unmappedTargetPolicy=ERROR} forces every new column to be
 * stated explicitly here.
 *
 * 用法 / Usage: 由 {@link McpRemoteProviderDraftPersistenceConverter} 通过 {@code Mappers} 持有，不对外暴露，也不登记为 Spring bean。
 * Held by {@link McpRemoteProviderDraftPersistenceConverter} through {@code Mappers}; neither published to callers nor registered as
 * a bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface McpRemoteProviderDraftMapping {

    /**
     * 中文说明：status 列在旧实现里的缺省值，写入时 content 缺失该键即落此值。
     * English summary: The legacy default of the status column, written when the content map lacks the key.
     */
    String DEFAULT_STATUS = "CONFIGURED";

    /**
     * 中文说明：执行 toBusiness 操作，按 {@code McpRemoteProviderDraftBO} 的字段顺序投影提供方行。
     * English summary: Executes the toBusiness operation, projecting a provider row onto {@code McpRemoteProviderDraftBO}.
     *
     * 用法 / Usage: 仅由 {@link McpRemoteProviderDraftPersistenceConverter#toBusiness(McpRemoteProviderDraftRecordPO)} 调用。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( text( source.getId() ) )")
    @Mapping(target = "gatewayGroupId", expression = "java( text( source.getGatewayGroupId() ) )")
    @Mapping(target = "providerCode", source = "providerCode")
    @Mapping(target = "content", expression = "java( content( source ) )")
    @Mapping(target = "enabled", expression = "java( flag( source.getEnabled() ) )")
    @Mapping(target = "revision", expression = "java( value( source.getRevision() ) )")
    McpRemoteProviderDraftBO toBusiness(McpRemoteProviderDraftRecordPO source);

    /**
     * 中文说明：执行 toRow 操作，把业务载体写回提供方行；受保护的技术列全部忽略。
     * English summary: Executes the toRow operation, writing the business carrier back onto a provider row; every protected technical
     * column stays ignored because the persistence metadata handler owns them.
     *
     * 用法 / Usage: 仅由 {@link McpRemoteProviderDraftPersistenceConverter#toPersistence(McpRemoteProviderDraftBO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( identifier( source.getId() ) )")
    @Mapping(target = "gatewayGroupId", expression = "java( identifier( source.getGatewayGroupId() ) )")
    @Mapping(target = "providerCode", source = "providerCode")
    @Mapping(target = "displayName", expression = "java( required( source.getContent(), \"displayName\" ) )")
    @Mapping(target = "dialect", expression = "java( required( source.getContent(), \"dialect\" ) )")
    @Mapping(target = "transportType", expression = "java( required( source.getContent(), \"transportType\" ) )")
    @Mapping(target = "endpointReference", expression = "java( required( source.getContent(), \"endpointReference\" ) )")
    @Mapping(target = "authProfileReference", expression = "java( optional( source.getContent(), \"authProfileReference\" ) )")
    @Mapping(target = "tlsProfileReference", expression = "java( optional( source.getContent(), \"tlsProfileReference\" ) )")
    @Mapping(target = "capabilityFingerprint", expression = "java( optional( source.getContent(), \"capabilityFingerprint\" ) )")
    @Mapping(target = "status", expression = "java( status( source.getContent() ) )")
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "revision", source = "revision")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    McpRemoteProviderDraftRecordPO toRow(McpRemoteProviderDraftBO source);

    /**
     * 中文说明：执行 content 操作，按旧实现的规范键序收拢提供方列；可空列缺失时不写键，与旧实现跳过 null 的行为一致。
     * English summary: Executes the content operation, gathering the provider columns in the legacy canonical key order; an absent
     * nullable column contributes no key, matching the legacy skip-null behaviour.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param source 参数 行模型；parameter row model。
     * @return 返回 内容映射；returns the content map.
     */
    default Map<String, Object> content(McpRemoteProviderDraftRecordPO source) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("displayName", source.getDisplayName());
        content.put("dialect", source.getDialect());
        content.put("transportType", source.getTransportType());
        content.put("endpointReference", source.getEndpointReference());
        put(content, "authProfileReference", source.getAuthProfileReference());
        put(content, "tlsProfileReference", source.getTlsProfileReference());
        put(content, "capabilityFingerprint", source.getCapabilityFingerprint());
        content.put("status", source.getStatus());
        return content;
    }

    /**
     * 中文说明：执行 text 操作，把 MP 的 bigint 列按十进制文本投影，{@code null} 保持 {@code null}。
     * English summary: Executes the text operation, projecting an MP bigint column as decimal text; {@code null} stays {@code null}.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 列值；parameter column value。
     * @return 返回 十进制文本；returns the decimal text.
     */
    default String text(Long value) {
        return value == null ? null : Long.toString(value);
    }

    /**
     * 中文说明：执行 identifier 操作，把业务十进制标识解析为 MP bigint；空白视为未生成，交由 {@code ASSIGN_ID} 补位。
     * English summary: Executes the identifier operation, parsing the business decimal identifier into the MP bigint; a blank value is
     * treated as not yet generated so {@code ASSIGN_ID} can fill it, while a malformed value is rejected.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 业务标识；parameter business identifier。
     * @return 返回 主键；returns the identifier.
     */
    default Long identifier(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(
                    "gateway_mcp_remote_provider identifier must be decimal: " + value,
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 value 操作，把可空 revision 列投影为基础类型；列缺失即 0，与旧默认值一致。
     * English summary: Executes the value operation, projecting the nullable revision column onto the primitive field; an absent
     * column reads as zero, matching the legacy default.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 列值；parameter column value。
     * @return 返回 基础类型值；returns the primitive value.
     */
    default long value(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 中文说明：执行 flag 操作，把可空布尔列投影为基础类型。
     * English summary: Executes the flag operation, projecting a nullable boolean column onto the primitive field.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 列值；parameter column value。
     * @return 返回 基础类型值；returns the primitive value.
     */
    default boolean flag(Boolean value) {
        return value != null && value;
    }

    /**
     * 中文说明：执行 required 操作，按旧实现语义取内容键：空白即抛 {@code <key> is required}，否则去首尾空白。
     * English summary: Executes the required operation, reading a content key with the legacy semantics: blank raises
     * {@code <key> is required}, otherwise the value is trimmed.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param content 参数 内容映射；parameter content map。
     * @param key 参数 键；parameter key。
     * @return 返回 非空文本；returns the non-blank text.
     */
    default String required(Map<String, Object> content, String key) {
        Object value = content == null ? null : content.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value.toString().trim();
    }

    /**
     * 中文说明：执行 optional 操作，按旧实现语义取内容键：缺失或空白写 NULL，否则去首尾空白。
     * English summary: Executes the optional operation, reading a content key with the legacy semantics: absent or blank writes NULL,
     * otherwise the value is trimmed.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param content 参数 内容映射；parameter content map。
     * @param key 参数 键；parameter key。
     * @return 返回 可空文本；returns the optional text.
     */
    default String optional(Map<String, Object> content, String key) {
        Object value = content == null ? null : content.get(key);
        return value == null || value.toString().isBlank()
                ? null
                : value.toString().trim();
    }

    /**
     * 中文说明：执行 status 操作，复刻旧实现的 {@code getOrDefault("status", "CONFIGURED")} 缺省。
     * English summary: Executes the status operation, reproducing the legacy {@code getOrDefault("status", "CONFIGURED")} default.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param content 参数 内容映射；parameter content map。
     * @return 返回 状态列值；returns the status column value.
     */
    default String status(Map<String, Object> content) {
        Object value = content == null ? null : content.get("status");
        return value == null
                ? DEFAULT_STATUS
                : value.toString();
    }

    /**
     * 中文说明：执行 put 操作，仅在列值存在时写入内容键，保持旧实现的「null 不入表」。
     * English summary: Executes the put operation, contributing a content key only when the column carries a value, keeping the
     * legacy "null stays out of the map" rule.
     *
     * 用法 / Usage: 仅由 {@link #content(McpRemoteProviderDraftRecordPO)} 调用。
     * @param target 参数 目标映射；parameter target map。
     * @param key 参数 键；parameter key。
     * @param value 参数 值；parameter value。
     */
    default void put(Map<String, Object> target, String key, String value) {
        if (value != null) {
            target.put(key, value);
        }
    }
}
