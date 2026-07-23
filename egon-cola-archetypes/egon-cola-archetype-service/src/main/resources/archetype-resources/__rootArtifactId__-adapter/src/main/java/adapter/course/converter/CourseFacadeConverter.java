#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.course.converter;
import ${package}.application.course.command.CreateCourseCommand;
import ${package}.application.course.command.ScheduleCourseCommand;
import ${package}.application.course.result.CourseResult;
import ${package}.application.course.result.CourseScheduleResult;
import top.egon.cola.evaluation.facade.course.dto.CourseResponse;
import top.egon.cola.evaluation.facade.course.dto.CourseScheduleResponse;
import top.egon.cola.evaluation.facade.course.dto.CreateCourseRequest;
import top.egon.cola.evaluation.facade.course.dto.ScheduleCourseRequest;
import org.springframework.stereotype.Component;
@Component
public class CourseFacadeConverter {
    public CreateCourseCommand toCommand(CreateCourseRequest request) {
        return new CreateCourseCommand(request.code(), request.name(), request.credit());
    }
    public ScheduleCourseCommand toCommand(ScheduleCourseRequest request) {
        return new ScheduleCourseCommand(request.courseId(), request.classId(), request.startsAt(), request.endsAt());
    }
    public CourseResponse toResponse(CourseResult result) {
        return new CourseResponse(result.id(), result.code(), result.name(), result.credit(), result.status());
    }
    public CourseScheduleResponse toResponse(CourseScheduleResult result) {
        return new CourseScheduleResponse(result.id(), result.courseId(), result.classId(),
                result.startsAt(), result.endsAt(), result.status());
    }
}
