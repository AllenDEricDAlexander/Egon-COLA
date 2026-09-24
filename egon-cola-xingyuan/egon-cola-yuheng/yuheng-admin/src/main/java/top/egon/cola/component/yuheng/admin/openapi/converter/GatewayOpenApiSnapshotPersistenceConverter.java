package top.egon.cola.component.yuheng.admin.openapi.converter;

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
import top.egon.cola.component.yuheng.admin.openapi.domain.bo.GatewayOpenApiSnapshotBO;
import top.egon.cola.component.yuheng.admin.openapi.domain.po.GatewayOpenApiSnapshotRecordPO;

/**
 * 中文说明：{@code GatewayOpenApiSnapshotPersistenceConverter} 是 gateway_openapi_snapshot 聚合在持久边界唯一的 MapStruct
 * 转换器，负责 {@link GatewayOpenApiSnapshotRecordPO} 行模型与 {@link GatewayOpenApiSnapshotBO} 业务载体之间的双向映射；
 * {@code id}、{@code application_id} 与可空的 {@code definition_set_id} 以十进制文本往返（空白即 {@code null}，不写空串），
 * {@code document_json} 与 {@code validation_messages} 两个 NOT NULL jsonb 列分别按对象与数组解码，
 * {@code document_sha256}/{@code canonical_sha256} 是纯文本摘要列、本转换器只透传，
 * {@code validation_status} 仍是 {@code VALID}/{@code INVALID} 文本列而非枚举，createdAt 只读投影自 MP {@code create_time}。
 * English summary: {@code GatewayOpenApiSnapshotPersistenceConverter} is the only MapStruct converter at the persistence boundary of the
 * gateway_openapi_snapshot aggregate, mapping {@link GatewayOpenApiSnapshotRecordPO} rows and {@link GatewayOpenApiSnapshotBO} carriers
 * both ways. {@code id}, {@code application_id} and the nullable {@code definition_set_id} round-trip as decimal text (blank becomes
 * {@code null}, never an empty string), the two NOT NULL jsonb columns {@code document_json} and {@code validation_messages} decode as an
 * object and as an array respectively, {@code document_sha256} and {@code canonical_sha256} are plain digest columns that are passed
 * through untouched, {@code validation_status} stays the {@code VALID}/{@code INVALID} text column rather than an enum, and createdAt is a
 * read-only projection of the MP {@code create_time}.
 *
 * 用法 / Usage: 由 gateway_openapi_snapshot 的受守卫 MP 仓储注入使用（bean 名 {@code gatewayOpenApiSnapshotPersistenceConverter}）；
 * 不可变契约校验与摘要规范仍归 {@link GatewayOpenApiSnapshotBO#validated(GatewayOpenApiSnapshotBO)} 所有，本转换器既不复制也不削弱它。
 * Injected by the guarded gateway_openapi_snapshot repository under the bean name
 * {@code gatewayOpenApiSnapshotPersistenceConverter}; the immutability contract and digest normalization stay owned by
 * {@link GatewayOpenApiSnapshotBO#validated(GatewayOpenApiSnapshotBO)}, which this converter neither duplicates nor weakens.
 */
