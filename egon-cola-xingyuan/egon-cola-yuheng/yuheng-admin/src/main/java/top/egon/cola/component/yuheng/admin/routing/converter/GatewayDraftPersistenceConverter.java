package top.egon.cola.component.yuheng.admin.routing.converter;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.component.yuheng.admin.routing.domain.bo.GatewayDraftBO;
import top.egon.cola.component.yuheng.admin.routing.domain.po.GatewayDraftRecordPO;

/**
 * 中文说明：{@code GatewayDraftPersistenceConverter} 是 gateway_draft 聚合头在持久边界唯一的 MapStruct 转换器，负责
 * {@link GatewayDraftRecordPO} 行模型与 {@link GatewayDraftBO} 业务载体之间的双向映射；
 * {@code gatewayGroupId} 以十进制文本往返，{@code basedOnReleaseId} 是对外不透明编码列故保持字符串，业务 {@code revision}
 * 与 MP {@code version} 互不替代，updatedAt/updatedBy 只读投影自 MP 审计列。
 * English summary: {@code GatewayDraftPersistenceConverter} is the only MapStruct converter at the persistence boundary of the
 * gateway_draft head, mapping {@link GatewayDraftRecordPO} rows and {@link GatewayDraftBO} carriers both ways. {@code gatewayGroupId}
 * round-trips as decimal text, {@code basedOnReleaseId} stays an opaque coded string, the business {@code revision} is separate from the
 * MP {@code version}, and updatedAt/updatedBy are read-only projections of the MP audit columns.
 *
 * 用法 / Usage: 由 {@code MpGatewayDraftRepository} 等受守卫仓储注入使用（bean 名 {@code gatewayDraftPersistenceConverter}）；
 * 草稿头业务载体不含主键，写方向无法给出 MP {@code id}，因此更新必须先按 {@code gateway_group_id} 载入行再回写业务列。
 * Injected by the guarded draft repository under the bean name {@code gatewayDraftPersistenceConverter}; the carrier carries no primary
 * key, so the write direction cannot supply the MP {@code id} and an update must load the row by {@code gateway_group_id} first.
 */
