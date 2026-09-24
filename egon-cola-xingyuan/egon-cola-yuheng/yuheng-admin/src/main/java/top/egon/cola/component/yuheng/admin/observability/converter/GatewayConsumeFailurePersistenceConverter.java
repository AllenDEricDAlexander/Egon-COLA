package top.egon.cola.component.yuheng.admin.observability.converter;

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
import top.egon.cola.component.yuheng.admin.observability.domain.bo.GatewayConsumeFailureBO;
import top.egon.cola.component.yuheng.admin.observability.domain.po.GatewayConsumeFailureRecordPO;

/**
 * 中文说明：{@code GatewayConsumeFailurePersistenceConverter} 是 {@code gateway_call_event_consume_failure} 在持久边界唯一的
 * MapStruct 转换器，负责 {@link GatewayConsumeFailureRecordPO} 行模型与 {@link GatewayConsumeFailureBO} 业务载体之间的双向映射：
 * 端口主键是十进制文本（旧列 {@code VARCHAR(64)}，由消费侧雪花号生成），迁移后落进 MP 的 bigint {@code id}，写方向按十进制解析、
 * 读方向按十进制文本原样还原；分区号与位点分别落列 {@code partition_no}、{@code offset_no}，两个 NOT NULL 数值列在载体上是
 * {@code int}/{@code long}，空列按旧 JDBC 的 {@code getInt/getLong} 读作 0；{@code event_id} 保持可空，
 * 业务发生时刻继续落在自己的列 {@code occurred_at}（不是 MP 审计列 {@code create_time}），受保护技术列写方向一律忽略。
 * English summary: {@code GatewayConsumeFailurePersistenceConverter} is the only MapStruct converter at the persistence boundary of
 * {@code gateway_call_event_consume_failure}, mapping {@link GatewayConsumeFailureRecordPO} rows and
 * {@link GatewayConsumeFailureBO} carriers both ways: the port key is decimal text (the legacy {@code VARCHAR(64)} column filled by
 * the consumer's snowflake generator) that now lands in the MP bigint {@code id}, so the write direction parses it as decimal and the
 * read direction renders back the very same decimal text; the partition and position land in {@code partition_no} and
 * {@code offset_no}, and those two NOT NULL numeric columns are {@code int}/{@code long} on the carrier with an absent column reading
 * as 0 exactly like the legacy {@code getInt/getLong}; {@code event_id} stays nullable and the business instant keeps its own column
 * {@code occurred_at} (not the MP audit column {@code create_time}), while every protected technical column stays ignored on write.
 *
 * 用法 / Usage: 由 {@code gateway_call_event_consume_failure} 的受守卫门面注入（bean 名
 * {@code gatewayConsumeFailurePersistenceConverter}）；写方向不触碰租户、审计、软删与 MP 版本列，因为它们由
 * {@code EgonColaMetaObjectHandler} 独占。/ Injected by the guarded consume-failure facade under the bean name
 * {@code gatewayConsumeFailurePersistenceConverter}; the write direction never touches the tenant, audit, soft-delete or version
 * columns, because {@code EgonColaMetaObjectHandler} owns them.
 */
