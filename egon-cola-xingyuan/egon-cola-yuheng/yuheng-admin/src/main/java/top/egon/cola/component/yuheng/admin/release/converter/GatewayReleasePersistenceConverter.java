package top.egon.cola.component.yuheng.admin.release.converter;

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
import top.egon.cola.component.yuheng.admin.release.domain.bo.GatewayReleaseBO;
import top.egon.cola.component.yuheng.admin.release.domain.enums.GatewayReleaseStatus;
import top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseRecordPO;

/**
 * 中文说明：{@code GatewayReleasePersistenceConverter} 是 gateway_release 聚合在持久边界唯一的 MapStruct 转换器，负责
 * {@link GatewayReleaseRecordPO} 行模型与 {@link GatewayReleaseBO} 业务载体之间的双向映射；{@code id} 与
 * {@code gatewayGroupId} 以十进制文本往返，{@code status} 只按枚举名读写（永不 ordinal），
 * {@code validation_report} 与 {@code structured_diff} 两个 NOT NULL jsonb 列经 Jackson 在节点与
 * {@code Map<String, Object>} 之间转换（载体为空时写 JSON {@code null} 节点而非 SQL NULL），
 * createdAt/createdBy/updatedAt 只读投影自 MP 审计列。
 * English summary: {@code GatewayReleasePersistenceConverter} is the only MapStruct converter at the persistence boundary of the
 * gateway_release aggregate, mapping {@link GatewayReleaseRecordPO} rows and {@link GatewayReleaseBO} carriers both ways.
 * {@code id} and {@code gatewayGroupId} round-trip as decimal text, {@code status} is stored as the enum name (never its ordinal), the
 * two NOT NULL jsonb columns {@code validation_report} and {@code structured_diff} travel through Jackson between a node and a
 * {@code Map<String, Object>} (an absent carrier value becomes a JSON {@code null} node, never a SQL NULL), and
 * createdAt/createdBy/updatedAt are read-only projections of the MP audit columns.
 *
 * 用法 / Usage: 由 {@code gateway_release} 的受守卫 MP 仓储注入使用（bean 名 {@code gatewayReleasePersistenceConverter}）；
 * 写方向不触碰租户、审计、软删与 version 列，因为它们由 {@code EgonColaMetaObjectHandler} 独占。
 * Injected by the guarded gateway_release repository under the bean name {@code gatewayReleasePersistenceConverter}; the write direction
 * never touches the tenant, audit, soft-delete or version columns because {@code EgonColaMetaObjectHandler} owns them.
 */
