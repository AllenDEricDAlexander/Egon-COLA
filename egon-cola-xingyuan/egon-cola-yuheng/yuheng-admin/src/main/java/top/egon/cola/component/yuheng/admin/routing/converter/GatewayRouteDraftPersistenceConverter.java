package top.egon.cola.component.yuheng.admin.routing.converter;

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
import top.egon.cola.component.yuheng.admin.routing.domain.bo.GatewayRouteDraftBO;
import top.egon.cola.component.yuheng.admin.routing.domain.po.GatewayRouteDraftRecordPO;

/**
 * 中文说明：{@code GatewayRouteDraftPersistenceConverter} 是 gateway_route_draft 聚合在持久边界唯一的 MapStruct 转换器，负责
 * {@link GatewayRouteDraftRecordPO} 行模型与 {@link GatewayRouteDraftBO} 业务载体之间的双向映射；
 * {@code gatewayGroupId} 与 {@code operationId} 以十进制文本往返，{@code routeId} 保持对外不透明编码字符串，
 * {@code route_content} jsonb 列经 Jackson 在节点与 {@code Map<String, Object>} 之间转换，updatedAt/updatedBy 只读投影自 MP 审计列。
 * English summary: {@code GatewayRouteDraftPersistenceConverter} is the only MapStruct converter at the persistence boundary of
 * gateway_route_draft, mapping {@link GatewayRouteDraftRecordPO} rows and {@link GatewayRouteDraftBO} carriers both ways.
 * {@code gatewayGroupId} and {@code operationId} round-trip as decimal text, {@code routeId} stays an opaque coded string, the
 * {@code route_content} jsonb column travels through Jackson between a node and a {@code Map<String, Object>}, and updatedAt/updatedBy
 * are read-only projections of the MP audit columns.
 *
 * 用法 / Usage: 由 {@code MpGatewayRouteDraftRepository} 等受守卫仓储注入使用（bean 名 {@code gatewayRouteDraftPersistenceConverter}）；
 * 子行以 {@code (gateway_group_id, route_id)} 为业务身份，写方向不生成 MP {@code id}，更新须先载入行再回写业务列。
 * Injected by the guarded route draft repository under the bean name {@code gatewayRouteDraftPersistenceConverter}; a child row is keyed
 * by {@code (gateway_group_id, route_id)}, so the write direction never produces an MP {@code id} and an update loads the row first.
 */
