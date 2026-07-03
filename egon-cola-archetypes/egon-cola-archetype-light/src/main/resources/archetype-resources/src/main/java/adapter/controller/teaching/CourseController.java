package ${package}.adapter.controller.teaching;

import ${package}.adapter.convertor.CourseAdapterConverter;
import ${package}.application.manage.teaching.CourseManage;
import ${package}.common.response.Response;
import ${package}.common.response.SingleResponse;
import ${package}.facade.dto.CourseDTO;
import ${package}.facade.dto.CreateCourseRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("courseController")
@RequestMapping("/courses")
@Validated
@RequiredArgsConstructor
public class CourseController {
    @Qualifier("courseManage")
    private final CourseManage courseManage;

    @Qualifier("courseAdapterConverter")
    private final CourseAdapterConverter courseAdapterConverter;

    @PostMapping
    public SingleResponse<CourseDTO> create(@Valid @RequestBody CreateCourseRequest request) {
        return SingleResponse.of(courseAdapterConverter.toDto(courseManage.create(request.name(), request.description())));
    }

    @PostMapping("/{courseId}/students/{studentId}")
    public Response assignCourse(@PathVariable String courseId, @PathVariable String studentId) {
        courseManage.assignCourse(studentId, courseId);
        return Response.success();
    }
}
