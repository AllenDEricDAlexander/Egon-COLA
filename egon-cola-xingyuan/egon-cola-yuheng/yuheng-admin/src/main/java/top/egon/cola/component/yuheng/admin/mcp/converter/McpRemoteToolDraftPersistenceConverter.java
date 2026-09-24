package top.egon.cola.component.yuheng.admin.mcp.converter;

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
import top.egon.cola.component.yuheng.admin.mcp.domain.bo.McpRemoteToolDraftBO;
import top.egon.cola.component.yuheng.admin.mcp.domain.po.McpRemoteToolDraftRecordPO;

/**
 * 中文说明：{@code McpRemoteToolDraftPersistenceConverter} 是 {@code gateway_mcp_remote_tool_draft} 在持久边界唯一的 MapStruct
 * 转换器：把 {@link McpRemoteToolDraftRecordPO} 行模型投影为 {@link McpRemoteToolDraftBO} 业务载体并反向渲染；id、
 * {@code gateway_group_id}、{@code server_id}、{@code remote_mount_id} 以十进制文本往返，业务名 {@code name} 落在
 * {@code tool_name} 列，{@code content} 以 jsonb 对象往返并逐字保留旧实现 {@code Map.copyOf} 的不可变、去序且拒绝 JSON null 成员的
 * 语义；业务 {@code revision} 与 MP {@code version} 互不替代。
 * English summary: {@code McpRemoteToolDraftPersistenceConverter} is the only MapStruct converter at the persistence boundary of
 * {@code gateway_mcp_remote_tool_draft}: it projects {@link McpRemoteToolDraftRecordPO} rows onto {@link McpRemoteToolDraftBO}
 * carriers and renders them back. The row, group, server and mount identifiers round-trip as decimal text, the business {@code name}
 * lives in the {@code tool_name} column, and {@code content} round-trips as a jsonb object keeping the legacy {@code Map.copyOf}
 * immutability, loss of order and rejection of JSON null members verbatim; the business {@code revision} stays separate from the MP
 * {@code version}.
 *
 * 用法 / Usage: 由 {@code MpMcpRemoteToolDraftRepository} 注入使用（bean 名
 * {@code mcpRemoteToolDraftPersistenceConverter}）；RecordPO 只能出现在本转换器与受守卫仓储之间，禁止进入端口或服务签名。
 * Injected by the guarded remote tool draft repository under the bean name {@code mcpRemoteToolDraftPersistenceConverter}; a RecordPO
 * may only meet the business carrier inside this converter and the guarded repository, never a port or service signature.
 */
