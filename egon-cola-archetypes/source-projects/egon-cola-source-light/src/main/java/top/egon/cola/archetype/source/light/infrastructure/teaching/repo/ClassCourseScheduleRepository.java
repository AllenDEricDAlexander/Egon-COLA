package top.egon.cola.archetype.source.light.infrastructure.teaching.repo;

import top.egon.cola.archetype.source.light.infrastructure.teaching.repo.dao.ClassCourseScheduleDAO;
import top.egon.cola.archetype.source.light.infrastructure.teaching.repo.po.ClassCourseSchedulePO;
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
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;
import java.util.Collection;
import java.util.List;

/** Logical persistence boundary; query implementations remain explicit Mapper SQL. */
@Slf4j
@Validated
@Repository("classCourseScheduleRepository")
@RequiredArgsConstructor
public class ClassCourseScheduleRepository extends EgonColaRepository<ClassCourseScheduleDAO, ClassCourseSchedulePO> {
    @Getter
    @Qualifier("classCourseScheduleDAO")
    private final ClassCourseScheduleDAO baseMapper;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egonColaModelValidationUtils")
    private final EgonColaModelValidationUtils modelValidationUtils;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egonColaMdcTenantIdProvider")
    private final EgonColaTenantIdProvider tenantIdProvider;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    public List<ClassCourseSchedulePO> selectBySchoolClassIdOrderByStartsAt(@NotNull @Positive Long schoolClassId) {
        return getBaseMapper().selectBySchoolClassIdOrderByStartsAt(schoolClassId);
    }
}
