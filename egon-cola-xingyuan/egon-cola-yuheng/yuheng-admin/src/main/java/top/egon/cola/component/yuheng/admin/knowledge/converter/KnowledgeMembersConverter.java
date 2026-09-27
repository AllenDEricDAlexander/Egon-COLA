package top.egon.cola.component.yuheng.admin.knowledge.converter;

import lombok.extern.slf4j.Slf4j;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeBaseBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeMembersVO;

/**
 * 中文说明：{@code KnowledgeMembersConverter} 是 API-012/013 成员结果的单向 MapStruct 投影。
 * English summary: {@code KnowledgeMembersConverter} is the one-way MapStruct projection for API-012/013 member results.
 */
@Slf4j
@Component("knowledgeMembersConverter")
public class KnowledgeMembersConverter implements BaseForwardConverter<KnowledgeBaseBO, KnowledgeMembersVO> {

    private static final KnowledgeMembersMapping MAPPING = Mappers.getMapper(KnowledgeMembersMapping.class);

    @Override
    public KnowledgeMembersVO toTarget(KnowledgeBaseBO source) {
        if (source == null) {
            log.debug("knowledge member projection source is absent");
            return null;
        }
        return MAPPING.toTarget(source);
    }
}

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface KnowledgeMembersMapping extends BaseForwardConverter<KnowledgeBaseBO, KnowledgeMembersVO> {

    @Override
    @BeanMapping(builder = @Builder(disableBuilder = true))
    KnowledgeMembersVO toTarget(KnowledgeBaseBO source);
}