@Slf4j
@Component("gatewayDraftPersistenceConverter")
public class GatewayDraftPersistenceConverter implements BaseConverter<
        GatewayDraftRecordPO,
        GatewayDraftBO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只做空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds null guarding and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final GatewayDraftMapping MAPPING =
            Mappers.getMapper(GatewayDraftMapping.class);

    /**
     * 中文说明：执行 toPersistence 操作，把草稿头业务载体渲染为 MP 行模型。
     * English summary: Executes the toPersistence operation, rendering a draft head carrier into the MyBatis-Plus row model; since the
     * carrier has no identifier, the MP key and all tenant, audit, soft-delete and version columns stay untouched.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayDraftPersistenceConverter.toPersistence(draftBO)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    public GatewayDraftRecordPO toPersistence(GatewayDraftBO carrier) {
        if (carrier == null) {
            log.debug("gateway draft business carrier is absent; no row rendered");
            return null;
        }
        return MAPPING.toRow(carrier);
    }

    /**
     * 中文说明：执行 toBusiness 操作，把草稿头行模型投影为业务载体。
     * English summary: Executes the toBusiness operation, projecting a draft head row onto the business carrier, whose last change
     * timestamp and actor come from the MP update audit columns.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayDraftPersistenceConverter.toBusiness(row)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    public GatewayDraftBO toBusiness(GatewayDraftRecordPO row) {
        if (row == null) {
            log.debug("gateway draft row is absent; no business carrier projected");
            return null;
        }
        return MAPPING.toBusiness(row);
    }

    /**
     * 中文说明：执行 toPersistenceList 操作，按列表顺序渲染草稿头载体。
     * English summary: Executes the toPersistenceList operation, rendering every draft head carrier in order; null or empty input yields
     * an empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayDraftPersistenceConverter.toPersistenceList(carriers)}。
     * @param carriers 参数 业务载体列表；parameter business carriers.
     * @return 返回 行模型列表；returns the row models.
     */
    public List<GatewayDraftRecordPO> toPersistenceList(List<GatewayDraftBO> carriers) {
        return toSourceList(carriers);
    }

    /**
     * 中文说明：执行 toBusinessList 操作，按列表顺序投影草稿头行。
     * English summary: Executes the toBusinessList operation, projecting every draft head row in order; null or empty input yields an
     * empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayDraftPersistenceConverter.toBusinessList(rows)}。
     * @param rows 参数 行模型列表；parameter row models.
     * @return 返回 业务载体列表；returns the business carriers.
     */
    public List<GatewayDraftBO> toBusinessList(List<GatewayDraftRecordPO> rows) {
        return toTargetList(rows);
    }

    /**
     * 中文说明：执行 toTarget 操作，即 {@code BaseConverter} 的正向投影（行模型 -> 业务载体）。
     * English summary: Executes the toTarget operation, the forward projection of {@code BaseConverter} (row model to business carrier).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayDraftPersistenceConverter.toTarget(row)}。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @Override
    public GatewayDraftBO toTarget(GatewayDraftRecordPO source) {
        return toBusiness(source);
    }

    /**
     * 中文说明：执行 toSource 操作，即 {@code BaseConverter} 的反向渲染（业务载体 -> 行模型）。
     * English summary: Executes the toSource operation, the reverse rendering of {@code BaseConverter} (business carrier to row model).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayDraftPersistenceConverter.toSource(carrier)}。
     * @param target 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @Override
    public GatewayDraftRecordPO toSource(GatewayDraftBO target) {
        return toPersistence(target);
    }

    /**
     * 中文说明：执行 newRow 操作，是 {@code toPersistence} 的插入别名，语义为「只带业务列的新行」，租户、审计、软删与版本列
     * 一律留空交由受守卫边界补齐。
     * English summary: Executes the newRow operation, an insert alias of {@code toPersistence} producing a row carrying business columns
     * only; the tenant, audit, soft-delete and version columns stay empty for the guarded boundary to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayDraftPersistenceConverter.newRow(draftBO)}。
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public GatewayDraftRecordPO newRow(GatewayDraftBO carrier) {
        return toPersistence(carrier);
    }
}

/**
 * 中文说明：{@code GatewayDraftMapping} 是 gateway_draft 的 MapStruct 结构映射契约，逐列复刻被替换的手写 JDBC 读写语义：
 * 草稿头以 {@code gateway_group_id} 为业务主键，故写方向不生成 MP {@code id}；状态与变更摘要是开放 varchar 列，按原 wire 字符串存取。
 * English summary: {@code GatewayDraftMapping} is the MapStruct structural contract for gateway_draft, mirroring the JDBC it replaces:
 * the draft head is keyed by {@code gateway_group_id} so the write direction never produces an MP {@code id}, and the open varchar
 * {@code status}/{@code changeSummary} columns keep their original wire strings.
 *
 * 用法 / Usage: 由 {@link GatewayDraftPersistenceConverter} 通过 {@code Mappers} 持有，不对外暴露，也不登记为 Spring bean。
 * Held by {@link GatewayDraftPersistenceConverter} through {@code Mappers}; neither published to callers nor registered as a bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface GatewayDraftMapping {

    /**
     * 中文说明：执行 toBusiness 操作，按 {@code GatewayDraftBO} 的字段顺序投影草稿头行。
     * English summary: Executes the toBusiness operation, projecting a draft head row onto {@code GatewayDraftBO}.
     *
     * 用法 / Usage: 仅由 {@link GatewayDraftPersistenceConverter#toBusiness(GatewayDraftRecordPO)} 调用。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "gatewayGroupId", expression = "java( text( source.getGatewayGroupId() ) )")
    @Mapping(target = "revision", source = "revision")
    @Mapping(target = "basedOnReleaseId", source = "basedOnReleaseId")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "changeSummary", source = "changeSummary")
    @Mapping(target = "updatedAt", source = "updateTime")
    @Mapping(target = "updatedBy", source = "updateUserId")
    GatewayDraftBO toBusiness(GatewayDraftRecordPO source);

    /**
     * 中文说明：执行 toRow 操作，把草稿头业务载体写回行模型；主键与受保护技术列全部忽略。
     * English summary: Executes the toRow operation, writing the draft head carrier back onto the row model; the identifier and every
     * protected technical column stay ignored because the guarded persistence layer owns them.
     *
     * 用法 / Usage: 仅由 {@link GatewayDraftPersistenceConverter#toPersistence(GatewayDraftBO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "gatewayGroupId", expression = "java( identifier( source.getGatewayGroupId() ) )")
    @Mapping(target = "revision", source = "revision")
    @Mapping(target = "basedOnReleaseId", source = "basedOnReleaseId")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "changeSummary", source = "changeSummary")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    GatewayDraftRecordPO toRow(GatewayDraftBO source);

    /**
     * 中文说明：执行 text 操作，把 MP 的 bigint 列按十进制文本投影，等价于原 JDBC 的 {@code getString("gateway_group_id")}。
     * English summary: Executes the text operation, projecting the MP bigint column as decimal text, exactly what the legacy
     * {@code getString("gateway_group_id")} produced; {@code null} stays {@code null}.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 标识；parameter identifier。
     * @return 返回 十进制文本；returns the decimal text.
     */
    default String text(Long value) {
        return value == null ? null : Long.toString(value);
    }

    /**
     * 中文说明：执行 identifier 操作，把业务十进制标识解析为 MP bigint；空白直接拒绝，因为草稿头必须归属某个网关分组。
     * English summary: Executes the identifier operation, parsing the business decimal identifier into the MP bigint; a blank value is
     * rejected outright because a draft head always belongs to a gateway group.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 业务标识；parameter business identifier。
     * @return 返回 标识；returns the identifier.
     */
    default Long identifier(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("gatewayGroupId is required");
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(
                    "gateway_draft identifier must be decimal: " + value,
                    failure
            );
        }
    }
}
