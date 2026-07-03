package top.egon.fable.web.infrastructure.repo.teaching.converter;

import top.egon.fable.web.domain.entities.teaching.SchoolClass;
import top.egon.fable.web.infrastructure.repo.teaching.po.SchoolClassPo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component("schoolClassPoConverter")
@RequiredArgsConstructor
public class SchoolClassPoConverter {
    @Qualifier("schoolClassPoMapperImpl")
    private final SchoolClassPoMapper schoolClassPoMapper;

    @Qualifier("schoolClassDomainMapperImpl")
    private final SchoolClassDomainMapper schoolClassDomainMapper;

    public SchoolClassPo toPo(SchoolClass schoolClass) {
        SchoolClassPo schoolClassPo = schoolClassPoMapper.convert(schoolClass);
        return new SchoolClassPo(
                schoolClassPo.getId(),
                schoolClassPo.getName(),
                schoolClassPo.getGradeName(),
                LocalDateTime.now());
    }

    public SchoolClass toEntity(SchoolClassPo schoolClassPo, List<String> userIds) {
        SchoolClass schoolClass = schoolClassDomainMapper.convert(schoolClassPo);
        return SchoolClass.restore(schoolClass.getId(), schoolClass.getName(), schoolClass.getGradeName(), userIds);
    }
}