@Slf4j
@Component("gatewayRouteDraftPersistenceConverter")
public class GatewayRouteDraftPersistenceConverter implements BaseConverter<
        GatewayRouteDraftRecordPO,
        GatewayRouteDraftBO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只做空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds null guarding and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final GatewayRouteDraftMapping MAPPING =
            Mappers.getMapper(GatewayRouteDraftMapping.class);

    /**
     * 中文说明：执行 toPersistence 操作，把路由草稿载体渲染为 MP 行模型。
     * English summary: Executes the toPersistence operation, rendering a route draft carrier into the MyBatis-Plus row model, encoding the
     * route content as a jsonb node and leaving the identifier and every technical column to the guarded persistence layer.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayRouteDraftPersistenceConverter.toPersistence(routeBO)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    public GatewayRouteDraftRecordPO toPersistence(GatewayRouteDraftBO carrier) {
        if (carrier == null) {
            log.debug("gateway route draft carrier is absent; no row rendered");
            return null;
        }
        return MAPPING.toRow(carrier);
    }

    /**
     * 中文说明：执行 toBusiness 操作，把路由草稿行投影为业务载体。
     * English summary: Executes the toBusiness operation, projecting a route draft row onto the business carrier, decoding the jsonb route
     * content and reading the last change instant and actor from the MP update audit columns.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayRouteDraftPersistenceConverter.toBusiness(row)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    public GatewayRouteDraftBO toBusiness(GatewayRouteDraftRecordPO row) {
        if (row == null) {
            log.debug("gateway route draft row is absent; no carrier projected");
            return null;
        }
        return MAPPING.toBusiness(row);
    }

    /**
     * 中文说明：执行 toPersistenceList 操作，按列表顺序渲染路由草稿载体。
     * English summary: Executes the toPersistenceList operation, rendering every route draft carrier in order; null or empty input yields
     * an empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayRouteDraftPersistenceConverter.toPersistenceList(carriers)}。
     * @param carriers 参数 业务载体列表；parameter business carriers.
     * @return 返回 行模型列表；returns the row models.
     */
    public List<GatewayRouteDraftRecordPO> toPersistenceList(
            List<GatewayRouteDraftBO> carriers) {
        return toSourceList(carriers);
    }

    /**
     * 中文说明：执行 toBusinessList 操作，按列表顺序投影路由草稿行。
     * English summary: Executes the toBusinessList operation, projecting every route draft row in order; null or empty input yields an
     * empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayRouteDraftPersistenceConverter.toBusinessList(rows)}。
     * @param rows 参数 行模型列表；parameter row models.
     * @return 返回 业务载体列表；returns the business carriers.
     */
    public List<GatewayRouteDraftBO> toBusinessList(
            List<GatewayRouteDraftRecordPO> rows) {
        return toTargetList(rows);
    }

    /**
     * 中文说明：执行 toTarget 操作，即 {@code BaseConverter} 的正向投影（行模型 -> 业务载体）。
     * English summary: Executes the toTarget operation, the forward projection of {@code BaseConverter} (row model to business carrier).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayRouteDraftPersistenceConverter.toTarget(row)}。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @Override
    public GatewayRouteDraftBO toTarget(GatewayRouteDraftRecordPO source) {
        return toBusiness(source);
    }

    /**
     * 中文说明：执行 toSource 操作，即 {@code BaseConverter} 的反向渲染（业务载体 -> 行模型）。
     * English summary: Executes the toSource operation, the reverse rendering of {@code BaseConverter} (business carrier to row model).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayRouteDraftPersistenceConverter.toSource(carrier)}。
     * @param target 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @Override
    public GatewayRouteDraftRecordPO toSource(GatewayRouteDraftBO target) {
        return toPersistence(target);
    }

    /**
     * 中文说明：执行 newRow 操作，是 {@code toPersistence} 的插入别名，语义为「只带业务列的新行」，
     * 技术主键、租户、审计、软删与版本列一律留空交由受守卫边界补齐。
     * English summary: Executes the newRow operation, an insert alias of {@code toPersistence} producing a row carrying business columns
     * only; the technical id, tenant, audit, soft-delete and version columns stay empty for the guarded boundary to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayRouteDraftPersistenceConverter.newRow(routeBO)}。
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public GatewayRouteDraftRecordPO newRow(GatewayRouteDraftBO carrier) {
        return toPersistence(carrier);
    }
}

/**
 * 中文说明：{@code GatewayRouteDraftMapping} 是 gateway_route_draft 的 MapStruct 结构映射契约，逐列复刻被替换的手写 JDBC 语义：
 * NOT NULL 的 {@code route_content} 在业务内容为空时写入 JSON {@code null} 节点（等价于原 {@code json(null)} 绑定），解码失败按原
 * 实现抛出「stored draft value is invalid」。
 * English summary: {@code GatewayRouteDraftMapping} is the MapStruct structural contract for gateway_route_draft, mirroring the JDBC it
 * replaces: the NOT NULL {@code route_content} column receives a JSON {@code null} node when the business content is absent (the legacy
 * {@code json(null)} binding) and a decoding failure raises the legacy {@code stored draft value is invalid} error.
 *
 * 用法 / Usage: 由 {@link GatewayRouteDraftPersistenceConverter} 通过 {@code Mappers} 持有，不对外暴露，也不登记为 Spring bean。
 * Held by {@link GatewayRouteDraftPersistenceConverter} through {@code Mappers}; neither published to callers nor registered as a bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface GatewayRouteDraftMapping {

    /**
     * 中文说明：保存 Jackson 序列化器，禁止 default typing，只做列值与结构化载体之间的转换。
     * English summary: Holds the Jackson converter between the column node and the structured carrier; default typing stays disabled.
     *
     * 用法 / Usage: 仅由本映射的默认方法使用。/ Used only by the default methods of this mapping.
     */
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 中文说明：表示 CONTENT_MAP 这一固定值，声明 {@code route_content} 列的结构化载体类型。
     * English summary: Represents the fixed content map value, the structured carrier type of the {@code route_content} column.
     *
     * 用法 / Usage: 仅由本映射的默认方法使用。/ Used only by the default methods of this mapping.
     */
    TypeReference<Map<String, Object>> CONTENT_MAP = new TypeReference<>() {
    };

    /**
     * 中文说明：执行 toBusiness 操作，按 {@code GatewayRouteDraftBO} 的字段顺序投影路由草稿行。
     * English summary: Executes the toBusiness operation, projecting a route draft row onto {@code GatewayRouteDraftBO}.
     *
     * 用法 / Usage: 仅由 {@link GatewayRouteDraftPersistenceConverter#toBusiness(GatewayRouteDraftRecordPO)} 调用。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "gatewayGroupId", expression = "java( text( source.getGatewayGroupId() ) )")
    @Mapping(target = "routeId", source = "routeId")
    @Mapping(target = "operationId", expression = "java( text( source.getOperationId() ) )")
    @Mapping(target = "content", expression = "java( content( source.getRouteContent() ) )")
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "updatedAt", source = "updateTime")
    @Mapping(target = "updatedBy", source = "updateUserId")
    GatewayRouteDraftBO toBusiness(GatewayRouteDraftRecordPO source);

    /**
     * 中文说明：执行 toRow 操作，把路由草稿载体写回行模型；主键与受保护技术列全部忽略。
     * English summary: Executes the toRow operation, writing the route draft carrier back onto the row model; the identifier and every
     * protected technical column stay ignored.
     *
     * 用法 / Usage: 仅由 {@link GatewayRouteDraftPersistenceConverter#toPersistence(GatewayRouteDraftBO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "gatewayGroupId", expression = "java( identifier( source.getGatewayGroupId() ) )")
    @Mapping(target = "routeId", source = "routeId")
    @Mapping(target = "operationId", expression = "java( identifier( source.getOperationId() ) )")
    @Mapping(target = "routeContent", expression = "java( requiredNode( source.getContent() ) )")
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    GatewayRouteDraftRecordPO toRow(GatewayRouteDraftBO source);

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
     * 中文说明：执行 identifier 操作，把业务十进制标识解析为 MP bigint；空白直接拒绝，因为路由必须归属分组并指向一个 operation。
     * English summary: Executes the identifier operation, parsing a business decimal identifier into the MP bigint; a blank value is
     * rejected outright because a route always belongs to a group and points at one operation.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 业务标识；parameter business identifier。
     * @return 返回 标识；returns the identifier.
     */
    default Long identifier(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("route draft identifier is required");
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(
                    "gateway route draft identifier must be decimal: " + value,
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 content 操作，把 {@code route_content} jsonb 节点解码为结构化对象；JSON {@code null} 投影为 {@code null}，
     * 结构不符按原实现抛出「stored draft value is invalid」。
     * English summary: Executes the content operation, decoding the {@code route_content} jsonb node into the structured object; a JSON
     * {@code null} projects to {@code null} and a shape mismatch raises the legacy {@code stored draft value is invalid} failure.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 jsonb 节点；parameter jsonb node。
     * @return 返回 结构化对象；returns the structured object.
     */
    default Map<String, Object> content(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(value, CONTENT_MAP);
        } catch (IllegalArgumentException failure) {
            throw new IllegalStateException(
                    "stored draft value is invalid",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 requiredNode 操作，把结构化对象编码为 jsonb 节点；NOT NULL 列在业务值为空时写入 JSON {@code null} 节点。
     * English summary: Executes the requiredNode operation, encoding the structured object into a jsonb node; the NOT NULL column receives
     * a JSON {@code null} node when the business value is absent, matching the replaced {@code json(null)} binding.
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
