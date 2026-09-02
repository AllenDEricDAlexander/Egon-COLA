package top.egon.cola.archetype.source.lightopen.adapter.teaching.convertor;

import top.egon.cola.archetype.source.lightopen.adapter.teaching.vo.CourseDetailVO;
import top.egon.cola.archetype.source.lightopen.adapter.teaching.vo.SchoolClassDetailVO;
import top.egon.cola.archetype.source.lightopen.application.teaching.result.CourseResult;
import top.egon.cola.archetype.source.lightopen.application.teaching.result.SchoolClassResult;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.Objects;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TeachingAdapterConvertor {

    CourseDetailVO toCourse(CourseResult result);

    SchoolClassDetailVO toSchoolClass(SchoolClassResult result);

    @BeforeMapping
    default void requireCourseResult(CourseResult result) {
        Objects.requireNonNull(result, "result");
    }

    @BeforeMapping
    default void requireSchoolClassResult(SchoolClassResult result) {
        Objects.requireNonNull(result, "result");
    }
}
