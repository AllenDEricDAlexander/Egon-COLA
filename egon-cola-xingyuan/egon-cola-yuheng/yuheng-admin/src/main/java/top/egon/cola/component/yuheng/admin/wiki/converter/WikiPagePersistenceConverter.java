package top.egon.cola.component.yuheng.admin.wiki.converter;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.component.yuheng.admin.wiki.domain.bo.WikiPageBO;
import top.egon.cola.component.yuheng.admin.wiki.domain.po.WikiPagePO;

/**
 * 中文说明：{@code WikiPagePersistenceConverter} 是 {@code gateway_wiki_page} 在持久边界唯一的 MapStruct 转换器，负责
 * {@link WikiPagePO} 行模型与 {@link WikiPageBO} 业务载体之间的双向映射：主键与两个 revision 指针都以十进制文本
 * 往返，未起草或未发布在两侧都是 {@code null} 而绝不被折算成 0；页面业务 {@code revision} 在 {@code Long} 与
 * {@code long} 之间换算（缺列即 1，那是 DDL 的初值而不是「未初始化」）；{@code createdAt}/{@code updatedAt} 只读地
 * 投影自 MP 审计列，租户、操作者、软删与技术 {@code version} 一律不由业务载体写入。
 * English summary: {@code WikiPagePersistenceConverter} is the only MapStruct converter at the persistence boundary of
 * {@code gateway_wiki_page}, mapping {@link WikiPagePO} rows and {@link WikiPageBO} carriers both ways: the key and the two
 * revision pointers round-trip as decimal text, an unset draft or publication staying {@code null} on either side instead of
 * being coerced to zero; the page business {@code revision} converts between {@code Long} and {@code long} (an absent column
 * reads as one, the DDL default rather than an uninitialised row); the audit instants are read-only projections of the MP
 * audit columns while tenant, operator, soft-delete and the technical {@code version} are never written from the carrier.
 *
 * 用法 / Usage: 由 {@code MpWikiRepository} 注入使用（bean 名 {@code wikiPagePersistenceConverter}）。{@code newRow} 产出
 * 只带业务列的待插入行（两个指针留空，业务 revision 由仓储固定为初值）；{@code applyPointers} 只在已加载的活跃行上
 * 覆盖两个指针与业务 revision，绝不触碰 {@code kb_id}/{@code slug}——二者在页面生命周期内不可变，租户与审计列同样
 * 由受守卫边界补齐。行模型不得越过持久边界出现在端口、服务签名或对外投影里。
 * Injected by {@code MpWikiRepository} as {@code wikiPagePersistenceConverter}. {@code newRow} renders a row carrying business
 * columns only (both pointers empty, the business revision fixed by the repository); {@code applyPointers} overwrites only the
 * two pointers and the business revision on a loaded active row and never touches {@code kb_id} or {@code slug}, which stay
 * immutable for the page's lifetime, while tenant and audit columns again come from the guarded boundary. The row model never
 * crosses the persistence boundary into a port, a signature or an outbound projection.
 */
