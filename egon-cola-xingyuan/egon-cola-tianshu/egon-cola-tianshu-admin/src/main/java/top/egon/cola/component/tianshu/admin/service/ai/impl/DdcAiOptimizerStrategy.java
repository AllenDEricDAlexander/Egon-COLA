package top.egon.cola.component.tianshu.admin.service.ai.impl;

import top.egon.cola.component.tianshu.admin.model.bo.DdcAiOptimizationContextBO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiModelRequestDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiContentDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;

/**
 * 按资源类型改写内容的策略：组装提示并把模型输出解析为草稿内容。
 * / Per-kind rewrite strategy: composes the prompt and parses model output into draft content.
 */
public interface DdcAiOptimizerStrategy {

    DdcAiKindEnum kind();

    DdcAiModelRequestDTO compose(DdcAiOptimizationContextBO context, String modelAlias, int maxOutputTokens,
            double temperature);

    /** 解析模型文本；结构不合法时抛出 IllegalArgumentException("INVALID_OUTPUT")。 / Parses model text or throws INVALID_OUTPUT. */
    DdcAiContentDTO parse(String modelText);
}
