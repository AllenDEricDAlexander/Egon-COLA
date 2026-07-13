#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.client.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ${evaluationFacadePackage}.facade.course.CourseFacade;
import ${evaluationFacadePackage}.facade.exam.ExamFacade;
import ${evaluationFacadePackage}.facade.exam.ScoreFacade;
import ${evaluationFacadePackage}.facade.dto.SingleResponse;
import ${evaluationFacadePackage}.facade.course.dto.CourseResponse;
import ${evaluationFacadePackage}.facade.course.dto.GetCourseRequest;
import ${evaluationFacadePackage}.facade.exam.dto.ExamResponse;
import ${evaluationFacadePackage}.facade.exam.dto.GetExamRequest;
import ${evaluationFacadePackage}.facade.exam.dto.GetScoreRequest;
import ${evaluationFacadePackage}.facade.exam.dto.ScoreResponse;
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
        when(courseFacade.getCourse(new GetCourseRequest("course-1"))).thenReturn(
                SingleResponse.of(new CourseResponse("course-1", "C-1", "Course One", 3, "ACTIVE")));

        assertThat(client.getCourse("course-1")).isEqualTo(
                new EvaluationCourse("course-1", "C-1", "Course One", 3, "ACTIVE"));
    }

    @Test
    void mapsExamToConsumerProjection() {
        Instant startsAt = Instant.parse("2026-07-11T01:00:00Z");
        Instant endsAt = Instant.parse("2026-07-11T02:00:00Z");
        when(examFacade.getExam(new GetExamRequest("exam-1"))).thenReturn(SingleResponse.of(
                new ExamResponse("exam-1", "course-1", "Exam One", startsAt, endsAt, "PUBLISHED")));

        assertThat(client.getExam("exam-1")).isEqualTo(new EvaluationExam(
                "exam-1", "course-1", "Exam One", startsAt, endsAt, "PUBLISHED"));
    }

    @Test
    void mapsScoreToConsumerProjection() {
        when(scoreFacade.getScore(new GetScoreRequest("score-1"))).thenReturn(SingleResponse.of(
                new ScoreResponse("score-1", "exam-1", "course-1", "student-1", 95, "RECORDED")));

        assertThat(client.getScore("score-1")).isEqualTo(new EvaluationScore(
                "score-1", "exam-1", "course-1", "student-1", 95, "RECORDED"));
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
        when(courseFacade.getCourse(new GetCourseRequest("course-1"))).thenThrow(new RpcException(
                RpcException.TIMEOUT_EXCEPTION, "remote timeout details"));

        assertFailure(() -> client.getCourse("course-1"), ExternalDependencyFailure.TIMEOUT);
    }

    @Test
    void mapsDubboAvailabilityFailure() {
        when(courseFacade.getCourse(new GetCourseRequest("course-1"))).thenThrow(new RpcException(
                RpcException.NETWORK_EXCEPTION, "remote network details"));

        assertFailure(() -> client.getCourse("course-1"), ExternalDependencyFailure.UNAVAILABLE);
    }

    @Test
    void rejectsNullProviderResponse() {
        when(courseFacade.getCourse(new GetCourseRequest("course-1"))).thenReturn(null);

        assertFailure(() -> client.getCourse("course-1"), ExternalDependencyFailure.CONTRACT_INCOMPATIBLE);
    }

    @Test
    void rejectsSuccessfulResponseWithoutData() {
        when(courseFacade.getCourse(new GetCourseRequest("course-1"))).thenReturn(SingleResponse.of(null));

        assertFailure(() -> client.getCourse("course-1"), ExternalDependencyFailure.CONTRACT_INCOMPATIBLE);
    }

    private void assertProviderFailure(String code, ExternalDependencyFailure expected) {
        when(courseFacade.getCourse(new GetCourseRequest("course-1")))
                .thenReturn(SingleResponse.fail(code, "remote details"));

        assertFailure(() -> client.getCourse("course-1"), expected);
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
