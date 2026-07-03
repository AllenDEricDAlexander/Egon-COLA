#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.starter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import ${package}.adapter.dto.ExamResultMessage;
import ${package}.adapter.mq.ExamResultMessageConsumer;
import ${package}.common.constants.ErrorCodes;
import ${package}.common.exception.BizException;
import ${package}.domain.entities.course.Course;
import ${package}.domain.enums.CourseStatus;
import ${package}.domain.repos.course.CourseRepository;
import ${package}.facade.api.CourseFacade;
import ${package}.facade.api.ExamResultFacade;
import ${package}.facade.dto.SingleResponse;
import ${package}.facade.dto.course.CourseDTO;
import ${package}.facade.dto.course.CreateCourseRequest;
import ${package}.facade.dto.examing.ExamResultDTO;
import ${package}.facade.dto.examing.RecordExamResultRequest;
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
        assertThat(courseResponse.getData().getStatus()).isEqualTo("ENABLED");

        CourseDTO course = courseResponse.getData();
        SingleResponse<ExamResultDTO> examResultResponse = examResultMessageConsumer.consume(
                new ExamResultMessage(course.getId(), "student-001", 90));

        assertThat(examResultResponse.isSuccess()).isTrue();
        assertThat(examResultResponse.getData().getStatus()).isEqualTo("PASSED");
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
