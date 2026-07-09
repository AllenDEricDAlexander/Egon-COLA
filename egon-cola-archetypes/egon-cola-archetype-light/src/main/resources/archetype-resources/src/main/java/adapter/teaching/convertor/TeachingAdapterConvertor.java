package ${package}.adapter.teaching.convertor;

import ${package}.adapter.teaching.vo.CourseDetailVO;
import ${package}.adapter.teaching.vo.SchoolClassDetailVO;
import ${package}.application.teaching.result.CourseResult;
import ${package}.application.teaching.result.SchoolClassResult;

public final class TeachingAdapterConvertor {
    private TeachingAdapterConvertor() {
    }

    public static CourseDetailVO toCourse(CourseResult result) {
        return new CourseDetailVO(result.id(), result.code(), result.name(), result.status());
    }

    public static SchoolClassDetailVO toSchoolClass(SchoolClassResult result) {
        return new SchoolClassDetailVO(
                result.id(), result.name(), result.semester(), result.status(), result.scheduleCount());
    }
}
