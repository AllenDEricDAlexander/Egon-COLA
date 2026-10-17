package top.egon.cola.component.tianshu.admin.service.ai.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import top.egon.cola.component.tianshu.admin.config.DdcAiOptimizationProperties;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiJudgeContextBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiUsageFeedbackBO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiModelResultDTO;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiRunErrorCodeEnum;
import top.egon.cola.component.tianshu.admin.repository.DdcAiOptimizationPolicyRepository;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiFeedbackService;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiModelPort;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiJudgeContextLoader;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiJudgeStrategy;
import top.egon.cola.component.tianshu.admin.service.ai.model.DdcAiModelException;
import top.egon.cola.component.tianshu.admin.service.ai.validation.DdcAiSystemActor;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * JOB-001：对无显式分数的样本做 LLM 评分。认领在短事务内完成，模型调用在事务外串行执行。
 * / JOB-001: LLM-judges samples without an explicit score. Claims happen in a short transaction; model calls run
 * serially outside it.
 */
@Slf4j
public class DdcAiJudgeScanner {

    private final DdcAiFeedbackService feedbackService;
    private final DdcAiOptimizationPolicyRepository policyRepository;
    private final DdcAiJudgeContextLoader contextLoader;
    private final DdcAiJudgeStrategy strategy;
    private final DdcAiModelPort modelPort;
    private final DdcAiTenantJobRunner tenantJobRunner;
    private final DdcAiOptimizationProperties properties;

    public DdcAiJudgeScanner(DdcAiFeedbackService feedbackService, DdcAiOptimizationPolicyRepository policyRepository,
            DdcAiJudgeContextLoader contextLoader, DdcAiJudgeStrategy strategy, DdcAiModelPort modelPort,
            DdcAiTenantJobRunner tenantJobRunner, DdcAiOptimizationProperties properties) {
        this.feedbackService = feedbackService;
        this.policyRepository = policyRepository;
        this.contextLoader = contextLoader;
        this.strategy = strategy;
        this.modelPort = modelPort;
        this.tenantJobRunner = tenantJobRunner;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${egon.cola.component.tianshu.admin.ai.judge.scan-interval:30s}",
            initialDelayString = "${egon.cola.component.tianshu.admin.ai.judge.scan-interval:30s}")
    public int scanJudge() {
        return tenantJobRunner.forEachTenant(DdcAiSystemActor.JUDGE, "AI judge scan", this::judgeTenant);
    }

    int judgeTenant() {
        Set<Long> resourceIds = policyRepository.findJudgeEnabledResourceIds();
        if (resourceIds.isEmpty()) return 0;
        List<DdcAiUsageFeedbackBO> claimed = feedbackService.claimForJudge(resourceIds,
                properties.getJudge().getBatchSize());
        int judged = 0;
        for (DdcAiUsageFeedbackBO sample : claimed) {
            if (judgeOne(sample)) judged++;
        }
        return judged;
    }

    private boolean judgeOne(DdcAiUsageFeedbackBO sample) {
        try {
            Optional<DdcAiJudgeContextBO> context = contextLoader.load(sample);
            if (context.isEmpty()) {
                feedbackService.failJudge(sample.getId(), sample.getVersion(), DdcAiRunErrorCodeEnum.NOT_PUBLISHED);
                return false;
            }
            DdcAiModelResultDTO result = modelPort.complete(
                    strategy.compose(context.get(), properties.getJudge().getMaxContextBytes()));
            if (result.truncated()) throw new IllegalArgumentException("INVALID_OUTPUT");
            feedbackService.completeJudge(sample.getId(), sample.getVersion(), strategy.parse(result.text()));
            log.info("AI_JUDGE resourceId={} sampleId={} result=DONE", sample.getResourceId(), sample.getId());
            return true;
        } catch (DdcAiModelException model) {
            feedbackService.failJudge(sample.getId(), sample.getVersion(), model.getCode());
            log.warn("AI_JUDGE resourceId={} sampleId={} result={}", sample.getResourceId(), sample.getId(),
                    model.getCode().getValue());
            return false;
        } catch (IllegalArgumentException invalid) {
            feedbackService.failJudge(sample.getId(), sample.getVersion(), DdcAiRunErrorCodeEnum.INVALID_OUTPUT);
            log.warn("AI_JUDGE resourceId={} sampleId={} result=INVALID_OUTPUT", sample.getResourceId(), sample.getId());
            return false;
        }
    }
}
