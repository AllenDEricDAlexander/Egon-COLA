package top.egon.cola.component.tianshu.admin.model;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiOptimizationPolicyBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiOptimizationRunBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiUsageFeedbackBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiEnumConverterImpl;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiOptimizationPolicyConverterImpl;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiOptimizationRunConverterImpl;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiUsageFeedbackConverterImpl;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiJudgeStatePersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiOutcomePersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiPolicyStatePersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiRunErrorCodeEnum;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiRunStatePersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiRunTriggerPersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiOptimizationPolicyPO;
import top.egon.cola.component.tianshu.model.ai.DdcAiOutcomeEnum;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcAiOptimizationConverterTest {

    private static final Instant NOW = Instant.parse("2026-10-17T05:00:00Z");

    @Test
    void policyRoundTripsDurationsAsSeconds() {
        DdcAiOptimizationPolicyConverterImpl converter = new DdcAiOptimizationPolicyConverterImpl();
        DdcAiOptimizationPolicyBO policy = DdcAiOptimizationPolicyBO.builder()
                .id(5L).tenantId(42L).version(3L).resourceId(7L).enabled(true)
                .scoreThreshold((short) 80).minSamples(20).evaluationWindow(Duration.ofHours(24))
                .maxIterations((short) 5).cooldown(Duration.ofHours(1)).autoPublish(false).judgeEnabled(true)
                .optimizerModelAlias("qwen-plus").judgeModelAlias("qwen-turbo").optimizerInstruction("")
                .state(DdcAiPolicyStatePersistenceEnum.ARMED).iterationsDone((short) 0).consecutiveFailures((short) 0)
                .lastAverageScore(new BigDecimal("71.50")).lastEvaluatedAt(NOW)
                .build();

        DdcAiOptimizationPolicyPO po = converter.toSource(policy);

        assertThat(po.getEvaluationWindowSeconds()).isEqualTo(86400);
        assertThat(po.getCooldownSeconds()).isEqualTo(3600);
        assertThat(po.getVersion()).isEqualTo(3L);
        assertThat(converter.toTarget(po)).usingRecursiveComparison().isEqualTo(policy);
    }

    @Test
    void feedbackAndRunRoundTripWithPersistenceEnums() {
        DdcAiUsageFeedbackBO feedback = DdcAiUsageFeedbackBO.builder()
                .id(1L).resourceId(7L).versionId(9L).usageId("01JAX0M7Q9K2R5T8V1W4X7Y0ZB")
                .reporterCredentialId("runtime").bizCode("xingyuan").envCode("dev").appCode("order")
                .outcome(DdcAiOutcomePersistenceEnum.FAILURE).judgeState(DdcAiJudgeStatePersistenceEnum.PENDING)
                .judgeAttempts((short) 0).occurredAt(NOW).build();
        DdcAiOptimizationRunBO run = DdcAiOptimizationRunBO.builder()
                .id(2L).resourceId(7L).policyId(5L).runNo(4L).iterationNo((short) 1)
                .trigger(DdcAiRunTriggerPersistenceEnum.MANUAL).state(DdcAiRunStatePersistenceEnum.SKIPPED)
                .errorCode(DdcAiRunErrorCodeEnum.DRAFT_DIRTY).baseVersionId(9L).sampleCount(25)
                .thresholdSnapshot((short) 80).windowStart(NOW.minusSeconds(86400)).windowEnd(NOW)
                .modelAlias("qwen-plus").autoPublished(false).note("").finishedAt(NOW).build();

        var feedbackConverter = new DdcAiUsageFeedbackConverterImpl();
        var runConverter = new DdcAiOptimizationRunConverterImpl();

        assertThat(feedbackConverter.toTarget(feedbackConverter.toSource(feedback)))
                .usingRecursiveComparison().isEqualTo(feedback);
        assertThat(runConverter.toTarget(runConverter.toSource(run))).usingRecursiveComparison().isEqualTo(run);
    }

    @Test
    void enumsUseStableValuesNotOrdinals() {
        assertThat(DdcAiRunStatePersistenceEnum.SKIPPED.getValue()).isEqualTo("SKIPPED");
        assertThat(DdcAiRunStatePersistenceEnum.SKIPPED.terminal()).isTrue();
        assertThat(DdcAiRunStatePersistenceEnum.RUNNING.terminal()).isFalse();
        assertThat(DdcAiPolicyStatePersistenceEnum.AWAITING_PUBLISH.evaluable()).isTrue();
        assertThat(DdcAiPolicyStatePersistenceEnum.CONVERGED.evaluable()).isFalse();
        assertThat(DdcAiRunErrorCodeEnum.fromValue("LEASE_EXPIRED")).isEqualTo(DdcAiRunErrorCodeEnum.LEASE_EXPIRED);
        assertThatThrownBy(() -> DdcAiRunErrorCodeEnum.fromValue("NOPE")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DdcAiJudgeStatePersistenceEnum.fromValue("0"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void outcomeMapsBetweenSdkAndPersistence() {
        DdcAiEnumConverterImpl converter = new DdcAiEnumConverterImpl();

        assertThat(converter.toPersistenceOutcome(DdcAiOutcomeEnum.UNKNOWN)).isEqualTo(DdcAiOutcomePersistenceEnum.UNKNOWN);
        assertThat(converter.toSdkOutcome(DdcAiOutcomePersistenceEnum.SUCCESS)).isEqualTo(DdcAiOutcomeEnum.SUCCESS);
    }
}
