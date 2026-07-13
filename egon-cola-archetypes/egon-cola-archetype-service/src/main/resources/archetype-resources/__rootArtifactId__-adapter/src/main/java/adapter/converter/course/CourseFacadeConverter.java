#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.converter.course;
import ${package}.application.command.course.CreateCourseCommand;
import ${package}.application.command.course.ScheduleCourseCommand;
import ${package}.application.result.course.CourseResult;
import ${package}.application.result.course.CourseScheduleResult;
import ${package}.facade.course.dto.CourseResponse;
import ${package}.facade.course.dto.CourseScheduleResponse;
import ${package}.facade.course.dto.CreateCourseRequest;
import ${package}.facade.course.dto.ScheduleCourseRequest;
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
