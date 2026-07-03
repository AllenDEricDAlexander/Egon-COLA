#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.convertor;

import ${package}.domain.common.Page;
import ${package}.domain.entities.course.Course;
import ${package}.domain.enums.CourseStatus;
import ${package}.facade.dto.PageResponse;
import ${package}.facade.dto.course.CourseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("courseAdapterConvertor")
@RequiredArgsConstructor
public class CourseAdapterConvertor {

    @Qualifier("courseAdapterMapperImpl")
    private final CourseAdapterMapper courseAdapterMapper;

    public CourseDTO toDTO(Course course) {
        CourseDTO courseDTO = courseAdapterMapper.convert(course);
        courseDTO.setStatus(toFacadeStatus(course.getStatus()));
        return courseDTO;
    }

    public PageResponse<CourseDTO> toPageResponse(Page<Course> page) {
        return PageResponse.of(
                page.records().stream().map(this::toDTO).toList(),
                page.currentPage(),
                page.totalPages(),
                page.pageSize(),
                page.totalCount());
    }

    private String toFacadeStatus(CourseStatus status) {
        if (CourseStatus.ACTIVE == status) {
            return "ENABLED";
        }
        return status.name();
    }
}
