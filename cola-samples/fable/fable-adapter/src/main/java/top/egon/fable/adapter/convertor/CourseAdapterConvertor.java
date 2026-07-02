package top.egon.fable.adapter.convertor;

import top.egon.fable.application.view.course.CourseView;
import top.egon.fable.facade.dto.course.CourseDTO;
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
