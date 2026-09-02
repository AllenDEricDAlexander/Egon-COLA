package top.egon.cola.archetype.source.lightopen.adapter.teaching.graphql;

import top.egon.cola.archetype.source.lightopen.adapter.teaching.convertor.TeachingAdapterConvertor;
import top.egon.cola.archetype.source.lightopen.adapter.teaching.vo.CourseDetailVO;
import top.egon.cola.archetype.source.lightopen.adapter.teaching.vo.SchoolClassDetailVO;
import top.egon.cola.archetype.source.lightopen.application.teaching.manage.CourseManage;
import top.egon.cola.archetype.source.lightopen.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.lightopen.application.teaching.query.GetCourseQuery;
import top.egon.cola.archetype.source.lightopen.application.teaching.query.GetSchoolClassQuery;
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
        return convertor.toCourse(courseManage.get(new GetCourseQuery(Long.valueOf(id))));
    }

    @QueryMapping
    public SchoolClassDetailVO schoolClass(@Argument String id) {
        return convertor.toSchoolClass(
                schoolClassManage.get(new GetSchoolClassQuery(Long.valueOf(id))));
    }
}
