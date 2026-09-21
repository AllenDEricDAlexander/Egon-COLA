package top.egon.cola.archetype.source.web.infrastructure.client.evaluation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.mapstruct.factory.Mappers;
import jakarta.validation.Validation;
import jakarta.validation.ConstraintViolationException;
import top.egon.cola.archetype.source.service.facade.course.CourseFacade;
import top.egon.cola.archetype.source.service.facade.exam.ExamFacade;
import top.egon.cola.archetype.source.service.facade.exam.ScoreFacade;
import top.egon.cola.archetype.source.service.facade.proto.CourseResponse;
import top.egon.cola.archetype.source.service.facade.proto.CourseRpcResponse;
import top.egon.cola.archetype.source.service.facade.proto.ExamResponse;
import top.egon.cola.archetype.source.service.facade.proto.ExamRpcResponse;
import top.egon.cola.archetype.source.service.facade.proto.GetCourseRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.GetExamRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.ScoreResponse;
import top.egon.cola.archetype.source.service.facade.proto.ScoreRpcResponse;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.common.exception.EgonRpcException;
import top.egon.cola.component.rpc.common.enums.EgonRpcErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import top.egon.cola.archetype.source.web.common.exception.ExternalDependencyException;
import top.egon.cola.archetype.source.web.common.enums.ExternalDependencyFailure;
import top.egon.cola.archetype.source.web.domain.teaching.vos.EvaluationCourseBO;
import top.egon.cola.archetype.source.web.domain.teaching.vos.EvaluationExamBO;
import top.egon.cola.archetype.source.web.domain.teaching.vos.EvaluationScoreBO;
import top.egon.cola.archetype.source.web.infrastructure.client.evaluation.impl.NativeEvaluationQueryClientImpl;
import java.time.Instant;

class NativeEvaluationQueryClientTest {
    private static final jakarta.validation.ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();
    private final ValidationUtils validation = new ValidationUtils(VALIDATORS.getValidator());
    private final EvaluationQueryConverter projection = Mappers.getMapper(EvaluationQueryConverter.class);
    private CourseFacade courseFacade;
    private ExamFacade examFacade;
    private ScoreFacade scoreFacade;
    private NativeEvaluationQueryClientImpl client;

    @BeforeEach
    void setUp() {
        courseFacade = mock(CourseFacade.class);
        examFacade = mock(ExamFacade.class);
        scoreFacade = mock(ScoreFacade.class);
        client = new NativeEvaluationQueryClientImpl(courseFacade, examFacade, scoreFacade, projection, validation);
    }

    @AfterAll
    static void closeValidationFactory() { VALIDATORS.close(); }

    @Test
    void maps_course_to_the_existing_consumer_projection() {
        when(courseFacade.getCourse(courseRequest())).thenReturn(courseSuccess(CourseResponse.newBuilder()
                .setId(1001L).setCode("C1").setName("Course One").setCredit(3).setStatus("ACTIVE").build()));
        assertThat(client.getCourse(1001L)).isEqualTo(new EvaluationCourseBO(1001L, "C1", "Course One", 3, "ACTIVE"));
        verify(courseFacade).getCourse(courseRequest());
    }

    @Test
    void maps_exam_without_losing_instant_nanoseconds() {
        var startsAt = Instant.parse("2026-09-08T01:02:03.123456789Z");
        var endsAt = Instant.parse("2026-09-08T02:03:04.987654321Z");
        var request = GetExamRpcRequest.newBuilder().setExamId(2001L).build();
        when(examFacade.getExam(request)).thenReturn(ExamRpcResponse.newBuilder()
                .setSuccess(true).setCode("SUCCESS").setMessage("success")
                .setData(ExamResponse.newBuilder()
                        .setId(2001L).setCourseId(1001L).setTitle("Exam One")
                        .setStartsAt(startsAt.toString()).setEndsAt(endsAt.toString())
                        .setStatus("PUBLISHED").build())
                .build());
        assertThat(client.getExam(2001L)).isEqualTo(new EvaluationExamBO(2001L, 1001L, "Exam One", startsAt, endsAt, "PUBLISHED"));
        verify(examFacade).getExam(request);
    }

