package top.egon.light.adapter.controller.teaching;

import top.egon.light.adapter.convertor.CourseAdapterConverter;
import top.egon.light.application.manage.teaching.CourseManage;
import top.egon.light.common.response.Response;
import top.egon.light.common.response.SingleResponse;
import top.egon.light.facade.dto.CourseDTO;
import top.egon.light.facade.dto.CreateCourseRequest;
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
