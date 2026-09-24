package top.egon.cola.component.yuheng.admin.release.converter;

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
import top.egon.cola.component.yuheng.admin.release.domain.bo.GatewayReleaseAttemptBO;
import top.egon.cola.component.yuheng.admin.release.domain.bo.GatewayReleaseTargetBO;
import top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseAttemptRecordPO;

/**
 * 中文说明：{@code GatewayReleaseAttemptPersistenceConverter} 是 gateway_release_attempt 聚合在持久边界唯一的 MapStruct 转换器，
 * 负责 {@link GatewayReleaseAttemptRecordPO} 行模型与 {@link GatewayReleaseAttemptBO} 业务载体之间的双向映射；
 * {@code attempt_no} 在 {@code int} 载体列与 MP {@code Long} 行列之间转换，{@code status} 按原实现以纯文本存取
 * （PENDING/PUBLISHING/…，由发布状态机决定写入值），一次尝试的子行 {@code targets} 不由本转换器物化。
 * English summary: {@code GatewayReleaseAttemptPersistenceConverter} is the only MapStruct converter at the persistence boundary of the
 * gateway_release_attempt aggregate, mapping {@link GatewayReleaseAttemptRecordPO} rows and {@link GatewayReleaseAttemptBO} carriers both
 * ways. {@code attempt_no} converts between the {@code int} carrier field and the MP {@code Long} row column, {@code status} stays plain
 * text exactly as the legacy repository stored it (PENDING/PUBLISHING/…, the release state machine decides the value), and the child
 * rows of an attempt ({@code targets}) are deliberately not materialized by this converter.
 *
 * 用法 / Usage: 由 gateway_release_attempt 的受守卫 MP 仓储注入使用（bean 名 {@code gatewayReleaseAttemptPersistenceConverter}）；
 * 子行由仓储在投影完成后经 {@code gatewayReleaseTargetPersistenceConverter} 单独装载，租户、审计、软删与版本列由
 * {@code EgonColaMetaObjectHandler} 独占，父键 {@code release_id} 与租约列 {@code lease_owner}/{@code lease_until} 不在业务载体内。
 * Injected by the guarded gateway_release_attempt repository under the bean name {@code gatewayReleaseAttemptPersistenceConverter}; the
 * child rows are loaded afterwards through {@code gatewayReleaseTargetPersistenceConverter}, the tenant/audit/soft-delete/version columns
 * belong to {@code EgonColaMetaObjectHandler}, and the parent key {@code release_id} plus the lease columns are not part of the carrier.
 */
