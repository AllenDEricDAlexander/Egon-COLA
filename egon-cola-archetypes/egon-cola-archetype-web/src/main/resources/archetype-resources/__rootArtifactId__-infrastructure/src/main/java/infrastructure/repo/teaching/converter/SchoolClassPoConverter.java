package ${package}.infrastructure.repo.teaching.converter;

import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.infrastructure.repo.teaching.po.SchoolClassPo;

import java.time.LocalDateTime;
import java.util.List;

public final class SchoolClassPoConverter {
    private SchoolClassPoConverter() {
    }

    public static SchoolClassPo toPo(SchoolClass schoolClass) {
        return new SchoolClassPo(
                schoolClass.getId(),
                schoolClass.getName(),
                schoolClass.getGradeName(),
                LocalDateTime.now());
    }

    public static SchoolClass toEntity(SchoolClassPo schoolClassPo, List<String> userIds) {
        return SchoolClass.restore(
                schoolClassPo.getId(),
                schoolClassPo.getName(),
                schoolClassPo.getGradeName(),
                userIds);
    }
}
