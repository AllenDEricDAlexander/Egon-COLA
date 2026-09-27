package top.egon.cola.component.tianshu.api.client;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import top.egon.cola.component.tianshu.model.ai.DdcAiResourceQuery;
import top.egon.cola.component.tianshu.model.ai.DdcPromptResult;

public interface DdcPromptClient {

    DdcPromptResult get(@NotNull @Valid DdcAiResourceQuery query);
}
