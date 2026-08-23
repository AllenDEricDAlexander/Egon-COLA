package ${package}.adapter.teaching.convertor;

import ${package}.adapter.teaching.vo.CourseDetailVO;
import ${package}.adapter.teaching.vo.SchoolClassDetailVO;
import ${package}.application.teaching.result.CourseResult;
import ${package}.application.teaching.result.SchoolClassResult;
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
