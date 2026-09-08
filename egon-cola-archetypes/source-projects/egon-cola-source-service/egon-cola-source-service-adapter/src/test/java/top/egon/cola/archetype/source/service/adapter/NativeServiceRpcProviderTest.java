package top.egon.cola.archetype.source.service.adapter;

import jakarta.validation.Validation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import top.egon.cola.evaluation.facade.rpc.*;
import top.egon.cola.evaluation.facade.course.CourseFacade;
import top.egon.cola.evaluation.facade.exam.ExamFacade;
import top.egon.cola.evaluation.facade.exam.ScoreFacade;
import top.egon.cola.evaluation.facade.course.dto.*;
import top.egon.cola.evaluation.facade.exam.dto.*;
import top.egon.cola.evaluation.facade.dto.SingleResponse;
import top.egon.cola.evaluation.facade.dto.PageResponse;
import top.egon.cola.archetype.source.service.adapter.course.rpc.CourseRpcProvider;
import top.egon.cola.archetype.source.service.adapter.exam.rpc.ExamRpcProvider;
import top.egon.cola.archetype.source.service.adapter.exam.rpc.ScoreRpcProvider;
import java.time.Instant;

class NativeServiceRpcProviderTest {
    private static final jakarta.validation.ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();
    private final ValidationUtils validation = new ValidationUtils(VALIDATORS.getValidator());
    private final EvaluationRpcConverter converter = Mappers.getMapper(EvaluationRpcConverter.class);

    @AfterAll
    static void closeValidationFactory() {
        VALIDATORS.close();
    }
    @Test
    void exports_all_eleven_native_methods_from_named_provider_beans() {
        int methods = 0;
        for (Class<?> provider : List.of(CourseRpcProvider.class, ExamRpcProvider.class, ScoreRpcProvider.class)) {
            assertThat(provider.getAnnotation(EgonRpcProvider.class)).isNotNull();
            assertThat(provider.getAnnotation(Component.class).value()).isNotBlank();
            for (Class<?> contract : provider.getInterfaces()) {
                methods += new RpcContractValidator().validate(contract).methods().size();
            }
        }
        assertThat(methods).isEqualTo(11);
    }

    @Test
    void createCourse_preserves_the_request_envelope_and_complete_result() {
        var facade = mock(CourseFacade.class);
        var provider = new CourseRpcProvider(facade, converter, validation);
        var input = new CreateCourseRequest("C1", "Course", 3);
        var expected = new CourseResponse(101L, "C1", "Course", 3, "ACTIVE");
        var request = converter.toTarget(input);
        when(facade.create(input)).thenReturn(SingleResponse.of(expected));
        var response = provider.createCourse(request);
        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.fromCourseRpcResponse(response).getData()).isEqualTo(expected);
        verify(facade).create(input);

