package top.egon.cola.component.tianshu.admin.service.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiSkillDraftUploadCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiSkillPublishCommand;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiPublicationReceiptResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiRevisionResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiVersionHeadResult;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiCreateCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiDeleteCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiDraftCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiPromotionCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiPublishCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiRollbackCommand;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiResourceSummaryDTO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiVersionSummaryDTO;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiIdQuery;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiPageQuery;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiPublicationPageQuery;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiVersionPageQuery;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiVersionQuery;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiPublicationResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiResourceResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiVersionResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiResourceQuery;

@Validated
public interface DdcAiResourceService {

    DdcAiResourceResult create(
            @NotNull @Valid DdcAiCreateCommand command);

    DdcAiResourceResult get(
            @NotNull @Valid DdcAiIdQuery query);

    DdcPageResult<DdcAiResourceSummaryDTO> page(
            @NotNull @Valid DdcAiPageQuery query);

    DdcAiResourceResult replaceDraft(
            @NotNull @Valid DdcAiIdQuery query,
            @NotNull @Valid DdcAiDraftCommand command);

    void delete(
            @NotNull @Valid DdcAiIdQuery query,
            @NotNull @Valid DdcAiDeleteCommand command);

    DdcAiPublicationResult publish(
            @NotNull @Valid DdcAiIdQuery query,
            @NotBlank @Size(min = 16, max = 64) @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$") String requestId,
            @NotNull @Valid DdcAiPublishCommand command);

    DdcAiPublicationResult rollback(
            @NotNull @Valid DdcAiIdQuery query,
            @NotBlank @Size(min = 16, max = 64) @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$") String requestId,
            @NotNull @Valid DdcAiRollbackCommand command);

    DdcAiPublicationResult promote(
            @NotNull @Valid DdcAiIdQuery sourceQuery,
            @NotNull @Valid DdcAiPromotionCommand command,
            @NotBlank @Size(min = 16, max = 64) @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$") String requestId);

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcAiVersionResult getVersion(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Valid DdcAiVersionQuery query);

    DdcAiVersionResult getPublishedVersion(
            @NotNull @Valid DdcAiResourceQuery query,
            @NotNull DdcAiKindEnum expectedKind);

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcPageResult<DdcAiVersionSummaryDTO> pageVersions(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Valid DdcAiVersionPageQuery query);

    /** RPC-004：只读版本头；查询不得固定版本号。 / RPC-004: read-only version head; the query must not pin a version. */
    DdcAiVersionHeadResult getVersionHead(@NotNull @Valid DdcAiResourceQuery query);

    /** RPC-005：校验 Skill 身份后整树替换草稿并返回修订回执。 / RPC-005: replaces a Skill draft after identity checks. */
    DdcAiRevisionResult replaceSkillDraft(@NotNull @Valid DdcAiSkillDraftUploadCommand command);

    /** RPC-006：校验 Skill 身份后幂等发布草稿。 / RPC-006: publishes a Skill draft idempotently after identity checks. */
    DdcAiPublicationReceiptResult publishSkillDraft(@NotNull @Valid DdcAiSkillPublishCommand command);

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcPageResult<DdcAiPublicationResult> pagePublications(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Valid DdcAiPublicationPageQuery query);
}
