package top.egon.cola.archetype.source.light.adapter.teaching.graphql;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import top.egon.cola.archetype.source.light.adapter.teaching.pojo.convertor.TeachingAdapterConvertor;
import top.egon.cola.archetype.source.light.adapter.teaching.pojo.vo.CourseDetailVO;
import top.egon.cola.archetype.source.light.adapter.teaching.pojo.vo.SchoolClassDetailVO;
import top.egon.cola.archetype.source.light.application.teaching.manage.CourseManage;
import top.egon.cola.archetype.source.light.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.light.application.teaching.pojo.query.GetCourseQuery;
import top.egon.cola.archetype.source.light.application.teaching.pojo.query.GetSchoolClassQuery;
import top.egon.cola.platform.tianquan.jianshen.starter.security.RBACAPIResource;

@Controller("courseResolver")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class CourseResolver {
    @Qualifier("courseManageImpl")
    private final CourseManage courseManage;
    @Qualifier("schoolClassManageImpl")
    private final SchoolClassManage schoolClassManage;
    @Qualifier("teachingAdapterConvertorImpl")
    private final TeachingAdapterConvertor convertor;

    @QueryMapping
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-light:course:read')")
    @RBACAPIResource(code = "egon-cola-source-light.graphql.query.course", permission = "egon-cola-source-light:course:read", name = "Read course")
    public CourseDetailVO course(@Argument String id) {
        return convertor.toTarget(courseManage.get(new GetCourseQuery(Long.valueOf(id))));
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-light:school-class:read')")
    @RBACAPIResource(code = "egon-cola-source-light.graphql.query.schoolClass", permission = "egon-cola-source-light:school-class:read", name = "Read school class")
    public SchoolClassDetailVO schoolClass(@Argument String id) {
        return convertor.toSchoolClassDetail(
                schoolClassManage.get(new GetSchoolClassQuery(Long.valueOf(id))));
    }
}
