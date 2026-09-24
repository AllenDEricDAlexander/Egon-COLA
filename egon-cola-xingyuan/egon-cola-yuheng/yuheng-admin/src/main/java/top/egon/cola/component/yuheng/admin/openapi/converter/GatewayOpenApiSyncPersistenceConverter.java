package top.egon.cola.component.yuheng.admin.openapi.converter;

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
import top.egon.cola.component.yuheng.admin.openapi.domain.bo.GatewayOpenApiSyncBO;
import top.egon.cola.component.yuheng.admin.openapi.domain.enums.GatewayOpenApiSyncStateEnum;
import top.egon.cola.component.yuheng.admin.openapi.domain.po.GatewayOpenApiSyncRecordPO;

/**
 * 中文说明：{@code GatewayOpenApiSyncPersistenceConverter} 是 gateway_openapi_sync_state 聚合在持久边界唯一的 MapStruct 转换器，
 * 负责 {@link GatewayOpenApiSyncRecordPO} 行模型与 {@link GatewayOpenApiSyncBO} 业务载体之间的双向映射；
 * {@code id}、{@code application_id} 与两个可空引用 {@code latest_snapshot_id}/{@code definition_set_id} 以十进制文本往返
 * （空白归一为 {@code null}，从不写空串），{@code status} 按 {@code GatewayOpenApiSyncStateEnum} 枚举名读写（绝不 ordinal），
 * 业务 CAS 列 {@code revision} 与 MP 乐观锁 {@code version} 各自独立、互不替代，updatedAt 只读投影自 MP
 * {@code update_time}，四个时间列全部保持 {@code java.time.Instant}。
 * English summary: {@code GatewayOpenApiSyncPersistenceConverter} is the only MapStruct converter at the persistence boundary of the
 * gateway_openapi_sync_state aggregate, mapping {@link GatewayOpenApiSyncRecordPO} rows and {@link GatewayOpenApiSyncBO} carriers both
 * ways. {@code id}, {@code application_id} and the two nullable references {@code latest_snapshot_id}/{@code definition_set_id}
 * round-trip as decimal text (a blank value normalizes to {@code null}, never to an empty string), {@code status} is stored as the
 * {@code GatewayOpenApiSyncStateEnum} name (never its ordinal), the business CAS column {@code revision} stays independent of the MP
 * optimistic-lock {@code version}, updatedAt is a read-only projection of the MP {@code update_time} and all four instants remain
 * {@code java.time.Instant}.
 *
 * 用法 / Usage: 由 gateway_openapi_sync_state 的受守卫 MP 仓储注入使用（bean 名
 * {@code gatewayOpenApiSyncPersistenceConverter}）；同步状态的取值域与状态机迁移仍由
 * {@link GatewayOpenApiSyncBO#validated(GatewayOpenApiSyncBO)} 与 {@code GatewayOpenApiSyncStateEnum} 负责，本转换器不重算也不放宽；
 * 租户、审计、软删与 MP 版本列由 {@code EgonColaMetaObjectHandler} 独占，写方向一律忽略。
 * Injected by the guarded gateway_openapi_sync_state repository under the bean name
 * {@code gatewayOpenApiSyncPersistenceConverter}; the value domain and the state machine stay owned by
 * {@link GatewayOpenApiSyncBO#validated(GatewayOpenApiSyncBO)} and {@code GatewayOpenApiSyncStateEnum}, which this converter neither
 * recomputes nor relaxes, and the tenant, audit, soft-delete and MP version columns belong to {@code EgonColaMetaObjectHandler} and stay
 * ignored on write.
 */
