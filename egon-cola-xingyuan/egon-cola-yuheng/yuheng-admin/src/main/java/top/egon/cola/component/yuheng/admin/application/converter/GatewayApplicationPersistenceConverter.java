package top.egon.cola.component.yuheng.admin.application.converter;

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
import top.egon.cola.component.yuheng.admin.application.domain.bo.GatewayApplicationBO;
import top.egon.cola.component.yuheng.admin.application.domain.po.GatewayApplicationRecordPO;

/**
 * 中文说明：{@code GatewayApplicationPersistenceConverter} 是 gateway_application 聚合在持久边界唯一的 MapStruct 转换器，
 * 负责 {@link GatewayApplicationRecordPO} 行模型与 {@link GatewayApplicationBO} 业务载体之间的双向映射；
 * id 以十进制文本往返，{@code bizCode} 作为业务编码列与主键分列，createdAt/createdBy/updatedAt/updatedBy 只读投影自
 * MP 基础审计列，业务 {@code deleted} 由 {@code deletedAt} 是否为空推导。
 * English summary: {@code GatewayApplicationPersistenceConverter} is the only MapStruct converter at the persistence boundary of the
 * gateway_application aggregate, mapping {@link GatewayApplicationRecordPO} rows and {@link GatewayApplicationBO} carriers both ways.
 * The identifier round-trips as decimal text, {@code bizCode} stays a business code column beside the key, the audit projections are
 * read-only and the business {@code deleted} flag is derived from {@code deletedAt}.
 *
 * 用法 / Usage: 由 {@code MpGatewayApplicationRepository} 等受守卫仓储注入使用（bean 名 {@code gatewayApplicationPersistenceConverter}），
 * RecordPO 只能出现在本转换器与受守卫仓储之间，禁止进入端口或服务签名。
 * Injected by the guarded gateway_application repository under the bean name {@code gatewayApplicationPersistenceConverter}; a RecordPO
 * may only meet the business carrier inside this converter and the guarded repository, never in a port or service signature.
 */
