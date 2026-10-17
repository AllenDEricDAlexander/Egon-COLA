package top.egon.cola.component.tianshu.admin.service.ai.impl;

import top.egon.cola.component.tianshu.admin.model.bo.DdcAiJudgeContextBO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiJudgeVerdictDTO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiModelRequestDTO;

/**
 * 评分提示的组装与模型输出解析策略。
 * / Strategy that composes judge prompts and parses model output.
 */
public interface DdcAiJudgeStrategy {

    DdcAiModelRequestDTO compose(DdcAiJudgeContextBO context, int maxContextBytes);

    /** 解析模型文本；不合法时抛出 IllegalArgumentException("INVALID_OUTPUT")。 / Parses model text or throws INVALID_OUTPUT. */
    DdcAiJudgeVerdictDTO parse(String modelText);
}
