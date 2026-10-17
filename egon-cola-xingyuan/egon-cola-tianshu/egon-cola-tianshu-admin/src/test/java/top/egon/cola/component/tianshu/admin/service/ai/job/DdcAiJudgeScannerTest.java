package top.egon.cola.component.tianshu.admin.service.ai.job;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.tianshu.admin.config.DdcAiOptimizationProperties;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiJudgeContextBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiUsageFeedbackBO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiJudgeVerdictDTO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiModelRequestDTO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiModelResultDTO;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiOutcomePersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiRunErrorCodeEnum;
import top.egon.cola.component.tianshu.admin.repository.DdcAiOptimizationPolicyRepository;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiFeedbackService;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiModelPort;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiJudgeContextLoader;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiJudgeStrategy;
import top.egon.cola.component.tianshu.admin.service.ai.model.DdcAiModelException;
import top.egon.cola.component.tianshu.admin.service.ai.validation.DdcAiSystemActor;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DdcAiJudgeScannerTest {

    private final DdcAiFeedbackService feedbackService = mock(DdcAiFeedbackService.class);
    private final DdcAiOptimizationPolicyRepository policyRepository = mock(DdcAiOptimizationPolicyRepository.class);
    private final DdcAiJudgeContextLoader contextLoader = mock(DdcAiJudgeContextLoader.class);
    private final DdcAiJudgeStrategy strategy = mock(DdcAiJudgeStrategy.class);
    private final DdcAiModelPort modelPort = mock(DdcAiModelPort.class);
    private final DdcAiTenantJobRunner runner = mock(DdcAiTenantJobRunner.class);
    private final DdcAiJudgeScanner scanner = new DdcAiJudgeScanner(feedbackService, policyRepository, contextLoader,
            strategy, modelPort, runner, new DdcAiOptimizationProperties());

    @Test
    @SuppressWarnings("unchecked")
    void scanRunsPerTenantAsTheJudgeActor() {
        when(runner.forEachTenant(eq(DdcAiSystemActor.JUDGE), anyString(), any(Supplier.class))).thenReturn(4);

        assertThat(scanner.scanJudge()).isEqualTo(4);
    }

    @Test
    void claimsJudgesAndWritesBackScores() {
        when(policyRepository.findJudgeEnabledResourceIds()).thenReturn(Set.of(7L));
        when(feedbackService.claimForJudge(Set.of(7L), 50)).thenReturn(List.of(sample(1L, 3L), sample(2L, 1L)));
        when(contextLoader.load(any())).thenReturn(Optional.of(context()));
        when(strategy.compose(any(), anyInt())).thenReturn(request());
        when(modelPort.complete(any())).thenReturn(new DdcAiModelResultDTO("{\"score\":66}", 1, 1, "m", "stop"));
        when(strategy.parse("{\"score\":66}")).thenReturn(new DdcAiJudgeVerdictDTO(66, "ok"));

        assertThat(DdcAiSystemActor.bind(DdcAiSystemActor.JUDGE, this::judgeTenant)).isEqualTo(2);

        verify(feedbackService).completeJudge(1L, 3L, new DdcAiJudgeVerdictDTO(66, "ok"));
        verify(feedbackService).completeJudge(2L, 1L, new DdcAiJudgeVerdictDTO(66, "ok"));
        verify(strategy, org.mockito.Mockito.times(2)).compose(any(), eq(16384));
    }

    @Test
    void modelFailureTruncationAndInvalidOutputAreRecordedAsJudgeFailures() {
        when(policyRepository.findJudgeEnabledResourceIds()).thenReturn(Set.of(7L));
        when(feedbackService.claimForJudge(Set.of(7L), 50)).thenReturn(List.of(sample(1L, 3L), sample(2L, 4L),
                sample(3L, 5L)));
        when(contextLoader.load(any())).thenReturn(Optional.of(context()));
        when(strategy.compose(any(), anyInt())).thenReturn(request());
        when(modelPort.complete(any()))
                .thenThrow(new DdcAiModelException(DdcAiRunErrorCodeEnum.MODEL_TIMEOUT, 0, "timeout", null))
                .thenReturn(new DdcAiModelResultDTO("{\"score\":", 1, 1, "m", "length"))
                .thenReturn(new DdcAiModelResultDTO("garbage", 1, 1, "m", "stop"));
        when(strategy.parse("garbage")).thenThrow(new IllegalArgumentException("INVALID_OUTPUT"));

        assertThat(judgeTenant()).isZero();

        verify(feedbackService).failJudge(1L, 3L, DdcAiRunErrorCodeEnum.MODEL_TIMEOUT);
        verify(feedbackService).failJudge(2L, 4L, DdcAiRunErrorCodeEnum.INVALID_OUTPUT);
        verify(feedbackService).failJudge(3L, 5L, DdcAiRunErrorCodeEnum.INVALID_OUTPUT);
        verify(feedbackService, never()).completeJudge(any(), any(), any());
    }

    @Test
    void missingContextFailsTheSampleWithoutCallingTheModel() {
        when(policyRepository.findJudgeEnabledResourceIds()).thenReturn(Set.of(7L));
        when(feedbackService.claimForJudge(Set.of(7L), 50)).thenReturn(List.of(sample(1L, 3L)));
        when(contextLoader.load(any())).thenReturn(Optional.empty());

        judgeTenant();

        verify(feedbackService).failJudge(1L, 3L, DdcAiRunErrorCodeEnum.NOT_PUBLISHED);
        verifyNoInteractions(modelPort);
    }

    @Test
    void noJudgeEnabledPolicyMeansNoClaims() {
        when(policyRepository.findJudgeEnabledResourceIds()).thenReturn(Set.of());

        assertThat(judgeTenant()).isZero();
        verifyNoInteractions(feedbackService, modelPort);
    }

    private int judgeTenant() {
        return scanner.judgeTenant();
    }

    private static DdcAiUsageFeedbackBO sample(Long id, Long version) {
        return DdcAiUsageFeedbackBO.builder().id(id).version(version).resourceId(7L).versionId(9L).build();
    }

    private static DdcAiJudgeContextBO context() {
        return DdcAiJudgeContextBO.builder().kind(DdcAiKindEnum.PROMPT).judgeModelAlias("qwen-turbo")
                .versionText("prompt").outcome(DdcAiOutcomePersistenceEnum.FAILURE).build();
    }

    private static DdcAiModelRequestDTO request() {
        return new DdcAiModelRequestDTO("qwen-turbo", "system", "user", 512, 0.0d, "JUDGE");
    }
}