@Slf4j
@Component("gatewayApplicationPersistenceConverter")
public class GatewayApplicationPersistenceConverter implements BaseConverter<
        GatewayApplicationRecordPO,
        GatewayApplicationBO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只做空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds null guarding and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final GatewayApplicationMapping MAPPING =
            Mappers.getMapper(GatewayApplicationMapping.class);

    /**
     * 中文说明：执行 toPersistence 操作，把 {@code gateway_application} 业务载体渲染为 MP 行模型。
     * English summary: Executes the toPersistence operation, rendering a {@code gateway_application} business carrier into the
     * MyBatis-Plus row model while leaving tenant, audit, soft-delete and version columns to the guarded persistence layer.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayApplicationPersistenceConverter.toPersistence(applicationBO)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    public GatewayApplicationRecordPO toPersistence(GatewayApplicationBO carrier) {
        if (carrier == null) {
            log.debug("gateway_application business carrier is absent; no row rendered");
            return null;
        }
        return MAPPING.toRow(carrier);
    }

    /**
     * 中文说明：执行 toBusiness 操作，把 {@code gateway_application} 行模型投影为业务载体。
     * English summary: Executes the toBusiness operation, projecting a {@code gateway_application} row onto the business carrier with
     * its read-only audit columns and the {@code deleted} flag derived from {@code deletedAt}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayApplicationPersistenceConverter.toBusiness(row)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    public GatewayApplicationBO toBusiness(GatewayApplicationRecordPO row) {
        if (row == null) {
            log.debug("gateway_application row is absent; no business carrier projected");
            return null;
        }
        return MAPPING.toBusiness(row);
    }

    /**
     * 中文说明：执行 toPersistenceList 操作，按列表顺序渲染业务载体。
     * English summary: Executes the toPersistenceList operation, rendering every carrier in order; null or empty input yields an empty
     * list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayApplicationPersistenceConverter.toPersistenceList(carriers)}。
     * @param carriers 参数 业务载体列表；parameter business carriers.
     * @return 返回 行模型列表；returns the row models.
     */
    public List<GatewayApplicationRecordPO> toPersistenceList(
            List<GatewayApplicationBO> carriers) {
        return toSourceList(carriers);
    }

    /**
     * 中文说明：执行 toBusinessList 操作，按列表顺序投影行模型。
     * English summary: Executes the toBusinessList operation, projecting every row in order; null or empty input yields an empty list
     * and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayApplicationPersistenceConverter.toBusinessList(rows)}。
     * @param rows 参数 行模型列表；parameter row models.
     * @return 返回 业务载体列表；returns the business carriers.
     */
    public List<GatewayApplicationBO> toBusinessList(
            List<GatewayApplicationRecordPO> rows) {
        return toTargetList(rows);
    }

    /**
     * 中文说明：执行 toTarget 操作，即 {@code BaseConverter} 的正向投影（行模型 -> 业务载体）。
     * English summary: Executes the toTarget operation, the forward projection of {@code BaseConverter} (row model to business carrier).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayApplicationPersistenceConverter.toTarget(row)}。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @Override
    public GatewayApplicationBO toTarget(GatewayApplicationRecordPO source) {
        return toBusiness(source);
    }

    /**
     * 中文说明：执行 toSource 操作，即 {@code BaseConverter} 的反向渲染（业务载体 -> 行模型）。
     * English summary: Executes the toSource operation, the reverse rendering of {@code BaseConverter} (business carrier to row model).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayApplicationPersistenceConverter.toSource(carrier)}。
     * @param target 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @Override
    public GatewayApplicationRecordPO toSource(GatewayApplicationBO target) {
        return toPersistence(target);
    }

    /**
     * 中文说明：执行 newRow 操作，是 {@code toPersistence} 的插入别名，语义为「只带业务列的新行」，租户、审计、软删与版本列
     * 一律留空交由受守卫边界补齐。
     * English summary: Executes the newRow operation, an insert alias of {@code toPersistence} producing a row carrying business columns
     * only; the tenant, audit, soft-delete and version columns stay empty for the guarded boundary to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayApplicationPersistenceConverter.newRow(applicationBO)}。
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public GatewayApplicationRecordPO newRow(GatewayApplicationBO carrier) {
        return toPersistence(carrier);
    }
}

/**
 * 中文说明：{@code GatewayApplicationMapping} 是 gateway_application 的 MapStruct 结构映射契约，逐列复刻被替换的手写 JDBC 读写语义，
 * {@code unmappedTargetPolicy=ERROR} 保证新增列必须在此显式表态。
 * English summary: {@code GatewayApplicationMapping} is the MapStruct structural contract for gateway_application, mirroring the
 * hand-written JDBC it replaces column by column; {@code unmappedTargetPolicy=ERROR} forces every new column to be stated here.
 *
 * 用法 / Usage: 由 {@link GatewayApplicationPersistenceConverter} 通过 {@code Mappers} 持有，不对外暴露，也不登记为 Spring bean。
 * Held by {@link GatewayApplicationPersistenceConverter} through {@code Mappers}; neither published to callers nor registered as a bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface GatewayApplicationMapping {

    /**
     * 中文说明：执行 toBusiness 操作，按 {@code GatewayApplicationBO} 的字段顺序投影 gateway_application 行。
     * English summary: Executes the toBusiness operation, projecting a gateway_application row onto {@code GatewayApplicationBO}.
     *
     * 用法 / Usage: 仅由 {@link GatewayApplicationPersistenceConverter#toBusiness(GatewayApplicationRecordPO)} 调用。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( text( source.getId() ) )")
    @Mapping(target = "applicationCode", source = "applicationCode")
    @Mapping(target = "bizCode", source = "bizCode")
    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "env", source = "env")
    @Mapping(target = "namespace", source = "namespace")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "revision", source = "revision")
    @Mapping(target = "deleted", expression = "java( isDeleted( source ) )")
    @Mapping(target = "createdAt", source = "createTime")
    @Mapping(target = "createdBy", source = "createUserId")
    @Mapping(target = "updatedAt", source = "updateTime")
    @Mapping(target = "updatedBy", source = "updateUserId")
    GatewayApplicationBO toBusiness(GatewayApplicationRecordPO source);

    /**
     * 中文说明：执行 toRow 操作，把业务载体写回 gateway_application 行；受保护的技术列全部忽略。
     * English summary: Executes the toRow operation, writing the business carrier back onto a gateway_application row; every protected
     * technical column stays ignored because {@code EgonColaMetaObjectHandler} owns them.
     *
     * 用法 / Usage: 仅由 {@link GatewayApplicationPersistenceConverter#toPersistence(GatewayApplicationBO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( identifier( source.getId() ) )")
    @Mapping(target = "applicationCode", source = "applicationCode")
    @Mapping(target = "bizCode", source = "bizCode")
    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "env", source = "env")
    @Mapping(target = "namespace", source = "namespace")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "revision", source = "revision")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    GatewayApplicationRecordPO toRow(GatewayApplicationBO source);

    /**
     * 中文说明：执行 text 操作，把 MP 的 bigint 主键按十进制文本投影，等价于原 JDBC 的 {@code getString("id")}。
     * English summary: Executes the text operation, projecting the MP bigint identifier as decimal text, exactly what the legacy
     * {@code getString("id")} produced; {@code null} stays {@code null}.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 主键；parameter identifier。
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
                    "gateway_application identifier must be decimal: " + value,
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 isDeleted 操作，由软删时间列推导业务 {@code deleted} 标志。
     * English summary: Executes the isDeleted operation, deriving the business {@code deleted} flag from the soft-delete column.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param source 参数 行模型；parameter row model。
     * @return 返回 是否已软删；returns whether the row is soft deleted.
     */
    default boolean isDeleted(GatewayApplicationRecordPO source) {
        return source.getDeletedAt() != null;
    }
}