        when(facade.create(input)).thenReturn(SingleResponse.fail("ORIGINAL_CODE", "original reason"));
        var failure = provider.createCourse(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("ORIGINAL_CODE");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        assertThat(failure.hasData()).isFalse();
        when(facade.create(input)).thenReturn(SingleResponse.of(null));
        var emptyData = provider.createCourse(request);
        assertThat(emptyData.getSuccess()).isTrue();
        assertThat(emptyData.hasData()).isFalse();
        when(facade.create(input)).thenReturn(null);
        assertThatThrownBy(() -> provider.createCourse(request)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void createCourse_rejects_missing_or_invalid_fields_before_the_facade() {
        var facade = mock(CourseFacade.class);
        var provider = new CourseRpcProvider(facade, converter, validation);
        assertThatThrownBy(() -> provider.createCourse(top.egon.cola.evaluation.facade.rpc.proto.CreateCourseRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void scheduleCourse_preserves_the_request_envelope_and_complete_result() {
        var facade = mock(CourseFacade.class);
        var provider = new CourseRpcProvider(facade, converter, validation);
        var input = new ScheduleCourseRequest(101L, 201L, Instant.parse("2026-09-08T01:02:03.123456789Z"), Instant.parse("2026-09-08T02:03:04.987654321Z"));
        var expected = new CourseScheduleResponse(601L, 101L, 201L, Instant.parse("2026-09-08T01:02:03.123456789Z"), Instant.parse("2026-09-08T02:03:04.987654321Z"), "SCHEDULED");
        var request = converter.toTarget(input);
        when(facade.scheduleCourse(input)).thenReturn(SingleResponse.of(expected));
        var response = provider.scheduleCourse(request);
        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.fromCourseScheduleRpcResponse(response).getData()).isEqualTo(expected);
        verify(facade).scheduleCourse(input);

        when(facade.scheduleCourse(input)).thenReturn(SingleResponse.fail("ORIGINAL_CODE", "original reason"));
        var failure = provider.scheduleCourse(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("ORIGINAL_CODE");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        assertThat(failure.hasData()).isFalse();
        when(facade.scheduleCourse(input)).thenReturn(SingleResponse.of(null));
        var emptyData = provider.scheduleCourse(request);
        assertThat(emptyData.getSuccess()).isTrue();
        assertThat(emptyData.hasData()).isFalse();
        when(facade.scheduleCourse(input)).thenReturn(null);
        assertThatThrownBy(() -> provider.scheduleCourse(request)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void scheduleCourse_rejects_missing_or_invalid_fields_before_the_facade() {
        var facade = mock(CourseFacade.class);
        var provider = new CourseRpcProvider(facade, converter, validation);
        assertThatThrownBy(() -> provider.scheduleCourse(top.egon.cola.evaluation.facade.rpc.proto.ScheduleCourseRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void getCourse_preserves_the_request_envelope_and_complete_result() {
        var facade = mock(CourseFacade.class);
        var provider = new CourseRpcProvider(facade, converter, validation);
        var input = new GetCourseRequest(101L);
        var expected = new CourseResponse(101L, "C1", "Course", 3, "ACTIVE");
        var request = converter.toTarget(input);
        when(facade.getCourse(input)).thenReturn(SingleResponse.of(expected));
        var response = provider.getCourse(request);
        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.fromCourseRpcResponse(response).getData()).isEqualTo(expected);
        verify(facade).getCourse(input);

        when(facade.getCourse(input)).thenReturn(SingleResponse.fail("ORIGINAL_CODE", "original reason"));
        var failure = provider.getCourse(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("ORIGINAL_CODE");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        assertThat(failure.hasData()).isFalse();
        when(facade.getCourse(input)).thenReturn(SingleResponse.of(null));
        var emptyData = provider.getCourse(request);
        assertThat(emptyData.getSuccess()).isTrue();
        assertThat(emptyData.hasData()).isFalse();
        when(facade.getCourse(input)).thenReturn(null);
        assertThatThrownBy(() -> provider.getCourse(request)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void getCourse_rejects_missing_or_invalid_fields_before_the_facade() {
        var facade = mock(CourseFacade.class);
        var provider = new CourseRpcProvider(facade, converter, validation);
        assertThatThrownBy(() -> provider.getCourse(top.egon.cola.evaluation.facade.rpc.proto.GetCourseRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void pageCourses_preserves_the_request_envelope_and_complete_result() {
        var facade = mock(CourseFacade.class);
        var provider = new CourseRpcProvider(facade, converter, validation);
        var input = new PageCourseRequest(2, 10);
        var expected = PageResponse.of(List.of(new CourseResponse(101L, "C1", "Course", 3, "ACTIVE")), 2, 5, 10, 45);
        var request = converter.toTarget(input);
        when(facade.pageCourses(input)).thenReturn(SingleResponse.of(expected));
        var response = provider.pageCourses(request);
        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.fromPageCourseRpcResponse(response).getData()).isEqualTo(expected);
        verify(facade).pageCourses(input);

        when(facade.pageCourses(input)).thenReturn(SingleResponse.fail("ORIGINAL_CODE", "original reason"));
        var failure = provider.pageCourses(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("ORIGINAL_CODE");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        assertThat(failure.hasData()).isFalse();
        when(facade.pageCourses(input)).thenReturn(SingleResponse.of(null));
        var emptyData = provider.pageCourses(request);
        assertThat(emptyData.getSuccess()).isTrue();
        assertThat(emptyData.hasData()).isFalse();
        when(facade.pageCourses(input)).thenReturn(null);
        assertThatThrownBy(() -> provider.pageCourses(request)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void pageCourses_rejects_missing_or_invalid_fields_before_the_facade() {
        var facade = mock(CourseFacade.class);
        var provider = new CourseRpcProvider(facade, converter, validation);
        assertThatThrownBy(() -> provider.pageCourses(top.egon.cola.evaluation.facade.rpc.proto.PageCourseRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void createExam_preserves_the_request_envelope_and_complete_result() {
        var facade = mock(ExamFacade.class);
        var provider = new ExamRpcProvider(facade, converter, validation);
        var input = new CreateExamRequest(101L, "Exam", Instant.parse("2026-09-08T01:02:03.123456789Z"), Instant.parse("2026-09-08T02:03:04.987654321Z"));
        var expected = new ExamResponse(301L, 101L, "Exam", Instant.parse("2026-09-08T01:02:03.123456789Z"), Instant.parse("2026-09-08T02:03:04.987654321Z"), "PUBLISHED");
        var request = converter.toTarget(input);
        when(facade.createExam(input)).thenReturn(SingleResponse.of(expected));
        var response = provider.createExam(request);
        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.fromExamRpcResponse(response).getData()).isEqualTo(expected);
        verify(facade).createExam(input);

        when(facade.createExam(input)).thenReturn(SingleResponse.fail("ORIGINAL_CODE", "original reason"));
        var failure = provider.createExam(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("ORIGINAL_CODE");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        assertThat(failure.hasData()).isFalse();
        when(facade.createExam(input)).thenReturn(SingleResponse.of(null));
        var emptyData = provider.createExam(request);
        assertThat(emptyData.getSuccess()).isTrue();
        assertThat(emptyData.hasData()).isFalse();
        when(facade.createExam(input)).thenReturn(null);
        assertThatThrownBy(() -> provider.createExam(request)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void createExam_rejects_missing_or_invalid_fields_before_the_facade() {
        var facade = mock(ExamFacade.class);
        var provider = new ExamRpcProvider(facade, converter, validation);
        assertThatThrownBy(() -> provider.createExam(top.egon.cola.evaluation.facade.rpc.proto.CreateExamRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void attachPaper_preserves_the_request_envelope_and_complete_result() {
        var facade = mock(ExamFacade.class);
        var provider = new ExamRpcProvider(facade, converter, validation);
        var input = new AttachExamPaperRequest(301L, "Paper", 100);
        var expected = new ExamPaperResponse(701L, 301L, "Paper", 100, "DRAFT");
        var request = converter.toTarget(input);
        when(facade.attachPaper(input)).thenReturn(SingleResponse.of(expected));
        var response = provider.attachPaper(request);
        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.fromExamPaperRpcResponse(response).getData()).isEqualTo(expected);
        verify(facade).attachPaper(input);

        when(facade.attachPaper(input)).thenReturn(SingleResponse.fail("ORIGINAL_CODE", "original reason"));
        var failure = provider.attachPaper(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("ORIGINAL_CODE");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        assertThat(failure.hasData()).isFalse();
        when(facade.attachPaper(input)).thenReturn(SingleResponse.of(null));
        var emptyData = provider.attachPaper(request);
        assertThat(emptyData.getSuccess()).isTrue();
        assertThat(emptyData.hasData()).isFalse();
        when(facade.attachPaper(input)).thenReturn(null);
        assertThatThrownBy(() -> provider.attachPaper(request)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void attachPaper_rejects_missing_or_invalid_fields_before_the_facade() {
        var facade = mock(ExamFacade.class);
        var provider = new ExamRpcProvider(facade, converter, validation);
        assertThatThrownBy(() -> provider.attachPaper(top.egon.cola.evaluation.facade.rpc.proto.AttachExamPaperRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void publishExam_preserves_the_request_envelope_and_complete_result() {
        var facade = mock(ExamFacade.class);
        var provider = new ExamRpcProvider(facade, converter, validation);
        var input = new PublishExamRequest(301L);
        var expected = new ExamResponse(301L, 101L, "Exam", Instant.parse("2026-09-08T01:02:03.123456789Z"), Instant.parse("2026-09-08T02:03:04.987654321Z"), "PUBLISHED");
        var request = converter.toTarget(input);
        when(facade.publishExam(input)).thenReturn(SingleResponse.of(expected));
        var response = provider.publishExam(request);
        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.fromExamRpcResponse(response).getData()).isEqualTo(expected);
        verify(facade).publishExam(input);

        when(facade.publishExam(input)).thenReturn(SingleResponse.fail("ORIGINAL_CODE", "original reason"));
        var failure = provider.publishExam(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("ORIGINAL_CODE");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        assertThat(failure.hasData()).isFalse();
        when(facade.publishExam(input)).thenReturn(SingleResponse.of(null));
        var emptyData = provider.publishExam(request);
        assertThat(emptyData.getSuccess()).isTrue();
        assertThat(emptyData.hasData()).isFalse();
        when(facade.publishExam(input)).thenReturn(null);
        assertThatThrownBy(() -> provider.publishExam(request)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void publishExam_rejects_missing_or_invalid_fields_before_the_facade() {
        var facade = mock(ExamFacade.class);
        var provider = new ExamRpcProvider(facade, converter, validation);
        assertThatThrownBy(() -> provider.publishExam(top.egon.cola.evaluation.facade.rpc.proto.PublishExamRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void getExam_preserves_the_request_envelope_and_complete_result() {
        var facade = mock(ExamFacade.class);
        var provider = new ExamRpcProvider(facade, converter, validation);
        var input = new GetExamRequest(301L);
        var expected = new ExamResponse(301L, 101L, "Exam", Instant.parse("2026-09-08T01:02:03.123456789Z"), Instant.parse("2026-09-08T02:03:04.987654321Z"), "PUBLISHED");
        var request = converter.toTarget(input);
        when(facade.getExam(input)).thenReturn(SingleResponse.of(expected));
        var response = provider.getExam(request);
        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.fromExamRpcResponse(response).getData()).isEqualTo(expected);
        verify(facade).getExam(input);

        when(facade.getExam(input)).thenReturn(SingleResponse.fail("ORIGINAL_CODE", "original reason"));
        var failure = provider.getExam(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("ORIGINAL_CODE");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        assertThat(failure.hasData()).isFalse();
        when(facade.getExam(input)).thenReturn(SingleResponse.of(null));
        var emptyData = provider.getExam(request);
        assertThat(emptyData.getSuccess()).isTrue();
        assertThat(emptyData.hasData()).isFalse();
        when(facade.getExam(input)).thenReturn(null);
        assertThatThrownBy(() -> provider.getExam(request)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void getExam_rejects_missing_or_invalid_fields_before_the_facade() {
        var facade = mock(ExamFacade.class);
        var provider = new ExamRpcProvider(facade, converter, validation);
        assertThatThrownBy(() -> provider.getExam(top.egon.cola.evaluation.facade.rpc.proto.GetExamRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void recordScore_preserves_the_request_envelope_and_complete_result() {
        var facade = mock(ScoreFacade.class);
        var provider = new ScoreRpcProvider(facade, converter, validation);
        var input = new RecordScoreRequest(301L, 401L, 95);
        var expected = new ScoreResponse(501L, 301L, 101L, 401L, 95, "RECORDED");
        var request = converter.toTarget(input);
        when(facade.recordScore(input)).thenReturn(SingleResponse.of(expected));
        var response = provider.recordScore(request);
        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.fromScoreRpcResponse(response).getData()).isEqualTo(expected);
        verify(facade).recordScore(input);

        when(facade.recordScore(input)).thenReturn(SingleResponse.fail("ORIGINAL_CODE", "original reason"));
        var failure = provider.recordScore(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("ORIGINAL_CODE");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        assertThat(failure.hasData()).isFalse();
        when(facade.recordScore(input)).thenReturn(SingleResponse.of(null));
        var emptyData = provider.recordScore(request);
        assertThat(emptyData.getSuccess()).isTrue();
        assertThat(emptyData.hasData()).isFalse();
        when(facade.recordScore(input)).thenReturn(null);
        assertThatThrownBy(() -> provider.recordScore(request)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void recordScore_rejects_missing_or_invalid_fields_before_the_facade() {
        var facade = mock(ScoreFacade.class);
        var provider = new ScoreRpcProvider(facade, converter, validation);
        assertThatThrownBy(() -> provider.recordScore(top.egon.cola.evaluation.facade.rpc.proto.RecordScoreRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void getScore_preserves_the_request_envelope_and_complete_result() {
        var facade = mock(ScoreFacade.class);
        var provider = new ScoreRpcProvider(facade, converter, validation);
        var input = new GetScoreRequest(301L, 501L);
        var expected = new ScoreResponse(501L, 301L, 101L, 401L, 95, "RECORDED");
        var request = converter.toTarget(input);
        when(facade.getScore(input)).thenReturn(SingleResponse.of(expected));
        var response = provider.getScore(request);
        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.fromScoreRpcResponse(response).getData()).isEqualTo(expected);
        verify(facade).getScore(input);

        when(facade.getScore(input)).thenReturn(SingleResponse.fail("ORIGINAL_CODE", "original reason"));
        var failure = provider.getScore(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("ORIGINAL_CODE");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        assertThat(failure.hasData()).isFalse();
        when(facade.getScore(input)).thenReturn(SingleResponse.of(null));
        var emptyData = provider.getScore(request);
        assertThat(emptyData.getSuccess()).isTrue();
        assertThat(emptyData.hasData()).isFalse();
        when(facade.getScore(input)).thenReturn(null);
        assertThatThrownBy(() -> provider.getScore(request)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void getScore_rejects_missing_or_invalid_fields_before_the_facade() {
        var facade = mock(ScoreFacade.class);
        var provider = new ScoreRpcProvider(facade, converter, validation);
        assertThatThrownBy(() -> provider.getScore(top.egon.cola.evaluation.facade.rpc.proto.GetScoreRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void pageScores_preserves_the_request_envelope_and_complete_result() {
        var facade = mock(ScoreFacade.class);
        var provider = new ScoreRpcProvider(facade, converter, validation);
        var input = new PageScoreRequest(301L, 2, 10);
        var expected = PageResponse.of(List.of(new ScoreResponse(501L, 301L, 101L, 401L, 95, "RECORDED")), 2, 5, 10, 45);
        var request = converter.toTarget(input);
        when(facade.pageScores(input)).thenReturn(SingleResponse.of(expected));
        var response = provider.pageScores(request);
        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.fromPageScoreRpcResponse(response).getData()).isEqualTo(expected);
        verify(facade).pageScores(input);

        when(facade.pageScores(input)).thenReturn(SingleResponse.fail("ORIGINAL_CODE", "original reason"));
        var failure = provider.pageScores(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("ORIGINAL_CODE");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        assertThat(failure.hasData()).isFalse();
        when(facade.pageScores(input)).thenReturn(SingleResponse.of(null));
        var emptyData = provider.pageScores(request);
        assertThat(emptyData.getSuccess()).isTrue();
        assertThat(emptyData.hasData()).isFalse();
        when(facade.pageScores(input)).thenReturn(null);
        assertThatThrownBy(() -> provider.pageScores(request)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void pageScores_rejects_missing_or_invalid_fields_before_the_facade() {
        var facade = mock(ScoreFacade.class);
        var provider = new ScoreRpcProvider(facade, converter, validation);
        assertThatThrownBy(() -> provider.pageScores(top.egon.cola.evaluation.facade.rpc.proto.PageScoreRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void validates_page_size_and_score_range_without_coercing_zero() {
        var course = mock(CourseFacade.class);
        var courseProvider = new CourseRpcProvider(course, converter, validation);
        assertThatThrownBy(() -> courseProvider.pageCourses(converter.toTarget(new PageCourseRequest(1, 201))))
                .isInstanceOf(ConstraintViolationException.class);
        var score = mock(ScoreFacade.class);
        var scoreProvider = new ScoreRpcProvider(score, converter, validation);
        assertThatThrownBy(() -> scoreProvider.recordScore(converter.toTarget(new RecordScoreRequest(301L, 401L, 101))))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(course, score);
        when(score.recordScore(new RecordScoreRequest(301L, 401L, 0))).thenReturn(SingleResponse.of(
                new ScoreResponse(501L, 301L, 101L, 401L, 0, "RECORDED")));
        assertThat(scoreProvider.recordScore(converter.toTarget(new RecordScoreRequest(301L, 401L, 0)))
                .getData().getPoints()).isZero();
    }
}
