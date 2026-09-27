package top.egon.cola.component.yuheng.admin.knowledge.converter;

import lombok.extern.slf4j.Slf4j;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeUploadReceiptBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeUploadReceiptVO;

/**
 * 中文说明：{@code KnowledgeUploadReceiptConverter} 是 API-015 受理结果的单向 MapStruct 投影。
 * English summary: {@code KnowledgeUploadReceiptConverter} is the one-way MapStruct projection for the API-015 receipt.
 */
@Slf4j
@Component("knowledgeUploadReceiptConverter")
public class KnowledgeUploadReceiptConverter
        implements BaseForwardConverter<KnowledgeUploadReceiptBO, KnowledgeUploadReceiptVO> {

    private static final KnowledgeUploadReceiptMapping MAPPING = Mappers.getMapper(KnowledgeUploadReceiptMapping.class);

    @Override
    public KnowledgeUploadReceiptVO toTarget(KnowledgeUploadReceiptBO source) {
        if (source == null) {
            log.debug("knowledge upload receipt source is absent");
            return null;
        }
        return MAPPING.toTarget(source);
    }
}

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface KnowledgeUploadReceiptMapping
        extends BaseForwardConverter<KnowledgeUploadReceiptBO, KnowledgeUploadReceiptVO> {

    @Override
    @BeanMapping(builder = @Builder(disableBuilder = true))
    KnowledgeUploadReceiptVO toTarget(KnowledgeUploadReceiptBO source);
}
