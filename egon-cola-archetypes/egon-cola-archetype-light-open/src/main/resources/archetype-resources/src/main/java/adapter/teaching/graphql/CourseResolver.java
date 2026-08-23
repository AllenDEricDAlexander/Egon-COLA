package ${package}.adapter.teaching.graphql;

import ${package}.adapter.teaching.convertor.TeachingAdapterConvertor;
import ${package}.adapter.teaching.vo.CourseDetailVO;
import ${package}.adapter.teaching.vo.SchoolClassDetailVO;
import ${package}.application.teaching.manage.CourseManage;
import ${package}.application.teaching.manage.SchoolClassManage;
import ${package}.application.teaching.query.GetCourseQuery;
import ${package}.application.teaching.query.GetSchoolClassQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class CourseResolver {
    private final CourseManage courseManage;
    private final SchoolClassManage schoolClassManage;
    private final TeachingAdapterConvertor convertor;

    @QueryMapping
    public CourseDetailVO course(@Argument String id) {
        return convertor.toCourse(courseManage.get(new GetCourseQuery(id)));
    }

    @QueryMapping
    public SchoolClassDetailVO schoolClass(@Argument String id) {
        return convertor.toSchoolClass(
                schoolClassManage.get(new GetSchoolClassQuery(id)));
    }
}
