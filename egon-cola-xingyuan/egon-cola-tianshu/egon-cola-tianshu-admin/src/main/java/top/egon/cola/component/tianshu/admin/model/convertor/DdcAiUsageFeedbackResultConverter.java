package top.egon.cola.component.tianshu.admin.model.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiUsageFeedbackBO;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiJudgeStatePersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiOutcomePersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiUsageFeedbackResult;

/** 使用样本 BO 到 API-017 视图的投影。 / Projection of a usage-sample BO into the API-017 view. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DdcAiUsageFeedbackResultConverter
        extends BaseForwardConverter<DdcAiUsageFeedbackBO, DdcAiUsageFeedbackResult> {

    @Override
    @Mapping(target = "env", source = "envCode")
    @Mapping(target = "outcome", source = "outcome", qualifiedByName = "outcomeValue")
    @Mapping(target = "judgeState", source = "judgeState", qualifiedByName = "judgeStateValue")
    @Mapping(target = "explicitScore", source = "explicitScore")
    @Mapping(target = "judgeScore", source = "judgeScore")
    @Mapping(target = "effectiveScore", expression = "java(effectiveScore(source))")
    @Mapping(target = "scoreSource", expression = "java(scoreSource(source))")
    @Mapping(target = "judgeAttempts", source = "judgeAttempts")
    @Mapping(target = "createdAt", source = "createTime")
    DdcAiUsageFeedbackResult toTarget(DdcAiUsageFeedbackBO source);

    default Integer effectiveScore(DdcAiUsageFeedbackBO source) {
        Short score = source.getExplicitScore() != null ? source.getExplicitScore() : source.getJudgeScore();
        return score == null ? null : score.intValue();
    }

    default String scoreSource(DdcAiUsageFeedbackBO source) {
        if (source.getExplicitScore() != null) return "EXPLICIT";
        return source.getJudgeScore() != null ? "JUDGE" : "NONE";
    }

    @Named("outcomeValue")
    default String outcomeValue(DdcAiOutcomePersistenceEnum value) {
        return value == null ? null : value.getValue();
    }

    @Named("judgeStateValue")
    default String judgeStateValue(DdcAiJudgeStatePersistenceEnum value) {
        return value == null ? null : value.getValue();
    }
}
