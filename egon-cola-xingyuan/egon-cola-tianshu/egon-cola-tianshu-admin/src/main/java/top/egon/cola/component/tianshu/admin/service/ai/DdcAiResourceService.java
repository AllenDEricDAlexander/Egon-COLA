package top.egon.cola.component.tianshu.admin.service.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiCreateCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiDeleteCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiDraftCommand;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiResourceSummaryDTO;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiIdQuery;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiPageQuery;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiResourceResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;

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
}
