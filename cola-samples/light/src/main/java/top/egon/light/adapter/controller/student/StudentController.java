package top.egon.light.adapter.controller.student;

import top.egon.light.adapter.convertor.StudentAdapterConverter;
import top.egon.light.application.manage.student.StudentManage;
import top.egon.light.common.response.SingleResponse;
import top.egon.light.facade.dto.RegisterStudentRequest;
import top.egon.light.facade.dto.StudentDTO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/students")
public class StudentController {
    private final StudentManage studentManage;

    public StudentController(StudentManage studentManage) {
        this.studentManage = studentManage;
    }

    @PostMapping
    public SingleResponse<StudentDTO> register(@Valid @RequestBody RegisterStudentRequest request) {
        return SingleResponse.of(StudentAdapterConverter.toDto(studentManage.register(request.name(), request.email())));
    }

    @GetMapping("/{studentId}")
    public SingleResponse<StudentDTO> getById(@PathVariable String studentId) {
        return SingleResponse.of(StudentAdapterConverter.toDto(studentManage.getById(studentId)));
    }
}
