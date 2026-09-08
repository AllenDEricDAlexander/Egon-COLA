package top.egon.cola.archetype.source.web.infrastructure.client.evaluation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.mapstruct.factory.Mappers;
import jakarta.validation.Validation;
import jakarta.validation.ConstraintViolationException;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.exception.EgonRpcException;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import top.egon.cola.evaluation.facade.rpc.*;
import top.egon.cola.evaluation.facade.course.dto.*;
import top.egon.cola.evaluation.facade.exam.dto.*;
import top.egon.cola.evaluation.facade.dto.SingleResponse;
import top.egon.cola.archetype.source.web.domain.client.ExternalDependencyException;
import top.egon.cola.archetype.source.web.domain.client.ExternalDependencyFailure;
import top.egon.cola.archetype.source.web.domain.client.evaluation.*;
import java.time.Instant;

class NativeEvaluationQueryClientTest {
    private static final jakarta.validation.ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();
    private final ValidationUtils validation = new ValidationUtils(VALIDATORS.getValidator());
    private final EvaluationRpcConverter converter = Mappers.getMapper(EvaluationRpcConverter.class);
    private final EvaluationQueryConverter projection = Mappers.getMapper(EvaluationQueryConverter.class);
    private CourseRpcService courseService;
    private ExamRpcService examService;
    private ScoreRpcService scoreService;
    private NativeEvaluationQueryClient client;

    @BeforeEach
    void setUp() {
        courseService = mock(CourseRpcService.class);
        examService = mock(ExamRpcService.class);
        scoreService = mock(ScoreRpcService.class);
        client = new NativeEvaluationQueryClient(courseService, examService, scoreService, converter, projection, validation);
    }

    @AfterAll
    static void closeValidationFactory() { VALIDATORS.close(); }

    @Test
    void maps_course_to_the_existing_consumer_projection() {
        when(courseService.getCourse(courseRequest())).thenReturn(converter.toCourseRpcResponse(
                SingleResponse.of(new CourseResponse(1001L, "C1", "Course One", 3, "ACTIVE"))));
        assertThat(client.getCourse(1001L)).isEqualTo(new EvaluationCourse(1001L, "C1", "Course One", 3, "ACTIVE"));
        verify(courseService).getCourse(courseRequest());
    }

    @Test
    void maps_exam_without_losing_instant_nanoseconds() {
        var startsAt = Instant.parse("2026-09-08T01:02:03.123456789Z");
        var endsAt = Instant.parse("2026-09-08T02:03:04.987654321Z");
        var request = converter.toTarget(new GetExamRequest(2001L));
        when(examService.getExam(request)).thenReturn(converter.toExamRpcResponse(SingleResponse.of(
                new ExamResponse(2001L, 1001L, "Exam One", startsAt, endsAt, "PUBLISHED"))));
        assertThat(client.getExam(2001L)).isEqualTo(new EvaluationExam(2001L, 1001L, "Exam One", startsAt, endsAt, "PUBLISHED"));
        verify(examService).getExam(request);
    }

    @Test
    void maps_score_with_both_query_identifiers() {
        var request = converter.toTarget(new GetScoreRequest(2001L, 3001L));
        when(scoreService.getScore(request)).thenReturn(converter.toScoreRpcResponse(SingleResponse.of(
                new ScoreResponse(3001L, 2001L, 1001L, 7001L, 95, "RECORDED"))));
        assertThat(client.getScore(2001L, 3001L)).isEqualTo(new EvaluationScore(3001L, 2001L, 1001L, 7001L, 95, "RECORDED"));
        verify(scoreService).getScore(request);
    }

    @Test
    void preserves_provider_failure_categories_and_sanitizes_messages() {
        assertProviderFailure("COURSE_NOT_FOUND", ExternalDependencyFailure.NOT_FOUND);
        assertProviderFailure("VALIDATION_FAILED", ExternalDependencyFailure.VALIDATION_FAILED);
        assertProviderFailure("COURSE_CONFLICT", ExternalDependencyFailure.BUSINESS_REJECTED);
        assertProviderFailure("INTERNAL_ERROR", ExternalDependencyFailure.SERVICE_FAILURE);
    }

    @Test
    void maps_native_timeout_availability_and_contract_errors() {
        assertTransportFailure(EgonRpcErrorCode.RPC_DEADLINE_EXCEEDED, ExternalDependencyFailure.TIMEOUT);
        assertTransportFailure(EgonRpcErrorCode.RPC_PROVIDER_UNAVAILABLE, ExternalDependencyFailure.UNAVAILABLE);
        assertTransportFailure(EgonRpcErrorCode.RPC_INVALID_CONTRACT, ExternalDependencyFailure.CONTRACT_INCOMPATIBLE);
        assertTransportFailure(EgonRpcErrorCode.RPC_METHOD_NOT_FOUND, ExternalDependencyFailure.CONTRACT_INCOMPATIBLE);
    }

