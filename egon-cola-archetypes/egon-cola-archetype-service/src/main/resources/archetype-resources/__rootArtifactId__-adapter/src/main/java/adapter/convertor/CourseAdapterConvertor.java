#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.convertor;

import ${package}.application.view.course.CourseView;
import ${package}.facade.dto.course.CourseDTO;
import org.springframework.stereotype.Component;

@Component
public class CourseAdapterConvertor {

    public CourseDTO toDTO(CourseView courseView) {
        return new CourseDTO(
                courseView.id(),
                courseView.name(),
                courseView.credit(),
                toFacadeStatus(courseView.status()));
    }

    private String toFacadeStatus(String status) {
        if ("ACTIVE".equals(status)) {
            return "ENABLED";
        }
        return status;
    }
}