@Slf4j
@Component("gatewayReleaseAttemptPersistenceConverter")
public class GatewayReleaseAttemptPersistenceConverter implements BaseConverter<
        GatewayReleaseAttemptRecordPO,
        GatewayReleaseAttemptBO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只做空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds null guarding and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final GatewayReleaseAttemptMapping MAPPING =
            Mappers.getMapper(GatewayReleaseAttemptMapping.class);

    /**
     * 中文说明：执行 toPersistence 操作，把尝试载体渲染为 MP 行模型。
     * English summary: Executes the toPersistence operation, rendering an attempt carrier into the MyBatis-Plus row model; the parent
     * release key, the lease columns, the identifier and every protected technical column stay untouched.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayReleaseAttemptPersistenceConverter.toPersistence(attemptBO)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    public GatewayReleaseAttemptRecordPO toPersistence(GatewayReleaseAttemptBO carrier) {
        if (carrier == null) {
            log.debug("gateway_release_attempt business carrier is absent; no row rendered");
            return null;
        }
        return MAPPING.toRow(carrier);
    }

    /**
     * 中文说明：执行 toBusiness 操作，把尝试行投影为业务载体。
     * English summary: Executes the toBusiness operation, projecting an attempt row onto the business carrier; the child target rows are
     * left as an empty list because the guarded repository hydrates them separately from gateway_release_target.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayReleaseAttemptPersistenceConverter.toBusiness(row)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    public GatewayReleaseAttemptBO toBusiness(GatewayReleaseAttemptRecordPO row) {
        if (row == null) {
            log.debug("gateway_release_attempt row is absent; no business carrier projected");
            return null;
        }
        return MAPPING.toBusiness(row);
    }

    /**
     * 中文说明：执行 toPersistenceList 操作，按列表顺序渲染尝试载体。
     * English summary: Executes the toPersistenceList operation, rendering every attempt carrier in order; null or empty input yields an
     * empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayReleaseAttemptPersistenceConverter.toPersistenceList(carriers)}。
     * @param carriers 参数 业务载体列表；parameter business carriers.
     * @return 返回 行模型列表；returns the row models.
     */
    public List<GatewayReleaseAttemptRecordPO> toPersistenceList(
            List<GatewayReleaseAttemptBO> carriers) {
        return toSourceList(carriers);
    }

    /**
     * 中文说明：执行 toBusinessList 操作，按列表顺序投影尝试行。
     * English summary: Executes the toBusinessList operation, projecting every attempt row in order; null or empty input yields an empty
     * list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayReleaseAttemptPersistenceConverter.toBusinessList(rows)}。
     * @param rows 参数 行模型列表；parameter row models.
     * @return 返回 业务载体列表；returns the business carriers.
     */
    public List<GatewayReleaseAttemptBO> toBusinessList(
            List<GatewayReleaseAttemptRecordPO> rows) {
        return toTargetList(rows);
    }

    /**
     * 中文说明：执行 toTarget 操作，即 {@code BaseConverter} 的正向投影（行模型 -> 业务载体）。
     * English summary: Executes the toTarget operation, the forward projection of {@code BaseConverter} (row model to business carrier).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayReleaseAttemptPersistenceConverter.toTarget(row)}。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @Override
    public GatewayReleaseAttemptBO toTarget(GatewayReleaseAttemptRecordPO source) {
        return toBusiness(source);
    }

    /**
     * 中文说明：执行 toSource 操作，即 {@code BaseConverter} 的反向渲染（业务载体 -> 行模型）。
     * English summary: Executes the toSource operation, the reverse rendering of {@code BaseConverter} (business carrier to row model).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayReleaseAttemptPersistenceConverter.toSource(carrier)}。
     * @param target 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @Override
    public GatewayReleaseAttemptRecordPO toSource(GatewayReleaseAttemptBO target) {
        return toPersistence(target);
    }

    /**
     * 中文说明：执行 newRow 操作，是 {@code toPersistence} 的插入别名，语义为「只带业务列的新行」，父键、租约列、技术主键与
     * 租户、审计、软删、版本列一律留空交由受守卫边界补齐。
     * English summary: Executes the newRow operation, an insert alias of {@code toPersistence} producing a row carrying business columns
     * only; the parent key, the lease columns, the technical identifier and the tenant, audit, soft-delete and version columns stay
     * empty for the guarded boundary to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayReleaseAttemptPersistenceConverter.newRow(attemptBO)}。
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public GatewayReleaseAttemptRecordPO newRow(GatewayReleaseAttemptBO carrier) {
        return toPersistence(carrier);
    }
}

/**
 * 中文说明：{@code GatewayReleaseAttemptMapping} 是 gateway_release_attempt 的 MapStruct 结构映射契约，逐列复刻被替换的手写
 * JDBC 读写语义：{@code status} 保持原文本列语义，时间列沿用 {@code java.time.Instant}，子行集合以空列表占位而绝不返回
 * {@code null}；{@code unmappedTargetPolicy=ERROR} 保证任何新增列都必须在此显式表态。
 * English summary: {@code GatewayReleaseAttemptMapping} is the MapStruct structural contract for gateway_release_attempt, mirroring the
 * hand-written JDBC it replaces: {@code status} keeps its plain text column semantics, the instants stay {@code java.time.Instant} and the
 * child collection is projected as an empty list rather than {@code null}; {@code unmappedTargetPolicy=ERROR} forces every new column to
 * be stated explicitly here.
 *
 * 用法 / Usage: 由 {@link GatewayReleaseAttemptPersistenceConverter} 通过 {@code Mappers} 持有，不对外暴露，也不登记为 Spring bean。
 * Held by {@link GatewayReleaseAttemptPersistenceConverter} through {@code Mappers}; neither published to callers nor registered as a bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface GatewayReleaseAttemptMapping {

    /**
     * 中文说明：执行 toBusiness 操作，按 {@code GatewayReleaseAttemptBO} 的字段顺序投影尝试行。
     * English summary: Executes the toBusiness operation, projecting an attempt row onto {@code GatewayReleaseAttemptBO}.
     *
     * 用法 / Usage: 仅由 {@link GatewayReleaseAttemptPersistenceConverter#toBusiness(GatewayReleaseAttemptRecordPO)} 调用。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "attemptNo", source = "attemptNo")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "changeId", source = "changeId")
    @Mapping(target = "startedAt", source = "startedAt")
    @Mapping(target = "completedAt", source = "completedAt")
    @Mapping(target = "errorCode", source = "errorCode")
    @Mapping(target = "errorMessage", source = "errorMessage")
    @Mapping(target = "targets", expression = "java( emptyTargets() )")
    GatewayReleaseAttemptBO toBusiness(GatewayReleaseAttemptRecordPO source);

    /**
     * 中文说明：执行 toRow 操作，把尝试载体写回行模型；父键、租约列与受保护技术列全部忽略。
     * English summary: Executes the toRow operation, writing the attempt carrier back onto the row model; the parent key, the lease
     * columns and every protected technical column stay ignored, the latter because {@code EgonColaMetaObjectHandler} owns them.
     *
     * 用法 / Usage: 仅由 {@link GatewayReleaseAttemptPersistenceConverter#toPersistence(GatewayReleaseAttemptBO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "attemptNo", source = "attemptNo")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "changeId", source = "changeId")
    @Mapping(target = "startedAt", source = "startedAt")
    @Mapping(target = "completedAt", source = "completedAt")
    @Mapping(target = "errorCode", source = "errorCode")
    @Mapping(target = "errorMessage", source = "errorMessage")
    @Mapping(target = "releaseId", ignore = true)
    @Mapping(target = "leaseOwner", ignore = true)
    @Mapping(target = "leaseUntil", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    GatewayReleaseAttemptRecordPO toRow(GatewayReleaseAttemptBO source);

    /**
     * 中文说明：执行 emptyTargets 操作，为子行集合给出非空起点，保持原实现「无目标行时为空列表」的不变量；
     * 真正的子行由受守卫仓储经 gateway_release_target 装载后整体替换。
     * English summary: Executes the emptyTargets operation, seeding the child collection so the legacy invariant "an attempt without
     * target rows carries an empty list" still holds; the guarded repository replaces it after loading gateway_release_target.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @return 返回 空的可变目标列表；returns an empty mutable target list.
     */
    default List<GatewayReleaseTargetBO> emptyTargets() {
        return new java.util.ArrayList<>();
    }
}
