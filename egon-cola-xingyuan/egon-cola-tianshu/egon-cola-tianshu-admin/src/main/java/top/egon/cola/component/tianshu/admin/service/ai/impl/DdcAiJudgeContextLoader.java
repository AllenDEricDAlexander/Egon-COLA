package top.egon.cola.component.tianshu.admin.service.ai.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiJudgeContextBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiOptimizationPolicyBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiResourceBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiUsageFeedbackBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiVersionBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiVersionNodeBO;
import top.egon.cola.component.tianshu.admin.repository.DdcAiOptimizationPolicyRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiResourceRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiVersionNodeRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiVersionRepository;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeTypeEnum;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 为单个样本读取评分上下文：资源类型、版本内容与策略评分模型。
 * / Loads the judge context of one sample: resource kind, version content and the policy judge model.
 */
@Slf4j
@Component("ddcAiJudgeContextLoader")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class DdcAiJudgeContextLoader {

    static final String SKILL_ROOT_FILE = "SKILL.md";

    @Qualifier("ddcAiResourceRepository")
    private final DdcAiResourceRepository resourceRepository;

    @Qualifier("ddcAiVersionRepository")
    private final DdcAiVersionRepository versionRepository;

    @Qualifier("ddcAiVersionNodeRepository")
    private final DdcAiVersionNodeRepository versionNodeRepository;

    @Qualifier("ddcAiOptimizationPolicyRepository")
    private final DdcAiOptimizationPolicyRepository policyRepository;

    @Qualifier("ddcAiReadTransactionTemplate")
    private final TransactionTemplate readTransactionTemplate;

    /** 资源、版本或策略缺失时返回空，样本按失败处理。 / Returns empty when resource, version or policy is gone. */
    public Optional<DdcAiJudgeContextBO> load(DdcAiUsageFeedbackBO sample) {
        return readTransactionTemplate.execute(status -> {
            Optional<DdcAiResourceBO> resource = resourceRepository.findActiveById(sample.getResourceId());
            Optional<DdcAiVersionBO> version = versionRepository.findActiveById(sample.getVersionId());
            Optional<DdcAiOptimizationPolicyBO> policy = policyRepository.findActiveByResourceId(sample.getResourceId());
            if (resource.isEmpty() || version.isEmpty() || policy.isEmpty()) return Optional.empty();
            return Optional.of(DdcAiJudgeContextBO.builder()
                    .kind(resource.get().getKind())
                    .judgeModelAlias(policy.get().getJudgeModelAlias())
                    .versionText(versionText(resource.get().getKind(), version.get()))
                    .inputExcerpt(sample.getInputExcerpt())
                    .outputExcerpt(sample.getOutputExcerpt())
                    .outcome(sample.getOutcome())
                    .build());
        });
    }

    private String versionText(DdcAiKindEnum kind, DdcAiVersionBO version) {
        if (kind == DdcAiKindEnum.PROMPT) {
            return version.getPromptText() == null ? "" : version.getPromptText();
        }
        List<DdcAiVersionNodeBO> files = versionNodeRepository.findTreeByVersionId(version.getId()).stream()
                .filter(node -> node.getNodeType() == DdcAiNodeTypeEnum.FILE)
                .sorted(Comparator.comparing(DdcAiVersionNodeBO::getPath))
                .toList();
        return files.stream().filter(node -> SKILL_ROOT_FILE.equals(node.getPath())).findFirst()
                .or(() -> files.stream().findFirst())
                .map(node -> node.getContent() == null ? "" : node.getContent())
                .orElse("");
    }
}
