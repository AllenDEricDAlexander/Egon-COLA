package top.egon.cola.archetype.source.web.application.teaching.pojo.convertor;

import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.web.application.teaching.pojo.result.SchoolClassDetailResult;
import top.egon.cola.archetype.source.web.domain.teaching.entities.SchoolClass;
import top.egon.cola.archetype.source.web.domain.user.vos.UserId;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;

import java.util.List;

/** School class entity-to-result projection; member identifiers flatten to their numeric values. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
@AnnotateWith(value = Component.class, elements = @AnnotateWith.Element(strings = "schoolClassConverterImpl"))
public interface SchoolClassConverter extends BaseForwardConverter<SchoolClass, SchoolClassDetailResult> {

    @Override
    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "gradeCode", source = "gradeCode.value")
    @Mapping(target = "gradeName", source = "gradeName")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "userIds", source = "userIds")
    SchoolClassDetailResult toTarget(SchoolClass source);

    /** Callable contract kept from the previous hand-written projection. */
    default SchoolClassDetailResult toResult(SchoolClass source) {
        return toTarget(source);
    }

    default List<Long> toUserIds(List<UserId> userIds) {
        return userIds == null ? List.of() : userIds.stream().map(UserId::value).toList();
    }
}
