package top.egon.cola.archetype.source.lightopen.adapter.teaching.pojo.convertor;

import org.mapstruct.AnnotateWith;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.lightopen.adapter.teaching.pojo.vo.CourseDetailVO;
import top.egon.cola.archetype.source.lightopen.adapter.teaching.pojo.vo.SchoolClassDetailVO;
import top.egon.cola.archetype.source.lightopen.application.teaching.pojo.result.CourseResult;
import top.egon.cola.archetype.source.lightopen.application.teaching.pojo.result.SchoolClassResult;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;

import java.util.Objects;

/** Teaching use-case result to view projection. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
@AnnotateWith(value = Component.class, elements = @AnnotateWith.Element(strings = "teachingAdapterConvertorImpl"))
public interface TeachingAdapterConvertor extends BaseForwardConverter<CourseResult, CourseDetailVO> {

    @Override
    CourseDetailVO toTarget(CourseResult source);

    SchoolClassDetailVO toSchoolClassDetail(SchoolClassResult source);

    @BeforeMapping
    default void requireCourseResult(CourseResult result) {
        Objects.requireNonNull(result, "result");
    }

    @BeforeMapping
    default void requireSchoolClassResult(SchoolClassResult result) {
        Objects.requireNonNull(result, "result");
    }
}