@Slf4j
@Component("wikiPagePersistenceConverter")
public class WikiPagePersistenceConverter implements BaseConverter<
        WikiPagePO,
        WikiPageBO> {

    /** MapStruct 生成的同包结构映射器，本类只做空值守护与日志边界。 / The MapStruct generated same-package mapper; this class only adds null guarding and logging. */
    private static final WikiPageMapping MAPPING = Mappers.getMapper(WikiPageMapping.class);

    /**
     * 中文说明：把页面行模型投影为业务载体。
     * English summary: Projects a page row onto the business carrier.
     * @param row 参数 行模型，可为 {@code null}；parameter the row model, nullable.
     * @return 返回 业务载体或 {@code null}；returns the carrier or {@code null}.
     */
    public WikiPageBO toBusiness(WikiPagePO row) {
        if (row == null) {
            log.debug("gateway_wiki_page row is absent; no business carrier projected");
            return null;
        }
        return MAPPING.toBusiness(row);
    }

    /**
     * 中文说明：把业务载体渲染为行模型。
     * English summary: Renders a business carrier into the MyBatis-Plus row model.
     * @param carrier 参数 业务载体，可为 {@code null}；parameter the carrier, nullable.
     * @return 返回 行模型或 {@code null}；returns the row model or {@code null}.
     */
    public WikiPagePO toPersistence(WikiPageBO carrier) {
        if (carrier == null) {
            log.debug("gateway_wiki_page business carrier is absent; no row rendered");
            return null;
        }
        return MAPPING.toRow(carrier);
    }

    /**
     * 中文说明：按列表顺序投影页面行；空输入返回空列表而非 {@code null}。
     * English summary: Projects page rows in list order; empty input yields an empty list, never {@code null}.
     * @param rows 参数 行模型列表；parameter the row models.
     * @return 返回 业务载体列表；returns the carriers.
     */
    public List<WikiPageBO> toBusinessList(List<WikiPagePO> rows) {
        return toTargetList(rows);
    }

    /** {@code BaseConverter} 的正向投影（行模型 -> 业务载体）。 / The forward projection (row model to business carrier). */
    @Override
    public WikiPageBO toTarget(WikiPagePO source) {
        return toBusiness(source);
    }

    /** {@code BaseConverter} 的反向渲染（业务载体 -> 行模型）。 / The reverse rendering (business carrier to row model). */
    @Override
    public WikiPagePO toSource(WikiPageBO target) {
        return toPersistence(target);
    }

    /**
     * 中文说明：{@code toPersistence} 的插入别名，语义为「只带业务列的新页」：两个指针与业务 revision 留待仓储落定，
     * 租户、审计、软删与版本列一律留空交由受守卫边界补齐。
     * English summary: An insert alias of {@code toPersistence} rendering a new page carrying business columns only: the two
     * pointers and the business revision settle in the repository, while tenant, audit, soft-delete and version stay empty for
     * the guarded boundary.
     * @param carrier 参数 业务载体；parameter the carrier.
     * @return 返回 待插入行模型；returns the row to insert.
     */
    public WikiPagePO newRow(WikiPageBO carrier) {
        return toPersistence(carrier);
    }

    /**
     * 中文说明：在已加载的活跃行上只覆盖两个 revision 指针与业务 revision——指针切换是页面唯一的写入形态，
     * {@code kb_id} 与 {@code slug} 属于不可变身份，技术 {@code version} 与租户/审计列由 MP 与受守卫边界负责。
     * English summary: Overwrites only the two revision pointers and the business revision on a loaded active row — a pointer
     * swap is the only write shape a page ever sees, {@code kb_id} and {@code slug} being immutable identity while the
     * technical {@code version}, tenant and audit columns belong to MyBatis-Plus and the guarded boundary.
     * @param carrier 参数 目标指针已就位的载体；parameter the carrier holding the target pointers.
     * @param row 参数 已加载行模型；parameter the loaded row model.
     */
    public void applyPointers(WikiPageBO carrier, WikiPagePO row) {
        if (carrier == null || row == null) {
            log.debug("gateway_wiki_page carrier or row is absent; nothing applied");
            return;
        }
        MAPPING.applyPointers(carrier, row);
    }
}

