package ${package}.adapter.teaching.convertor;

import ${package}.adapter.teaching.vo.CourseDetailVO;
import ${package}.adapter.teaching.vo.SchoolClassDetailVO;
import ${package}.application.teaching.result.CourseResult;
import ${package}.application.teaching.result.SchoolClassResult;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TeachingAdapterConvertor {

    CourseDetailVO toCourse(CourseResult result);

    SchoolClassDetailVO toSchoolClass(SchoolClassResult result);
}
