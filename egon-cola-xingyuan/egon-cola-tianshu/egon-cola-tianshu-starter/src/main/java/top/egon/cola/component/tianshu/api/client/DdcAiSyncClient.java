package top.egon.cola.component.tianshu.api.client;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import top.egon.cola.component.tianshu.model.ai.DdcAiPublicationReceiptDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiResourceQuery;
import top.egon.cola.component.tianshu.model.ai.DdcAiRevisionDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiSkillDraftUploadDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiSkillPublishDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiVersionHeadDTO;

/**
 * 下游 Skill 工作区对账与创作目录上传使用的同步端口。
 * / Synchronization port used by downstream Skill workspace reconciliation and authoring-directory upload.
 */
public interface DdcAiSyncClient {

    /** 读取当前发布版本头；查询不得指定 versionNo。 / Reads the current published head; the query must not set versionNo. */
    DdcAiVersionHeadDTO getVersionHead(@NotNull @Valid DdcAiResourceQuery query);

    /** 以修订号 CAS 整树替换 Skill 草稿。 / Replaces the whole Skill draft guarded by a revision CAS. */
    DdcAiRevisionDTO replaceSkillDraft(@NotNull @Valid DdcAiSkillDraftUploadDTO upload);

    /** 按 requestId 幂等发布当前草稿。 / Publishes the current draft idempotently by requestId. */
    DdcAiPublicationReceiptDTO publishSkillDraft(@NotNull @Valid DdcAiSkillPublishDTO publish);
}