/**
 * 中文说明：{@code WikiPageMapping} 是 gateway_wiki_page 的 MapStruct 结构映射契约，逐列声明读写形态：三个 bigint 标识在
 * {@code Long} 与十进制 {@code String} 之间换算，业务 {@code revision} 经 {@code @Named} 换算器把可空列读成正 long，
 * {@code unmappedTargetPolicy=ERROR} 保证任何新增列都必须在此显式表态。
 * English summary: {@code WikiPageMapping} is the MapStruct structural contract for gateway_wiki_page, stating every column
 * shape: the three bigint identifiers convert between {@code Long} and decimal {@code String}, the business {@code revision}
 * converts through a {@code @Named} qualifier reading a nullable column as a positive long, and
 * {@code unmappedTargetPolicy=ERROR} forces every new column to be declared here.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface WikiPageMapping {

    /**
     * 中文说明：按 {@code WikiPageBO} 的字段顺序投影页面行，指针为空即映射为 {@code null}。
     * English summary: Projects a page row onto {@code WikiPageBO}, an absent pointer mapping to {@code null}.
     * @param source 参数 行模型；parameter the row model.
     * @return 返回 业务载体；returns the carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( text( source.getId() ) )")
    @Mapping(target = "kbId", expression = "java( text( source.getKbId() ) )")
    @Mapping(target = "slug", source = "slug")
    @Mapping(target = "draftRevisionId", expression = "java( text( source.getDraftRevisionId() ) )")
    @Mapping(target = "publishedRevisionId", expression = "java( text( source.getPublishedRevisionId() ) )")
    @Mapping(target = "revision", expression = "java( revisionValue( source.getRevision() ) )")
    @Mapping(target = "createdAt", source = "createTime")
    @Mapping(target = "updatedAt", source = "updateTime")
    WikiPageBO toBusiness(WikiPagePO source);

    /**
     * 中文说明：把业务载体渲染为待插入行，只搬运业务列并把十进制文本换算回 bigint。
     * English summary: Renders the carrier into a row, transporting business columns and converting decimal text back to bigint.
     * @param source 参数 业务载体；parameter the carrier.
     * @return 返回 行模型；returns the row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( identifier( source.getId() ) )")
    @Mapping(target = "kbId", expression = "java( identifier( source.getKbId() ) )")
    @Mapping(target = "slug", source = "slug")
    @Mapping(target = "draftRevisionId", expression = "java( identifier( source.getDraftRevisionId() ) )")
    @Mapping(target = "publishedRevisionId", expression = "java( identifier( source.getPublishedRevisionId() ) )")
    @Mapping(target = "revision", source = "revision")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    WikiPagePO toRow(WikiPageBO source);

    /**
     * 中文说明：整行覆盖两个指针与业务 revision，身份列与受保护元数据全部忽略。
     * English summary: Overwrites both pointers and the business revision, ignoring identity and protected metadata.
     * @param source 参数 业务载体；parameter the carrier.
     * @param target 参数 已加载行模型；parameter the loaded row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "draftRevisionId", expression = "java( identifier( source.getDraftRevisionId() ) )")
    @Mapping(target = "publishedRevisionId", expression = "java( identifier( source.getPublishedRevisionId() ) )")
    @Mapping(target = "revision", source = "revision")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "kbId", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void applyPointers(WikiPageBO source, @MappingTarget WikiPagePO target);

    /** 中文说明：把可空 bigint 换算为十进制文本，缺失仍为缺失。 English summary: Converts a nullable bigint into decimal text, an absent value staying absent. */
    default String text(Long value) {
        return value == null ? null : Long.toString(value);
    }

    /** 中文说明：把十进制文本换算回 bigint，空白按 {@code null} 处理而非伪造 0。 English summary: Converts decimal text back to bigint, blank meaning {@code null} rather than a fabricated zero. */
    default Long identifier(String value) {
        return value == null || value.isBlank() ? null : Long.valueOf(value);
    }

    /**
     * 中文说明：把可空业务 revision 投影为正 long，缺列按 DDL 初值 1（那是「刚建好、指针未动过」而不是未初始化）。
     * English summary: Projects a nullable business revision as a positive long, an absent column reading as the DDL default of
     * one — freshly created with pointers unMoved, not uninitialised.
     * @param value 参数 列值；parameter the column value.
     * @return 返回 业务 revision；returns the business revision.
     */
    default long revisionValue(Long value) {
        return value == null ? 1L : value;
    }
}
