package top.egon.cola.evaluation.facade;

import org.junit.jupiter.api.Test;
import top.egon.cola.evaluation.facade.course.dto.CourseResponse;
import top.egon.cola.evaluation.facade.course.dto.CourseScheduleResponse;
import top.egon.cola.evaluation.facade.course.dto.GetCourseRequest;
import top.egon.cola.evaluation.facade.course.dto.ScheduleCourseRequest;
import top.egon.cola.evaluation.facade.exam.dto.ExamPaperResponse;
import top.egon.cola.evaluation.facade.exam.dto.ExamResponse;
import top.egon.cola.evaluation.facade.exam.dto.GetScoreRequest;
import top.egon.cola.evaluation.facade.exam.dto.RecordScoreRequest;
import top.egon.cola.evaluation.facade.exam.dto.ScoreResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvaluationFacadeContractTest {

    @Test
    void shouldExposeProtocolOwnedScheduleRequest() {
        ScheduleCourseRequest request = new ScheduleCourseRequest(
                101L,
                201L,
                Instant.parse("2026-09-01T01:00:00Z"),
                Instant.parse("2026-09-01T02:00:00Z"));

        assertEquals(101L, request.courseId());
        assertNotNull(ScheduleCourseRequest.class.getRecordComponents());
    }

    @Test
    void shouldKeepScoreValidationOnFacadeRequest() throws NoSuchMethodException {
        var studentId = RecordScoreRequest.class.getMethod("studentId");
        var points = RecordScoreRequest.class.getMethod("points");

        assertEquals(Long.class, studentId.getReturnType());
        assertTrue(studentId.isAnnotationPresent(NotNull.class));
        assertTrue(studentId.isAnnotationPresent(Positive.class));
        assertEquals(0, points.getAnnotation(Min.class).value());
        assertEquals(100, points.getAnnotation(Max.class).value());
    }

    @Test
    void shouldRequireExamIdWhenGettingScore() throws NoSuchMethodException {
        var examId = GetScoreRequest.class.getMethod("examId");
        var scoreId = GetScoreRequest.class.getMethod("scoreId");

        assertEquals(Long.class, examId.getReturnType());
        assertEquals(Long.class, scoreId.getReturnType());
        assertTrue(examId.isAnnotationPresent(NotNull.class));
        assertTrue(examId.isAnnotationPresent(Positive.class));
        assertTrue(scoreId.isAnnotationPresent(NotNull.class));
        assertTrue(scoreId.isAnnotationPresent(Positive.class));
    }

    @Test
    void exposesPositiveLongIdentityAcrossCourseExamAndScoreRecords() throws NoSuchMethodException {
        assertPositiveLongRecord(GetCourseRequest.class, "courseId");
        assertPositiveLongRecord(ScheduleCourseRequest.class, "courseId");
        assertPositiveLongRecord(ScheduleCourseRequest.class, "classId");
        assertPositiveLongRecord(ExamResponse.class, "id");
        assertPositiveLongRecord(ExamResponse.class, "courseId");
        assertPositiveLongRecord(ExamPaperResponse.class, "id");
        assertPositiveLongRecord(ExamPaperResponse.class, "examId");
        assertPositiveLongRecord(ScoreResponse.class, "id");
        assertPositiveLongRecord(ScoreResponse.class, "examId");
        assertPositiveLongRecord(ScoreResponse.class, "courseId");
        assertPositiveLongRecord(ScoreResponse.class, "studentId");
        assertPositiveLongRecord(CourseResponse.class, "id");
        assertPositiveLongRecord(CourseScheduleResponse.class, "id");
        assertPositiveLongRecord(CourseScheduleResponse.class, "courseId");
        assertPositiveLongRecord(CourseScheduleResponse.class, "classId");
    }

    @Test
    void preservesLargeLongIdentityWithoutNarrowing() throws Exception {
        long snowflake = 9_007_199_254_740_993L;
        var constructor = java.util.Arrays.stream(ScoreResponse.class.getDeclaredConstructors())
                .filter(candidate -> candidate.getParameterTypes().length == 6
                        && candidate.getParameterTypes()[0].equals(Long.class))
                .findFirst()
                .orElseThrow();
        Object response = constructor.newInstance(
                snowflake, snowflake, 100L, 200L, 99, "RECORDED");
        assertEquals(snowflake, ScoreResponse.class.getMethod("id").invoke(response));
        assertEquals(snowflake, ScoreResponse.class.getMethod("examId").invoke(response));
    }

    private static void assertPositiveLongRecord(Class<?> type, String componentName)
            throws NoSuchMethodException {
        var component = java.util.Arrays.stream(type.getRecordComponents())
                .filter(candidate -> candidate.getName().equals(componentName))
                .findFirst()
                .orElseThrow();
        assertEquals(Long.class, component.getType());
        var accessor = component.getAccessor();
        assertTrue(accessor.isAnnotationPresent(NotNull.class));
        assertTrue(accessor.isAnnotationPresent(Positive.class));
    }
}
