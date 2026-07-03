package ${package}.adapter.convertor;

import ${package}.domain.common.Page;
import ${package}.domain.student.model.Student;
import ${package}.facade.dto.PageResponse;
import ${package}.facade.dto.StudentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("studentAdapterConverter")
@RequiredArgsConstructor
public class StudentAdapterConverter {
    @Qualifier("studentAdapterMapperImpl")
    private final StudentAdapterMapper studentAdapterMapper;

    public StudentDTO toDto(Student student) {
        StudentDTO dto = studentAdapterMapper.convert(student);
        dto.setStatus(student.getStatus().name());
        return dto;
    }

    public PageResponse<StudentDTO> toPageResponse(Page<Student> page) {
        return PageResponse.of(
                page.records().stream().map(this::toDto).toList(),
                page.currentPage(),
                page.totalPages(),
                page.pageSize(),
                page.totalCount());
    }
}
