package top.egon.cola.archetype.source.service.infrastructure.exam.repo.converter;

import top.egon.cola.archetype.source.service.domain.exam.entities.ExamPaper;
import top.egon.cola.archetype.source.service.domain.exam.enums.ExamPaperStatus;
import top.egon.cola.archetype.source.service.domain.exam.vos.ExamId;
import top.egon.cola.archetype.source.service.infrastructure.exam.repo.po.ExamPaperPO;
import org.mapstruct.Mapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ObjectFactory;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ExamPaperConverter extends BaseConverter<ExamPaper, ExamPaperPO> {

    @Override
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "examId", expression = "java(source.getExamId().value())")
    @Mapping(target = "status", expression = "java(source.getStatus().name())")
    ExamPaperPO toTarget(ExamPaper source);

    @Override
    @BeanMapping(ignoreByDefault = true, qualifiedByName = "restoreDomain")
    ExamPaper toSource(ExamPaperPO target);

    @ObjectFactory
    @Named("restoreDomain")
    default ExamPaper restoreDomain(ExamPaperPO target) {
        return new ExamPaper(
                target.getId(), new ExamId(target.getExamId()), target.getTitle(),
                target.getTotalPoints(), ExamPaperStatus.valueOf(target.getStatus()));
    }
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "createUserId", source = "createUserId")
    @Mapping(target = "createTime", source = "createTime")
    @Mapping(target = "version", source = "version")
    void updateMetadata(@MappingTarget ExamPaperPO target, ExamPaperPO source);
}
