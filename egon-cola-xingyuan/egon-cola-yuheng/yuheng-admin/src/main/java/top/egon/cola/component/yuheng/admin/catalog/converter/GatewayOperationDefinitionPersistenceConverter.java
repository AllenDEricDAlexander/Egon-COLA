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
import top.egon.cola.component.yuheng.admin.catalog.domain.bo.GatewayOperationDefinitionBO;
import top.egon.cola.component.yuheng.admin.catalog.domain.po.GatewayOperationDefinitionRecordPO;

/**
 * 中文说明：{@code GatewayOperationDefinitionPersistenceConverter} 是 gateway_operation_definition 聚合在持久边界唯一的 MapStruct 转换器，
 * 负责 {@link GatewayOperationDefinitionRecordPO} 行模型与 {@link GatewayOperationDefinitionBO} 业务载体之间的双向映射：
 * 主键与 {@code operationId} 外键以十进制文本往返，{@code tags}/{@code requestSchema}/{@code responseSchema}/{@code errorSchema}/
 * {@code descriptorSnapshot}/{@code attributes} 六个 jsonb 列经 Jackson 在节点与结构化载体之间转换，业务
 * {@code definitionVersion} 与 MP {@code version} 互不替代。
 * English summary: {@code GatewayOperationDefinitionPersistenceConverter} is the only MapStruct converter at the persistence boundary of
 * gateway_operation_definition, mapping {@link GatewayOperationDefinitionRecordPO} rows and {@link GatewayOperationDefinitionBO}
 * carriers both ways. The key and the {@code operationId} foreign key round-trip as decimal text, the six jsonb columns
 * {@code tags}/{@code requestSchema}/{@code responseSchema}/{@code errorSchema}/{@code descriptorSnapshot}/{@code attributes} travel
 * through Jackson between nodes and structured carriers, and the business {@code definitionVersion} stays separate from the MP
 * {@code version}.
 *
 * 用法 / Usage: 由 {@code MpGatewayOperationDefinitionPersistenceRepository} 等受守卫仓储注入使用（bean 名
 * {@code gatewayOperationDefinitionPersistenceConverter}），RecordPO 只能出现在本转换器与受守卫仓储之间，禁止进入端口或服务签名。
 * Injected by the guarded definition repository under the bean name {@code gatewayOperationDefinitionPersistenceConverter}; a RecordPO
 * may only meet the business carrier inside this converter and the guarded repository, never in a port or service signature.
 */
