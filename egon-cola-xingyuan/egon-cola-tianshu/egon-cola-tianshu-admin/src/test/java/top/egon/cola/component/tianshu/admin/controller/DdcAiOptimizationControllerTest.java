package top.egon.cola.component.tianshu.admin.controller;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.tianshu.admin.config.DdcAiWebConfiguration;
import top.egon.cola.component.tianshu.admin.controller.ai.DdcAiResourceController;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiOptimizationPolicyCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiOptimizationTriggerCommand;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiOutcomePersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiRunStatePersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiIdQuery;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiOptimizationRunPageQuery;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiUsageFeedbackPageQuery;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiOptimizationPolicyResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiOptimizationRunResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiUsageFeedbackResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiFeedbackService;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiOptimizationService;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiResourceService;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DdcAiResourceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(DdcAiWebConfiguration.class)
class DdcAiOptimizationControllerTest {

    private static final String BASE = "/api/v1/tianshu/ai/resources";
    private static final String TENANT = "X-Egon-Tianshu-Tenant";
    private static final String KEY = "opt-key-0123456789abcdef";
    private static final Instant NOW = Instant.parse("2026-10-17T06:00:00Z");

    @Autowired private MockMvc mockMvc;
    @MockBean(name = "ddcAiResourceService") private DdcAiResourceService resourceService;
    @MockBean(name = "ddcAiOptimizationService") private DdcAiOptimizationService optimizationService;
    @MockBean(name = "ddcAiFeedbackService") private DdcAiFeedbackService feedbackService;

