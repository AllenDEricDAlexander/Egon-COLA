package ${package}.adapter.controller.student;

import ${package}.adapter.convertor.StudentAdapterConverter;
import ${package}.application.manage.student.StudentManage;
import ${package}.common.response.SingleResponse;
import ${package}.facade.dto.PageResponse;
import ${package}.facade.dto.RegisterStudentRequest;
import ${package}.facade.dto.StudentDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("studentController")
@RequestMapping("/students")
@Validated
@RequiredArgsConstructor
public class StudentController {
    @Qualifier("studentManage")
    private final StudentManage studentManage;

    @Qualifier("studentAdapterConverter")
    private final StudentAdapterConverter studentAdapterConverter;

    @PostMapping
    public SingleResponse<StudentDTO> register(@Valid @RequestBody RegisterStudentRequest request) {
        return SingleResponse.of(studentAdapterConverter.toDto(studentManage.register(request.name(), request.email())));
    }

    @GetMapping("/{studentId}")
    public SingleResponse<StudentDTO> getById(@PathVariable String studentId) {
        return SingleResponse.of(studentAdapterConverter.toDto(studentManage.getById(studentId)));
    }

    @GetMapping
    public SingleResponse<PageResponse<StudentDTO>> getPage(
            @RequestParam(defaultValue = "1") int currentPage,
            @RequestParam(defaultValue = "10") int pageSize) {
        return SingleResponse.of(studentAdapterConverter.toPageResponse(studentManage.getPage(currentPage, pageSize)));
    }
}
