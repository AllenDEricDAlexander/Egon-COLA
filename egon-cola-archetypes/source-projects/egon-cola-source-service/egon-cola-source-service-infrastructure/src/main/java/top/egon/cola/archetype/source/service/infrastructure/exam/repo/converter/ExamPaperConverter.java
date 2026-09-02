package top.egon.cola.archetype.source.service.infrastructure.exam.repo.converter;

import top.egon.cola.archetype.source.service.domain.exam.entities.ExamPaper;
import top.egon.cola.archetype.source.service.domain.exam.enums.ExamPaperStatus;
import top.egon.cola.archetype.source.service.domain.exam.vos.ExamId;
import top.egon.cola.archetype.source.service.infrastructure.exam.repo.po.ExamPaperPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ExamPaperConverter extends BaseConverter<ExamPaper, ExamPaperPO> {

    @Override
    @Mapping(target = "examId", expression = "java(source.getExamId().value())")
    @Mapping(target = "status", expression = "java(source.getStatus().name())")
    ExamPaperPO toTarget(ExamPaper source);

    @Override
    default ExamPaper toSource(ExamPaperPO target) {
        return new ExamPaper(
                target.getId(), new ExamId(target.getExamId()), target.getTitle(),
                target.getTotalPoints(), ExamPaperStatus.valueOf(target.getStatus()));
    }
}