    @Test
    void getPolicyReturnsDefaultsWhenNotPersisted() throws Exception {
        when(optimizationService.getPolicy(new DdcAiIdQuery(7L))).thenReturn(policy(false, 0L));

        mockMvc.perform(get(BASE + "/{id}/optimization-policy", "7").header(TENANT, "42"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.resourceId").value("7"))
                .andExpect(jsonPath("$.data.persisted").value(false))
                .andExpect(jsonPath("$.data.revision").value("0"))
                .andExpect(jsonPath("$.data.evaluationWindow").value("PT24H"))
                .andExpect(jsonPath("$.data.cooldown").value("PT1H"))
                .andExpect(jsonPath("$.data.evaluation.averageScore").value("61.50"));
    }

    @Test
    void replacePolicyBindsDurationsAndMapsStaleRevisionTo409() throws Exception {
        when(optimizationService.replacePolicy(eq(new DdcAiIdQuery(7L)), any())).thenReturn(policy(true, 1L));

        mockMvc.perform(put(BASE + "/{id}/optimization-policy", "7").header(TENANT, "42")
                        .contentType(MediaType.APPLICATION_JSON).content(policyJson("PT24H", "PT1H", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.revision").value("1"));
        ArgumentCaptor<DdcAiOptimizationPolicyCommand> command =
                ArgumentCaptor.forClass(DdcAiOptimizationPolicyCommand.class);
        verify(optimizationService).replacePolicy(eq(new DdcAiIdQuery(7L)), command.capture());
        assertThat(command.getValue().getEvaluationWindow()).isEqualTo(Duration.ofHours(24));
        assertThat(command.getValue().getCooldown()).isEqualTo(Duration.ofHours(1));
        assertThat(command.getValue().getExpectedRevision()).isEqualTo("0");

        when(optimizationService.replacePolicy(eq(new DdcAiIdQuery(7L)), any()))
                .thenThrow(new CommonException(56303, "TIANSHU_AI_REVISION_CONFLICT", "stale"));
        mockMvc.perform(put(BASE + "/{id}/optimization-policy", "7").header(TENANT, "42")
                        .contentType(MediaType.APPLICATION_JSON).content(policyJson("PT24H", "PT1H", "")))
                .andExpect(status().isConflict())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value(56303));
    }

    @Test
    void replacePolicyRejectsRearmWithActiveRun409() throws Exception {
        when(optimizationService.replacePolicy(any(), any()))
                .thenThrow(new CommonException(56323, "TIANSHU_AI_RUN_ACTIVE", "active"));

        mockMvc.perform(put(BASE + "/{id}/optimization-policy", "7").header(TENANT, "42")
                        .contentType(MediaType.APPLICATION_JSON).content(policyJson("PT24H", "PT1H", "")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(56323));
    }

    @Test
    void replacePolicyRejectsDurationOutOfRangeAndUnknownFields400() throws Exception {
        mockMvc.perform(put(BASE + "/{id}/optimization-policy", "7").header(TENANT, "42")
                        .contentType(MediaType.APPLICATION_JSON).content(policyJson("PT1M", "PT1H", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(56301));
        mockMvc.perform(put(BASE + "/{id}/optimization-policy", "7").header(TENANT, "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(policyJson("PT24H", "PT1H", ",\"unexpected\":true")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(56301));

        verify(optimizationService, never()).replacePolicy(any(), any());
    }

    @Test
    void triggerReturns202WithLocationAndPassesTheIdempotencyKey() throws Exception {
        when(optimizationService.trigger(eq(new DdcAiIdQuery(7L)), eq(KEY), any())).thenReturn(run(88L, "PENDING"));

        mockMvc.perform(post(BASE + "/{id}/optimization-runs", "7").header(TENANT, "42")
                        .header("Idempotency-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"try now\"}"))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", BASE + "/7/optimization-runs/88"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.id").value("88"))
                .andExpect(jsonPath("$.data.state").value("PENDING"));

        ArgumentCaptor<DdcAiOptimizationTriggerCommand> command =
                ArgumentCaptor.forClass(DdcAiOptimizationTriggerCommand.class);
        verify(optimizationService).trigger(eq(new DdcAiIdQuery(7L)), eq(KEY), command.capture());
        assertThat(command.getValue().getNote()).isEqualTo("try now");
    }

    @Test
    void triggerRejectsMissingOrInvalidIdempotencyKey400() throws Exception {
        mockMvc.perform(post(BASE + "/{id}/optimization-runs", "7").header(TENANT, "42")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(56301));
        mockMvc.perform(post(BASE + "/{id}/optimization-runs", "7").header(TENANT, "42")
                        .header("Idempotency-Key", "short")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(56301));

        verifyNoInteractions(optimizationService);
    }

    @Test
    void triggerMapsChannelDisabledTo503AndNotPublishedTo400() throws Exception {
        when(optimizationService.trigger(any(), any(), any()))
                .thenThrow(new CommonException(56322, "TIANSHU_AI_OPTIMIZATION_DISABLED", "disabled"))
                .thenThrow(new CommonException(56321, "TIANSHU_AI_NOT_PUBLISHED", "not published"));

        mockMvc.perform(post(BASE + "/{id}/optimization-runs", "7").header(TENANT, "42")
                        .header("Idempotency-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(56322));
        mockMvc.perform(post(BASE + "/{id}/optimization-runs", "7").header(TENANT, "42")
                        .header("Idempotency-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(56321));
    }

    @Test
    void pageRunsBindsStateFilterAndPaging() throws Exception {
        when(optimizationService.pageRuns(any())).thenReturn(DdcPageResult.<DdcAiOptimizationRunResult>builder()
                .records(java.util.List.of(run(88L, "FAILED"))).total(1).pageNo(2).pageSize(10).build());

        mockMvc.perform(get(BASE + "/{id}/optimization-runs", "7").header(TENANT, "42")
                        .param("state", "FAILED").param("pageNo", "2").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.records[0].id").value("88"))
                .andExpect(jsonPath("$.page.pageNo").value(2));

        ArgumentCaptor<DdcAiOptimizationRunPageQuery> query = ArgumentCaptor.forClass(DdcAiOptimizationRunPageQuery.class);
        verify(optimizationService).pageRuns(query.capture());
        assertThat(query.getValue()).isEqualTo(DdcAiOptimizationRunPageQuery.builder().resourceId(7L)
                .state(DdcAiRunStatePersistenceEnum.FAILED).pageNo(2).pageSize(10).build());

        mockMvc.perform(get(BASE + "/{id}/optimization-runs", "7").header(TENANT, "42").param("pageSize", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRunReturns404ForForeignResource() throws Exception {
        when(optimizationService.getRun(new DdcAiIdQuery(7L), 99L))
                .thenThrow(new CommonException(56325, "TIANSHU_AI_RUN_NOT_FOUND", "not found"));
        when(optimizationService.getRun(new DdcAiIdQuery(7L), 88L)).thenReturn(run(88L, "SUCCEEDED"));

        mockMvc.perform(get(BASE + "/{id}/optimization-runs/{runId}", "7", "99").header(TENANT, "42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(56325));
        mockMvc.perform(get(BASE + "/{id}/optimization-runs/{runId}", "7", "88").header(TENANT, "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.averageScore").value("55.25"))
                .andExpect(jsonPath("$.data.windowEnd").value("2026-10-17T06:00:00Z"));
    }

    @Test
    void pageFeedbackBindsFiltersAndPropagatesPermissionDenial() throws Exception {
        when(feedbackService.pageFeedback(any())).thenReturn(DdcPageResult.<DdcAiUsageFeedbackResult>builder()
                .records(java.util.List.of(DdcAiUsageFeedbackResult.builder().id(5L).resourceId(7L).versionNo(3L)
                        .usageId("01JAX0M7Q9K2R5T8V1W4X7Y0ZB").outcome("FAILURE").judgeScore(40)
                        .effectiveScore(40).scoreSource("JUDGE").occurredAt(NOW).build()))
                .total(1).pageNo(1).pageSize(20).build());

        mockMvc.perform(get(BASE + "/{id}/usage-feedback", "7").header(TENANT, "42")
                        .param("versionNo", "3").param("outcome", "FAILURE").param("scoreSource", "JUDGE")
                        .param("maxScore", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].id").value("5"))
                .andExpect(jsonPath("$.records[0].scoreSource").value("JUDGE"))
                .andExpect(jsonPath("$.records[0].occurredAt").value("2026-10-17T06:00:00Z"));
        ArgumentCaptor<DdcAiUsageFeedbackPageQuery> query = ArgumentCaptor.forClass(DdcAiUsageFeedbackPageQuery.class);
        verify(feedbackService).pageFeedback(query.capture());
        assertThat(query.getValue()).isEqualTo(DdcAiUsageFeedbackPageQuery.builder().resourceId(7L).versionNo(3L)
                .outcome(DdcAiOutcomePersistenceEnum.FAILURE).scoreSource("JUDGE").maxScore(60).pageNo(1)
                .pageSize(20).build());

        mockMvc.perform(get(BASE + "/{id}/usage-feedback", "7").header(TENANT, "42").param("scoreSource", "OTHER"))
                .andExpect(status().isBadRequest());

        when(feedbackService.pageFeedback(any()))
                .thenThrow(new CommonException(56307, "TIANSHU_AI_PERMISSION_DENIED", "denied"));
        mockMvc.perform(get(BASE + "/{id}/usage-feedback", "7").header(TENANT, "42"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(56307));
    }

    private static String policyJson(String window, String cooldown, String extra) {
        return "{\"expectedRevision\":\"0\",\"enabled\":true,\"scoreThreshold\":80,\"minSamples\":20,"
                + "\"evaluationWindow\":\"" + window + "\",\"maxIterations\":5,\"cooldown\":\"" + cooldown + "\","
                + "\"autoPublish\":false,\"judgeEnabled\":true,\"optimizerModelAlias\":\"gpt-opt\","
                + "\"judgeModelAlias\":\"gpt-judge\",\"optimizerInstruction\":\"keep it short\",\"rearm\":false"
                + extra + "}";
    }

    private static DdcAiOptimizationPolicyResult policy(boolean persisted, long revision) {
        return DdcAiOptimizationPolicyResult.builder().resourceId(7L).persisted(persisted).revision(revision)
                .enabled(true).scoreThreshold(80).minSamples(20).evaluationWindow(Duration.ofHours(24))
                .maxIterations(5).cooldown(Duration.ofHours(1)).autoPublish(false).judgeEnabled(true)
                .optimizerModelAlias("gpt-opt").judgeModelAlias("gpt-judge").optimizerInstruction("")
                .state("ARMED").iterationsDone(0).consecutiveFailures(0)
                .evaluation(DdcAiOptimizationPolicyResult.EvaluationResult.builder().evaluatedAt(NOW).versionId(9L)
                        .versionNo(3L).sampleCount(24).averageScore(new BigDecimal("61.50")).pendingJudgeCount(2)
                        .build())
                .build();
    }

    private static DdcAiOptimizationRunResult run(long id, String state) {
        return DdcAiOptimizationRunResult.builder().id(id).resourceId(7L).policyId(3L).runNo(4L).iterationNo(1)
                .trigger("MANUAL").state(state).baseVersionId(9L).baseVersionNo(3L).sampleCount(24)
                .averageScore(new BigDecimal("55.25")).thresholdSnapshot(80).windowStart(NOW.minusSeconds(86_400))
                .windowEnd(NOW).modelAlias("gpt-opt").autoPublished(false).requestId(KEY).note("")
                .createdAt(NOW).createdBy("user-1").build();
    }
}
