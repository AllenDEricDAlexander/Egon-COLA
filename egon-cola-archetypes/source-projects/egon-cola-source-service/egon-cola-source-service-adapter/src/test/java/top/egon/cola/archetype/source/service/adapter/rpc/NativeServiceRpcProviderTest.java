package top.egon.cola.archetype.source.service.adapter.rpc;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.service.adapter.course.facade.impl.CourseFacadeImpl;
import top.egon.cola.archetype.source.service.adapter.exam.facade.impl.ExamFacadeImpl;
import top.egon.cola.archetype.source.service.adapter.exam.facade.impl.ScoreFacadeImpl;
import top.egon.cola.archetype.source.service.adapter.handler.GlobalFacadeExceptionHandler;
import top.egon.cola.archetype.source.service.adapter.pojo.convertor.EvaluationFacadeConverter;
import top.egon.cola.archetype.source.service.application.course.manage.CourseManage;
import top.egon.cola.archetype.source.service.application.course.pojo.result.CourseResult;
import top.egon.cola.archetype.source.service.application.course.pojo.result.CourseScheduleResult;
import top.egon.cola.archetype.source.service.application.exam.manage.ExamManage;
import top.egon.cola.archetype.source.service.application.exam.manage.ScoreManage;
import top.egon.cola.archetype.source.service.application.exam.pojo.result.ExamDetailResult;
import top.egon.cola.archetype.source.service.application.exam.pojo.result.ExamPaperResult;
import top.egon.cola.archetype.source.service.application.exam.pojo.result.ScoreResult;
import top.egon.cola.archetype.source.service.common.enums.ApplicationErrorCode;
import top.egon.cola.archetype.source.service.common.exception.ApplicationException;
import top.egon.cola.archetype.source.service.application.pojo.result.PageResult;
import top.egon.cola.archetype.source.service.facade.proto.AttachExamPaperRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.CreateCourseRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.CreateExamRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.GetCourseRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.GetExamRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.GetScoreRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.PageCourseRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.PageScoreRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.PublishExamRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.RecordScoreRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.ScheduleCourseRpcRequest;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Wire contract of the merged native providers: Protobuf in, use case, unchanged Protobuf out. */
class NativeServiceRpcProviderTest {