    @Test
    void maps_score_with_both_query_identifiers() {
        var request = top.egon.cola.archetype.source.service.facade.proto.GetScoreRpcRequest.newBuilder()
                .setExamId(2001L).setScoreId(3001L).build();
        when(scoreFacade.getScore(request)).thenReturn(ScoreRpcResponse.newBuilder()
                .setSuccess(true).setCode("SUCCESS").setMessage("success")
                .setData(ScoreResponse.newBuilder()
                        .setId(3001L).setExamId(2001L).setCourseId(1001L).setStudentId(7001L)
                        .setPoints(95).setStatus("RECORDED").build())
                .build());
        assertThat(client.getScore(2001L, 3001L)).isEqualTo(new EvaluationScoreBO(3001L, 2001L, 1001L, 7001L, 95, "RECORDED"));
        verify(scoreFacade).getScore(request);
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
        assertTransportFailure(EgonRpcErrorCode.RPC_SERVICE_NOT_FOUND, ExternalDependencyFailure.UNAVAILABLE);
        assertTransportFailure(EgonRpcErrorCode.RPC_INVALID_CONTRACT, ExternalDependencyFailure.CONTRACT_INCOMPATIBLE);
        assertTransportFailure(EgonRpcErrorCode.RPC_METHOD_NOT_FOUND, ExternalDependencyFailure.CONTRACT_INCOMPATIBLE);
    }

    @Test
    void rejects_null_missing_or_invalid_success_data() {
        when(courseFacade.getCourse(courseRequest())).thenReturn(null);
        assertFailure(() -> client.getCourse(1001L), ExternalDependencyFailure.CONTRACT_INCOMPATIBLE);
        when(courseFacade.getCourse(courseRequest())).thenReturn(CourseRpcResponse.newBuilder().setSuccess(true).build());
        assertFailure(() -> client.getCourse(1001L), ExternalDependencyFailure.CONTRACT_INCOMPATIBLE);
        when(courseFacade.getCourse(courseRequest())).thenReturn(courseSuccess(
                CourseResponse.newBuilder().setId(0L).setCredit(3).build()));
        assertFailure(() -> client.getCourse(1001L), ExternalDependencyFailure.CONTRACT_INCOMPATIBLE);
    }

    @Test
    void validates_local_identifiers_before_any_remote_call() {
        assertThatThrownBy(() -> client.getCourse(null)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> client.getExam(0L)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> client.getScore(1L, -1L)).isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(courseFacade, examFacade, scoreFacade);
    }

    @Test
    void reverse_projection_round_trips_every_port_owned_field() {
        var course = new EvaluationCourseBO(1001L, "C1", "Course One", 3, "ACTIVE");
        assertThat(projection.toTarget(projection.toSource(course))).isEqualTo(course);
        var score = new EvaluationScoreBO(3001L, 2001L, 1001L, 7001L, 95, "RECORDED");
        assertThat(projection.toTarget(projection.toSource(score))).isEqualTo(score);
    }

    private GetCourseRpcRequest courseRequest() {
        return GetCourseRpcRequest.newBuilder().setCourseId(1001L).build();
    }

    private static CourseRpcResponse courseSuccess(CourseResponse data) {
        return CourseRpcResponse.newBuilder().setSuccess(true).setCode("SUCCESS").setMessage("success")
                .setData(data).build();
    }

    private void assertProviderFailure(String code, ExternalDependencyFailure expected) {
        when(courseFacade.getCourse(courseRequest())).thenReturn(CourseRpcResponse.newBuilder()
                .setSuccess(false).setCode(code).setMessage("remote details").setTraceId("trace-1").build());
        assertFailure(() -> client.getCourse(1001L), expected);
    }

    private void assertTransportFailure(EgonRpcErrorCode code, ExternalDependencyFailure expected) {
        doThrow(new EgonRpcException(code, "remote details")).when(courseFacade).getCourse(courseRequest());
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
                new top.egon.cola.component.rpc.contract.validation.RpcContractValidator(validation),
                strategies, proxies,
                new top.egon.cola.component.rpc.context.identity.RpcProcessIdentity("consumer", "test", "localhost", 1L, "consumer-1"),
                new NativeEvaluationRpcProperties("biz", "evaluation-app", "course", "exam", "score", "1.0", 5000),
                new top.egon.cola.component.rpc.config.EgonRpcProperties(), validation);
        config.evaluationCourseFacade();
        config.evaluationExamFacade();
        config.evaluationScoreFacade();
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
