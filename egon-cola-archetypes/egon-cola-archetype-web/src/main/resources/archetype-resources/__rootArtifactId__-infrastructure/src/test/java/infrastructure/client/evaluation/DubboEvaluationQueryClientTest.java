#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.client.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import top.egon.cola.evaluation.facade.course.CourseFacade;
import top.egon.cola.evaluation.facade.exam.ExamFacade;
import top.egon.cola.evaluation.facade.exam.ScoreFacade;
import top.egon.cola.evaluation.facade.dto.SingleResponse;
import top.egon.cola.evaluation.facade.course.dto.CourseResponse;
import top.egon.cola.evaluation.facade.course.dto.GetCourseRequest;
import top.egon.cola.evaluation.facade.exam.dto.ExamResponse;
import top.egon.cola.evaluation.facade.exam.dto.GetExamRequest;
import top.egon.cola.evaluation.facade.exam.dto.GetScoreRequest;
import top.egon.cola.evaluation.facade.exam.dto.ScoreResponse;
import ${package}.domain.client.ExternalDependencyException;
import ${package}.domain.client.ExternalDependencyFailure;
import ${package}.domain.client.evaluation.EvaluationCourse;
import ${package}.domain.client.evaluation.EvaluationExam;
import ${package}.domain.client.evaluation.EvaluationScore;
import java.time.Instant;
import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DubboEvaluationQueryClientTest {

    private CourseFacade courseFacade;
    private ExamFacade examFacade;
    private ScoreFacade scoreFacade;
    private DubboEvaluationQueryClient client;

    @BeforeEach
    void setUp() {
        courseFacade = mock(CourseFacade.class);
        examFacade = mock(ExamFacade.class);
        scoreFacade = mock(ScoreFacade.class);
        client = new DubboEvaluationQueryClient(courseFacade, examFacade, scoreFacade);
    }

    @Test
    void mapsCourseToConsumerProjection() {
        when(courseFacade.getCourse(new GetCourseRequest(1001L))).thenReturn(
                SingleResponse.of(new CourseResponse(1001L, "C-1", "Course One", 3, "ACTIVE")));

        assertThat(client.getCourse(1001L)).isEqualTo(
                new EvaluationCourse(1001L, "C-1", "Course One", 3, "ACTIVE"));
    }

    @Test
    void mapsExamToConsumerProjection() {
        Instant startsAt = Instant.parse("2026-07-11T01:00:00Z");
        Instant endsAt = Instant.parse("2026-07-11T02:00:00Z");
        when(examFacade.getExam(new GetExamRequest(2001L))).thenReturn(SingleResponse.of(
                new ExamResponse(2001L, 1001L, "Exam One", startsAt, endsAt, "PUBLISHED")));

        assertThat(client.getExam(2001L)).isEqualTo(new EvaluationExam(
                2001L, 1001L, "Exam One", startsAt, endsAt, "PUBLISHED"));
    }

    @Test
    void mapsScoreToConsumerProjection() {
        when(scoreFacade.getScore(new GetScoreRequest(2001L, 3001L))).thenReturn(SingleResponse.of(
                new ScoreResponse(3001L, 2001L, 1001L, 7001L, 95, "RECORDED")));

        assertThat(client.getScore(2001L, 3001L)).isEqualTo(new EvaluationScore(
                3001L, 2001L, 1001L, 7001L, 95, "RECORDED"));
    }

    @Test
    void mapsProviderFailureCodes() {
        assertProviderFailure("COURSE_NOT_FOUND", ExternalDependencyFailure.NOT_FOUND);
        assertProviderFailure("VALIDATION_FAILED", ExternalDependencyFailure.VALIDATION_FAILED);
        assertProviderFailure("COURSE_CONFLICT", ExternalDependencyFailure.BUSINESS_REJECTED);
        assertProviderFailure("INTERNAL_ERROR", ExternalDependencyFailure.SERVICE_FAILURE);
    }

    @Test
    void mapsDubboTimeout() {
        when(courseFacade.getCourse(new GetCourseRequest(1001L))).thenThrow(new RpcException(
                RpcException.TIMEOUT_EXCEPTION, "remote timeout details"));

        assertFailure(() -> client.getCourse(1001L), ExternalDependencyFailure.TIMEOUT);
    }

    @Test
    void mapsDubboAvailabilityFailure() {
        when(courseFacade.getCourse(new GetCourseRequest(1001L))).thenThrow(new RpcException(
                RpcException.NETWORK_EXCEPTION, "remote network details"));

        assertFailure(() -> client.getCourse(1001L), ExternalDependencyFailure.UNAVAILABLE);
    }

    @Test
    void rejectsNullProviderResponse() {
        when(courseFacade.getCourse(new GetCourseRequest(1001L))).thenReturn(null);

        assertFailure(() -> client.getCourse(1001L), ExternalDependencyFailure.CONTRACT_INCOMPATIBLE);
    }

    @Test
    void rejectsSuccessfulResponseWithoutData() {
        when(courseFacade.getCourse(new GetCourseRequest(1001L))).thenReturn(SingleResponse.of(null));

        assertFailure(() -> client.getCourse(1001L), ExternalDependencyFailure.CONTRACT_INCOMPATIBLE);
    }

    private void assertProviderFailure(String code, ExternalDependencyFailure expected) {
        when(courseFacade.getCourse(new GetCourseRequest(1001L)))
                .thenReturn(SingleResponse.fail(code, "remote details"));

        assertFailure(() -> client.getCourse(1001L), expected);
    }

    private static void assertFailure(Runnable invocation, ExternalDependencyFailure expected) {
        assertThatThrownBy(invocation::run)
                .isInstanceOfSatisfying(ExternalDependencyException.class, failure -> {
                    assertThat(failure.dependency()).isEqualTo("evaluation");
                    assertThat(failure.failure()).isEqualTo(expected);
                    assertThat(failure.getMessage()).doesNotContain("remote details");
                });
    }
}
