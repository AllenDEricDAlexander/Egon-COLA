package top.egon.cola.archetype.source.light.application.teaching.assemblers;

import top.egon.cola.archetype.source.light.application.teaching.result.CourseResult;
import top.egon.cola.archetype.source.light.domain.teaching.vos.CourseSnapshot;
import org.springframework.stereotype.Component;

@Component
public class TeachingAssembler {
    public CourseResult assemble(CourseSnapshot course) {
        return new CourseResult(
                course.id(), course.code().value(), course.name(), course.status().name());
    }
}
