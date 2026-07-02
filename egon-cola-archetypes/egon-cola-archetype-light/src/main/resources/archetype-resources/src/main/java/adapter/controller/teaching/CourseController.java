package ${package}.adapter.controller.teaching;

import ${package}.adapter.convertor.CourseAdapterConverter;
import ${package}.application.manage.teaching.CourseManage;
import ${package}.common.response.Response;
import ${package}.common.response.SingleResponse;
import ${package}.facade.dto.CourseDTO;
import ${package}.facade.dto.CreateCourseRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/courses")
public class CourseController {
    private final CourseManage courseManage;

    public CourseController(CourseManage courseManage) {
        this.courseManage = courseManage;
    }

    @PostMapping
    public SingleResponse<CourseDTO> create(@Valid @RequestBody CreateCourseRequest request) {
        return SingleResponse.of(CourseAdapterConverter.toDto(courseManage.create(request.name(), request.description())));
    }

    @PostMapping("/{courseId}/students/{studentId}")
    public Response assignCourse(@PathVariable String courseId, @PathVariable String studentId) {
        courseManage.assignCourse(studentId, courseId);
        return Response.success();
    }
}