@Slf4j
@Component("mcpRemoteToolDraftPersistenceConverter")
public class McpRemoteToolDraftPersistenceConverter implements BaseConverter<
        McpRemoteToolDraftRecordPO,
        McpRemoteToolDraftBO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只做空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds null guarding and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final McpRemoteToolDraftMapping MAPPING =
            Mappers.getMapper(McpRemoteToolDraftMapping.class);

    /**
     * 中文说明：执行 toBusiness 操作，把远端工具草稿行模型投影为业务载体。
     * English summary: Executes the toBusiness operation, projecting a remote tool draft row onto the business carrier.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpRemoteToolDraftPersistenceConverter.toBusiness(row)}。传入 {@code null} 返回
     * {@code null}。/ Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    public McpRemoteToolDraftBO toBusiness(McpRemoteToolDraftRecordPO row) {
        if (row == null) {
            log.debug("gateway_mcp_remote_tool_draft row is absent; no business carrier projected");
            return null;
        }
        return MAPPING.toBusiness(row);
    }

    /**
     * 中文说明：执行 toPersistence 操作，把业务载体渲染为 MP 行模型，技术列留给受守卫边界补齐。
     * English summary: Executes the toPersistence operation, rendering the business carrier into the MyBatis-Plus row model while the
     * technical columns stay for the guarded boundary to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpRemoteToolDraftPersistenceConverter.toPersistence(draftBO)}。传入 {@code null}
     * 返回 {@code null}。/ Passing {@code null} returns {@code null}.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    public McpRemoteToolDraftRecordPO toPersistence(McpRemoteToolDraftBO carrier) {
        if (carrier == null) {
            log.debug("gateway_mcp_remote_tool_draft business carrier is absent; no row rendered");
            return null;
        }
        return MAPPING.toRow(carrier);
    }

    /**
     * 中文说明：执行 toBusinessList 操作，按列表顺序投影行模型；null 或空输入返回空列表而非 {@code null}。
     * English summary: Executes the toBusinessList operation, projecting every row in order; null or empty input yields an empty list
     * and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpRemoteToolDraftPersistenceConverter.toBusinessList(rows)}。
     * @param rows 参数 行模型列表；parameter row models.
     * @return 返回 业务载体列表；returns the business carriers.
     */
    public List<McpRemoteToolDraftBO> toBusinessList(
            List<McpRemoteToolDraftRecordPO> rows) {
        return toTargetList(rows);
    }

    /**
     * 中文说明：执行 toPersistenceList 操作，按列表顺序渲染业务载体；null 或空输入返回空列表而非 {@code null}。
     * English summary: Executes the toPersistenceList operation, rendering every carrier in order; null or empty input yields an empty
     * list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpRemoteToolDraftPersistenceConverter.toPersistenceList(carriers)}。
     * @param carriers 参数 业务载体列表；parameter business carriers.
     * @return 返回 行模型列表；returns the row models.
     */
    public List<McpRemoteToolDraftRecordPO> toPersistenceList(
            List<McpRemoteToolDraftBO> carriers) {
        return toSourceList(carriers);
    }

    /**
     * 中文说明：执行 toTarget 操作，即 {@code BaseConverter} 的正向投影（行模型 -> 业务载体）。
     * English summary: Executes the toTarget operation, the forward projection of {@code BaseConverter} (row model to business
     * carrier).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpRemoteToolDraftPersistenceConverter.toTarget(row)}。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @Override
    public McpRemoteToolDraftBO toTarget(McpRemoteToolDraftRecordPO source) {
        return toBusiness(source);
    }

    /**
     * 中文说明：执行 toSource 操作，即 {@code BaseConverter} 的反向渲染（业务载体 -> 行模型）。
     * English summary: Executes the toSource operation, the reverse rendering of {@code BaseConverter} (business carrier to row
     * model).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpRemoteToolDraftPersistenceConverter.toSource(carrier)}。
     * @param target 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @Override
    public McpRemoteToolDraftRecordPO toSource(McpRemoteToolDraftBO target) {
        return toPersistence(target);
    }

    /**
     * 中文说明：执行 newRow 操作，是 {@code toPersistence} 的插入别名，语义为「只带业务列的新行」。
     * English summary: Executes the newRow operation, an insert alias of {@code toPersistence} producing a row carrying business
     * columns only.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpRemoteToolDraftPersistenceConverter.newRow(draftBO)}。
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public McpRemoteToolDraftRecordPO newRow(McpRemoteToolDraftBO carrier) {
        return toPersistence(carrier);
    }
}

/**
 * 中文说明：{@code McpRemoteToolDraftMapping} 是 gateway_mcp_remote_tool_draft 的 MapStruct 结构映射契约，逐列复刻被替换的手写 SQL
 * 读写语义；{@code unmappedTargetPolicy=ERROR} 保证任何新增列都必须在此显式表态。
 * English summary: {@code McpRemoteToolDraftMapping} is the MapStruct structural contract for gateway_mcp_remote_tool_draft,
 * mirroring the hand-written SQL it replaces column by column; {@code unmappedTargetPolicy=ERROR} forces every new column to be
 * stated explicitly here.
 *
 * 用法 / Usage: 由 {@link McpRemoteToolDraftPersistenceConverter} 通过 {@code Mappers} 持有，不对外暴露，也不登记为 Spring bean。
 * Held by {@link McpRemoteToolDraftPersistenceConverter} through {@code Mappers}; neither published to callers nor registered as a
 * bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface McpRemoteToolDraftMapping {

    /**
     * 中文说明：与 jsonb 类型处理器同源的 Jackson 序列化器，仅用于本映射契约内的列形态转换。
     * English summary: The Jackson serializer behind the column shapes of this mapping contract, matching the jsonb type handler.
     */
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 中文说明：content 列的对象读取形态。
     * English summary: The read shape of the content object column.
     */
    TypeReference<Map<String, Object>> CONTENT_MAP = new TypeReference<>() {
    };

    /**
     * 中文说明：执行 toBusiness 操作，按 {@code McpRemoteToolDraftBO} 的字段顺序投影远端工具草稿行。
     * English summary: Executes the toBusiness operation, projecting a remote tool draft row onto {@code McpRemoteToolDraftBO}.
     *
     * 用法 / Usage: 仅由 {@link McpRemoteToolDraftPersistenceConverter#toBusiness(McpRemoteToolDraftRecordPO)} 调用。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( text( source.getId() ) )")
    @Mapping(target = "gatewayGroupId", expression = "java( text( source.getGatewayGroupId() ) )")
    @Mapping(target = "serverId", expression = "java( text( source.getServerId() ) )")
    @Mapping(target = "name", source = "toolName")
    @Mapping(target = "remoteMountId", expression = "java( text( source.getRemoteMountId() ) )")
    @Mapping(target = "content", expression = "java( content( source.getContent() ) )")
    @Mapping(target = "enabled", expression = "java( flag( source.getEnabled() ) )")
    @Mapping(target = "revision", expression = "java( value( source.getRevision() ) )")
    McpRemoteToolDraftBO toBusiness(McpRemoteToolDraftRecordPO source);

    /**
     * 中文说明：执行 toRow 操作，把业务载体写回远端工具草稿行；受保护的技术列全部忽略。
     * English summary: Executes the toRow operation, writing the business carrier back onto a remote tool draft row; every protected
     * technical column stays ignored because the persistence metadata handler owns them.
     *
     * 用法 / Usage: 仅由 {@link McpRemoteToolDraftPersistenceConverter#toPersistence(McpRemoteToolDraftBO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( identifier( source.getId() ) )")
    @Mapping(target = "gatewayGroupId", expression = "java( identifier( source.getGatewayGroupId() ) )")
    @Mapping(target = "serverId", expression = "java( identifier( source.getServerId() ) )")
    @Mapping(target = "toolName", source = "name")
    @Mapping(target = "remoteMountId", expression = "java( identifier( source.getRemoteMountId() ) )")
    @Mapping(target = "content", expression = "java( contentNode( source.getContent() ) )")
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "revision", source = "revision")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    McpRemoteToolDraftRecordPO toRow(McpRemoteToolDraftBO source);

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
                    "gateway_mcp_remote_tool_draft identifier must be decimal: " + value,
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 value 操作，把可空 revision 列投影为基础类型；列缺失即 0，与旧默认值一致。
     * English summary: Executes the value operation, projecting the nullable revision column onto the primitive field; an absent column
     * reads as zero, matching the legacy default.
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
     * 中文说明：执行 content 操作，把 jsonb 对象解码为不可变映射，逐字复刻旧实现 {@code Map.copyOf} 的语义。
     * English summary: Executes the content operation, decoding the jsonb object into an immutable map and reproducing the legacy
     * {@code Map.copyOf} semantics verbatim.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 列值；parameter column value。
     * @return 返回 内容映射；returns the content map.
     */
    default Map<String, Object> content(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return Map.copyOf(OBJECT_MAPPER.convertValue(value, CONTENT_MAP));
        } catch (IllegalArgumentException failure) {
            throw new IllegalStateException(
                    "stored MCP persistence value is invalid",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 contentNode 操作，把内容映射渲染为 jsonb 节点；NOT NULL 列在缺失时写入 JSON null 而非 SQL NULL。
     * English summary: Executes the contentNode operation, rendering the content map as a jsonb node; an absent value writes JSON null
     * into the NOT NULL column instead of SQL NULL.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 内容映射；parameter content map。
     * @return 返回 jsonb 节点；returns the jsonb node.
     */
    default JsonNode contentNode(Map<String, Object> value) {
        return value == null
                ? OBJECT_MAPPER.nullNode()
                : OBJECT_MAPPER.valueToTree(value);
    }
}
