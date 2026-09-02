package top.egon.cola.archetype.source.lightopen.application.teaching.assemblers;

import top.egon.cola.archetype.source.lightopen.application.teaching.result.CourseResult;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.CourseSnapshot;
import org.springframework.stereotype.Component;

@Component
public class TeachingAssembler {
    public CourseResult assemble(CourseSnapshot course) {
        return new CourseResult(
                course.id(), course.code().value(), course.name(), course.status().name());
    }
}