    private static final ValidationUtils VALIDATION_UTILS =
            new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator());
    private static final ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();

    private final ValidationUtils validation = new ValidationUtils(VALIDATORS.getValidator());
    private final EvaluationFacadeConverter converter = Mappers.getMapper(EvaluationFacadeConverter.class);

    @AfterAll
    static void closeValidationFactory() {
        VALIDATORS.close();
    }

    private CourseFacadeImpl course(CourseManage manage) {
        return new CourseFacadeImpl(manage, converter, validation, new GlobalFacadeExceptionHandler());
    }

    private ExamFacadeImpl exam(ExamManage manage) {
        return new ExamFacadeImpl(manage, converter, validation, new GlobalFacadeExceptionHandler());
    }

    private ScoreFacadeImpl score(ScoreManage manage) {
        return new ScoreFacadeImpl(manage, converter, validation, new GlobalFacadeExceptionHandler());
    }

    @Test
    void exports_all_eleven_native_methods_from_named_provider_beans() {
        int methods = 0;
        for (Class<?> provider : List.of(CourseFacadeImpl.class, ExamFacadeImpl.class, ScoreFacadeImpl.class)) {
            assertThat(provider.getAnnotation(EgonRpcProvider.class)).isNotNull();
            assertThat(provider.getAnnotation(Component.class).value()).isNotBlank();
            for (Class<?> contract : provider.getInterfaces()) {
                methods += new RpcContractValidator(VALIDATION_UTILS).validate(contract).methods().size();
            }
        }
        assertThat(methods).isEqualTo(11);
    }

    @Test
    void createCourse_preserves_the_envelope_and_complete_result() {
        CourseManage manage = mock(CourseManage.class);
        var request = CreateCourseRpcRequest.newBuilder()
                .setCode("C1").setName("Course").setCredit(3).build();
        var expected = new CourseResult(101L, "C1", "Course", 3, "ACTIVE");
        when(manage.create(converter.toSource(request))).thenReturn(expected);

        var response = course(manage).createCourse(request);

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getData().getId()).isEqualTo(101L);
        assertThat(response.getData().getCode()).isEqualTo("C1");
        assertThat(response.getData().getCredit()).isEqualTo(3);
    }

    @Test
    void createCourse_keeps_a_business_rejection_on_the_string_code() {
        CourseManage manage = mock(CourseManage.class);
        when(manage.create(any())).thenThrow(new ApplicationException(
                ApplicationErrorCode.COURSE_NOT_FOUND, "course 101 is absent"));

        var response = course(manage).createCourse(CreateCourseRpcRequest.newBuilder()
                .setCode("C1").setName("Course").setCredit(3).build());

        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo("COURSE_NOT_FOUND");
        assertThat(response.getMessage()).isEqualTo("course 101 is absent");
        assertThat(response.hasData()).isFalse();
    }

    @Test
    void scheduleCourse_preserves_the_envelope_and_complete_result() {
        CourseManage manage = mock(CourseManage.class);
        var startsAt = Instant.parse("2026-09-08T01:02:03.123456789Z");
        var endsAt = Instant.parse("2026-09-08T02:03:04.987654321Z");
        when(manage.schedule(any())).thenReturn(new CourseScheduleResult(
                601L, 101L, 201L, startsAt, endsAt, "SCHEDULED"));

        var response = course(manage).scheduleCourse(ScheduleCourseRpcRequest.newBuilder()
                .setCourseId(101L).setClassId(201L)
                .setStartsAt(startsAt.toString()).setEndsAt(endsAt.toString()).build());

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getData().getId()).isEqualTo(601L);
        assertThat(response.getData().getStartsAt()).isEqualTo("2026-09-08T01:02:03.123456789Z");
        assertThat(response.getData().getEndsAt()).isEqualTo("2026-09-08T02:03:04.987654321Z");
        assertThat(response.getData().getStatus()).isEqualTo("SCHEDULED");
    }

    @Test
    void getCourse_preserves_the_envelope_and_complete_result() {
        CourseManage manage = mock(CourseManage.class);
        when(manage.get(any())).thenReturn(new CourseResult(101L, "C1", "Course", 3, "ACTIVE"));

        var response = course(manage).getCourse(GetCourseRpcRequest.newBuilder().setCourseId(101L).build());

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getData().getName()).isEqualTo("Course");
    }

    @Test
    void pageCourses_preserves_the_envelope_and_complete_result() {
        CourseManage manage = mock(CourseManage.class);
        when(manage.page(any())).thenReturn(PageResult.of(
                List.of(new CourseResult(101L, "C1", "Course", 3, "ACTIVE")), 2, 5, 10, 45L));

        var response = course(manage).pageCourses(PageCourseRpcRequest.newBuilder()
                .setCurrentPage(2).setPageSize(10).build());

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getData().getRecordsList())
                .singleElement().extracting(record -> record.getId()).isEqualTo(101L);
        assertThat(response.getData().getCurrentPage()).isEqualTo(2);
        assertThat(response.getData().getTotalPages()).isEqualTo(5);
        assertThat(response.getData().getTotalCount()).isEqualTo(45L);
    }

    @Test
    void createExam_preserves_the_envelope_and_complete_result() {
        ExamManage manage = mock(ExamManage.class);
        when(manage.create(any())).thenReturn(new ExamDetailResult(301L, 101L, "Exam",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(60), "PUBLISHED"));

        var response = exam(manage).createExam(CreateExamRpcRequest.newBuilder()
                .setCourseId(101L).setTitle("Exam")
                .setStartsAt(Instant.EPOCH.toString())
                .setEndsAt(Instant.EPOCH.plusSeconds(60).toString()).build());

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getData().getId()).isEqualTo(301L);
        assertThat(response.getData().getTitle()).isEqualTo("Exam");
    }

    @Test
    void attachPaper_preserves_the_envelope_and_complete_result() {
        ExamManage manage = mock(ExamManage.class);
        when(manage.attachPaper(any())).thenReturn(new ExamPaperResult(701L, 301L, "Paper", 100, "DRAFT"));

        var response = exam(manage).attachPaper(AttachExamPaperRpcRequest.newBuilder()
                .setExamId(301L).setTitle("Paper").setTotalPoints(100).build());

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getData().getId()).isEqualTo(701L);
        assertThat(response.getData().getTotalPoints()).isEqualTo(100);
    }

    @Test
    void publishExam_preserves_the_envelope_and_complete_result() {
        ExamManage manage = mock(ExamManage.class);
        when(manage.publish(any())).thenReturn(new ExamDetailResult(301L, 101L, "Exam",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(60), "PUBLISHED"));

        var response = exam(manage).publishExam(PublishExamRpcRequest.newBuilder().setExamId(301L).build());

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getData().getStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    void getExam_preserves_the_envelope_and_complete_result() {
        ExamManage manage = mock(ExamManage.class);
        when(manage.get(any())).thenReturn(new ExamDetailResult(301L, 101L, "Exam",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(60), "PUBLISHED"));

        var response = exam(manage).getExam(GetExamRpcRequest.newBuilder().setExamId(301L).build());

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getData().getCourseId()).isEqualTo(101L);
    }

    @Test
    void recordScore_preserves_the_envelope_and_complete_result() {
        ScoreManage manage = mock(ScoreManage.class);
        when(manage.record(any())).thenReturn(new ScoreResult(501L, 301L, 101L, 401L, 95, "RECORDED"));

        var response = score(manage).recordScore(RecordScoreRpcRequest.newBuilder()
                .setExamId(301L).setStudentId(401L).setPoints(95).build());

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getData().getPoints()).isEqualTo(95);
    }

    @Test
    void getScore_preserves_the_envelope_and_complete_result() {
        ScoreManage manage = mock(ScoreManage.class);
        when(manage.get(any())).thenReturn(new ScoreResult(501L, 301L, 101L, 401L, 95, "RECORDED"));

        var response = score(manage).getScore(GetScoreRpcRequest.newBuilder()
                .setExamId(301L).setScoreId(501L).build());

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getData().getStudentId()).isEqualTo(401L);
    }

    @Test
    void pageScores_preserves_the_envelope_and_complete_result() {
        ScoreManage manage = mock(ScoreManage.class);
        when(manage.page(any())).thenReturn(PageResult.of(
                List.of(new ScoreResult(501L, 301L, 101L, 401L, 95, "RECORDED")), 2, 5, 10, 45L));

        var response = score(manage).pageScores(PageScoreRpcRequest.newBuilder()
                .setExamId(301L).setCurrentPage(2).setPageSize(10).build());

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getData().getRecordsList())
                .singleElement().extracting(record -> record.getId()).isEqualTo(501L);
        assertThat(response.getData().getTotalCount()).isEqualTo(45L);
    }

    @Test
    void rejects_every_missing_field_before_the_use_case_runs() {
        CourseManage courseManage = mock(CourseManage.class);
        ExamManage examManage = mock(ExamManage.class);
        ScoreManage scoreManage = mock(ScoreManage.class);
        var facade = course(courseManage);
        var examFacade = exam(examManage);
        var scoreFacade = score(scoreManage);

        assertThatThrownBy(() -> facade.createCourse(CreateCourseRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> facade.scheduleCourse(ScheduleCourseRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> facade.getCourse(GetCourseRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> facade.pageCourses(PageCourseRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> examFacade.createExam(CreateExamRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> examFacade.attachPaper(AttachExamPaperRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> examFacade.publishExam(PublishExamRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> examFacade.getExam(GetExamRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> scoreFacade.recordScore(RecordScoreRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> scoreFacade.getScore(GetScoreRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> scoreFacade.pageScores(PageScoreRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);

        verifyNoInteractions(courseManage, examManage, scoreManage);
    }

    @Test
    void validates_page_size_and_score_range_without_coercing_zero() {
        CourseManage courseManage = mock(CourseManage.class);
        assertThatThrownBy(() -> course(courseManage).pageCourses(PageCourseRpcRequest.newBuilder()
                .setCurrentPage(1).setPageSize(201).build()))
                .isInstanceOf(ConstraintViolationException.class);

        ScoreManage scoreManage = mock(ScoreManage.class);
        var scoreFacade = score(scoreManage);
        assertThatThrownBy(() -> scoreFacade.recordScore(RecordScoreRpcRequest.newBuilder()
                .setExamId(301L).setStudentId(401L).setPoints(101).build()))
                .isInstanceOf(ConstraintViolationException.class);
        when(scoreManage.record(any())).thenReturn(new ScoreResult(501L, 301L, 101L, 401L, 0, "RECORDED"));

        assertThat(scoreFacade.recordScore(RecordScoreRpcRequest.newBuilder()
                .setExamId(301L).setStudentId(401L).setPoints(0).build())
                .getData().getPoints()).isZero();
        verifyNoInteractions(courseManage);
    }
}