    @Test
    void rejects_null_missing_or_invalid_success_data() {
        when(courseService.getCourse(courseRequest())).thenReturn(null);
        assertFailure(() -> client.getCourse(1001L), ExternalDependencyFailure.CONTRACT_INCOMPATIBLE);
        when(courseService.getCourse(courseRequest())).thenReturn(converter.toCourseRpcResponse(SingleResponse.of(null)));
        assertFailure(() -> client.getCourse(1001L), ExternalDependencyFailure.CONTRACT_INCOMPATIBLE);
        when(courseService.getCourse(courseRequest())).thenReturn(converter.toCourseRpcResponse(SingleResponse.of(
                new CourseResponse(0L, null, null, 0, null))));
        assertFailure(() -> client.getCourse(1001L), ExternalDependencyFailure.CONTRACT_INCOMPATIBLE);
    }

    @Test
    void validates_local_identifiers_before_any_remote_call() {
        assertThatThrownBy(() -> client.getCourse(null)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> client.getExam(0L)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> client.getScore(1L, -1L)).isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(courseService, examService, scoreService);
    }

    private top.egon.cola.evaluation.facade.rpc.proto.GetCourseRpcRequest courseRequest() {
        return converter.toTarget(new GetCourseRequest(1001L));
    }

    private void assertProviderFailure(String code, ExternalDependencyFailure expected) {
        when(courseService.getCourse(courseRequest())).thenReturn(converter.toCourseRpcResponse(SingleResponse.fail(code, "remote details")));
        assertFailure(() -> client.getCourse(1001L), expected);
    }

    private void assertTransportFailure(EgonRpcErrorCode code, ExternalDependencyFailure expected) {
        doThrow(new EgonRpcException(code, "remote details")).when(courseService).getCourse(courseRequest());
        assertFailure(() -> client.getCourse(1001L), expected);
    }

    private static void assertFailure(Runnable invocation, ExternalDependencyFailure expected) {
        assertThatThrownBy(invocation::run).isInstanceOfSatisfying(ExternalDependencyException.class, failure -> {
            assertThat(failure.dependency()).isEqualTo("evaluation");
            assertThat(failure.failure()).isEqualTo(expected);
            assertThat(failure.getMessage()).doesNotContain("remote details");
        });
    }

    @Test
    void builds_exact_direct_queries_with_bounded_timeout_and_no_retry_or_fallback() {
        var strategies = mock(top.egon.cola.component.rpc.consumer.reference.RpcReferenceStrategyFactory.class);
        var proxies = mock(top.egon.cola.component.rpc.consumer.proxy.RpcConsumerProxyFactory.class);
        when(strategies.create(any())).thenReturn(mock(top.egon.cola.component.rpc.consumer.reference.RpcReferenceStrategy.class));
        when(proxies.create(any(), any(), any())).thenAnswer(invocation -> {
            var contract = invocation.getArgument(0, top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor.class);
            return mock(contract.contractType());
        });
        var config = new NativeEvaluationRpcConfiguration(
                new top.egon.cola.component.rpc.contract.validation.RpcContractValidator(), strategies, proxies,
                new top.egon.cola.component.rpc.context.identity.RpcProcessIdentity("consumer", "test", "localhost", 1L, "consumer-1"),
                new NativeEvaluationRpcProperties("biz", "evaluation-app", "course", "exam", "score", "1.0", 5000),
                new top.egon.cola.component.rpc.config.EgonRpcProperties(), validation);
        config.evaluationCourseRpcService();
        config.evaluationExamRpcService();
        config.evaluationScoreRpcService();
        var definitions = org.mockito.ArgumentCaptor.forClass(top.egon.cola.component.rpc.consumer.reference.RpcReferenceDefinition.class);
        verify(strategies, times(3)).create(definitions.capture());
        for (var definition : definitions.getAllValues()) {
            assertThat(definition.mode()).isEqualTo(top.egon.cola.component.rpc.consumer.reference.RpcReferenceMode.DIRECT);
            assertThat(definition.directQuery().bizCode()).isEqualTo("biz");
            assertThat(definition.directQuery().appCode()).isEqualTo("evaluation-app");
            assertThat(definition.directQuery().env()).isEqualTo("test");
            assertThat(definition.directQuery().version()).isEqualTo("1.0");
            assertThat(definition.directQuery().protocol()).isEqualTo("grpc");
            assertThat(definition.directQuery().group()).isEqualTo(definition.serviceIdentity().group());
            assertThat(definition.typedPolicies()).isNotEmpty();
            for (var policy : definition.typedPolicies().values()) {
                assertThat(policy.timeoutMs()).isEqualTo(3000);
                assertThat(policy.retries()).isZero();
                assertThat(policy.failStrategy()).isEqualTo(top.egon.cola.component.rpc.annotation.FailStrategy.FAIL_CLOSED);
                assertThat(policy.fallbackBean()).isEmpty();
            }
        }
    }
}