@Slf4j
@Component("gatewayConsumeFailurePersistenceConverter")
public class GatewayConsumeFailurePersistenceConverter implements BaseConverter<
        GatewayConsumeFailureRecordPO,
        GatewayConsumeFailureBO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只做空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds null guarding and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final GatewayConsumeFailureMapping MAPPING =
            Mappers.getMapper(GatewayConsumeFailureMapping.class);

    /**
     * 中文说明：执行 toPersistence 操作，把消费失败载体渲染为 MP 行模型；行标识按十进制解析，分区与位点写入
     * {@code partition_no}/{@code offset_no}，租户、审计、软删与版本列一律留空交由受守卫边界补齐。
     * English summary: Executes the toPersistence operation, rendering a consume-failure carrier into the MyBatis-Plus row model; the
     * row identifier is parsed as decimal text, the partition and position write into {@code partition_no} and {@code offset_no}, while
     * the tenant, audit, soft-delete and version columns stay empty for the guarded boundary to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayConsumeFailurePersistenceConverter.toPersistence(consumeFailureBO)}。
     * 传入 {@code null} 返回 {@code null}。/ Passing {@code null} returns {@code null}.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    public GatewayConsumeFailureRecordPO toPersistence(GatewayConsumeFailureBO carrier) {
        if (carrier == null) {
            log.debug("gateway consume failure business carrier is absent; no row rendered");
            return null;
        }
        return MAPPING.toRow(carrier);
    }

    /**
     * 中文说明：执行 toBusiness 操作，把消费失败行投影为业务载体：行标识还原为十进制文本，
     * 分区与位点从 {@code partition_no}/{@code offset_no} 读回并按旧 JDBC 的空列口径落 0。
     * English summary: Executes the toBusiness operation, projecting a consume-failure row onto the business carrier: the row identifier
     * returns as decimal text while the partition and position read back from {@code partition_no} and {@code offset_no} and, as the
     * legacy JDBC did, an absent column becomes 0.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayConsumeFailurePersistenceConverter.toBusiness(row)}。
     * 传入 {@code null} 返回 {@code null}。/ Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    public GatewayConsumeFailureBO toBusiness(GatewayConsumeFailureRecordPO row) {
        if (row == null) {
            log.debug("gateway consume failure row is absent; no business carrier projected");
            return null;
        }
        return MAPPING.toBusiness(row);
    }

    /**
     * 中文说明：执行 toPersistenceList 操作，按列表顺序渲染消费失败载体。
     * English summary: Executes the toPersistenceList operation, rendering every consume-failure carrier in order; null or empty input
     * yields an empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayConsumeFailurePersistenceConverter.toPersistenceList(carriers)}。
     * @param carriers 参数 业务载体列表；parameter business carriers.
     * @return 返回 行模型列表；returns the row models.
     */
    public List<GatewayConsumeFailureRecordPO> toPersistenceList(
            List<GatewayConsumeFailureBO> carriers) {
        return toSourceList(carriers);
    }

    /**
     * 中文说明：执行 toBusinessList 操作，按列表顺序投影消费失败行。
     * English summary: Executes the toBusinessList operation, projecting every consume-failure row in order; null or empty input yields
     * an empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayConsumeFailurePersistenceConverter.toBusinessList(rows)}。
     * @param rows 参数 行模型列表；parameter row models.
     * @return 返回 业务载体列表；returns the business carriers.
     */
    public List<GatewayConsumeFailureBO> toBusinessList(
            List<GatewayConsumeFailureRecordPO> rows) {
        return toTargetList(rows);
    }

    /**
     * 中文说明：执行 toTarget 操作，即 {@code BaseConverter} 的正向投影（行模型 -> 业务载体）。
     * English summary: Executes the toTarget operation, the forward projection of {@code BaseConverter} (row model to business carrier).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayConsumeFailurePersistenceConverter.toTarget(row)}。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @Override
    public GatewayConsumeFailureBO toTarget(GatewayConsumeFailureRecordPO source) {
        return toBusiness(source);
    }

    /**
     * 中文说明：执行 toSource 操作，即 {@code BaseConverter} 的反向渲染（业务载体 -> 行模型）。
     * English summary: Executes the toSource operation, the reverse rendering of {@code BaseConverter} (business carrier to row model).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayConsumeFailurePersistenceConverter.toSource(carrier)}。
     * @param target 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @Override
    public GatewayConsumeFailureRecordPO toSource(GatewayConsumeFailureBO target) {
        return toPersistence(target);
    }

    /**
     * 中文说明：执行 newRow 操作，是 {@code toPersistence} 的插入别名，语义为「只带业务列的新行」，技术主键按载体标识
     * 写入（缺失时由 {@code ASSIGN_ID} 补位），租户、审计、软删与版本列一律留空交由受守卫边界补齐。
     * English summary: Executes the newRow operation, an insert alias of {@code toPersistence} producing a row carrying business columns
     * only; the technical key rides the carrier identifier (with {@code ASSIGN_ID} filling a missing one) while the tenant, audit,
     * soft-delete and version columns stay empty for the guarded boundary to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayConsumeFailurePersistenceConverter.newRow(consumeFailureBO)}。
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public GatewayConsumeFailureRecordPO newRow(GatewayConsumeFailureBO carrier) {
        return toPersistence(carrier);
    }
}

/**
 * 中文说明：{@code GatewayConsumeFailureMapping} 是 {@code gateway_call_event_consume_failure} 的 MapStruct 结构映射契约，
 * 逐列复刻被替换的持久化边界：端口十进制主键与 MP bigint {@code id} 双向桥接（空白视为未生成，交给 {@code ASSIGN_ID}），
 * 载体的 {@code partition}/{@code offset} 分别对应列属性 {@code partitionNo}/{@code offsetNo}，
 * 三个 NOT NULL 数值列的空值按旧 JDBC 的 {@code getInt/getLong} 读作 0；受保护技术列除 {@code id} 外全部忽略，因为
 * {@code EgonColaMetaObjectHandler} 独占租户、审计、软删与 MP 版本列；{@code unmappedTargetPolicy=ERROR}
 * 保证任何新增列都必须在此显式表态。
 * English summary: {@code GatewayConsumeFailureMapping} is the MapStruct structural contract for
 * {@code gateway_call_event_consume_failure}, mirroring the persistence boundary it replaces column by column: the port's decimal key
 * bridges the MP bigint {@code id} in both directions (a blank value means not yet generated and is left to {@code ASSIGN_ID}), the
 * carrier's {@code partition} and {@code offset} correspond to the {@code partitionNo} and {@code offsetNo} column properties, and the
 * three NOT NULL numeric columns read as 0 when absent exactly like the legacy {@code getInt/getLong}; every protected technical column
 * except {@code id} stays ignored because {@code EgonColaMetaObjectHandler} owns the tenant, audit, soft-delete and MP version columns;
 * {@code unmappedTargetPolicy=ERROR} forces every new column to be stated explicitly here.
 *
 * 用法 / Usage: 由 {@link GatewayConsumeFailurePersistenceConverter} 通过 {@code Mappers} 持有，不对外暴露，
 * 也不登记为 Spring bean。/ Held by {@link GatewayConsumeFailurePersistenceConverter} through {@code Mappers}; neither published to
 * callers nor registered as a bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface GatewayConsumeFailureMapping {

    /**
     * 中文说明：执行 toBusiness 操作，按 {@code GatewayConsumeFailureBO} 的字段顺序投影消费失败行。
     * English summary: Executes the toBusiness operation, projecting a consume-failure row onto {@code GatewayConsumeFailureBO}.
     *
     * 用法 / Usage: 仅由 {@link GatewayConsumeFailurePersistenceConverter#toBusiness(GatewayConsumeFailureRecordPO)} 调用。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( text( source.getId() ) )")
    @Mapping(target = "topic", source = "topic")
    @Mapping(target = "partition", expression = "java( count( source.getPartitionNo() ) )")
    @Mapping(target = "offset", expression = "java( position( source.getOffsetNo() ) )")
    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "failureCode", source = "failureCode")
    @Mapping(target = "failureMessage", source = "failureMessage")
    @Mapping(target = "payloadSha256", source = "payloadSha256")
    @Mapping(target = "payloadSize", expression = "java( count( source.getPayloadSize() ) )")
    @Mapping(target = "occurredAt", source = "occurredAt")
    GatewayConsumeFailureBO toBusiness(GatewayConsumeFailureRecordPO source);

    /**
     * 中文说明：执行 toRow 操作，把消费失败载体写回行模型；调用方的雪花号写进 {@code id}，分区与位点写进
     * {@code partitionNo}/{@code offsetNo}，受保护技术列（除 {@code id}）全部忽略。
     * English summary: Executes the toRow operation, writing the consume-failure carrier back onto the row model; the caller's snowflake
     * value fills {@code id}, the partition and position fill {@code partitionNo} and {@code offsetNo}, and every protected technical
     * column except {@code id} stays ignored.
     *
     * 用法 / Usage: 仅由 {@link GatewayConsumeFailurePersistenceConverter#toPersistence(GatewayConsumeFailureBO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( identifier( source.getId() ) )")
    @Mapping(target = "topic", source = "topic")
    @Mapping(target = "partitionNo", source = "partition")
    @Mapping(target = "offsetNo", source = "offset")
    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "failureCode", source = "failureCode")
    @Mapping(target = "failureMessage", source = "failureMessage")
    @Mapping(target = "payloadSha256", source = "payloadSha256")
    @Mapping(target = "payloadSize", source = "payloadSize")
    @Mapping(target = "occurredAt", source = "occurredAt")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    GatewayConsumeFailureRecordPO toRow(GatewayConsumeFailureBO source);

    /**
     * 中文说明：执行 text 操作，把 MP 的 bigint 列按十进制文本投影，等价于旧实现的 {@code getString("id")}；
     * {@code null} 保持 {@code null}。
     * English summary: Executes the text operation, projecting the MP bigint column as decimal text, exactly what the replaced
     * {@code getString("id")} produced; {@code null} stays {@code null}.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 标识；parameter identifier。
     * @return 返回 十进制文本；returns the decimal text.
     */
    default String text(Long value) {
        return value == null ? null : Long.toString(value);
    }

    /**
     * 中文说明：执行 identifier 操作，把业务十进制标识解析为 MP bigint；空白视为未生成，主键交由 {@code ASSIGN_ID}
     * 补位，非法值直接拒绝。
     * English summary: Executes the identifier operation, parsing a business decimal identifier into the MP bigint; a blank value is
     * treated as not yet generated so {@code ASSIGN_ID} can fill the key, while a malformed value is rejected.
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
                    "gateway consume failure identifier must be decimal: " + value,
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 count 操作，把可空数值列投影为载体的 {@code int} 字段，等价旧 JDBC 的
     * {@code getInt(...)}（列缺失即 0）。
     * English summary: Executes the count operation, narrowing a nullable numeric column onto the carrier's {@code int} field,
     * equivalent to the legacy {@code getInt(...)} which yields 0 for an absent value.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 数值列；parameter stored counter。
     * @return 返回 数值；returns the counter.
     */
    default int count(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 中文说明：执行 position 操作，把可空位点列投影为载体的 {@code long} 字段，等价旧 JDBC 的
     * {@code getLong("offset_no")}（列缺失即 0）。
     * English summary: Executes the position operation, narrowing a nullable position column onto the carrier's {@code long} field,
     * equivalent to the legacy {@code getLong("offset_no")} which yields 0 for an absent value.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 位点列；parameter stored offset.
     * @return 返回 位点；returns the offset.
     */
    default long position(Long value) {
        return value == null ? 0L : value;
    }
}