@Slf4j
@Component("gatewayReleasePersistenceConverter")
public class GatewayReleasePersistenceConverter implements BaseConverter<
        GatewayReleaseRecordPO,
        GatewayReleaseBO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只做空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds null guarding and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final GatewayReleaseMapping MAPPING =
            Mappers.getMapper(GatewayReleaseMapping.class);

    /**
     * 中文说明：执行 toPersistence 操作，把 {@code gateway_release} 业务载体渲染为 MP 行模型。
     * English summary: Executes the toPersistence operation, rendering a {@code gateway_release} business carrier into the MyBatis-Plus
     * row model, encoding the two jsonb metadata columns and leaving the tenant, audit, soft-delete and version columns for the
     * guarded persistence layer to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayReleasePersistenceConverter.toPersistence(releaseBO)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    public GatewayReleaseRecordPO toPersistence(GatewayReleaseBO carrier) {
        if (carrier == null) {
            log.debug("gateway_release business carrier is absent; no row rendered");
            return null;
        }
        return MAPPING.toRow(carrier);
    }

    /**
     * 中文说明：执行 toBusiness 操作，把 {@code gateway_release} 行模型投影为业务载体。
     * English summary: Executes the toBusiness operation, projecting a {@code gateway_release} row onto the business carrier, decoding the
     * jsonb validation report and structured diff, restoring the status enum from its stored name and reading the creation and last
     * change instants from the MP audit columns.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayReleasePersistenceConverter.toBusiness(row)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    public GatewayReleaseBO toBusiness(GatewayReleaseRecordPO row) {
        if (row == null) {
            log.debug("gateway_release row is absent; no business carrier projected");
            return null;
        }
        return MAPPING.toBusiness(row);
    }

    /**
     * 中文说明：执行 toPersistenceList 操作，按列表顺序渲染业务载体。
     * English summary: Executes the toPersistenceList operation, rendering every carrier of the list in order; null or empty input
     * yields an empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayReleasePersistenceConverter.toPersistenceList(carriers)}。
     * @param carriers 参数 业务载体列表；parameter business carriers.
     * @return 返回 行模型列表；returns the row models.
     */
    public List<GatewayReleaseRecordPO> toPersistenceList(
            List<GatewayReleaseBO> carriers) {
        return toSourceList(carriers);
    }

    /**
     * 中文说明：执行 toBusinessList 操作，按列表顺序投影行模型。
     * English summary: Executes the toBusinessList operation, projecting every row of the list in order; null or empty input yields an
     * empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayReleasePersistenceConverter.toBusinessList(rows)}。
     * @param rows 参数 行模型列表；parameter row models.
     * @return 返回 业务载体列表；returns the business carriers.
     */
    public List<GatewayReleaseBO> toBusinessList(
            List<GatewayReleaseRecordPO> rows) {
        return toTargetList(rows);
    }

    /**
     * 中文说明：执行 toTarget 操作，即 {@code BaseConverter} 的正向投影（行模型 -> 业务载体）。
     * English summary: Executes the toTarget operation, the forward projection of {@code BaseConverter} (row model to business carrier).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayReleasePersistenceConverter.toTarget(row)}。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @Override
    public GatewayReleaseBO toTarget(GatewayReleaseRecordPO source) {
        return toBusiness(source);
    }

    /**
     * 中文说明：执行 toSource 操作，即 {@code BaseConverter} 的反向渲染（业务载体 -> 行模型）。
     * English summary: Executes the toSource operation, the reverse rendering of {@code BaseConverter} (business carrier to row model).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayReleasePersistenceConverter.toSource(carrier)}。
     * @param target 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @Override
    public GatewayReleaseRecordPO toSource(GatewayReleaseBO target) {
        return toPersistence(target);
    }

    /**
     * 中文说明：执行 newRow 操作，是 {@code toPersistence} 的插入别名，语义为「只带业务列的新行」，
     * 租户、审计、软删与版本列一律留空交由受守卫边界补齐。
     * English summary: Executes the newRow operation, an insert alias of {@code toPersistence} producing a row carrying business columns
     * only; the tenant, audit, soft-delete and version columns stay empty for the guarded boundary to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayReleasePersistenceConverter.newRow(releaseBO)}。
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public GatewayReleaseRecordPO newRow(GatewayReleaseBO carrier) {
        return toPersistence(carrier);
    }
}

/**
 * 中文说明：{@code GatewayReleaseMapping} 是 gateway_release 的 MapStruct 结构映射契约，逐列复刻被替换的手写 JDBC 语义：
 * 状态列按 {@code GatewayReleaseStatus} 枚举名读写，两个 NOT NULL jsonb 列的解码失败沿用原「stored release metadata is
 * invalid」错误，写方向在载体缺值时落 JSON {@code null} 节点（等价原 {@code json(null)} 绑定）；
 * {@code unmappedTargetPolicy=ERROR} 保证任何新增列都必须在此显式表态。
 * English summary: {@code GatewayReleaseMapping} is the MapStruct structural contract for gateway_release, mirroring column by column
 * the hand-written JDBC it replaces: the status column is stored as the {@code GatewayReleaseStatus} name, a decoding failure of the
 * two NOT NULL jsonb columns keeps the legacy {@code stored release metadata is invalid} error, and the write direction renders an
 * absent carrier value as a JSON {@code null} node (the legacy {@code json(null)} binding);
 * {@code unmappedTargetPolicy=ERROR} forces every new column to be stated explicitly here.
 *
 * 用法 / Usage: 由 {@link GatewayReleasePersistenceConverter} 通过 {@code Mappers} 持有，不对外暴露，也不登记为 Spring bean。
 * Held by {@link GatewayReleasePersistenceConverter} through {@code Mappers}; neither published to callers nor registered as a bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface GatewayReleaseMapping {

    /**
     * 中文说明：保存 Jackson 序列化器，禁止 default typing，只做列值与结构化载体之间的转换。
     * English summary: Holds the Jackson converter between the column node and the structured carrier; default typing stays disabled.
     *
     * 用法 / Usage: 仅由本映射的默认方法使用。/ Used only by the default methods of this mapping.
     */
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 中文说明：表示 RELEASE_METADATA 这一固定值，声明 {@code validation_report} 与 {@code structured_diff} 两个 jsonb 列的结构化载体类型。
     * English summary: Represents the fixed release metadata value, the structured carrier type of the {@code validation_report} and
     * {@code structured_diff} jsonb columns.
     *
     * 用法 / Usage: 仅由本映射的默认方法使用。/ Used only by the default methods of this mapping.
     */
    TypeReference<Map<String, Object>> RELEASE_METADATA = new TypeReference<>() {
    };

    /**
     * 中文说明：执行 toBusiness 操作，按 {@code GatewayReleaseBO} 的字段顺序投影 gateway_release 行。
     * English summary: Executes the toBusiness operation, projecting a gateway_release row onto {@code GatewayReleaseBO}.
     *
     * 用法 / Usage: 仅由 {@link GatewayReleasePersistenceConverter#toBusiness(GatewayReleaseRecordPO)} 调用。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( text( source.getId() ) )")
    @Mapping(target = "gatewayGroupId", expression = "java( text( source.getGatewayGroupId() ) )")
    @Mapping(target = "draftRevision", source = "draftRevision")
    @Mapping(target = "basedOnReleaseId", source = "basedOnReleaseId")
    @Mapping(target = "rollbackOfReleaseId", source = "rollbackOfReleaseId")
    @Mapping(target = "status", expression = "java( status( source.getStatus() ) )")
    @Mapping(target = "partialApplied", source = "partialApplied")
    @Mapping(target = "changeId", source = "changeId")
    @Mapping(target = "validationReport", expression = "java( metadata( source.getValidationReport() ) )")
    @Mapping(target = "structuredDiff", expression = "java( metadata( source.getStructuredDiff() ) )")
    @Mapping(target = "changeReason", source = "changeReason")
    @Mapping(target = "createdAt", source = "createTime")
    @Mapping(target = "createdBy", source = "createUserId")
    @Mapping(target = "updatedAt", source = "updateTime")
    GatewayReleaseBO toBusiness(GatewayReleaseRecordPO source);

    /**
     * 中文说明：执行 toRow 操作，把业务载体写回 gateway_release 行；受保护技术列全部忽略，因为
     * {@code EgonColaMetaObjectHandler} 独占租户、审计、软删与版本列。
     * English summary: Executes the toRow operation, writing the business carrier back onto a gateway_release row; every protected
     * technical column stays ignored because {@code EgonColaMetaObjectHandler} owns the tenant, audit, soft-delete and version columns.
     *
     * 用法 / Usage: 仅由 {@link GatewayReleasePersistenceConverter#toPersistence(GatewayReleaseBO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( identifier( source.getId() ) )")
    @Mapping(target = "gatewayGroupId", expression = "java( identifier( source.getGatewayGroupId() ) )")
    @Mapping(target = "draftRevision", source = "draftRevision")
    @Mapping(target = "basedOnReleaseId", source = "basedOnReleaseId")
    @Mapping(target = "rollbackOfReleaseId", source = "rollbackOfReleaseId")
    @Mapping(target = "status", expression = "java( statusName( source.getStatus() ) )")
    @Mapping(target = "partialApplied", source = "partialApplied")
    @Mapping(target = "changeId", source = "changeId")
    @Mapping(target = "validationReport", expression = "java( requiredNode( source.getValidationReport() ) )")
    @Mapping(target = "structuredDiff", expression = "java( requiredNode( source.getStructuredDiff() ) )")
    @Mapping(target = "changeReason", source = "changeReason")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    GatewayReleaseRecordPO toRow(GatewayReleaseBO source);

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
     * 中文说明：执行 identifier 操作，把业务十进制标识解析为 MP bigint；空白视为尚未生成，交由 {@code ASSIGN_ID} 补位，
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
                    "gateway release identifier must be decimal: " + value,
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 status 操作，按存储的枚举名还原 {@code GatewayReleaseStatus}，与原
     * {@code GatewayReleaseStatus.valueOf(...)} 一致：空值保持为空，未知名直接抛错而不兜底。
     * English summary: Executes the status operation, restoring {@code GatewayReleaseStatus} from the stored enum name exactly as the
     * legacy {@code GatewayReleaseStatus.valueOf(...)} did: {@code null} stays {@code null} and an unknown name fails instead of
     * falling back.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 存储的枚举名；parameter stored enum name。
     * @return 返回 状态枚举或 {@code null}；returns the status enum or {@code null}.
     */
    default GatewayReleaseStatus status(String value) {
        return value == null ? null : GatewayReleaseStatus.valueOf(value);
    }

    /**
     * 中文说明：执行 statusName 操作，按枚举名写回状态列，绝不写入 ordinal，与原 {@code status.name()} 绑定相同。
     * English summary: Executes the statusName operation, writing the status column as the enum name and never its ordinal, matching the
     * legacy {@code status.name()} binding.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 状态枚举；parameter status enum。
     * @return 返回 枚举名或 {@code null}；returns the enum name or {@code null}.
     */
    default String statusName(GatewayReleaseStatus value) {
        return value == null ? null : value.name();
    }

    /**
     * 中文说明：执行 metadata 操作，把 {@code validation_report} / {@code structured_diff} jsonb 节点解码为结构化对象；
     * SQL NULL 或 JSON {@code null} 投影为 {@code null}，结构不符按原实现抛出「stored release metadata is invalid」。
     * English summary: Executes the metadata operation, decoding a {@code validation_report} or {@code structured_diff} jsonb node into
     * the structured object; a SQL NULL or a JSON {@code null} projects to {@code null} and a shape mismatch raises the legacy
     * {@code stored release metadata is invalid} failure.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 jsonb 节点；parameter jsonb node。
     * @return 返回 结构化对象；returns the structured object.
     */
    default Map<String, Object> metadata(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(value, RELEASE_METADATA);
        } catch (IllegalArgumentException failure) {
            throw new IllegalStateException(
                    "stored release metadata is invalid",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 requiredNode 操作，把结构化对象编码为 jsonb 节点；NOT NULL 列在业务值为空时写入 JSON {@code null} 节点，
     * 与原 {@code ?::jsonb} + {@code json(null)} 绑定一致，绝不下发 SQL NULL。
     * English summary: Executes the requiredNode operation, encoding the structured object into a jsonb node; the NOT NULL column
     * receives a JSON {@code null} node when the business value is absent, exactly like the replaced {@code ?::jsonb} binding of
     * {@code json(null)}, and never a SQL NULL.
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