@Slf4j
@Component("gatewayOpenApiSyncPersistenceConverter")
public class GatewayOpenApiSyncPersistenceConverter implements BaseConverter<
        GatewayOpenApiSyncRecordPO,
        GatewayOpenApiSyncBO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只做空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds null guarding and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final GatewayOpenApiSyncMapping MAPPING =
            Mappers.getMapper(GatewayOpenApiSyncMapping.class);

    /**
     * 中文说明：执行 toPersistence 操作，把同步状态载体渲染为 MP 行模型。
     * English summary: Executes the toPersistence operation, rendering a synchronization state carrier into the MyBatis-Plus row model,
     * storing the state as its enum name, parsing the nullable bigint references and leaving every protected technical column to the
     * guarded persistence layer.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayOpenApiSyncPersistenceConverter.toPersistence(syncBO)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    public GatewayOpenApiSyncRecordPO toPersistence(GatewayOpenApiSyncBO carrier) {
        if (carrier == null) {
            log.debug("gateway_openapi_sync_state business carrier is absent; no row rendered");
            return null;
        }
        return MAPPING.toRow(carrier);
    }

    /**
     * 中文说明：执行 toBusiness 操作，把同步状态行投影为业务载体。
     * English summary: Executes the toBusiness operation, projecting a synchronization state row onto the business carrier, restoring the
     * state enum from its stored name, rendering the bigint references as decimal text and reading the last change instant from the MP
     * audit column.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayOpenApiSyncPersistenceConverter.toBusiness(row)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    public GatewayOpenApiSyncBO toBusiness(GatewayOpenApiSyncRecordPO row) {
        if (row == null) {
            log.debug("gateway_openapi_sync_state row is absent; no business carrier projected");
            return null;
        }
        return MAPPING.toBusiness(row);
    }

    /**
     * 中文说明：执行 toPersistenceList 操作，按列表顺序渲染同步状态载体。
     * English summary: Executes the toPersistenceList operation, rendering every state carrier in order; null or empty input yields an
     * empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayOpenApiSyncPersistenceConverter.toPersistenceList(carriers)}。
     * @param carriers 参数 业务载体列表；parameter business carriers.
     * @return 返回 行模型列表；returns the row models.
     */
    public List<GatewayOpenApiSyncRecordPO> toPersistenceList(
            List<GatewayOpenApiSyncBO> carriers) {
        return toSourceList(carriers);
    }

    /**
     * 中文说明：执行 toBusinessList 操作，按列表顺序投影同步状态行。
     * English summary: Executes the toBusinessList operation, projecting every state row in order; null or empty input yields an empty
     * list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayOpenApiSyncPersistenceConverter.toBusinessList(rows)}。
     * @param rows 参数 行模型列表；parameter row models.
     * @return 返回 业务载体列表；returns the business carriers.
     */
    public List<GatewayOpenApiSyncBO> toBusinessList(
            List<GatewayOpenApiSyncRecordPO> rows) {
        return toTargetList(rows);
    }

    /**
     * 中文说明：执行 toTarget 操作，即 {@code BaseConverter} 的正向投影（行模型 -> 业务载体）。
     * English summary: Executes the toTarget operation, the forward projection of {@code BaseConverter} (row model to business carrier).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayOpenApiSyncPersistenceConverter.toTarget(row)}。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @Override
    public GatewayOpenApiSyncBO toTarget(GatewayOpenApiSyncRecordPO source) {
        return toBusiness(source);
    }

    /**
     * 中文说明：执行 toSource 操作，即 {@code BaseConverter} 的反向渲染（业务载体 -> 行模型）。
     * English summary: Executes the toSource operation, the reverse rendering of {@code BaseConverter} (business carrier to row model).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayOpenApiSyncPersistenceConverter.toSource(carrier)}。
     * @param target 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @Override
    public GatewayOpenApiSyncRecordPO toSource(GatewayOpenApiSyncBO target) {
        return toPersistence(target);
    }

    /**
     * 中文说明：执行 newRow 操作，是 {@code toPersistence} 的插入别名，语义为「只带业务列的新行」，
     * 租户、审计、软删与 MP 版本列一律留空交由受守卫边界补齐，业务 {@code revision} 按载体初值原样写入。
     * English summary: Executes the newRow operation, an insert alias of {@code toPersistence} producing a row carrying business columns
     * only; the tenant, audit, soft-delete and MP version columns stay empty for the guarded boundary to fill, while the business
     * {@code revision} is written as the carrier carries it.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayOpenApiSyncPersistenceConverter.newRow(syncBO)}。
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public GatewayOpenApiSyncRecordPO newRow(GatewayOpenApiSyncBO carrier) {
        return toPersistence(carrier);
    }
}

/**
 * 中文说明：{@code GatewayOpenApiSyncMapping} 是 gateway_openapi_sync_state 的 MapStruct 结构映射契约，逐列复刻被替换的手写
 * JDBC 语义：状态列按 {@code GatewayOpenApiSyncStateEnum} 枚举名存取、未知名不兜底，{@code last_attempt_at}/
 * {@code last_success_at}/{@code next_retry_at} 三个可空时间列保持 {@code null} 而不补造，{@code last_error_code}/
 * {@code last_error_message} 的可空文本按原样透传；{@code unmappedTargetPolicy=ERROR} 保证任何新增列都必须在此显式表态。
 * English summary: {@code GatewayOpenApiSyncMapping} is the MapStruct structural contract for gateway_openapi_sync_state, mirroring the
 * hand-written JDBC it replaces: the status column travels as the {@code GatewayOpenApiSyncStateEnum} name with no fallback for an
 * unknown value, the three nullable instants {@code last_attempt_at}/{@code last_success_at}/{@code next_retry_at} keep their
 * {@code null} instead of a fabricated one, and the nullable error text passes through unchanged;
 * {@code unmappedTargetPolicy=ERROR} forces every new column to be stated explicitly here.
 *
 * 用法 / Usage: 由 {@link GatewayOpenApiSyncPersistenceConverter} 通过 {@code Mappers} 持有，不对外暴露，也不登记为 Spring bean。
 * Held by {@link GatewayOpenApiSyncPersistenceConverter} through {@code Mappers}; neither published to callers nor registered as a bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface GatewayOpenApiSyncMapping {

    /**
     * 中文说明：执行 toBusiness 操作，按 {@code GatewayOpenApiSyncBO} 的字段顺序投影同步状态行。
     * English summary: Executes the toBusiness operation, projecting a synchronization state row onto {@code GatewayOpenApiSyncBO}.
     *
     * 用法 / Usage: 仅由 {@link GatewayOpenApiSyncPersistenceConverter#toBusiness(GatewayOpenApiSyncRecordPO)} 调用。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( text( source.getId() ) )")
    @Mapping(target = "applicationId", expression = "java( text( source.getApplicationId() ) )")
    @Mapping(target = "buildId", source = "buildId")
    @Mapping(target = "artifactVersion", source = "artifactVersion")
    @Mapping(target = "openapiGroup", source = "openapiGroup")
    @Mapping(target = "providerServiceName", source = "providerServiceName")
    @Mapping(target = "providerGroup", source = "providerGroup")
    @Mapping(target = "providerVersion", source = "providerVersion")
    @Mapping(target = "status", expression = "java( status( source.getStatus() ) )")
    @Mapping(target = "latestSnapshotId", expression = "java( text( source.getLatestSnapshotId() ) )")
    @Mapping(target = "definitionSetId", expression = "java( text( source.getDefinitionSetId() ) )")
    @Mapping(target = "lastInstanceId", source = "lastInstanceId")
    @Mapping(target = "attemptCount", source = "attemptCount")
    @Mapping(target = "lastErrorCode", source = "lastErrorCode")
    @Mapping(target = "lastErrorMessage", source = "lastErrorMessage")
    @Mapping(target = "firstDiscoveredAt", source = "firstDiscoveredAt")
    @Mapping(target = "lastAttemptAt", source = "lastAttemptAt")
    @Mapping(target = "lastSuccessAt", source = "lastSuccessAt")
    @Mapping(target = "nextRetryAt", source = "nextRetryAt")
    @Mapping(target = "revision", source = "revision")
    @Mapping(target = "updatedAt", source = "updateTime")
    GatewayOpenApiSyncBO toBusiness(GatewayOpenApiSyncRecordPO source);

    /**
     * 中文说明：执行 toRow 操作，把同步状态载体写回行模型；受保护技术列全部忽略，因为
     * {@code EgonColaMetaObjectHandler} 独占租户、审计、软删与 MP 版本列，而业务 CAS 列 {@code revision} 由状态机自行推进。
     * English summary: Executes the toRow operation, writing the state carrier back onto the row model; every protected technical column
     * stays ignored because {@code EgonColaMetaObjectHandler} owns the tenant, audit, soft-delete and MP version columns, while the
     * business CAS column {@code revision} is advanced by the state machine itself.
     *
     * 用法 / Usage: 仅由 {@link GatewayOpenApiSyncPersistenceConverter#toPersistence(GatewayOpenApiSyncBO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( identifier( source.getId() ) )")
    @Mapping(target = "applicationId", expression = "java( identifier( source.getApplicationId() ) )")
    @Mapping(target = "buildId", source = "buildId")
    @Mapping(target = "artifactVersion", source = "artifactVersion")
    @Mapping(target = "openapiGroup", source = "openapiGroup")
    @Mapping(target = "providerServiceName", source = "providerServiceName")
    @Mapping(target = "providerGroup", source = "providerGroup")
    @Mapping(target = "providerVersion", source = "providerVersion")
    @Mapping(target = "status", expression = "java( statusName( source.getStatus() ) )")
    @Mapping(target = "latestSnapshotId", expression = "java( optionalIdentifier( source.getLatestSnapshotId() ) )")
    @Mapping(target = "definitionSetId", expression = "java( optionalIdentifier( source.getDefinitionSetId() ) )")
    @Mapping(target = "lastInstanceId", source = "lastInstanceId")
    @Mapping(target = "attemptCount", source = "attemptCount")
    @Mapping(target = "lastErrorCode", source = "lastErrorCode")
    @Mapping(target = "lastErrorMessage", source = "lastErrorMessage")
    @Mapping(target = "firstDiscoveredAt", source = "firstDiscoveredAt")
    @Mapping(target = "lastAttemptAt", source = "lastAttemptAt")
    @Mapping(target = "lastSuccessAt", source = "lastSuccessAt")
    @Mapping(target = "nextRetryAt", source = "nextRetryAt")
    @Mapping(target = "revision", source = "revision")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    GatewayOpenApiSyncRecordPO toRow(GatewayOpenApiSyncBO source);

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
                    "gateway OpenAPI sync identifier must be decimal: " + value,
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
     * 中文说明：执行 status 操作，按存储的枚举名还原 {@code GatewayOpenApiSyncStateEnum}，与原
     * {@code GatewayOpenApiSyncStateEnum.valueOf(...)} 一致：空值保持为空，未知名抛错而不兜底。
     * English summary: Executes the status operation, restoring {@code GatewayOpenApiSyncStateEnum} from the stored enum name exactly as
     * the legacy {@code GatewayOpenApiSyncStateEnum.valueOf(...)} did: {@code null} stays {@code null} and an unknown name fails instead
     * of falling back.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 存储的状态名；parameter stored state name。
     * @return 返回 同步状态或 {@code null}；returns the sync state or {@code null}.
     */
    default GatewayOpenApiSyncStateEnum status(String value) {
        return value == null ? null : GatewayOpenApiSyncStateEnum.valueOf(value);
    }

    /**
     * 中文说明：执行 statusName 操作，按枚举名写回状态列，与原 {@code state.name()} 绑定相同，绝不写入 ordinal。
     * English summary: Executes the statusName operation, writing the status column as the enum name and never its ordinal, matching the
     * legacy {@code state.name()} binding.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 同步状态；parameter sync state。
     * @return 返回 状态名或 {@code null}；returns the state name or {@code null}.
     */
    default String statusName(GatewayOpenApiSyncStateEnum value) {
        return value == null ? null : value.name();
    }
}
