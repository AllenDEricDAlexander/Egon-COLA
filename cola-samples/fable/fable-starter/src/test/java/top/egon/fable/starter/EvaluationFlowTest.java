package top.egon.fable.starter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import top.egon.fable.adapter.dto.ExamResultMessage;
import top.egon.fable.adapter.mq.ExamResultMessageConsumer;
import top.egon.fable.common.constants.ErrorCodes;
import top.egon.fable.common.exception.BizException;
import top.egon.fable.common.response.SingleResponse;
import top.egon.fable.domain.entities.course.Course;
import top.egon.fable.domain.enums.CourseStatus;
import top.egon.fable.domain.repos.course.CourseRepository;
import top.egon.fable.facade.api.CourseFacade;
import top.egon.fable.facade.api.ExamResultFacade;
import top.egon.fable.facade.dto.course.CourseDTO;
import top.egon.fable.facade.dto.course.CreateCourseRequest;
import top.egon.fable.facade.dto.examing.ExamResultDTO;
import top.egon.fable.facade.dto.examing.RecordExamResultRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

@SpringBootTest
class EvaluationFlowTest {

    @Autowired
    private CourseFacade courseFacade;

    @Autowired
    private ExamResultFacade examResultFacade;

    @Autowired
    private ExamResultMessageConsumer examResultMessageConsumer;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    void shouldCreateCourseAndRecordPassedExamResult() {
        SingleResponse<CourseDTO> courseResponse = courseFacade.createCourse(new CreateCourseRequest("Mathematics", 3));

        assertThat(courseResponse.isSuccess()).isTrue();
        assertThat(courseResponse.getData().status()).isEqualTo("ENABLED");

        CourseDTO course = courseResponse.getData();
        SingleResponse<ExamResultDTO> examResultResponse = examResultMessageConsumer.consume(
                new ExamResultMessage(course.id(), "student-001", 90));

        assertThat(examResultResponse.isSuccess()).isTrue();
        assertThat(examResultResponse.getData().status()).isEqualTo("PASSED");
    }

    @Test
    void shouldRejectNullAdapterPayloadsAsBizFailures() {
        assertBizFailure(courseFacade.createCourse(null));
        assertBizFailure(examResultFacade.record(null));
        assertBizFailure(examResultMessageConsumer.consume(null));
    }

    @Test
    void shouldRejectNullAndBlankReadIdsAsBizFailures() {
        assertBizFailure(courseFacade.getCourse(null));
        assertBizFailure(courseFacade.getCourse(" "));
        assertBizFailure(examResultFacade.getResult(null));
        assertBizFailure(examResultFacade.getResult(" "));
    }

    @Test
    void shouldRejectInvalidExamPayloadFieldsAsBizFailures() {
        assertBizFailure(examResultFacade.record(new RecordExamResultRequest(null, "student-001", 90)));
        assertBizFailure(examResultFacade.record(new RecordExamResultRequest(" ", "student-001", 90)));
        assertBizFailure(examResultFacade.record(new RecordExamResultRequest("course-001", null, 90)));
        assertBizFailure(examResultFacade.record(new RecordExamResultRequest("course-001", " ", 90)));
        assertBizFailure(examResultFacade.record(new RecordExamResultRequest("course-001", "student-001", 101)));

        assertBizFailure(examResultMessageConsumer.consume(new ExamResultMessage(null, "student-001", 90)));
        assertBizFailure(examResultMessageConsumer.consume(new ExamResultMessage(" ", "student-001", 90)));
        assertBizFailure(examResultMessageConsumer.consume(new ExamResultMessage("course-001", null, 90)));
        assertBizFailure(examResultMessageConsumer.consume(new ExamResultMessage("course-001", " ", 90)));
        assertBizFailure(examResultMessageConsumer.consume(new ExamResultMessage("course-001", "student-001", -1)));
    }

    @Test
    void shouldTranslateOnlyCourseNameUniqueIntegrityViolation() {
        String duplicateName = "Course-" + UUID.randomUUID();
        courseRepository.save(Course.create(UUID.randomUUID().toString(), duplicateName, 3));

        assertThatThrownBy(() -> courseRepository.save(Course.create(UUID.randomUUID().toString(), duplicateName, 3)))
                .isInstanceOf(BizException.class)
                .satisfies(exception -> assertThat(((BizException) exception).getCode())
                        .isEqualTo(ErrorCodes.COURSE_NAME_DUPLICATED));

        Course invalidCourse = new Course();
        invalidCourse.setId(UUID.randomUUID().toString());
        invalidCourse.setName(null);
        invalidCourse.setCredit(3);
        invalidCourse.setStatus(CourseStatus.ACTIVE);

        assertThatThrownBy(() -> courseRepository.save(invalidCourse))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void assertBizFailure(SingleResponse<?> response) {
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo("BIZ_ERROR");
    }
}
