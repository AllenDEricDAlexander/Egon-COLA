package top.egon.cola.component.yuheng.admin.wiki.converter;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.yuheng.admin.wiki.domain.bo.WikiRevisionBO;
import top.egon.cola.component.yuheng.admin.wiki.domain.dto.WikiDraftCommandDTO;
import top.egon.cola.component.yuheng.admin.wiki.domain.dto.WikiSourceDTO;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiPublicationPolicyEnum;

/**
 * 中文说明：{@code WikiRevisionConverter} 是把可信草稿命令变成修订载体的唯一出口，负责把
 * {@link WikiDraftCommandDTO} 的五个内容列（标题、正文、标签、链接、来源）搬运成不可变形态，其余列一律不由命令决定：
 * 页面的两个定位键、作者与生成作业只由服务侧从已核验的身份与归属填入，内容摘要、DRAFT 起点、评审初值、
 * 两个版本与「曾发布过」标记全部由 {@link WikiRevisionBO#newDraft} 这一条工厂语句确定，避免同样的不变式在
 * 转换器与载体之间各写一遍；命令里的 {@code expectedRevision} 是并发谓词，绝不作为内容列进入载体。
 * English summary: {@code WikiRevisionConverter} is the only exit turning a trusted draft command into a revision carrier: it
 * transports the five content columns of {@link WikiDraftCommandDTO} (title, body, tags, links and sources) into their immutable
 * form while nothing else is decided by the command — the two page locators, the author and the generation job come from the
 * service's already verified identity and ownership, and the digest, the DRAFT starting point, the initial review status, both
 * versions and the ever-published flag all follow from the single factory statement
 * {@link WikiRevisionBO#newDraft} rather than being restated here; the command's {@code expectedRevision} is a concurrency
 * predicate and never enters the carrier as content.
 *
 * 用法 / Usage: 由 {@code WikiServiceImpl} 注入使用（bean 名 {@code wikiRevisionConverter}），保存草稿与替换草稿都经
 * {@link #newDraft} 取得可落库的修订载体；{@link #toTarget} 只产出五个内容列，供需要「先看内容再定位」的调用方使用，
 * 不是一个可插入的载体。转换器的产物是业务载体，绝不产出行模型。
 * Injected by {@code WikiServiceImpl} as {@code wikiRevisionConverter}: saving and replacing a draft obtain their persistable
 * carrier through {@link #newDraft}, while {@link #toTarget} yields the five content columns only for a caller that needs to read
 * content before locating the page and is not an insertable carrier. What this converter produces is a business carrier, never
 * a row model.
 */
@Slf4j
@Component("wikiRevisionConverter")
public class WikiRevisionConverter implements BaseForwardConverter<
        WikiDraftCommandDTO,
        WikiRevisionBO> {

    /** MapStruct 生成的同包内容列投影器，本类只做空值守护与工厂委托。 / The MapStruct generated same-package content projection; this class only guards nulls and delegates to the factory. */
    private static final WikiRevisionProjection MAPPING = Mappers.getMapper(WikiRevisionProjection.class);

    /**
     * 中文说明：把已核验的命令投影成只带五个内容列的修订载体，列表按 {@code List.copyOf} 冻结。
     * English summary: Projects a verified command onto a carrier holding the five content columns only, lists frozen by
     * {@code List.copyOf}.
     * @param source 参数 草稿命令，可为 {@code null}；parameter the draft command, nullable.
     * @return 返回 内容载体或 {@code null}；returns the content carrier or {@code null}.
     */
    @Override
    public WikiRevisionBO toTarget(WikiDraftCommandDTO source) {
        if (source == null) {
            log.debug("a wiki draft command is absent; no content carrier projected");
            return null;
        }
        return MAPPING.content(source);
    }

    /**
     * 中文说明：按已核验的归属与身份把命令落成可插入的 DRAFT 修订载体：内容列取自命令，其余不变式全部由
     * {@link WikiRevisionBO#newDraft} 一处决定，因此「新修订必然在 DRAFT、必然自带摘要、必然不动已发布指针」
     * 不会因为第二个写入点而漂移。
     * English summary: Turns a verified command into an insertable DRAFT carrier under the verified ownership and identity: the
     * content columns come from the command while every other invariant follows from
     * {@link WikiRevisionBO#newDraft} alone, so "a new revision necessarily sits in DRAFT, necessarily carries its own digest and
     * necessarily leaves the published pointer alone" cannot drift through a second write site.
     * @param command 参数 已通过校验与授权复核的草稿命令；parameter the validated, authorized draft command.
     * @param kbId 参数 已复核归属的知识库十进制字符串 id；parameter the ownership-checked knowledge base id.
     * @param pageId 参数 目标页面十进制字符串 id，新建页面留空由持久边界落定；parameter the target page id, blank for a new page.
     * @param authorActorId 参数 可信主体标识；parameter the trusted actor identity.
     * @param generationJobId 参数 生成作业十进制字符串 id，人工草稿为 {@code null}；parameter the generation job id, {@code null} for a human draft.
     * @param policy 参数 冻结的发布策略快照；parameter the frozen publication policy snapshot.
     * @return 返回 可插入的 DRAFT 修订载体；returns the insertable DRAFT carrier.
     */
    public WikiRevisionBO newDraft(WikiDraftCommandDTO command,
                                   String kbId,
                                   String pageId,
                                   String authorActorId,
                                   String generationJobId,
                                   WikiPublicationPolicyEnum policy) {
        WikiRevisionBO content = toTarget(command);
        if (content == null) {
            return null;
        }
        return WikiRevisionBO.newDraft(
                kbId,
                pageId,
                content.getTitle(),
                content.getMarkdown(),
                content.getTags(),
                content.getLinks(),
                content.getSources(),
                authorActorId,
                generationJobId,
                policy);
    }
}

