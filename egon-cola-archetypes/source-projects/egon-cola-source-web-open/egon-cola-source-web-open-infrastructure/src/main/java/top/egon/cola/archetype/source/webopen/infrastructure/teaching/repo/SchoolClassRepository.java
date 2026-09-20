package top.egon.cola.archetype.source.webopen.infrastructure.teaching.repo;

import top.egon.cola.archetype.source.webopen.infrastructure.teaching.repo.dao.SchoolClassDAO;
import top.egon.cola.archetype.source.webopen.infrastructure.teaching.repo.po.SchoolClassPO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import java.util.Collection;
import java.util.List;
import java.time.Instant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Max;

/** Logical persistence boundary; query implementations remain explicit Mapper SQL. */
@Slf4j
@Validated
@Repository("schoolClassRepository")
@RequiredArgsConstructor
public class SchoolClassRepository extends EgonColaRepository<SchoolClassDAO, SchoolClassPO> {
    @Getter
    @Qualifier("schoolClassDAO")
    private final SchoolClassDAO baseMapper;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    public SchoolClassPO selectByGradeIdAndId(@Positive Long gradeId, @Positive Long id) {
        return getBaseMapper().selectByGradeIdAndId(gradeId, id);
    }

    public long countByGradeIdAndNameIgnoreCase(@Positive Long gradeId, @NotBlank String name) {
        return getBaseMapper().countByGradeIdAndNameIgnoreCase(gradeId, name);
    }
}
