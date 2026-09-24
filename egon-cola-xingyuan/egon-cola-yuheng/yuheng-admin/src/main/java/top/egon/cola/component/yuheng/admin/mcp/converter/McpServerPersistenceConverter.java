package top.egon.cola.component.yuheng.admin.mcp.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.component.yuheng.admin.mcp.domain.bo.McpServerBO;
import top.egon.cola.component.yuheng.admin.mcp.domain.po.McpServerRecordPO;

/**
 * 中文说明：{@code McpServerPersistenceConverter} 是 {@code gateway_mcp_server} 聚合在持久边界唯一的 MapStruct 转换器，
 * 负责把 {@link McpServerRecordPO} 行模型投影为 {@link McpServerBO} 业务载体，以及把业务载体写回行模型；
 * id 与 {@code gateway_group_id} 以十进制文本往返，{@code dialects} 以 jsonb 字符串数组往返（复刻旧实现读取时的
 * {@code Set.copyOf} 去序语义），{@code createdAt/createdBy/updatedAt/updatedBy} 只读地投影自 MP 基础审计列，
 * 业务 {@code deleted} 由 {@code deletedAt} 是否为空推导，tenant/审计/软删/version 列一律不由业务写入。
 * English summary: {@code McpServerPersistenceConverter} is the only MapStruct converter at the persistence boundary of the
 * {@code gateway_mcp_server} aggregate: it projects {@link McpServerRecordPO} rows onto {@link McpServerBO} carriers and renders
 * carriers back into rows. The identifier and group key round-trip as decimal text, {@code dialects} round-trips as a jsonb string
 * array reproducing the unordered {@code Set.copyOf} the legacy read produced, the audit projections are read-only, the business
 * {@code deleted} flag is derived from {@code deletedAt}, and tenant/audit/soft-delete/version columns are never written from the
 * business carrier.
 *
 * 用法 / Usage: 由 {@code MpMcpServerRepository} 注入使用（bean 名 {@code mcpServerPersistenceConverter}），RecordPO 只能出现在本转换器
 * 与受守卫仓储之间，禁止进入端口或服务签名。
 * Injected by the guarded MCP server repository under the bean name {@code mcpServerPersistenceConverter}; a RecordPO may only meet
 * the business carrier inside this converter and the guarded repository, never in a port or service signature.
 */
