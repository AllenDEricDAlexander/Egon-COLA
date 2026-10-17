package top.egon.cola.component.tianshu.admin.service.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiModelRequestDTO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiModelResultDTO;

/**
 * 评分与改写共用的模型补全端口；失败统一抛出 DdcAiModelException。
 * / Model completion port shared by judging and rewriting; failures are always DdcAiModelException.
 */
@Validated
public interface DdcAiModelPort {

    DdcAiModelResultDTO complete(@NotNull @Valid DdcAiModelRequestDTO request);
}
