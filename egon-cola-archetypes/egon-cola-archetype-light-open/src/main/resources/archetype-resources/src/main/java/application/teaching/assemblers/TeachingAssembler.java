package ${package}.application.teaching.assemblers;

import ${package}.application.teaching.result.CourseResult;
import ${package}.domain.teaching.vos.CourseSnapshot;
import org.springframework.stereotype.Component;

@Component
public class TeachingAssembler {
    public CourseResult assemble(CourseSnapshot course) {
        return new CourseResult(
                course.id(), course.code().value(), course.name(), course.status().name());
    }
}