/**
 * 中文说明：{@code WikiRevisionProjection} 是草稿命令到修订载体的 MapStruct 投影契约：只承认五个内容列，其余目标
 * 逐列显式忽略，{@code unmappedTargetPolicy=ERROR} 让「新增一个命令字段必须在此表态」成为编译期约束；列表在两个
 * 方向上都按 {@code List.copyOf} 冻结，避免调用方随后修改请求体而改写已核验的内容。
 * English summary: {@code WikiRevisionProjection} is the MapStruct contract projecting a draft command onto a revision carrier:
 * only the five content columns are admitted and every other target is ignored column by column, so
 * {@code unmappedTargetPolicy=ERROR} makes "a new command field must be declared here" a compile-time constraint; lists are
 * frozen by {@code List.copyOf} so a caller mutating its request body afterwards cannot rewrite content that was already verified.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface WikiRevisionProjection {

    /**
     * 中文说明：投影五个内容列；主键、定位键、状态与版本列一律忽略，由工厂或受守卫边界决定。
     * English summary: Projects the five content columns, ignoring the key, the locators, the status and the version columns for
     * the factory or the guarded boundary.
     * @param source 参数 草稿命令；parameter the draft command.
     * @return 返回 内容载体；returns the content carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "title", source = "title")
    @Mapping(target = "markdown", source = "markdown")
    @Mapping(target = "tags", expression = "java( frozen( source.getTags() ) )")
    @Mapping(target = "links", expression = "java( frozen( source.getLinks() ) )")
    @Mapping(target = "sources", expression = "java( frozen( source.getSources() ) )")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "kbId", ignore = true)
    @Mapping(target = "pageId", ignore = true)
    @Mapping(target = "contentHash", ignore = true)
    @Mapping(target = "authorActorId", ignore = true)
    @Mapping(target = "generationJobId", ignore = true)
    @Mapping(target = "publicationStatus", ignore = true)
    @Mapping(target = "reviewStatus", ignore = true)
    @Mapping(target = "publicationPolicySnapshot", ignore = true)
    @Mapping(target = "publicationVersion", ignore = true)
    @Mapping(target = "reviewInstanceId", ignore = true)
    @Mapping(target = "reviewerActorId", ignore = true)
    @Mapping(target = "reviewedAt", ignore = true)
    @Mapping(target = "reviewDecisionCode", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "publishedByActorId", ignore = true)
    @Mapping(target = "archivedAt", ignore = true)
    @Mapping(target = "archivedByActorId", ignore = true)
    @Mapping(target = "publicationErrorCode", ignore = true)
    @Mapping(target = "everPublished", ignore = true)
    @Mapping(target = "revision", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    WikiRevisionBO content(WikiDraftCommandDTO source);

    /**
     * 中文说明：把命令里的列表冻结成不可变副本，空列表保持为空而非 {@code null}。
     * English summary: Freezes a command list into an immutable copy, an empty list staying empty rather than {@code null}.
     * @param value 参数 命令列值；parameter the command value.
     * @param <T> 参数 列元素类型；parameter the element type.
     * @return 返回 冻结列表或 {@code null}；returns the frozen list or {@code null}.
     */
    default <T> List<T> frozen(List<T> value) {
        return value == null ? null : List.copyOf(value);
    }
}
