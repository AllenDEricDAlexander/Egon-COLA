package top.egon.cola.evaluation.facade;

import top.egon.cola.evaluation.facade.course.dto.ScheduleCourseRequest;
import top.egon.cola.evaluation.facade.exam.dto.GetScoreRequest;
import top.egon.cola.evaluation.facade.exam.dto.RecordScoreRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EvaluationFacadeContractTest {

    @Test
    void shouldExposeProtocolOwnedScheduleRequest() {
        ScheduleCourseRequest request = new ScheduleCourseRequest(
                "course-1",
                "class-1",
                Instant.parse("2026-09-01T01:00:00Z"),
                Instant.parse("2026-09-01T02:00:00Z"));

        assertEquals("course-1", request.courseId());
        assertNotNull(ScheduleCourseRequest.class.getRecordComponents());
    }

    @Test
    void shouldKeepScoreValidationOnFacadeRequest() throws NoSuchMethodException {
        var studentId = RecordScoreRequest.class.getMethod("studentId");
        var points = RecordScoreRequest.class.getMethod("points");

        assertNotNull(studentId.getAnnotation(NotBlank.class));
        assertEquals(0, points.getAnnotation(Min.class).value());
        assertEquals(100, points.getAnnotation(Max.class).value());
    }

    @Test
    void shouldRequireExamIdWhenGettingScore() throws NoSuchMethodException {
        var examId = GetScoreRequest.class.getMethod("examId");
        var scoreId = GetScoreRequest.class.getMethod("scoreId");

        assertNotNull(examId.getAnnotation(NotBlank.class));
        assertNotNull(scoreId.getAnnotation(NotBlank.class));
    }
}
