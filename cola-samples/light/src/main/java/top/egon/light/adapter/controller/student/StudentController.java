package top.egon.light.adapter.controller.student;

import top.egon.light.adapter.convertor.StudentAdapterConverter;
import top.egon.light.application.manage.student.StudentManage;
import top.egon.light.common.response.SingleResponse;
import top.egon.light.facade.dto.PageResponse;
import top.egon.light.facade.dto.RegisterStudentRequest;
import top.egon.light.facade.dto.StudentDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("studentController")
@RequestMapping("/students")
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