@Slf4j
@Component("gatewayOpenApiSnapshotPersistenceConverter")
public class GatewayOpenApiSnapshotPersistenceConverter implements BaseConverter<
        GatewayOpenApiSnapshotRecordPO,
        GatewayOpenApiSnapshotBO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只做空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds null guarding and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final GatewayOpenApiSnapshotMapping MAPPING =
            Mappers.getMapper(GatewayOpenApiSnapshotMapping.class);

    /**
     * 中文说明：执行 toPersistence 操作，把快照载体渲染为 MP 行模型。
     * English summary: Executes the toPersistence operation, rendering a snapshot carrier into the MyBatis-Plus row model, encoding the
     * document object and the validation message array as jsonb nodes and leaving the tenant, audit, soft-delete and version columns for
     * the guarded persistence layer to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayOpenApiSnapshotPersistenceConverter.toPersistence(snapshotBO)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    public GatewayOpenApiSnapshotRecordPO toPersistence(GatewayOpenApiSnapshotBO carrier) {
        if (carrier == null) {
            log.debug("gateway_openapi_snapshot business carrier is absent; no row rendered");
            return null;
        }
        return MAPPING.toRow(carrier);
    }

    /**
     * 中文说明：执行 toBusiness 操作，把快照行投影为业务载体。
     * English summary: Executes the toBusiness operation, projecting a snapshot row onto the business carrier, decoding the jsonb document
     * and message array with the legacy error messages and reading the creation instant from the MP audit column.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayOpenApiSnapshotPersistenceConverter.toBusiness(row)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    public GatewayOpenApiSnapshotBO toBusiness(GatewayOpenApiSnapshotRecordPO row) {
        if (row == null) {
            log.debug("gateway_openapi_snapshot row is absent; no business carrier projected");
            return null;
        }
        return MAPPING.toBusiness(row);
    }

    /**
     * 中文说明：执行 toPersistenceList 操作，按列表顺序渲染快照载体。
     * English summary: Executes the toPersistenceList operation, rendering every snapshot carrier in order; null or empty input yields an
     * empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayOpenApiSnapshotPersistenceConverter.toPersistenceList(carriers)}。
     * @param carriers 参数 业务载体列表；parameter business carriers.
     * @return 返回 行模型列表；returns the row models.
     */
    public List<GatewayOpenApiSnapshotRecordPO> toPersistenceList(
            List<GatewayOpenApiSnapshotBO> carriers) {
        return toSourceList(carriers);
    }

    /**
     * 中文说明：执行 toBusinessList 操作，按列表顺序投影快照行。
     * English summary: Executes the toBusinessList operation, projecting every snapshot row in order; null or empty input yields an empty
     * list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayOpenApiSnapshotPersistenceConverter.toBusinessList(rows)}。
     * @param rows 参数 行模型列表；parameter row models.
     * @return 返回 业务载体列表；returns the business carriers.
     */
    public List<GatewayOpenApiSnapshotBO> toBusinessList(
            List<GatewayOpenApiSnapshotRecordPO> rows) {
        return toTargetList(rows);
    }

    /**
     * 中文说明：执行 toTarget 操作，即 {@code BaseConverter} 的正向投影（行模型 -> 业务载体）。
     * English summary: Executes the toTarget operation, the forward projection of {@code BaseConverter} (row model to business carrier).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayOpenApiSnapshotPersistenceConverter.toTarget(row)}。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @Override
    public GatewayOpenApiSnapshotBO toTarget(GatewayOpenApiSnapshotRecordPO source) {
        return toBusiness(source);
    }

    /**
     * 中文说明：执行 toSource 操作，即 {@code BaseConverter} 的反向渲染（业务载体 -> 行模型）。
     * English summary: Executes the toSource operation, the reverse rendering of {@code BaseConverter} (business carrier to row model).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayOpenApiSnapshotPersistenceConverter.toSource(carrier)}。
     * @param target 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @Override
    public GatewayOpenApiSnapshotRecordPO toSource(GatewayOpenApiSnapshotBO target) {
        return toPersistence(target);
    }

    /**
     * 中文说明：执行 newRow 操作，是 {@code toPersistence} 的插入别名，语义为「只带业务列的新行」，
     * 租户、审计、软删与版本列一律留空交由受守卫边界补齐。
     * English summary: Executes the newRow operation, an insert alias of {@code toPersistence} producing a row carrying business columns
     * only; the tenant, audit, soft-delete and version columns stay empty for the guarded boundary to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayOpenApiSnapshotPersistenceConverter.newRow(snapshotBO)}。
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public GatewayOpenApiSnapshotRecordPO newRow(GatewayOpenApiSnapshotBO carrier) {
        return toPersistence(carrier);
    }
}

/**
 * 中文说明：{@code GatewayOpenApiSnapshotMapping} 是 gateway_openapi_snapshot 的 MapStruct 结构映射契约，逐列复刻被替换的手写
 * JDBC 语义：{@code document_json} 解码失败沿用「invalid JSON object in document_json」、{@code validation_messages} 沿用
 * 「invalid JSON array in validation_messages」，两个 NOT NULL jsonb 列在写方向永远得到 JSON 节点（载体缺值时为 JSON
 * {@code null}）而绝非 SQL NULL；{@code unmappedTargetPolicy=ERROR} 保证任何新增列都必须在此显式表态。
 * English summary: {@code GatewayOpenApiSnapshotMapping} is the MapStruct structural contract for gateway_openapi_snapshot, mirroring the
 * hand-written JDBC it replaces: decoding {@code document_json} keeps the legacy {@code invalid JSON object in document_json} failure,
 * {@code validation_messages} keeps {@code invalid JSON array in validation_messages}, and both NOT NULL jsonb columns always receive a
 * JSON node on write (a JSON {@code null} when the carrier value is absent) and never a SQL NULL;
 * {@code unmappedTargetPolicy=ERROR} forces every new column to be stated explicitly here.
 *
 * 用法 / Usage: 由 {@link GatewayOpenApiSnapshotPersistenceConverter} 通过 {@code Mappers} 持有，不对外暴露，也不登记为 Spring bean。
 * Held by {@link GatewayOpenApiSnapshotPersistenceConverter} through {@code Mappers}; neither published to callers nor registered as a
 * bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface GatewayOpenApiSnapshotMapping {

    /**
     * 中文说明：保存 Jackson 序列化器，禁止 default typing，只做列值与结构化载体之间的转换。
     * English summary: Holds the Jackson converter between the column node and the structured carrier; default typing stays disabled.
     *
     * 用法 / Usage: 仅由本映射的默认方法使用。/ Used only by the default methods of this mapping.
     */
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 中文说明：表示 DOCUMENT_JSON 这一固定值，声明 {@code document_json} 列的结构化载体类型。
     * English summary: Represents the fixed document json value, the structured carrier type of the {@code document_json} column.
     *
     * 用法 / Usage: 仅由本映射的默认方法使用。/ Used only by the default methods of this mapping.
     */
    TypeReference<Map<String, Object>> DOCUMENT_JSON = new TypeReference<>() {
    };

    /**
     * 中文说明：表示 VALIDATION_MESSAGES 这一固定值，声明 {@code validation_messages} 列的字符串数组载体类型。
     * English summary: Represents the fixed validation messages value, the string array carrier type of the
     * {@code validation_messages} column.
     *
     * 用法 / Usage: 仅由本映射的默认方法使用。/ Used only by the default methods of this mapping.
     */
    TypeReference<List<String>> VALIDATION_MESSAGES = new TypeReference<>() {
    };

    /**
     * 中文说明：执行 toBusiness 操作，按 {@code GatewayOpenApiSnapshotBO} 的字段顺序投影快照行。
     * English summary: Executes the toBusiness operation, projecting a snapshot row onto {@code GatewayOpenApiSnapshotBO}.
     *
     * 用法 / Usage: 仅由 {@link GatewayOpenApiSnapshotPersistenceConverter#toBusiness(GatewayOpenApiSnapshotRecordPO)} 调用。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( text( source.getId() ) )")
    @Mapping(target = "applicationId", expression = "java( text( source.getApplicationId() ) )")
    @Mapping(target = "definitionSetId", expression = "java( text( source.getDefinitionSetId() ) )")
    @Mapping(target = "buildId", source = "buildId")
    @Mapping(target = "artifactVersion", source = "artifactVersion")
    @Mapping(target = "openapiGroup", source = "openapiGroup")
    @Mapping(target = "openapiVersion", source = "openapiVersion")
    @Mapping(target = "documentSha256", source = "documentSha256")
    @Mapping(target = "canonicalSha256", source = "canonicalSha256")
    @Mapping(target = "documentJson", expression = "java( documentJson( source.getDocumentJson() ) )")
    @Mapping(target = "validationStatus", source = "validationStatus")
    @Mapping(target = "validationMessages", expression = "java( validationMessages( source.getValidationMessages() ) )")
    @Mapping(target = "operationCount", source = "operationCount")
    @Mapping(target = "schemaCount", source = "schemaCount")
    @Mapping(target = "fetchedFromInstanceId", source = "fetchedFromInstanceId")
    @Mapping(target = "fetchedAt", source = "fetchedAt")
    @Mapping(target = "validatedAt", source = "validatedAt")
    @Mapping(target = "createdAt", source = "createTime")
    GatewayOpenApiSnapshotBO toBusiness(GatewayOpenApiSnapshotRecordPO source);

    /**
     * 中文说明：执行 toRow 操作，把快照载体写回行模型；受保护技术列全部忽略，因为
     * {@code EgonColaMetaObjectHandler} 独占租户、审计、软删与版本列。
     * English summary: Executes the toRow operation, writing the snapshot carrier back onto the row model; every protected technical
     * column stays ignored because {@code EgonColaMetaObjectHandler} owns the tenant, audit, soft-delete and version columns.
     *
     * 用法 / Usage: 仅由 {@link GatewayOpenApiSnapshotPersistenceConverter#toPersistence(GatewayOpenApiSnapshotBO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( identifier( source.getId() ) )")
    @Mapping(target = "applicationId", expression = "java( identifier( source.getApplicationId() ) )")
    @Mapping(target = "definitionSetId", expression = "java( optionalIdentifier( source.getDefinitionSetId() ) )")
    @Mapping(target = "buildId", source = "buildId")
    @Mapping(target = "artifactVersion", source = "artifactVersion")
    @Mapping(target = "openapiGroup", source = "openapiGroup")
    @Mapping(target = "openapiVersion", source = "openapiVersion")
    @Mapping(target = "documentSha256", source = "documentSha256")
    @Mapping(target = "canonicalSha256", source = "canonicalSha256")
    @Mapping(target = "documentJson", expression = "java( documentNode( source.getDocumentJson() ) )")
    @Mapping(target = "validationStatus", source = "validationStatus")
    @Mapping(target = "validationMessages", expression = "java( messagesNode( source.getValidationMessages() ) )")
    @Mapping(target = "operationCount", source = "operationCount")
    @Mapping(target = "schemaCount", source = "schemaCount")
    @Mapping(target = "fetchedFromInstanceId", source = "fetchedFromInstanceId")
    @Mapping(target = "fetchedAt", source = "fetchedAt")
    @Mapping(target = "validatedAt", source = "validatedAt")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    GatewayOpenApiSnapshotRecordPO toRow(GatewayOpenApiSnapshotBO source);

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
     * 中文说明：执行 identifier 操作，把业务十进制标识解析为 MP bigint；空白视为未生成，交由 {@code ASSIGN_ID} 补位，
     * 非法值按原「must be decimal」语义拒绝。
     * English summary: Executes the identifier operation, parsing a business decimal identifier into the MP bigint; a blank value is
     * treated as not yet generated so {@code ASSIGN_ID} can fill it, while a malformed value is rejected with the legacy message.
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
                    "gateway OpenAPI snapshot identifier must be decimal: " + value,
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 optionalIdentifier 操作，解析可空的 bigint 引用列；空白直接归一为 {@code null}，
     * 与原 {@code optional(...)} 的「空白即未链接」语义一致。
     * English summary: Executes the optionalIdentifier operation for a nullable bigint reference column; a blank value normalizes straight
     * to {@code null}, matching the legacy {@code optional(...)} semantics of "blank means not linked".
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 业务标识；parameter business identifier。
     * @return 返回 标识或 {@code null}；returns the identifier or {@code null}.
     */
    default Long optionalIdentifier(String value) {
        return value == null || value.isBlank() ? null : identifier(value);
    }

    /**
     * 中文说明：执行 documentJson 操作，把 {@code document_json} jsonb 对象解码为结构化文档；SQL NULL 或 JSON {@code null}
     * 投影为 {@code null}，结构不符按原实现抛出「invalid JSON object in document_json」。
     * English summary: Executes the documentJson operation, decoding the {@code document_json} jsonb object into the structured document;
     * a SQL NULL or a JSON {@code null} projects to {@code null} and a shape mismatch raises the legacy
     * {@code invalid JSON object in document_json} failure.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 jsonb 节点；parameter jsonb node。
     * @return 返回 结构化文档；returns the structured document.
     */
    default Map<String, Object> documentJson(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(value, DOCUMENT_JSON);
        } catch (IllegalArgumentException failure) {
            throw new IllegalStateException(
                    "invalid JSON object in document_json",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 validationMessages 操作，把 {@code validation_messages} jsonb 数组解码为字符串列表；SQL NULL 或 JSON
     * {@code null} 投影为 {@code null} 并由 {@link GatewayOpenApiSnapshotBO#validated(GatewayOpenApiSnapshotBO)} 拒绝，
     * 结构不符按原实现抛出「invalid JSON array in validation_messages」。
     * English summary: Executes the validationMessages operation, decoding the {@code validation_messages} jsonb array into a string list;
     * a SQL NULL or a JSON {@code null} projects to {@code null} so {@link GatewayOpenApiSnapshotBO#validated(GatewayOpenApiSnapshotBO)}
     * rejects it, and a shape mismatch raises the legacy {@code invalid JSON array in validation_messages} failure.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 jsonb 节点；parameter jsonb node。
     * @return 返回 校验消息列表；returns the validation messages.
     */
    default List<String> validationMessages(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(value, VALIDATION_MESSAGES);
        } catch (IllegalArgumentException failure) {
            throw new IllegalStateException(
                    "invalid JSON array in validation_messages",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 documentNode 操作，把结构化文档编码为 {@code document_json} jsonb 节点；NOT NULL 列在业务值为空时
     * 写入 JSON {@code null} 节点，绝不下发 SQL NULL。
     * English summary: Executes the documentNode operation, encoding the structured document into the {@code document_json} jsonb node; the
     * NOT NULL column receives a JSON {@code null} node when the business value is absent and never a SQL NULL.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 结构化文档；parameter structured document。
     * @return 返回 jsonb 节点；returns the jsonb node.
     */
    default JsonNode documentNode(Map<String, Object> value) {
        return value == null
                ? OBJECT_MAPPER.nullNode()
                : OBJECT_MAPPER.valueToTree(value);
    }

    /**
     * 中文说明：执行 messagesNode 操作，把校验消息列表编码为 {@code validation_messages} jsonb 数组节点；NOT NULL 列在
     * 业务值为空时写入 JSON {@code null} 节点，与原 {@code json(list)} 绑定一致。
     * English summary: Executes the messagesNode operation, encoding the validation message list into the {@code validation_messages} jsonb
     * array node; the NOT NULL column receives a JSON {@code null} node when the business list is absent, exactly like the legacy
     * {@code json(list)} binding.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 校验消息列表；parameter validation messages.
     * @return 返回 jsonb 节点；returns the jsonb node.
     */
    default JsonNode messagesNode(List<String> value) {
        return value == null
                ? OBJECT_MAPPER.nullNode()
                : OBJECT_MAPPER.valueToTree(value);
    }
}