@Slf4j
@Component("mcpServerPersistenceConverter")
public class McpServerPersistenceConverter implements BaseConverter<
        McpServerRecordPO,
        McpServerBO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只做空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds null guarding and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final McpServerMapping MAPPING =
            Mappers.getMapper(McpServerMapping.class);

    /**
     * 中文说明：执行 toBusiness 操作，把 {@code gateway_mcp_server} 行模型投影为业务载体，含只读审计列与由 {@code deletedAt}
     * 推导的 {@code deleted} 标志。
     * English summary: Executes the toBusiness operation, projecting a {@code gateway_mcp_server} row onto the business carrier,
     * including the read-only audit columns and the {@code deleted} flag derived from {@code deletedAt}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpServerPersistenceConverter.toBusiness(row)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    public McpServerBO toBusiness(McpServerRecordPO row) {
        if (row == null) {
            log.debug("gateway_mcp_server row is absent; no business carrier projected");
            return null;
        }
        return MAPPING.toBusiness(row);
    }

    /**
     * 中文说明：执行 toPersistence 操作，把业务载体渲染为 MP 行模型，租户、审计、软删与版本列留给受守卫边界补齐。
     * English summary: Executes the toPersistence operation, rendering a {@code gateway_mcp_server} business carrier into the
     * MyBatis-Plus row model while leaving the tenant, audit, soft-delete and version columns for the guarded boundary to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpServerPersistenceConverter.toPersistence(serverBO)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    public McpServerRecordPO toPersistence(McpServerBO carrier) {
        if (carrier == null) {
            log.debug("gateway_mcp_server business carrier is absent; no row rendered");
            return null;
        }
        return MAPPING.toRow(carrier);
    }

    /**
     * 中文说明：执行 toBusinessList 操作，按列表顺序投影行模型；null 或空输入返回空列表而非 {@code null}。
     * English summary: Executes the toBusinessList operation, projecting every row in order; null or empty input yields an empty
     * list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpServerPersistenceConverter.toBusinessList(rows)}。
     * @param rows 参数 行模型列表；parameter row models.
     * @return 返回 业务载体列表；returns the business carriers.
     */
    public List<McpServerBO> toBusinessList(List<McpServerRecordPO> rows) {
        return toTargetList(rows);
    }

    /**
     * 中文说明：执行 toPersistenceList 操作，按列表顺序渲染业务载体；null 或空输入返回空列表而非 {@code null}。
     * English summary: Executes the toPersistenceList operation, rendering every carrier in order; null or empty input yields an
     * empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpServerPersistenceConverter.toPersistenceList(carriers)}。
     * @param carriers 参数 业务载体列表；parameter business carriers.
     * @return 返回 行模型列表；returns the row models.
     */
    public List<McpServerRecordPO> toPersistenceList(List<McpServerBO> carriers) {
        return toSourceList(carriers);
    }

    /**
     * 中文说明：执行 toTarget 操作，即 {@code BaseConverter} 的正向投影（行模型 -> 业务载体）。
     * English summary: Executes the toTarget operation, the forward projection of {@code BaseConverter} (row model to business
     * carrier).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpServerPersistenceConverter.toTarget(row)}。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @Override
    public McpServerBO toTarget(McpServerRecordPO source) {
        return toBusiness(source);
    }

    /**
     * 中文说明：执行 toSource 操作，即 {@code BaseConverter} 的反向渲染（业务载体 -> 行模型）。
     * English summary: Executes the toSource operation, the reverse rendering of {@code BaseConverter} (business carrier to row
     * model).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpServerPersistenceConverter.toSource(carrier)}。
     * @param target 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @Override
    public McpServerRecordPO toSource(McpServerBO target) {
        return toPersistence(target);
    }

    /**
     * 中文说明：执行 newRow 操作，是 {@code toPersistence} 的插入别名，语义为「只带业务列的新行」。
     * English summary: Executes the newRow operation, an insert alias of {@code toPersistence} producing a row carrying business
     * columns only.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpServerPersistenceConverter.newRow(serverBO)}。
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public McpServerRecordPO newRow(McpServerBO carrier) {
        return toPersistence(carrier);
    }
}

/**
 * 中文说明：{@code McpServerMapping} 是 gateway_mcp_server 的 MapStruct 结构映射契约，逐列复刻被替换的读写语义；
 * {@code unmappedTargetPolicy=ERROR} 保证任何新增列都必须在此显式表态。
 * English summary: {@code McpServerMapping} is the MapStruct structural contract for gateway_mcp_server, mirroring the read and
 * write semantics of the code it replaces column by column; {@code unmappedTargetPolicy=ERROR} forces every new column to be
 * stated explicitly here.
 *
 * 用法 / Usage: 由 {@link McpServerPersistenceConverter} 通过 {@code Mappers} 持有，不对外暴露，也不登记为 Spring bean。
 * Held by {@link McpServerPersistenceConverter} through {@code Mappers}; neither published to callers nor registered as a bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface McpServerMapping {

    /**
     * 中文说明：与 jsonb 类型处理器同源的 Jackson 序列化器，仅用于本映射契约内的列形态转换。
     * English summary: The Jackson serializer behind the column shapes of this mapping contract, matching the jsonb type handler.
     */
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 中文说明：dialects 列的字符串数组读取形态。
     * English summary: The read shape of the dialects string array column.
     */
    TypeReference<Set<String>> DIALECT_SET = new TypeReference<>() {
    };

    /**
     * 中文说明：执行 toBusiness 操作，按 {@code McpServerBO} 的字段顺序投影 gateway_mcp_server 行。
     * English summary: Executes the toBusiness operation, projecting a gateway_mcp_server row onto {@code McpServerBO}.
     *
     * 用法 / Usage: 仅由 {@link McpServerPersistenceConverter#toBusiness(McpServerRecordPO)} 调用。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( text( source.getId() ) )")
    @Mapping(target = "gatewayGroupId", expression = "java( text( source.getGatewayGroupId() ) )")
    @Mapping(target = "serverCode", source = "serverCode")
    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "instructions", source = "instructions")
    @Mapping(target = "dialects", expression = "java( dialects( source.getDialects() ) )")
    @Mapping(target = "resourceUri", source = "resourceUri")
    @Mapping(target = "listCacheTtlSeconds", expression = "java( value( source.getListCacheTtlSeconds() ) )")
    @Mapping(target = "enabled", expression = "java( flag( source.getEnabled() ) )")
    @Mapping(target = "revision", expression = "java( value( source.getRevision() ) )")
    @Mapping(target = "deleted", expression = "java( isDeleted( source ) )")
    @Mapping(target = "createdAt", source = "createTime")
    @Mapping(target = "createdBy", source = "createUserId")
    @Mapping(target = "updatedAt", source = "updateTime")
    @Mapping(target = "updatedBy", source = "updateUserId")
    McpServerBO toBusiness(McpServerRecordPO source);

    /**
     * 中文说明：执行 toRow 操作，把业务载体写回 gateway_mcp_server 行；受保护的技术列全部忽略。
     * English summary: Executes the toRow operation, writing the business carrier back onto a gateway_mcp_server row; every
     * protected technical column stays ignored because the persistence metadata handler owns them.
     *
     * 用法 / Usage: 仅由 {@link McpServerPersistenceConverter#toPersistence(McpServerBO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( identifier( source.getId() ) )")
    @Mapping(target = "gatewayGroupId", expression = "java( identifier( source.getGatewayGroupId() ) )")
    @Mapping(target = "serverCode", source = "serverCode")
    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "instructions", source = "instructions")
    @Mapping(target = "dialects", expression = "java( dialectsNode( source.getDialects() ) )")
    @Mapping(target = "resourceUri", source = "resourceUri")
    @Mapping(target = "listCacheTtlSeconds", source = "listCacheTtlSeconds")
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "revision", source = "revision")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    McpServerRecordPO toRow(McpServerBO source);

    /**
     * 中文说明：执行 text 操作，把 MP 的 bigint 列按十进制文本投影，{@code null} 保持 {@code null}。
     * English summary: Executes the text operation, projecting an MP bigint column as decimal text; {@code null} stays
     * {@code null}.
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
     * English summary: Executes the identifier operation, parsing the business decimal identifier into the MP bigint; a blank value
     * is treated as not yet generated so {@code ASSIGN_ID} can fill it, while a malformed value is rejected.
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
                    "gateway_mcp_server identifier must be decimal: " + value,
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 value 操作，把可空 bigint 列投影为业务基础类型；旧列缺失即 0，与旧默认值一致。
     * English summary: Executes the value operation, projecting a nullable bigint column onto the primitive business field; an
     * absent column reads as zero, matching the legacy default.
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
     * 中文说明：执行 dialects 操作，把 jsonb 字符串数组解码为无序集合，等价于旧实现的 {@code Set.copyOf} 读取；
     * 列缺失时返回 {@code null}，交由业务载体的构造不变量如实拒绝。
     * English summary: Executes the dialects operation, decoding the jsonb string array into an unordered set exactly as the legacy
     * {@code Set.copyOf} read did; a missing column yields {@code null} so the carrier's own constructor invariant rejects it
     * truthfully.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 列值；parameter column value。
     * @return 返回 协议集合；returns the dialect set.
     */
    default Set<String> dialects(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return Set.copyOf(OBJECT_MAPPER.convertValue(value, DIALECT_SET));
        } catch (IllegalArgumentException failure) {
            throw new IllegalStateException(
                    "stored MCP string set is invalid",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 dialectsNode 操作，把协议集合渲染为 jsonb 数组；NOT NULL 列在缺失时写入 JSON null 而非 SQL NULL。
     * English summary: Executes the dialectsNode operation, rendering the dialect set as a jsonb array; an absent value writes JSON
     * null into the NOT NULL column instead of SQL NULL.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 协议集合；parameter dialect set。
     * @return 返回 jsonb 节点；returns the jsonb node.
     */
    default JsonNode dialectsNode(Set<String> value) {
        return value == null
                ? OBJECT_MAPPER.nullNode()
                : OBJECT_MAPPER.valueToTree(value);
    }

    /**
     * 中文说明：执行 isDeleted 操作，由软删时间列推导业务 {@code deleted} 标志。
     * English summary: Executes the isDeleted operation, deriving the business {@code deleted} flag from the soft-delete column.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param source 参数 行模型；parameter row model。
     * @return 返回 是否已软删；returns whether the row is soft deleted.
     */
    default boolean isDeleted(McpServerRecordPO source) {
        return source.getDeletedAt() != null;
    }
}