@Slf4j
@Component("gatewayOperationDefinitionPersistenceConverter")
public class GatewayOperationDefinitionPersistenceConverter implements BaseConverter<
        GatewayOperationDefinitionRecordPO,
        GatewayOperationDefinitionBO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只做空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds null guarding and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final GatewayOperationDefinitionMapping MAPPING =
            Mappers.getMapper(GatewayOperationDefinitionMapping.class);

    /**
     * 中文说明：执行 toPersistence 操作，把 definition 业务载体渲染为 MP 行模型。
     * English summary: Executes the toPersistence operation, rendering a definition carrier into the MyBatis-Plus row model; the jsonb
     * columns are encoded as nodes and {@code definitionSetId} plus the tenant, audit, soft-delete and version columns are left alone.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayOperationDefinitionPersistenceConverter.toPersistence(definitionBO)}。
     * 传入 {@code null} 返回 {@code null}。/ Passing {@code null} returns {@code null}.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    public GatewayOperationDefinitionRecordPO toPersistence(GatewayOperationDefinitionBO carrier) {
        if (carrier == null) {
            log.debug("gateway operation definition carrier is absent; no row rendered");
            return null;
        }
        return MAPPING.toRow(carrier);
    }

    /**
     * 中文说明：执行 toBusiness 操作，把 definition 行模型投影为业务载体。
     * English summary: Executes the toBusiness operation, projecting a definition row onto the business carrier, decoding every jsonb
     * column and reading the creation instant and author from the MP audit columns.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayOperationDefinitionPersistenceConverter.toBusiness(row)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    public GatewayOperationDefinitionBO toBusiness(GatewayOperationDefinitionRecordPO row) {
        if (row == null) {
            log.debug("gateway operation definition row is absent; no carrier projected");
            return null;
        }
        return MAPPING.toBusiness(row);
    }

    /**
     * 中文说明：执行 toPersistenceList 操作，按列表顺序渲染 definition 载体。
     * English summary: Executes the toPersistenceList operation, rendering every definition carrier in order; null or empty input yields
     * an empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayOperationDefinitionPersistenceConverter.toPersistenceList(carriers)}。
     * @param carriers 参数 业务载体列表；parameter business carriers.
     * @return 返回 行模型列表；returns the row models.
     */
    public List<GatewayOperationDefinitionRecordPO> toPersistenceList(
            List<GatewayOperationDefinitionBO> carriers) {
        return toSourceList(carriers);
    }

    /**
     * 中文说明：执行 toBusinessList 操作，按列表顺序投影 definition 行。
     * English summary: Executes the toBusinessList operation, projecting every definition row in order; null or empty input yields an
     * empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayOperationDefinitionPersistenceConverter.toBusinessList(rows)}。
     * @param rows 参数 行模型列表；parameter row models.
     * @return 返回 业务载体列表；returns the business carriers.
     */
    public List<GatewayOperationDefinitionBO> toBusinessList(
            List<GatewayOperationDefinitionRecordPO> rows) {
        return toTargetList(rows);
    }

    /**
     * 中文说明：执行 toTarget 操作，即 {@code BaseConverter} 的正向投影（行模型 -> 业务载体）。
     * English summary: Executes the toTarget operation, the forward projection of {@code BaseConverter} (row model to business carrier).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayOperationDefinitionPersistenceConverter.toTarget(row)}。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @Override
    public GatewayOperationDefinitionBO toTarget(GatewayOperationDefinitionRecordPO source) {
        return toBusiness(source);
    }

    /**
     * 中文说明：执行 toSource 操作，即 {@code BaseConverter} 的反向渲染（业务载体 -> 行模型）。
     * English summary: Executes the toSource operation, the reverse rendering of {@code BaseConverter} (business carrier to row model).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayOperationDefinitionPersistenceConverter.toSource(carrier)}。
     * @param target 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @Override
    public GatewayOperationDefinitionRecordPO toSource(GatewayOperationDefinitionBO target) {
        return toPersistence(target);
    }

    /**
     * 中文说明：执行 newRow 操作，是 {@code toPersistence} 的插入别名，语义为「只带业务列的新行」，
     * {@code definitionSetId} 与租户、审计、软删、版本列一律留空交由受守卫边界补齐。
     * English summary: Executes the newRow operation, an insert alias of {@code toPersistence} producing a row carrying business columns
     * only; {@code definitionSetId} plus the tenant, audit, soft-delete and version columns stay empty for the guarded boundary to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayOperationDefinitionPersistenceConverter.newRow(definitionBO)}。
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public GatewayOperationDefinitionRecordPO newRow(GatewayOperationDefinitionBO carrier) {
        return toPersistence(carrier);
    }
}

/**
 * 中文说明：{@code GatewayOperationDefinitionMapping} 是 gateway_operation_definition 的 MapStruct 结构映射契约，逐列复刻被替换的手写
 * JDBC 语义：NOT NULL 的 jsonb 列在业务值为空时写入 JSON {@code null} 节点（等价于原 {@code json(null)} 绑定），可空的
 * {@code descriptor_snapshot} 保持 SQL NULL；{@code definition_set_id} 由原实现写 NULL 且从不回读，故只忽略。
 * English summary: {@code GatewayOperationDefinitionMapping} is the MapStruct structural contract for gateway_operation_definition,
 * mirroring the JDBC it replaces column by column: NOT NULL jsonb columns store a JSON {@code null} node when the business value is
 * absent (the legacy {@code json(null)} binding), the nullable {@code descriptor_snapshot} keeps a SQL NULL, and
 * {@code definition_set_id} - written NULL and never read back by the legacy insert - stays ignored.
 *
 * 用法 / Usage: 由 {@link GatewayOperationDefinitionPersistenceConverter} 通过 {@code Mappers} 持有，不对外暴露，也不登记为 Spring bean。
 * Held by {@link GatewayOperationDefinitionPersistenceConverter} through {@code Mappers}; neither published to callers nor registered as
 * a bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface GatewayOperationDefinitionMapping {

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
     * 中文说明：表示 TAGS 这一固定值，声明 {@code tags} 列的字符串列表载体类型。
     * English summary: Represents the fixed tags value, the structured string-list carrier type of the {@code tags} column.
     *
     * 用法 / Usage: 仅由本映射的默认方法使用。/ Used only by the default methods of this mapping.
     */
    TypeReference<List<String>> TAGS = new TypeReference<>() {
    };

    /**
     * 中文说明：表示 ERROR_SCHEMA 这一固定值，声明 {@code error_schema} 列的对象列表载体类型。
     * English summary: Represents the fixed error schema value, the structured object-list carrier type of the {@code error_schema} column.
     *
     * 用法 / Usage: 仅由本映射的默认方法使用。/ Used only by the default methods of this mapping.
     */
    TypeReference<List<Map<String, Object>>> ERROR_SCHEMA = new TypeReference<>() {
    };

    /**
     * 中文说明：执行 toBusiness 操作，按 {@code GatewayOperationDefinitionBO} 的字段顺序投影 definition 行。
     * English summary: Executes the toBusiness operation, projecting a definition row onto {@code GatewayOperationDefinitionBO}.
     *
     * 用法 / Usage: 仅由 {@link GatewayOperationDefinitionPersistenceConverter#toBusiness(GatewayOperationDefinitionRecordPO)} 调用。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( text( source.getId() ) )")
    @Mapping(target = "operationId", expression = "java( text( source.getOperationId() ) )")
    @Mapping(target = "definitionVersion", source = "definitionVersion")
    @Mapping(target = "definitionSha256", source = "definitionSha256")
    @Mapping(target = "summary", source = "summary")
    @Mapping(target = "tags", expression = "java( tags( source.getTags() ) )")
    @Mapping(target = "requestSchema",
            expression = "java( objectMap( source.getRequestSchema(), \"request schema\" ) )")
    @Mapping(target = "responseSchema",
            expression = "java( objectMap( source.getResponseSchema(), \"response schema\" ) )")
    @Mapping(target = "errorSchema", expression = "java( errorSchema( source.getErrorSchema() ) )")
    @Mapping(target = "descriptorSnapshot",
            expression = "java( objectMap( source.getDescriptorSnapshot(), \"descriptor snapshot\" ) )")
    @Mapping(target = "attributes", expression = "java( objectMap( source.getAttributes(), \"attributes\" ) )")
    @Mapping(target = "externalAccessible", source = "externalAccessible")
    @Mapping(target = "createdAt", source = "createTime")
    @Mapping(target = "createdBy", source = "createUserId")
    GatewayOperationDefinitionBO toBusiness(GatewayOperationDefinitionRecordPO source);

    /**
     * 中文说明：执行 toRow 操作，把 definition 业务载体写回行模型；定义集外键与受保护技术列全部忽略。
     * English summary: Executes the toRow operation, writing the definition carrier back onto the row model; the definition-set foreign
     * key and every protected technical column stay ignored.
     *
     * 用法 / Usage: 仅由 {@link GatewayOperationDefinitionPersistenceConverter#toPersistence(GatewayOperationDefinitionBO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( identifier( source.getId() ) )")
    @Mapping(target = "operationId", expression = "java( identifier( source.getOperationId() ) )")
    @Mapping(target = "definitionVersion", source = "definitionVersion")
    @Mapping(target = "definitionSha256", source = "definitionSha256")
    @Mapping(target = "summary", source = "summary")
    @Mapping(target = "tags", expression = "java( tagsNode( source.getTags() ) )")
    @Mapping(target = "requestSchema",
            expression = "java( requiredNode( source.getRequestSchema() ) )")
    @Mapping(target = "responseSchema",
            expression = "java( requiredNode( source.getResponseSchema() ) )")
    @Mapping(target = "errorSchema", expression = "java( errorSchemaNode( source.getErrorSchema() ) )")
    @Mapping(target = "descriptorSnapshot",
            expression = "java( nullableNode( source.getDescriptorSnapshot() ) )")
    @Mapping(target = "attributes", expression = "java( requiredNode( source.getAttributes() ) )")
    @Mapping(target = "externalAccessible", source = "externalAccessible")
    @Mapping(target = "definitionSetId", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    GatewayOperationDefinitionRecordPO toRow(GatewayOperationDefinitionBO source);

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
     * 中文说明：执行 identifier 操作，把业务十进制标识解析为 MP bigint；空白视为未生成，非法值直接拒绝。
     * English summary: Executes the identifier operation, parsing a business decimal identifier into the MP bigint; blank means not yet
     * generated and a malformed value is rejected.
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
                    "gateway operation definition identifier must be decimal: " + value,
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
     * 中文说明：执行 tags 操作，把 {@code tags} jsonb 数组解码为字符串列表，语义与原 {@code list(...)} 读取一致。
     * English summary: Executes the tags operation, decoding the {@code tags} jsonb array into a string list with the semantics of the
     * legacy {@code list(...)} read.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 jsonb 节点；parameter jsonb node。
     * @return 返回 字符串列表；returns the string list.
     */
    default List<String> tags(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(value, TAGS);
        } catch (IllegalArgumentException failure) {
            throw new IllegalStateException("stored tags are invalid", failure);
        }
    }

    /**
     * 中文说明：执行 errorSchema 操作，把 {@code error_schema} jsonb 数组解码为对象列表，语义与原 {@code mapList(...)} 读取一致。
     * English summary: Executes the errorSchema operation, decoding the {@code error_schema} jsonb array into an object list with the
     * semantics of the legacy {@code mapList(...)} read.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 jsonb 节点；parameter jsonb node。
     * @return 返回 对象列表；returns the object list.
     */
    default List<Map<String, Object>> errorSchema(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(value, ERROR_SCHEMA);
        } catch (IllegalArgumentException failure) {
            throw new IllegalStateException("stored error schema is invalid", failure);
        }
    }

    /**
     * 中文说明：执行 requiredNode 操作，把结构化对象编码为 jsonb 节点；NOT NULL 列在业务值为空时写入 JSON {@code null} 节点。
     * English summary: Executes the requiredNode operation, encoding a structured object into a jsonb node; a NOT NULL column receives a
     * JSON {@code null} node when the business value is absent, matching the replaced {@code json(null)} binding.
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

    /**
     * 中文说明：执行 nullableNode 操作，编码可空 jsonb 列；业务值为空时保持 Java {@code null}，由列写 SQL NULL。
     * English summary: Executes the nullableNode operation for a nullable jsonb column, keeping Java {@code null} so the column stores a
     * SQL NULL, exactly as the legacy guard did for {@code descriptor_snapshot}.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 结构化对象；parameter structured object。
     * @return 返回 jsonb 节点或 {@code null}；returns the jsonb node or {@code null}.
     */
    default JsonNode nullableNode(Map<String, Object> value) {
        return value == null ? null : OBJECT_MAPPER.valueToTree(value);
    }

    /**
     * 中文说明：执行 tagsNode 操作，把字符串列表编码为 {@code tags} jsonb 节点。
     * English summary: Executes the tagsNode operation, encoding the string list into the {@code tags} jsonb node.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 字符串列表；parameter string list。
     * @return 返回 jsonb 节点；returns the jsonb node.
     */
    default JsonNode tagsNode(List<String> value) {
        return value == null
                ? OBJECT_MAPPER.nullNode()
                : OBJECT_MAPPER.valueToTree(value);
    }

    /**
     * 中文说明：执行 errorSchemaNode 操作，把对象列表编码为 {@code error_schema} jsonb 节点。
     * English summary: Executes the errorSchemaNode operation, encoding the object list into the {@code error_schema} jsonb node.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 对象列表；parameter object list。
     * @return 返回 jsonb 节点；returns the jsonb node.
     */
    default JsonNode errorSchemaNode(List<Map<String, Object>> value) {
        return value == null
                ? OBJECT_MAPPER.nullNode()
                : OBJECT_MAPPER.valueToTree(value);
    }
}
